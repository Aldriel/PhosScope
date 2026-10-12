/*
 * Copyright 2026 Maxime Ethier - Consultant en Bioinformatique/Biocomputing Consultant
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bioinfo.phosscope.parse;

import com.bioinfo.phosscope.model.PhosphoSite;
import com.bioinfo.phosscope.model.QuantTable;
import com.bioinfo.phosscope.model.TableFormat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Reads a MaxQuant, FragPipe or generic quantitative table.
 *
 * <p>MaxQuant files are often UTF-8 or UTF-16 with a byte-order mark, and sometimes
 * gzip-compressed. A cell that is empty, {@code NaN}, zero or negative is a missing intensity: the
 * site was not detected. The plain {@code Intensity} column of MaxQuant is the sum of the samples
 * and is not itself a sample.
 */
public final class SiteTableParser {

    /** Largest uncompressed table accepted, in characters. */
    public static final int MAX_CHARACTERS = 80 * 1024 * 1024;

    private static final Set<String> METADATA =
            Set.of(
                    "protein",
                    "proteins",
                    "protein id",
                    "protein ids",
                    "gene",
                    "gene name",
                    "gene names",
                    "genes",
                    "site",
                    "position",
                    "positions",
                    "positions within proteins",
                    "amino acid",
                    "residue",
                    "peptide",
                    "modified peptide",
                    "protein names",
                    "protein name",
                    "description");

    private SiteTableParser() {}

    /**
     * Reads a table from the bytes of a file.
     *
     * @param content file bytes, possibly gzip-compressed
     * @param fileName name used in the report; a blank name becomes {@code upload.txt}
     * @return the samples and the site rows
     * @throws ParseException when the file is empty, not text, too large, or has no intensities
     */
    public static QuantTable parse(byte[] content, String fileName) throws ParseException {
        return parse(content, fileName, MAX_CHARACTERS);
    }

    /**
     * Same as {@link #parse(byte[], String)} with an explicit size limit, so tests can reject a
     * small table without allocating the production limit.
     *
     * @param content file bytes
     * @param fileName report name
     * @param maxCharacters largest accepted text, in characters
     * @return the table
     * @throws ParseException when the file cannot be read
     */
    static QuantTable parse(byte[] content, String fileName, int maxCharacters)
            throws ParseException {
        String text = decode(content, maxCharacters);
        List<String> lines = meaningfulLines(text);
        if (lines.isEmpty()) {
            throw new ParseException(ParseException.EMPTY, "The file has no table header.");
        }
        char separator = lines.get(0).indexOf('\t') >= 0 ? '\t' : ',';
        List<String> headers = split(lines.get(0), separator);
        Map<String, Integer> index = headerIndex(headers);
        TableFormat format = detectFormat(index, headers);
        List<Integer> sampleColumns = sampleColumns(format, headers, lines, separator);
        if (sampleColumns.size() < 2) {
            throw new ParseException(
                    ParseException.NO_COLUMNS, "At least two intensity columns are required.");
        }
        List<String> sampleNames = displayNames(headers, sampleColumns);
        List<PhosphoSite> sites = new ArrayList<>();
        for (int line = 1; line < lines.size(); line++) {
            List<String> cells = split(lines.get(line), separator);
            if (isBlankRow(cells)) {
                continue;
            }
            sites.add(site(index, cells, sampleColumns, separator));
        }
        if (sites.isEmpty()) {
            throw new ParseException(
                    ParseException.EMPTY, "The file has a header but no data row.");
        }
        return new QuantTable(format, fileName, sampleNames, sites);
    }

    private static String decode(byte[] content, int maxCharacters) throws ParseException {
        if (content == null || content.length == 0) {
            throw new ParseException(ParseException.EMPTY, "The file is empty.");
        }
        byte[] raw = gunzip(content, maxCharacters);
        Charset charset = charset(raw);
        int offset = bomOffset(raw);
        String text = new String(raw, offset, raw.length - offset, charset);
        if (text.length() > maxCharacters) {
            throw new ParseException(
                    ParseException.TOO_LARGE, "The table is larger than the limit.");
        }
        if (!looksLikeText(text)) {
            throw new ParseException(ParseException.NOT_TEXT, "The file is not a text table.");
        }
        return text;
    }

    private static byte[] gunzip(byte[] content, int maxCharacters) throws ParseException {
        if (content.length < 2 || content[0] != 0x1f || content[1] != (byte) 0x8b) {
            return content;
        }
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(content))) {
            byte[] decoded = in.readNBytes(maxCharacters + 1);
            if (decoded.length > maxCharacters || in.read() >= 0) {
                throw new ParseException(
                        ParseException.TOO_LARGE, "The archive is larger than the limit.");
            }
            return decoded;
        } catch (ParseException e) {
            throw e;
        } catch (IOException e) {
            throw new ParseException(ParseException.NOT_TEXT, "The gzip archive cannot be read.");
        }
    }

    private static Charset charset(byte[] raw) {
        if (raw.length >= 2 && raw[0] == (byte) 0xff && raw[1] == (byte) 0xfe) {
            return StandardCharsets.UTF_16LE;
        }
        if (raw.length >= 2 && raw[0] == (byte) 0xfe && raw[1] == (byte) 0xff) {
            return StandardCharsets.UTF_16BE;
        }
        return StandardCharsets.UTF_8;
    }

    private static int bomOffset(byte[] raw) {
        if (raw.length >= 3
                && raw[0] == (byte) 0xef
                && raw[1] == (byte) 0xbb
                && raw[2] == (byte) 0xbf) {
            return 3;
        }
        if (raw.length >= 2 && (raw[0] == (byte) 0xff || raw[0] == (byte) 0xfe)) {
            return 2;
        }
        return 0;
    }

    private static boolean looksLikeText(String text) {
        int checked = Math.min(text.length(), 4000);
        int odd = 0;
        for (int i = 0; i < checked; i++) {
            char c = text.charAt(i);
            if (c != '\t' && c != '\n' && c != '\r' && Character.isISOControl(c)) {
                odd++;
            }
        }
        return odd * 10 < checked;
    }

    private static List<String> meaningfulLines(String text) {
        String[] rows = text.split("\\R", -1);
        List<String> lines = new ArrayList<>();
        for (String row : rows) {
            String trimmed = row.trim();
            if (trimmed.isEmpty() || trimmed.charAt(0) == '#') {
                continue;
            }
            lines.add(row);
        }
        return lines;
    }

    private static Map<String, Integer> headerIndex(List<String> headers) {
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            index.putIfAbsent(headers.get(i).trim().toLowerCase(Locale.ROOT), i);
        }
        return index;
    }

    private static TableFormat detectFormat(Map<String, Integer> index, List<String> headers) {
        if (index.containsKey("potential contaminant")
                || index.containsKey("localization prob")
                || index.containsKey("positions within proteins")) {
            return TableFormat.MAXQUANT;
        }
        for (String header : headers) {
            if (header.toLowerCase(Locale.ROOT).endsWith(" intensity")) {
                return TableFormat.FRAGPIPE;
            }
        }
        return TableFormat.GENERIC;
    }

    private static List<Integer> sampleColumns(
            TableFormat format, List<String> headers, List<String> lines, char separator) {
        if (format == TableFormat.MAXQUANT) {
            List<Integer> intensity = columnsWithPrefix(headers, "intensity ");
            if (!intensity.isEmpty()) {
                return intensity;
            }
            return columnsWithPrefix(headers, "lfq intensity ");
        }
        if (format == TableFormat.FRAGPIPE) {
            List<Integer> maxLfq = columnsWithSuffix(headers, " maxlfq intensity");
            if (!maxLfq.isEmpty()) {
                return maxLfq;
            }
            return columnsWithSuffix(headers, " intensity");
        }
        return numericColumns(headers, lines, separator);
    }

    private static List<Integer> columnsWithPrefix(List<String> headers, String prefix) {
        List<Integer> columns = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).trim().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                columns.add(i);
            }
        }
        return columns;
    }

    private static List<Integer> columnsWithSuffix(List<String> headers, String suffix) {
        List<Integer> columns = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            String name = headers.get(i).trim().toLowerCase(Locale.ROOT);
            if (name.endsWith(suffix) && !name.equals("intensity")) {
                columns.add(i);
            }
        }
        return columns;
    }

    private static List<Integer> numericColumns(
            List<String> headers, List<String> lines, char separator) {
        List<Integer> columns = new ArrayList<>();
        for (int column = 0; column < headers.size(); column++) {
            if (METADATA.contains(headers.get(column).trim().toLowerCase(Locale.ROOT))) {
                continue;
            }
            int numeric = 0;
            int filled = 0;
            for (int line = 1; line < lines.size(); line++) {
                List<String> cells = split(lines.get(line), separator);
                if (column >= cells.size()) {
                    continue;
                }
                String cell = cells.get(column).trim();
                if (cell.isEmpty()) {
                    continue;
                }
                filled++;
                if (parseNumber(cell, separator) != null) {
                    numeric++;
                }
            }
            if (numeric >= 1 && numeric * 2 >= filled) {
                columns.add(column);
            }
        }
        return columns;
    }

    private static List<String> displayNames(List<String> headers, List<Integer> sampleColumns) {
        List<String> names = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (int column : sampleColumns) {
            String base = displayName(headers.get(column));
            String name = base.isEmpty() ? "sample" : base;
            int suffix = 2;
            while (!used.add(name)) {
                name = base + " (" + suffix + ")";
                suffix++;
            }
            names.add(name);
        }
        return names;
    }

    static String displayName(String header) {
        String name = header == null ? "" : header.trim();
        String lower = name.toLowerCase(Locale.ROOT);
        String[] prefixes = {"lfq intensity ", "intensity "};
        for (String prefix : prefixes) {
            if (lower.startsWith(prefix)) {
                return name.substring(prefix.length()).trim();
            }
        }
        String[] suffixes = {" maxlfq intensity", " intensity"};
        for (String suffix : suffixes) {
            if (lower.endsWith(suffix)) {
                return name.substring(0, name.length() - suffix.length()).trim();
            }
        }
        return name;
    }

    private static PhosphoSite site(
            Map<String, Integer> index,
            List<String> cells,
            List<Integer> sampleColumns,
            char separator) {
        String protein =
                firstText(cells, index, "proteins", "protein", "protein id", "protein ids");
        String gene = firstText(cells, index, "gene names", "gene name", "gene", "genes");
        String proteinName =
                firstText(cells, index, "protein names", "protein name", "description");
        String position =
                firstText(
                        cells, index, "positions within proteins", "position", "positions", "site");
        String aminoAcid = firstText(cells, index, "amino acid", "residue");
        if (aminoAcid.isEmpty()) {
            aminoAcid = residueFromSite(position);
        }
        int multiplicity = multiplicity(cells, index, position, separator);
        double localization =
                firstNumber(
                        cells,
                        index,
                        separator,
                        "localization prob",
                        "localization probability",
                        "best localization probability",
                        "ascore");
        boolean reverse = flagged(cells, index, "reverse") || protein.startsWith("REV__");
        boolean contaminant =
                flagged(cells, index, "potential contaminant", "contaminant")
                        || protein.startsWith("CON__");
        boolean onlySite = flagged(cells, index, "only identified by site");
        double[] intensities = new double[sampleColumns.size()];
        for (int i = 0; i < sampleColumns.size(); i++) {
            intensities[i] = intensity(cell(cells, sampleColumns.get(i)), separator);
        }
        return new PhosphoSite(
                protein,
                gene,
                proteinName,
                position,
                aminoAcid,
                multiplicity,
                localization,
                reverse,
                contaminant,
                onlySite,
                intensities);
    }

    private static String residueFromSite(String position) {
        if (position.length() >= 2 && Character.isLetter(position.charAt(0))) {
            return position.substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return "";
    }

    private static int multiplicity(
            List<String> cells, Map<String, Integer> index, String position, char separator) {
        Double value =
                firstNumberBox(cells, index, separator, "number of phospho (sty)", "multiplicity");
        if (value != null && value >= 1) {
            return value.intValue();
        }
        int semis = 0;
        for (int i = 0; i < position.length(); i++) {
            if (position.charAt(i) == ';') {
                semis++;
            }
        }
        return semis > 0 ? semis + 1 : 1;
    }

    private static boolean flagged(
            List<String> cells, Map<String, Integer> index, String... names) {
        for (String name : names) {
            Integer column = index.get(name);
            if (column == null) {
                continue;
            }
            String value = cell(cells, column).trim().toLowerCase(Locale.ROOT);
            if (value.equals("+")
                    || value.equals("true")
                    || value.equals("yes")
                    || value.equals("1")) {
                return true;
            }
        }
        return false;
    }

    private static String firstText(
            List<String> cells, Map<String, Integer> index, String... names) {
        for (String name : names) {
            Integer column = index.get(name);
            if (column != null && !cell(cells, column).isBlank()) {
                return cell(cells, column).trim();
            }
        }
        return "";
    }

    private static double firstNumber(
            List<String> cells, Map<String, Integer> index, char separator, String... names) {
        Double value = firstNumberBox(cells, index, separator, names);
        return value == null ? Double.NaN : value;
    }

    private static Double firstNumberBox(
            List<String> cells, Map<String, Integer> index, char separator, String... names) {
        for (String name : names) {
            Integer column = index.get(name);
            if (column == null) {
                continue;
            }
            Double value = parseNumber(cell(cells, column), separator);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static double intensity(String raw, char separator) {
        Double value = parseNumber(raw, separator);
        if (value == null || value <= 0) {
            return Double.NaN;
        }
        return value;
    }

    /**
     * Parses a number. A comma is a decimal separator when the file itself is tab-separated, which
     * is how some European MaxQuant exports are written. The separator argument selects that case.
     */
    private static Double parseNumber(String raw, char separator) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.equals("nan")
                || lower.equals("na")
                || lower.equals("n/a")
                || lower.equals("filtered")) {
            return null;
        }
        if (separator == '\t' && text.indexOf(',') >= 0 && text.indexOf('.') < 0) {
            text = text.replace(',', '.');
        }
        try {
            return Double.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String cell(List<String> cells, int column) {
        if (column < 0 || column >= cells.size()) {
            return "";
        }
        return cells.get(column);
    }

    private static boolean isBlankRow(List<String> cells) {
        for (String cell : cells) {
            if (!cell.isBlank()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Splits a delimited line and honours double quotes, including a quote escaped as {@code ""}.
     */
    static List<String> split(String line, char separator) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == separator) {
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cells.add(current.toString());
        return cells;
    }
}

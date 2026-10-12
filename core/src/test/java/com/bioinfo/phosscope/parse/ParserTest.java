package com.bioinfo.phosscope.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.phosscope.model.Group;
import com.bioinfo.phosscope.model.QuantTable;
import com.bioinfo.phosscope.model.TableFormat;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

/** MaxQuant, FragPipe and generic tables, including the encodings a real export uses. */
class ParserTest {

    @Test
    void maxQuantIntensityColumnsSkipTheTotalAndTreatZeroAsMissing() {
        String text =
                "Proteins\tAmino acid\tPositions within proteins\tGene names\tLocalization prob"
                    + "\tReverse\tPotential contaminant\tOnly identified by site\tNumber of Phospho"
                    + " (STY)\tIntensity\tIntensity Control_1\tIntensity Disease_1\n"
                    + "P1\tS\t15\tTP53\t0,99\t\t\t\t1\t10\t8\t4\n"
                    + "\t\t\t\t\t\t\t\t\t\t\t\n"
                    + "CON__P2\tT\t1\tALB\t0.9\t\t\t\t1\t1\t0\t3\n"
                    + "REV__P3\tY\t2\tX\t0.8\t\t\t\t2\t1\t5\tNaN\n";
        QuantTable table = parse(text);
        assertEquals(TableFormat.MAXQUANT, table.format());
        assertEquals(listOf("Control_1", "Disease_1"), table.sampleNames());
        assertEquals(3, table.sites().size());
        assertEquals(8, table.sites().get(0).intensities()[0], 0);
        assertEquals(0.99, table.sites().get(0).localization(), 1e-9);
        assertTrue(Double.isNaN(table.sites().get(1).intensities()[0]));
        assertTrue(table.sites().get(1).contaminant());
        assertTrue(table.sites().get(2).reverse());
        assertEquals(2, table.sites().get(2).multiplicity());
        assertTrue(Double.isNaN(table.sites().get(2).intensities()[1]));
    }

    @Test
    void encodingsCommentsGzipAndQuotes() throws Exception {
        String text = "\uFEFF# comment\nProtein\tIntensity A_1\tIntensity B_1\nP\t1\t2\n";
        assertEquals("P", parse(text).sites().get(0).protein());
        String plain = "# comment\nProtein\tIntensity A_1\tIntensity B_1\nP\t1\t2\n";
        byte[] utf16 = plain.getBytes(StandardCharsets.UTF_16LE);
        byte[] withBom = new byte[utf16.length + 2];
        withBom[0] = (byte) 0xff;
        withBom[1] = (byte) 0xfe;
        System.arraycopy(utf16, 0, withBom, 2, utf16.length);
        assertEquals(1, SiteTableParser.parse(withBom, "u.txt").sites().size());
        ByteArrayOutputStream gzip = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(gzip)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(
                TableFormat.GENERIC, SiteTableParser.parse(gzip.toByteArray(), "g.txt").format());
        String quoted =
                "Protein,Site,\"Control, one\" Intensity,\"Disease\" Intensity\n"
                        + "\"A, B\",S473,1.5,2\n";
        QuantTable generic =
                SiteTableParser.parse(quoted.getBytes(StandardCharsets.UTF_8), "q.csv");
        assertEquals(TableFormat.FRAGPIPE, generic.format());
        assertEquals("A, B", generic.sites().get(0).protein());
        assertEquals("S", generic.sites().get(0).aminoAcid());
        assertEquals("S473", generic.sites().get(0).siteLabel());
        assertEquals(1, generic.sites().get(0).multiplicity());
    }

    @Test
    void fragPipePrefersMaxLfqAndGenericCsvWorks() {
        String frag =
                "Protein ID\tGene\tSite\tControl MaxLFQ Intensity\tDisease MaxLFQ Intensity\t"
                        + "Control Intensity\tDisease Intensity\n"
                        + "P9\tAKT1\tS473;T308\t10\t40\t1\t2\n";
        QuantTable table = parse(frag);
        assertEquals(TableFormat.FRAGPIPE, table.format());
        assertEquals(listOf("Control", "Disease"), table.sampleNames());
        assertEquals(10, table.sites().get(0).intensities()[0], 0);
        assertEquals(2, table.sites().get(0).multiplicity());
        String csv = "Site,Alpha,Beta\nS1,1,2\nS2,3,4\n";
        QuantTable generic = parse(csv);
        assertEquals(TableFormat.GENERIC, generic.format());
        assertEquals(listOf("Alpha", "Beta"), generic.sampleNames());
        assertEquals("S1", generic.sites().get(0).position());
    }

    @Test
    void lfqFallbackDuplicateNamesAndFlags() {
        String lfq =
                "Proteins\tPositions within proteins\tLocalization prob\tOnly identified by site"
                        + "\tLFQ intensity Control_1\tLFQ intensity Control_1\tLFQ intensity"
                        + " Disease_1\n"
                        + "P\tS15\t0.2\tyes\t4\t5\t6\n";
        QuantTable table = parse(lfq);
        assertEquals(listOf("Control_1", "Control_1 (2)", "Disease_1"), table.sampleNames());
        assertTrue(table.sites().get(0).onlyIdentifiedBySite());
        assertEquals("S", table.sites().get(0).aminoAcid());
    }

    @Test
    void rejections() {
        ParseException empty =
                assertThrows(ParseException.class, () -> SiteTableParser.parse(null, "a"));
        assertEquals(ParseException.EMPTY, empty.code());
        assertThrows(ParseException.class, () -> SiteTableParser.parse(new byte[0], "a"));
        ParseException columns =
                assertThrows(
                        ParseException.class,
                        () ->
                                SiteTableParser.parse(
                                        "Proteins\tLocalization prob\nP\t0.9\n"
                                                .getBytes(StandardCharsets.UTF_8),
                                        "c"));
        assertEquals(ParseException.NO_COLUMNS, columns.code());
        ParseException headerOnly =
                assertThrows(
                        ParseException.class,
                        () ->
                                SiteTableParser.parse(
                                        ("Proteins\tPotential contaminant\tIntensity A\t"
                                                        + "Intensity B\n")
                                                .getBytes(StandardCharsets.UTF_8),
                                        "h"));
        assertEquals(ParseException.EMPTY, headerOnly.code());
        ParseException binary =
                assertThrows(
                        ParseException.class,
                        () -> SiteTableParser.parse(new byte[] {1, 1, 1, 1, 1}, "b"));
        assertEquals(ParseException.NOT_TEXT, binary.code());
        byte[] broken = {(byte) 0x1f, (byte) 0x8b, 0, 0, 0};
        ParseException gzip =
                assertThrows(ParseException.class, () -> SiteTableParser.parse(broken, "z"));
        assertEquals(ParseException.NOT_TEXT, gzip.code());
        ParseException large =
                assertThrows(
                        ParseException.class,
                        () ->
                                SiteTableParser.parse(
                                        "Protein\tA\tB\nP\t1\t2\n".getBytes(StandardCharsets.UTF_8),
                                        "t",
                                        8));
        assertEquals(ParseException.TOO_LARGE, large.code());
        byte[] archive = gzip("Protein\tIntensity A\tIntensity B\nP\t1\t2\n");
        ParseException archiveTooBig =
                assertThrows(ParseException.class, () -> SiteTableParser.parse(archive, "z", 4));
        assertEquals(ParseException.TOO_LARGE, archiveTooBig.code());
        assertEquals(
                "upload.txt", parse("Protein\tIntensity A\tIntensity B\nP\t1\t2\n").fileName());
    }

    @Test
    void groupWords() {
        assertEquals(Group.CONTROL, GroupGuesser.guess("Témoin-1"));
        assertEquals(Group.CONTROL, GroupGuesser.guess("wt"));
        assertEquals(Group.DISEASE, GroupGuesser.guess("Disease_vs_control"));
        assertEquals(Group.UNASSIGNED, GroupGuesser.guess("normalisation"));
        assertEquals(Group.UNASSIGNED, GroupGuesser.guess(null));
        assertEquals(Group.UNASSIGNED, GroupGuesser.guess("  "));
        assertEquals(Group.DISEASE, GroupGuesser.guess("patient3"));
        assertEquals("Control", SiteTableParser.displayName("Intensity Control"));
        assertEquals("Tumor", SiteTableParser.displayName("Tumor MaxLFQ Intensity"));
        assertEquals("plain", SiteTableParser.displayName("plain"));
        assertEquals("", SiteTableParser.displayName(null));
        assertEquals(listOf("a", "b, c"), SiteTableParser.split("a,\"b, c\"", ','));
        assertEquals(listOf("a\"b"), SiteTableParser.split("\"a\"\"b\"", ','));
    }

    @Test
    void moreEncodingsFlagsAndRejectedNumbers() throws Exception {
        String plain = "Protein\tIntensity A\tIntensity B\nP\t1\t2\n";
        byte[] be = plain.getBytes(StandardCharsets.UTF_16BE);
        byte[] withBom = new byte[be.length + 2];
        withBom[0] = (byte) 0xfe;
        withBom[1] = (byte) 0xff;
        System.arraycopy(be, 0, withBom, 2, be.length);
        assertEquals(1, SiteTableParser.parse(withBom, "be.txt").sites().size());
        String flagged =
                "Proteins\tPotential contaminant\tReverse\tOnly identified by site\t"
                        + "Number of Phospho (STY)\tPositions within proteins\t"
                        + "Intensity A\tIntensity B\n"
                        + "P\t1\ttrue\t+\t0\tS15;T16\tfiltered\tn/a\n"
                        + " \t \t \t \t \t \t \t \n";
        QuantTable table = SiteTableParser.parse(flagged.getBytes(StandardCharsets.UTF_8), "f.txt");
        assertEquals("A", table.sampleNames().get(0));
        QuantTable blankHeader = parse(",Alpha,Beta\n1,2,3\n4,5,6\n");
        assertEquals("sample", blankHeader.sampleNames().get(0));
        assertEquals(1, table.sites().size());
        assertTrue(table.sites().get(0).contaminant());
        assertTrue(table.sites().get(0).reverse());
        assertEquals(2, table.sites().get(0).multiplicity());
        assertTrue(Double.isNaN(table.sites().get(0).intensities()[0]));
        String shortRow = "Site,Alpha,Beta,Gamma\nS1,1,2\nS2,word,4,5\n";
        QuantTable generic = parse(shortRow);
        assertTrue(generic.sampleNames().size() >= 2);
        assertEquals(2, generic.sites().size());
    }

    private static QuantTable parse(String text) {
        try {
            return SiteTableParser.parse(text.getBytes(StandardCharsets.UTF_8), "  ");
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    private static java.util.List<String> listOf(String... values) {
        return java.util.List.of(values);
    }

    private static byte[] gzip(String text) {
        try {
            ByteArrayOutputStream gzip = new ByteArrayOutputStream();
            try (GZIPOutputStream out = new GZIPOutputStream(gzip)) {
                out.write(text.getBytes(StandardCharsets.UTF_8));
            }
            return gzip.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

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

package com.bioinfo.phosscope.example;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;

/**
 * Simulated MaxQuant phosphosite table of a control group and a disease group.
 *
 * <p>The intensities are invented by PhosScope (seed 7). A few sites have a large planted fold
 * change, others do not, and the file also contains the rows a real export asks the cleaner to
 * drop: a contaminant, a reversed hit, a site identified only by the modification, a poorly
 * localized site and a site too incomplete to test. Nothing here comes from a patient.
 */
public final class SimulatedCohort {

    /** Identifier used by the desktop menu and the web API. */
    public static final String ID = "simulated-tumor";

    /** File name shown as if the user had opened a MaxQuant export. */
    public static final String FILE_NAME = "Phospho (STY)Sites.txt";

    private static final String HEADER =
            "Proteins\tPositions within proteins\tAmino acid\tNumber of Phospho (STY)\t"
                    + "Protein names\tGene names\tLocalization prob\tReverse\t"
                    + "Potential contaminant\tOnly identified by site\tIntensity\t"
                    + "Intensity Control_1\tIntensity Control_2\tIntensity Control_3\t"
                    + "Intensity Disease_1\tIntensity Disease_2\tIntensity Disease_3";

    private SimulatedCohort() {}

    /**
     * Tab-separated text of the example.
     *
     * @return the MaxQuant-like table
     */
    public static String text() {
        Random random = new Random(7);
        StringBuilder out = new StringBuilder(HEADER).append('\n');
        add(
                out,
                random,
                "P31749",
                "473",
                "S",
                "AKT1 kinase",
                "AKT1",
                0.99,
                "",
                "",
                "",
                20,
                22.5,
                none());
        add(
                out,
                random,
                "P28482",
                "185",
                "T",
                "MAP kinase 1",
                "MAPK1",
                0.97,
                "",
                "",
                "",
                19.5,
                22,
                none());
        add(
                out,
                random,
                "P00533",
                "1173",
                "Y",
                "EGF receptor",
                "EGFR",
                0.98,
                "",
                "",
                "",
                22,
                19.5,
                none());
        add(
                out,
                random,
                "P60484",
                "380",
                "S",
                "PTEN phosphatase",
                "PTEN",
                0.96,
                "",
                "",
                "",
                21.5,
                19,
                none());
        add(out, random, "P04406", "83", "S", "GAPDH", "GAPDH", 0.99, "", "", "", 21, 21, none());
        add(out, random, "P60709", "41", "T", "Actin", "ACTB", 0.95, "", "", "", 18, 18.1, none());
        add(
                out,
                random,
                "P07437",
                "172",
                "S",
                "Tubulin beta",
                "TUBB",
                0.91,
                "",
                "",
                "",
                19,
                19.05,
                none());
        add(
                out,
                random,
                "P49841",
                "9",
                "S",
                "GSK3 beta",
                "GSK3B",
                0.93,
                "",
                "",
                "",
                18,
                20.8,
                holes());
        add(out, random, "P04637", "15", "S", "p53", "TP53", 0.94, "", "", "", 20, 22, scarce());
        add(
                out,
                random,
                "CON__P02768",
                "82",
                "T",
                "Albumin",
                "ALB",
                0.99,
                "",
                "+",
                "",
                17,
                17,
                none());
        add(
                out,
                random,
                "REV__P31749",
                "10",
                "S",
                "Decoy",
                "AKT1",
                0.9,
                "+",
                "",
                "",
                18,
                18,
                none());
        add(
                out,
                random,
                "P12931",
                "17",
                "S",
                "Src kinase",
                "SRC",
                0.88,
                "",
                "",
                "+",
                19,
                21,
                none());
        add(out, random, "Q15796", "465", "S", "SMAD2", "SMAD2", 0.4, "", "", "", 18, 21, none());
        add(out, random, "P42345", "2448", "S", "mTOR", "MTOR", 0.92, "", "", "", 16, 16.2, none());
        return out.toString();
    }

    /**
     * UTF-8 bytes of {@link #text()}.
     *
     * @return the file a user would open
     */
    public static byte[] bytes() {
        return text().getBytes(StandardCharsets.UTF_8);
    }

    private static boolean[] none() {
        return new boolean[6];
    }

    /** One missing value in each group; two measured values remain, so the site is kept. */
    private static boolean[] holes() {
        return new boolean[] {true, false, false, true, false, false};
    }

    /** A single disease value, which is not enough to estimate a variance. */
    private static boolean[] scarce() {
        return new boolean[] {false, false, false, true, true, false};
    }

    private static void add(
            StringBuilder out,
            Random random,
            String protein,
            String position,
            String aminoAcid,
            String proteinName,
            String gene,
            double localization,
            String reverse,
            String contaminant,
            String onlySite,
            double controlLog,
            double diseaseLog,
            boolean[] missing) {
        out.append(
                row(
                        random,
                        protein,
                        position,
                        aminoAcid,
                        proteinName,
                        gene,
                        localization,
                        reverse,
                        contaminant,
                        onlySite,
                        controlLog,
                        diseaseLog,
                        missing));
    }

    /**
     * One MaxQuant row. {@code missing} marks intensities left blank. The six log2 centres are the
     * three control replicates followed by the three disease replicates, plus a small noise.
     */
    private static String row(
            Random random,
            String protein,
            String position,
            String aminoAcid,
            String proteinName,
            String gene,
            double localization,
            String reverse,
            String contaminant,
            String onlySite,
            double controlLog,
            double diseaseLog,
            boolean[] missing) {
        StringBuilder line = new StringBuilder();
        line.append(protein).append('\t').append(position).append('\t').append(aminoAcid);
        line.append("\t1\t").append(proteinName).append('\t').append(gene).append('\t');
        line.append(String.format(Locale.ROOT, "%.2f", localization)).append('\t');
        line.append(reverse).append('\t').append(contaminant).append('\t').append(onlySite);
        line.append("\t0");
        double[] centres = {controlLog, controlLog, controlLog, diseaseLog, diseaseLog, diseaseLog};
        for (int i = 0; i < centres.length; i++) {
            line.append('\t');
            if (!missing[i]) {
                double log = centres[i] + random.nextGaussian() * 0.15;
                line.append(String.format(Locale.ROOT, "%.4f", Math.pow(2, log)));
            }
        }
        line.append('\n');
        return line.toString();
    }
}

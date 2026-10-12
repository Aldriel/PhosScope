package com.bioinfo.phosscope.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

/** Records that guard their own invariants, and the language choice. */
class ModelTest {

    @Test
    void siteLabelAndDisplayGene() {
        PhosphoSite site = site("P1", "AKT1", "473", "S", 1, 0.9, false, false, false, 1, 2);
        assertEquals("S473", site.siteLabel());
        assertEquals("AKT1", site.displayGene());
        PhosphoSite already = site("P1", "", "S473", "S", 0, Double.NaN, false, false, false, 1);
        assertEquals("S473", already.siteLabel());
        assertEquals("P1", already.displayGene());
        assertEquals(1, already.multiplicity());
        PhosphoSite bare =
                site(null, null, null, null, 0, 0.2, false, false, false, (double[]) null);
        assertEquals("", bare.siteLabel());
        double[] original = {1};
        PhosphoSite copied = site("P", "G", "1", "T", 1, 0.5, false, false, false, original);
        original[0] = 9;
        assertEquals(1, copied.intensities()[0], 0);
    }

    @Test
    void tableRejectsAShortRowAndFillsABlankName() {
        PhosphoSite site = site("P", "G", "1", "S", 1, 0.5, false, false, false, 1);
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuantTable(null, "a", List.of("a"), List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new QuantTable(
                                TableFormat.GENERIC, "a", List.of("only", "other"), List.of(site)));
        QuantTable table = new QuantTable(TableFormat.GENERIC, null, null, null);
        assertEquals("upload.txt", table.fileName());
        assertTrue(table.sites().isEmpty());
    }

    @Test
    void settingsRejectOutOfRangeChoicesAndGuessGroups() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AnalysisSettings.Builder().fdr(0).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> new AnalysisSettings.Builder().fdr(1.1).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> new AnalysisSettings.Builder().minAbsLog2FoldChange(-1).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> new AnalysisSettings.Builder().minAbsLog2FoldChange(21).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> new AnalysisSettings.Builder().minLocalization(-0.1).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> new AnalysisSettings.Builder().minLocalization(1.1).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> new AnalysisSettings.Builder().downshift(-1).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> new AnalysisSettings.Builder().downshift(6).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> new AnalysisSettings.Builder().width(0).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> new AnalysisSettings.Builder().width(1.1).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> new AnalysisSettings.Builder().minValidPerGroup(1).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> new AnalysisSettings.Builder().minValidPerGroup(101).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> new AnalysisSettings.Builder().imputation(null).build());
        AnalysisSettings defaults =
                AnalysisSettings.defaultsFor(List.of("Control_1", "Disease_1", "mystery"));
        assertEquals(Group.CONTROL, defaults.groups().get("Control_1"));
        assertEquals(Group.DISEASE, defaults.groups().get("Disease_1"));
        assertEquals(Group.UNASSIGNED, defaults.groups().get("mystery"));
        AnalysisSettings empty = AnalysisSettings.defaultsFor(null);
        assertTrue(empty.groups().isEmpty());
        AnalysisSettings rebuilt = defaults.toBuilder().fdr(0.1).groups(null).build();
        assertEquals(0.1, rebuilt.fdr(), 0);
        assertTrue(rebuilt.groups().isEmpty());
    }

    @Test
    void languageFallsBackToEnglish() {
        assertEquals(Language.FR, Language.fromLocale(Locale.FRENCH));
        assertEquals(Language.FR, Language.fromLocale(Locale.CANADA_FRENCH));
        assertEquals(Language.EN, Language.fromLocale(null));
        assertEquals(Language.EN, Language.fromLocale(Locale.GERMAN));
        assertEquals(Language.FR, Language.parse(" fr-CA "));
        assertEquals(Language.EN, Language.parse(null));
        assertEquals(Language.EN, Language.parse("  "));
        assertEquals(Language.EN, Language.parse("de"));
    }

    @Test
    void volcanoHeightIsCapped() {
        SiteStatistic zero = statistic(0);
        assertEquals(20, zero.negLog10P(), 0);
        SiteStatistic ordinary = statistic(0.01);
        assertEquals(2, ordinary.negLog10P(), 1e-9);
        SiteStatistic labeled =
                new SiteStatistic(
                        "P", "G", "name", "S15", "S", 1, 1, 2, 1, 1, 1, 0.01, 0.01, 2, 2, false);
        assertEquals("S15", labeled.siteLabel());
        SiteStatistic bare =
                new SiteStatistic(
                        "P", "G", "name", null, "", 1, 1, 2, 1, 1, 1, Double.NaN, 0.01, 2, 2, true);
        assertEquals("", bare.siteLabel());
        SiteStatistic nullAmino =
                new SiteStatistic(
                        "P", "G", "name", "15", null, 1, 1, 2, 1, 1, 1, 0.2, 0.2, 2, 2, false);
        assertEquals("15", nullAmino.siteLabel());
        assertEquals(20, bare.negLog10P(), 0);
    }

    private static SiteStatistic statistic(double p) {
        return new SiteStatistic("P", "G", "", "1", "S", 1, 1, 2, 1, 1, 1, p, p, 2, 2, false);
    }

    private static PhosphoSite site(
            String protein,
            String gene,
            String position,
            String aminoAcid,
            int multiplicity,
            double localization,
            boolean reverse,
            boolean contaminant,
            boolean onlySite,
            double... intensities) {
        return new PhosphoSite(
                protein,
                gene,
                " name ",
                position,
                aminoAcid,
                multiplicity,
                localization,
                reverse,
                contaminant,
                onlySite,
                intensities);
    }
}

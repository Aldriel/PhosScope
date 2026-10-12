package com.bioinfo.phosscope.clean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.phosscope.model.AnalysisSettings;
import com.bioinfo.phosscope.model.Group;
import com.bioinfo.phosscope.model.PhosphoSite;
import com.bioinfo.phosscope.model.QuantTable;
import com.bioinfo.phosscope.model.TableFormat;

import org.junit.jupiter.api.Test;

import java.util.List;

/** Each filter, the log2 transform, and the AScore scale that must not be filtered. */
class CleanerTest {

    @Test
    void eachFlagAndTheMissingnessRuleDropsItsOwnRows() {
        QuantTable table =
                new QuantTable(
                        TableFormat.MAXQUANT,
                        "sites.txt",
                        List.of("Control_1", "Control_2", "Disease_1", "Disease_2"),
                        List.of(
                                row("KEEP", false, false, false, 0.9, 8, 8, 4, 4),
                                row("CONT", true, false, false, 0.9, 8, 8, 4, 4),
                                row("REV", false, true, false, 0.9, 8, 8, 4, 4),
                                row("ONLY", false, false, true, 0.9, 8, 8, 4, 4),
                                row("LOW", false, false, false, 0.2, 8, 8, 4, 4),
                                row("MISS", false, false, false, 0.9, 8, 0, 4, 0),
                                row("ZERO", false, false, false, Double.NaN, 8, 8, 4, 4)));
        CleaningResult cleaned =
                DataCleaner.clean(
                        table, groups(), AnalysisSettings.defaultsFor(table.sampleNames()));
        assertEquals(7, cleaned.sitesRead());
        assertEquals(1, cleaned.droppedContaminant());
        assertEquals(1, cleaned.droppedReverse());
        assertEquals(1, cleaned.droppedOnlyBySite());
        assertEquals(1, cleaned.droppedLocalization());
        assertEquals(1, cleaned.droppedMissing());
        assertEquals(2, cleaned.kept().size());
        assertEquals("KEEP", cleaned.kept().get(0).protein());
        assertEquals("ZERO", cleaned.kept().get(1).protein());
        assertEquals(3, cleaned.log2()[0][0], 1e-9);
        assertTrue(cleaned.probabilityScale());
    }

    @Test
    void filtersCanBeTurnedOffAndAnAScoreIsNotAProbability() {
        PhosphoSite ascore = row("AS", false, false, false, 80, 8, 8, 4, 4);
        QuantTable table =
                new QuantTable(
                        TableFormat.FRAGPIPE,
                        "f.txt",
                        List.of("C1", "C2", "D1", "D2"),
                        List.of(ascore));
        AnalysisSettings settings =
                AnalysisSettings.defaultsFor(table.sampleNames()).toBuilder()
                        .dropContaminants(false)
                        .dropReverse(false)
                        .dropOnlyBySite(false)
                        .minLocalization(0.75)
                        .build();
        CleaningResult cleaned = DataCleaner.clean(table, groups(), settings);
        assertFalse(cleaned.probabilityScale());
        assertEquals(0, cleaned.droppedLocalization());
        assertEquals(1, cleaned.kept().size());
        List<Group> unassigned =
                List.of(Group.UNASSIGNED, Group.UNASSIGNED, Group.CONTROL, Group.CONTROL);
        CleaningResult waiting = DataCleaner.clean(table, unassigned, settings);
        assertEquals(0, waiting.droppedMissing());
        assertEquals(1, waiting.kept().size());
    }

    @Test
    void negativeLocalizationAndAnInfiniteIntensityAreNotProbabilities() {
        QuantTable negative =
                new QuantTable(
                        TableFormat.MAXQUANT,
                        "n.txt",
                        List.of("C1", "C2", "D1", "D2"),
                        List.of(row("NEG", false, false, false, -0.2, 8, 8, 4, 4)));
        assertFalse(
                DataCleaner.clean(
                                negative,
                                groups(),
                                AnalysisSettings.defaultsFor(negative.sampleNames()))
                        .probabilityScale());
        List<Group> withGap =
                List.of(
                        Group.CONTROL,
                        Group.CONTROL,
                        Group.UNASSIGNED,
                        Group.DISEASE,
                        Group.DISEASE);
        QuantTable wide =
                new QuantTable(
                        TableFormat.GENERIC,
                        "w.txt",
                        List.of("C1", "C2", "X", "D1", "D2"),
                        List.of(
                                row(
                                        "INF",
                                        false,
                                        false,
                                        false,
                                        0.9,
                                        Double.POSITIVE_INFINITY,
                                        8,
                                        8,
                                        4,
                                        4)));
        CleaningResult cleaned =
                DataCleaner.clean(wide, withGap, AnalysisSettings.defaultsFor(wide.sampleNames()));
        assertEquals(1, cleaned.droppedMissing());
    }

    @Test
    void log2OfANonPositiveValueIsMissing() {
        double[] logged =
                DataCleaner.toLog2(new double[] {2, 0, -1, Double.NaN, Double.POSITIVE_INFINITY});
        assertEquals(1, logged[0], 1e-12);
        assertTrue(Double.isNaN(logged[1]));
        assertTrue(Double.isNaN(logged[2]));
        assertTrue(Double.isNaN(logged[3]));
        assertTrue(Double.isNaN(logged[4]));
    }

    private static List<Group> groups() {
        return List.of(Group.CONTROL, Group.CONTROL, Group.DISEASE, Group.DISEASE);
    }

    private static PhosphoSite row(
            String protein,
            boolean contaminant,
            boolean reverse,
            boolean onlySite,
            double localization,
            double... intensities) {
        return new PhosphoSite(
                protein,
                protein,
                "",
                "1",
                "S",
                1,
                localization,
                reverse,
                contaminant,
                onlySite,
                intensities);
    }
}

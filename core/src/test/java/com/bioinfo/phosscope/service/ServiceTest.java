package com.bioinfo.phosscope.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.phosscope.example.SimulatedCohort;
import com.bioinfo.phosscope.model.AnalysisSettings;
import com.bioinfo.phosscope.model.AnalysisStatus;
import com.bioinfo.phosscope.model.Group;
import com.bioinfo.phosscope.model.ImputationMethod;
import com.bioinfo.phosscope.model.Language;
import com.bioinfo.phosscope.model.QuantTable;
import com.bioinfo.phosscope.model.SiteStatistic;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/** The facade on the simulated cohort, and the report in both languages. */
class ServiceTest {

    private final PhosService service = new PhosService();

    @Test
    void simulatedCohortRecoversThePlantedSites() throws Exception {
        QuantTable table = service.read(SimulatedCohort.bytes(), SimulatedCohort.FILE_NAME);
        AnalysisSettings settings = AnalysisSettings.defaultsFor(table.sampleNames());
        Analysis first = service.analyze(table, settings);
        Analysis second = service.analyze(table, settings);
        assertEquals(AnalysisStatus.OK, first.status());
        assertEquals(first.significantCount(), second.significantCount());
        assertTrue(first.imputedValues() > 0);
        assertEquals(1, first.droppedContaminant());
        assertEquals(1, first.droppedReverse());
        assertEquals(1, first.droppedOnlyBySite());
        assertEquals(1, first.droppedLocalization());
        assertEquals(1, first.droppedMissing());
        SiteStatistic akt = site(first, "AKT1");
        SiteStatistic egfr = site(first, "EGFR");
        SiteStatistic gapdh = site(first, "GAPDH");
        assertTrue(akt.significant() && akt.log2FoldChange() > 1.5);
        assertTrue(egfr.significant() && egfr.log2FoldChange() < -1.5);
        assertFalse(gapdh.significant());
        assertTrue(Math.abs(gapdh.log2FoldChange()) < 0.8);
        assertEquals("S473", akt.siteLabel());
        String exported = service.exportTable(first, Language.FR);
        assertTrue(exported.startsWith("\uFEFFGène\t"));
        assertTrue(exported.contains("AKT1"));
        assertTrue(service.exportTable(first, null).contains("Gene\t"));
        assertTrue(service.exportTable(null, Language.EN).startsWith("\uFEFFGene\t"));
    }

    @Test
    void groupsAndImputationChangeTheStatus() throws Exception {
        QuantTable table = service.read(SimulatedCohort.bytes(), SimulatedCohort.FILE_NAME);
        AnalysisSettings allControl =
                AnalysisSettings.defaultsFor(table.sampleNames()).toBuilder()
                        .groups(
                                java.util.Map.of(
                                        "Control_1", Group.CONTROL,
                                        "Control_2", Group.CONTROL,
                                        "Control_3", Group.CONTROL,
                                        "Disease_1", Group.CONTROL,
                                        "Disease_2", Group.UNASSIGNED,
                                        "Disease_3", Group.UNASSIGNED))
                        .build();
        assertEquals(AnalysisStatus.NEED_GROUPS, service.analyze(table, allControl).status());
        AnalysisSettings oneDisease =
                allControl.toBuilder()
                        .groups(
                                java.util.Map.of(
                                        "Control_1", Group.CONTROL,
                                        "Control_2", Group.CONTROL,
                                        "Control_3", Group.UNASSIGNED,
                                        "Disease_1", Group.DISEASE,
                                        "Disease_2", Group.UNASSIGNED,
                                        "Disease_3", Group.UNASSIGNED))
                        .build();
        assertEquals(AnalysisStatus.NEED_REPLICATES, service.analyze(table, oneDisease).status());
        AnalysisSettings strict =
                AnalysisSettings.defaultsFor(table.sampleNames()).toBuilder()
                        .minLocalization(1)
                        .imputation(ImputationMethod.NONE)
                        .build();
        assertEquals(AnalysisStatus.NO_SITE, service.analyze(table, strict).status());
        AnalysisSettings half =
                AnalysisSettings.defaultsFor(table.sampleNames()).toBuilder()
                        .imputation(ImputationMethod.HALF_MINIMUM)
                        .build();
        Analysis halfResult = service.analyze(table, half);
        assertEquals(AnalysisStatus.OK, halfResult.status());
        assertTrue(halfResult.imputedValues() > 0);
    }

    @Test
    void reportContainsTheContactLineInBothLanguages() throws Exception {
        QuantTable table = service.read(SimulatedCohort.bytes(), SimulatedCohort.FILE_NAME);
        Analysis analysis =
                service.analyze(table, AnalysisSettings.defaultsFor(table.sampleNames()));
        String french = text(service.report(analysis, Language.FR, "  ", true));
        String english = text(service.report(analysis, null, "Tumor\nstudy", false));
        assertTrue(french.contains("simul"));
        assertTrue(french.contains("contact@maximeethier.com"));
        assertTrue(french.contains("AKT1"));
        assertTrue(english.contains("Tumor study"));
        assertTrue(english.contains("ko-fi.com"));
        assertFalse(english.contains("simul"));
        String blank = text(service.report(null, Language.EN, null, false));
        assertTrue(blank.contains("No analysis"));
        List<SiteStatistic> many = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            many.add(
                    new SiteStatistic(
                            "P" + i,
                            "G" + i,
                            "n",
                            "1",
                            "S",
                            1,
                            1,
                            3,
                            2,
                            4,
                            4,
                            0.001,
                            0.01,
                            3,
                            3,
                            i % 2 == 0));
        }
        Analysis longAnalysis =
                new Analysis(
                        analysis.format(),
                        analysis.fileName(),
                        analysis.sampleNames(),
                        analysis.settings(),
                        analysis.sitesRead(),
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        true,
                        AnalysisStatus.OK,
                        many);
        byte[] pdf = service.report(longAnalysis, Language.EN, "é".repeat(10) + "\u2603", false);
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertTrue(document.getNumberOfPages() >= 2);
            String body = new PDFTextStripper().getText(document);
            assertTrue(body.contains("?"));
            assertTrue(body.contains("limited to the first 40"));
        }
    }

    @Test
    void eachGroupSideAndIncompleteRowsAfterNoImputation() throws Exception {
        QuantTable table = service.read(SimulatedCohort.bytes(), SimulatedCohort.FILE_NAME);
        java.util.Map<String, Group> diseaseOnly = new java.util.LinkedHashMap<>();
        java.util.Map<String, Group> oneControl = new java.util.LinkedHashMap<>();
        boolean first = true;
        for (String name : table.sampleNames()) {
            diseaseOnly.put(name, Group.DISEASE);
            oneControl.put(name, first ? Group.CONTROL : Group.DISEASE);
            first = false;
        }
        AnalysisSettings base = AnalysisSettings.defaultsFor(table.sampleNames());
        assertEquals(
                AnalysisStatus.NEED_GROUPS,
                service.analyze(table, base.toBuilder().groups(diseaseOnly).build()).status());
        assertEquals(
                AnalysisStatus.NEED_REPLICATES,
                service.analyze(table, base.toBuilder().groups(oneControl).build()).status());
        Analysis strict =
                service.analyze(table, base.toBuilder().fdr(1).minAbsLog2FoldChange(20).build());
        assertEquals(0, strict.significantCount());
        String header =
                "Proteins\tAmino acid\tPositions within proteins\tGene names\tLocalization prob\t"
                        + "Intensity Control_1\tIntensity Control_2\tIntensity Control_3\t"
                        + "Intensity Disease_1\tIntensity Disease_2\n";
        String hole = "P\tS\t2\tHOLE\t0.9\t8\t8\t\t4\t4\n";
        QuantTable mixed =
                service.read(
                        (header + "P\tS\t1\tFULL\t0.9\t8\t8\t8\t4\t4\n" + hole)
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        "h.txt");
        AnalysisSettings none =
                AnalysisSettings.defaultsFor(mixed.sampleNames()).toBuilder()
                        .imputation(ImputationMethod.NONE)
                        .build();
        Analysis kept = service.analyze(mixed, none);
        assertEquals(AnalysisStatus.OK, kept.status());
        assertEquals(1, kept.droppedIncomplete());
        QuantTable onlyHole =
                service.read(
                        (header + hole).getBytes(java.nio.charset.StandardCharsets.UTF_8), "o.txt");
        assertEquals(
                AnalysisStatus.NO_SITE,
                service.analyze(
                                onlyHole,
                                AnalysisSettings.defaultsFor(onlyHole.sampleNames()).toBuilder()
                                        .imputation(ImputationMethod.NONE)
                                        .build())
                        .status());
    }

    private static SiteStatistic site(Analysis analysis, String gene) {
        return analysis.sites().stream()
                .filter(site -> gene.equals(site.gene()))
                .findFirst()
                .orElseThrow();
    }

    private static String text(byte[] pdf) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }
}

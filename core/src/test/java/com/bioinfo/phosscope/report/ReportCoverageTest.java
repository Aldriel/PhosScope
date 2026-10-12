package com.bioinfo.phosscope.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.phosscope.model.AnalysisSettings;
import com.bioinfo.phosscope.model.AnalysisStatus;
import com.bioinfo.phosscope.model.Group;
import com.bioinfo.phosscope.model.ImputationMethod;
import com.bioinfo.phosscope.model.Language;
import com.bioinfo.phosscope.model.SiteStatistic;
import com.bioinfo.phosscope.model.TableFormat;
import com.bioinfo.phosscope.service.Analysis;

import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The sentences of the report that the simulated cohort never reaches. */
class ReportCoverageTest {

    @Test
    void everyStatusAndImputationIsWrittenInBothLanguages() throws Exception {
        for (Language language : Language.values()) {
            for (AnalysisStatus status : AnalysisStatus.values()) {
                String text = join(status, ImputationMethod.PERSEUS, language, false);
                assertFalse(text.isBlank());
            }
            for (ImputationMethod method : ImputationMethod.values()) {
                String text = join(AnalysisStatus.OK, method, language, true);
                assertTrue(text.contains("G") || text.contains("PhosScope"));
            }
        }
        assertTrue(join(null, Language.FR, "\n").contains("Aucune"));
        assertTrue(join(null, Language.EN, "  ").contains("No analysis"));
        List<SiteStatistic> many = new ArrayList<>();
        for (int i = 0; i < 41; i++) {
            many.add(site(i % 2 == 0, i % 3 == 0 ? -1 : 1));
        }
        String limited =
                String.join(
                        "\n",
                        PdfReportWriter.lines(
                                analysis(AnalysisStatus.OK, ImputationMethod.NONE, many),
                                Language.FR,
                                "é".repeat(90),
                                false));
        assertTrue(limited.contains("40"));
        String exported =
                ResultTableWriter.write(
                        analysis(
                                AnalysisStatus.OK,
                                ImputationMethod.HALF_MINIMUM,
                                List.of(
                                        site(true, 1),
                                        new SiteStatistic(
                                                null,
                                                null,
                                                null,
                                                "1",
                                                null,
                                                1,
                                                Double.NaN,
                                                1,
                                                1,
                                                1,
                                                1,
                                                Double.NaN,
                                                0.2,
                                                1,
                                                1,
                                                false))),
                        Language.FR);
        assertTrue(exported.contains("oui"));
        assertTrue(exported.contains("non"));
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        assertEquals("", PdfReportWriter.encodable(null, font));
        assertEquals("A", PdfReportWriter.encodable("A", font));
    }

    private static String join(
            AnalysisStatus status, ImputationMethod method, Language language, boolean simulated) {
        return String.join(
                "\n",
                PdfReportWriter.lines(
                        analysis(status, method, List.of(site(false, 0), site(true, -2))),
                        language,
                        "Title",
                        simulated));
    }

    private static String join(Analysis analysis, Language language, String title) {
        return String.join("\n", PdfReportWriter.lines(analysis, language, title, false));
    }

    private static Analysis analysis(
            AnalysisStatus status, ImputationMethod method, List<SiteStatistic> sites) {
        AnalysisSettings settings =
                AnalysisSettings.defaultsFor(List.of("Control", "Other", "Disease")).toBuilder()
                        .imputation(method)
                        .groups(
                                Map.of(
                                        "Control", Group.CONTROL,
                                        "Other", Group.UNASSIGNED,
                                        "Disease", Group.DISEASE))
                        .build();
        return new Analysis(
                TableFormat.GENERIC,
                "f.txt",
                List.of("Control", "Other", "Disease"),
                settings,
                3,
                1,
                1,
                1,
                1,
                1,
                1,
                0,
                false,
                status,
                sites);
    }

    private static SiteStatistic site(boolean significant, double fold) {
        return new SiteStatistic(
                "P",
                "G\tH\n",
                "name",
                "S1",
                "S",
                1,
                1,
                1 + fold,
                fold,
                1,
                1,
                Double.NaN,
                0.01,
                2,
                2,
                significant);
    }
}

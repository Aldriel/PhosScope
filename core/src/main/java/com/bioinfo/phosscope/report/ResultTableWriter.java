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

package com.bioinfo.phosscope.report;

import com.bioinfo.phosscope.model.Language;
import com.bioinfo.phosscope.model.SiteStatistic;
import com.bioinfo.phosscope.service.Analysis;

import java.util.Locale;

/**
 * Tab-separated export of the tested sites. A byte-order mark is written first so that Excel
 * detects UTF-8.
 */
public final class ResultTableWriter {

    private ResultTableWriter() {}

    /**
     * Writes every tested site.
     *
     * @param analysis result to export; a missing analysis gives a header only
     * @param language language of the header
     * @return the text, starting with a byte-order mark
     */
    public static String write(Analysis analysis, Language language) {
        Language actual = language == null ? Language.EN : language;
        StringBuilder out = new StringBuilder();
        out.append('\uFEFF');
        out.append(header(actual)).append('\n');
        if (analysis == null) {
            return out.toString();
        }
        for (SiteStatistic site : analysis.sites()) {
            out.append(cell(site.gene())).append('\t');
            out.append(cell(site.protein())).append('\t');
            out.append(cell(site.siteLabel())).append('\t');
            out.append(site.multiplicity()).append('\t');
            out.append(number(site.controlMean())).append('\t');
            out.append(number(site.diseaseMean())).append('\t');
            out.append(number(site.log2FoldChange())).append('\t');
            out.append(number(site.pValue())).append('\t');
            out.append(number(site.qValue())).append('\t');
            out.append(site.significant() ? yes(actual) : no(actual)).append('\n');
        }
        return out.toString();
    }

    private static String header(Language language) {
        if (language == Language.FR) {
            return "Gène\tProtéine\tSite\tMultiplicité\tMoyenne log2 témoin\t"
                    + "Moyenne log2 malade\tLog2 FC\tp\tq\tSignificatif";
        }
        return "Gene\tProtein\tSite\tMultiplicity\tControl log2 mean\t"
                + "Disease log2 mean\tLog2 FC\tp\tq\tSignificant";
    }

    private static String yes(Language language) {
        return language == Language.FR ? "oui" : "yes";
    }

    private static String no(Language language) {
        return language == Language.FR ? "non" : "no";
    }

    private static String cell(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    private static String number(double value) {
        if (!Double.isFinite(value)) {
            return "";
        }
        return String.format(Locale.ROOT, "%.6g", value);
    }
}

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

package com.bioinfo.phosscope.clean;

import com.bioinfo.phosscope.model.AnalysisSettings;
import com.bioinfo.phosscope.model.Group;
import com.bioinfo.phosscope.model.PhosphoSite;
import com.bioinfo.phosscope.model.QuantTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes rows that should not be tested and transforms the intensities that remain to log2.
 *
 * <p>A localization column is treated as a probability only when every finite value is between 0
 * and 1. An AScore, which is often larger than 1, is kept for information and is not filtered,
 * because a threshold of 0.75 would drop the whole table.
 */
public final class DataCleaner {

    private DataCleaner() {}

    /**
     * Cleans one table.
     *
     * @param table rows read from the file
     * @param groups one group per sample, aligned with {@link QuantTable#sampleNames()}
     * @param settings filters; the imputation choices are ignored here
     * @return the rows that remain and the reason each other row was dropped
     */
    public static CleaningResult clean(
            QuantTable table, List<Group> groups, AnalysisSettings settings) {
        int contaminants = 0;
        int reverse = 0;
        int onlyBySite = 0;
        int localization = 0;
        int missing = 0;
        boolean probability = probabilityScale(table.sites());
        boolean groupsReady = bothGroupsHaveSamples(groups, settings.minValidPerGroup());
        List<PhosphoSite> kept = new ArrayList<>();
        for (PhosphoSite site : table.sites()) {
            if (settings.dropContaminants() && site.contaminant()) {
                contaminants++;
            } else if (settings.dropReverse() && site.reverse()) {
                reverse++;
            } else if (settings.dropOnlyBySite() && site.onlyIdentifiedBySite()) {
                onlyBySite++;
            } else if (probability && lowLocalization(site, settings.minLocalization())) {
                localization++;
            } else if (groupsReady && tooMissing(site, groups, settings.minValidPerGroup())) {
                missing++;
            } else {
                kept.add(site);
            }
        }
        double[][] log2 = new double[kept.size()][];
        for (int row = 0; row < kept.size(); row++) {
            log2[row] = toLog2(kept.get(row).intensities());
        }
        return new CleaningResult(
                table.sites().size(),
                contaminants,
                reverse,
                onlyBySite,
                localization,
                missing,
                List.copyOf(kept),
                log2,
                probability);
    }

    /**
     * Log2 of positive intensities. A non-positive or non-finite value becomes missing.
     *
     * @param intensities raw intensities
     * @return log2 values of the same length
     */
    public static double[] toLog2(double[] intensities) {
        double[] logged = new double[intensities.length];
        for (int i = 0; i < intensities.length; i++) {
            double value = intensities[i];
            logged[i] =
                    value > 0 && Double.isFinite(value)
                            ? Math.log(value) / Math.log(2)
                            : Double.NaN;
        }
        return logged;
    }

    private static boolean probabilityScale(List<PhosphoSite> sites) {
        boolean seen = false;
        for (PhosphoSite site : sites) {
            if (!Double.isFinite(site.localization())) {
                continue;
            }
            seen = true;
            if (site.localization() < 0 || site.localization() > 1) {
                return false;
            }
        }
        return seen;
    }

    private static boolean lowLocalization(PhosphoSite site, double minimum) {
        return Double.isFinite(site.localization()) && site.localization() < minimum;
    }

    private static boolean bothGroupsHaveSamples(List<Group> groups, int minimum) {
        return count(groups, Group.CONTROL) >= minimum && count(groups, Group.DISEASE) >= minimum;
    }

    private static int count(List<Group> groups, Group group) {
        int total = 0;
        for (Group candidate : groups) {
            if (candidate == group) {
                total++;
            }
        }
        return total;
    }

    private static boolean tooMissing(PhosphoSite site, List<Group> groups, int minimum) {
        int control = 0;
        int disease = 0;
        double[] intensities = site.intensities();
        for (int i = 0; i < intensities.length; i++) {
            if (!(intensities[i] > 0) || !Double.isFinite(intensities[i])) {
                continue;
            }
            if (groups.get(i) == Group.CONTROL) {
                control++;
            } else if (groups.get(i) == Group.DISEASE) {
                disease++;
            }
        }
        return control < minimum || disease < minimum;
    }
}

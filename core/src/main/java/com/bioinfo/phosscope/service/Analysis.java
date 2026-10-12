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

package com.bioinfo.phosscope.service;

import com.bioinfo.phosscope.model.AnalysisSettings;
import com.bioinfo.phosscope.model.AnalysisStatus;
import com.bioinfo.phosscope.model.SiteStatistic;
import com.bioinfo.phosscope.model.TableFormat;

import java.util.List;

/**
 * Outcome of one analysis: how many rows were dropped, and the sites that were tested.
 *
 * <p>When {@link #status()} is not {@link AnalysisStatus#OK}, {@link #sites()} is empty. The drop
 * counts are still filled, so the interface can explain why nothing was tested.
 *
 * @param format recognized file format
 * @param fileName name of the uploaded file
 * @param sampleNames intensity columns, in table order
 * @param settings choices used for this result
 * @param sitesRead rows in the file
 * @param droppedContaminant contaminant rows
 * @param droppedReverse reversed decoy hits
 * @param droppedOnlyBySite rows identified only as a modified site
 * @param droppedLocalization rows under the localization probability
 * @param droppedMissing rows without enough measured values
 * @param droppedIncomplete rows still incomplete after imputation (only with method NONE)
 * @param imputedValues cells filled by the imputation
 * @param probabilityScale {@code true} when localization was filtered as a probability
 * @param status whether a comparison was computed
 * @param sites tested sites, significant ones first
 */
public record Analysis(
        TableFormat format,
        String fileName,
        List<String> sampleNames,
        AnalysisSettings settings,
        int sitesRead,
        int droppedContaminant,
        int droppedReverse,
        int droppedOnlyBySite,
        int droppedLocalization,
        int droppedMissing,
        int droppedIncomplete,
        int imputedValues,
        boolean probabilityScale,
        AnalysisStatus status,
        List<SiteStatistic> sites) {

    /** Copies the lists so the result cannot change after it is returned. */
    public Analysis {
        sampleNames = List.copyOf(sampleNames);
        sites = List.copyOf(sites);
    }

    /**
     * Counts the sites that pass both thresholds.
     *
     * @return the size of the significant list
     */
    public int significantCount() {
        int count = 0;
        for (SiteStatistic site : sites) {
            if (site.significant()) {
                count++;
            }
        }
        return count;
    }
}

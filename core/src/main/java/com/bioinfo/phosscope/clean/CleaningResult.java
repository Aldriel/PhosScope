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

import com.bioinfo.phosscope.model.PhosphoSite;

import java.util.List;

/**
 * Rows that survived the filters, already transformed to log2, plus the counts of each reason a row
 * was removed.
 *
 * @param sitesRead rows present in the file
 * @param droppedContaminant contaminant rows
 * @param droppedReverse reversed decoy hits
 * @param droppedOnlyBySite rows identified only as a modified site
 * @param droppedLocalization rows under the localization probability
 * @param droppedMissing rows without enough measured values in each group
 * @param kept rows that remain, in file order
 * @param log2 log2 intensities of {@code kept}; missing values are {@link Double#NaN}
 * @param probabilityScale {@code true} when localization looks like a probability in [0, 1]
 */
public record CleaningResult(
        int sitesRead,
        int droppedContaminant,
        int droppedReverse,
        int droppedOnlyBySite,
        int droppedLocalization,
        int droppedMissing,
        List<PhosphoSite> kept,
        double[][] log2,
        boolean probabilityScale) {}

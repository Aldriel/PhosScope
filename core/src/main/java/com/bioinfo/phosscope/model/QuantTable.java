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

package com.bioinfo.phosscope.model;

import java.util.List;

/**
 * Quantitative table read from one file: sample names and one row per phosphosite.
 *
 * @param format how the file was recognized
 * @param fileName name to show and to write in the report
 * @param sampleNames intensity columns, in the order of {@link PhosphoSite#intensities()}
 * @param sites every data row that could be read, including rows the cleaner will drop
 */
public record QuantTable(
        TableFormat format, String fileName, List<String> sampleNames, List<PhosphoSite> sites) {

    /** Rejects a table whose rows do not line up with the sample columns. */
    public QuantTable {
        if (format == null) {
            throw new IllegalArgumentException("FORMAT");
        }
        fileName = fileName == null || fileName.isBlank() ? "upload.txt" : fileName.trim();
        sampleNames = List.copyOf(sampleNames == null ? List.of() : sampleNames);
        sites = List.copyOf(sites == null ? List.of() : sites);
        for (PhosphoSite site : sites) {
            if (site.intensities().length != sampleNames.size()) {
                throw new IllegalArgumentException("INTENSITY_WIDTH");
            }
        }
    }
}

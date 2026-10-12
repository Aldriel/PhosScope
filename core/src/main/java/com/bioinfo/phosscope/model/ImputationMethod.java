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

/**
 * Strategy used to replace missing intensities before the statistical test.
 *
 * <p>Phosphoproteomics data are missing mostly when a site is too weak to be detected (missing not
 * at random). {@link #PERSEUS} is the down-shifted normal draw popularized by the Perseus software
 * of the MaxQuant authors. {@link #HALF_MINIMUM} is the simpler detection-limit fill. {@link #NONE}
 * keeps only sites observed in every sample.
 */
public enum ImputationMethod {
    /** Draw each missing log2 value from a narrow normal law below the sample mean. */
    PERSEUS,
    /** Replace a missing log2 value by one unit under the lowest observed value of the sample. */
    HALF_MINIMUM,
    /** Do not invent values; sites that are still incomplete are not tested. */
    NONE
}

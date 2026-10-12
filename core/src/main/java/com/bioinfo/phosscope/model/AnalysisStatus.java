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

/** Whether a comparison between the two groups could be computed. */
public enum AnalysisStatus {
    /** Sites were tested. The significant list may still be empty. */
    OK,
    /** At least one of the two groups has no sample. */
    NEED_GROUPS,
    /** A group has a single sample, so a variance cannot be estimated. */
    NEED_REPLICATES,
    /** Filters and imputation left no site that can be tested. */
    NO_SITE
}

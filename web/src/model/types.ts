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

/** One tested phosphosite, as returned by the API. */
export interface SiteStatistic {
  gene: string;
  protein: string;
  position: string;
  aminoAcid: string;
  log2FoldChange: number;
  pValue: number;
  qValue: number;
  significant: boolean;
}

/** Choices stored with the analysis. */
export interface AnalysisSettings {
  groups: Record<string, 'CONTROL' | 'DISEASE' | 'UNASSIGNED'>;
  dropContaminants: boolean;
  dropReverse: boolean;
  dropOnlyBySite: boolean;
  minLocalization: number;
  minValidPerGroup: number;
  imputation: 'PERSEUS' | 'HALF_MINIMUM' | 'NONE';
  downshift: number;
  width: number;
  seed: number;
  fdr: number;
  minAbsLog2FoldChange: number;
}

/** Comparison of the two groups. Null until a table is open. */
export interface Analysis {
  fileName: string;
  format: string;
  sampleNames: string[];
  settings: AnalysisSettings;
  sitesRead: number;
  droppedContaminant: number;
  droppedReverse: number;
  droppedOnlyBySite: number;
  droppedLocalization: number;
  droppedMissing: number;
  imputedValues: number;
  status: 'OK' | 'NEED_GROUPS' | 'NEED_REPLICATES' | 'NO_SITE';
  sites: SiteStatistic[];
}

/** One visitor's workspace. */
export interface Workspace {
  id: string;
  fileName: string | null;
  format: string | null;
  simulated: boolean;
  analysis: Analysis | null;
}

/** Error answered by the API. */
export interface ApiFailure {
  error?: string;
  kind?: string;
  code?: string;
}

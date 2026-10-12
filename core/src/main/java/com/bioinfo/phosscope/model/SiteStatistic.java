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
 * Result of the two-group comparison for one phosphosite.
 *
 * <p>Means and the fold change are on the log2 scale, after imputation. {@code observedControl} and
 * {@code observedDisease} count values that were really measured, before imputation. The fold
 * change is the disease mean minus the control mean: a positive value means the site is more
 * phosphorylated in the disease group.
 *
 * @param protein protein accession
 * @param gene gene symbol, or the accession when the file has no gene
 * @param proteinName descriptive name
 * @param position position text from the file
 * @param aminoAcid one-letter residue
 * @param multiplicity number of phospho groups
 * @param controlMean mean log2 intensity of the control group
 * @param diseaseMean mean log2 intensity of the disease group
 * @param log2FoldChange disease mean minus control mean
 * @param tStatistic Welch statistic
 * @param degreesOfFreedom Welch–Satterthwaite degrees of freedom
 * @param pValue two-sided Welch p-value
 * @param qValue Benjamini–Hochberg adjusted p-value
 * @param observedControl measured (not imputed) values in the control group
 * @param observedDisease measured values in the disease group
 * @param significant {@code true} when the site passes both the q-value and the fold-change cut
 */
public record SiteStatistic(
        String protein,
        String gene,
        String proteinName,
        String position,
        String aminoAcid,
        int multiplicity,
        double controlMean,
        double diseaseMean,
        double log2FoldChange,
        double tStatistic,
        double degreesOfFreedom,
        double pValue,
        double qValue,
        int observedControl,
        int observedDisease,
        boolean significant) {

    /**
     * Height of a volcano plot, capped so a p-value of zero stays on the chart.
     *
     * @return {@code -log10(p)}, or 20 when the p-value is zero
     */
    public double negLog10P() {
        if (pValue <= 0 || !Double.isFinite(pValue)) {
            return 20;
        }
        return Math.min(20, -Math.log10(pValue));
    }

    /**
     * Label of the modified residue, such as {@code S473}.
     *
     * @return the amino acid and the position
     */
    public String siteLabel() {
        if (aminoAcid == null || aminoAcid.isEmpty() || position.startsWith(aminoAcid)) {
            return position == null ? "" : position;
        }
        return aminoAcid + position;
    }
}

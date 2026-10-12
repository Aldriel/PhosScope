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

import java.util.Arrays;

/**
 * One row of a quantitative phosphoproteomics table, before the log2 transform.
 *
 * <p>Missing intensities are {@link Double#NaN}. A zero or a negative number in the file is stored
 * as missing, because it means the site was not detected in that sample.
 *
 * @param protein accession, or several accessions separated as in the source file
 * @param gene gene symbol, possibly empty
 * @param proteinName descriptive name, possibly empty
 * @param position position in the protein, as written in the file ({@code 473} or {@code S473})
 * @param aminoAcid one-letter residue, possibly empty when the file does not give it
 * @param multiplicity number of phospho groups on the peptide
 * @param localization localization probability between 0 and 1, an AScore, or {@link Double#NaN}
 * @param reverse {@code true} when the hit comes from the reversed decoy database
 * @param contaminant {@code true} for a contaminant protein
 * @param onlyIdentifiedBySite {@code true} when MaxQuant saw the peptide only as a modified site
 * @param intensities raw intensities, one per sample, never {@code null}
 */
public record PhosphoSite(
        String protein,
        String gene,
        String proteinName,
        String position,
        String aminoAcid,
        int multiplicity,
        double localization,
        boolean reverse,
        boolean contaminant,
        boolean onlyIdentifiedBySite,
        double[] intensities) {

    /** Copies the intensities so a caller cannot change a site after it has been read. */
    public PhosphoSite {
        protein = blankToEmpty(protein);
        gene = blankToEmpty(gene);
        proteinName = blankToEmpty(proteinName);
        position = blankToEmpty(position);
        aminoAcid = blankToEmpty(aminoAcid);
        multiplicity = Math.max(1, multiplicity);
        intensities =
                intensities == null
                        ? new double[0]
                        : Arrays.copyOf(intensities, intensities.length);
    }

    /**
     * Short label of the site, such as {@code S473}.
     *
     * @return the amino acid followed by the position, without duplicating a letter already present
     */
    public String siteLabel() {
        if (aminoAcid.isEmpty() || position.startsWith(aminoAcid)) {
            return position.isEmpty() ? protein : position;
        }
        return aminoAcid + position;
    }

    /**
     * Gene symbol when the file has one, otherwise the protein accession.
     *
     * @return the preferred short name of the protein
     */
    public String displayGene() {
        return gene.isEmpty() ? protein : gene;
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

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

import com.bioinfo.phosscope.parse.GroupGuesser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Choices the user can change: which samples belong to which group, how rows are filtered, how
 * missing values are filled, and when a site is called significant.
 *
 * <p>The constructor rejects a choice outside the ranges documented on each component. The message
 * of {@link IllegalArgumentException} is a stable code ({@code FDR_RANGE}, {@code WIDTH_RANGE}…)
 * that the interfaces translate.
 *
 * @param groups sample name to group; names absent from the table are ignored
 * @param dropContaminants drop rows flagged as contaminants
 * @param dropReverse drop reversed decoy hits
 * @param dropOnlyBySite drop MaxQuant rows identified only as a modified site
 * @param minLocalization minimum localization probability; ignored when the column is an AScore
 * @param minValidPerGroup measured values required in each group before a site is tested
 * @param imputation how the remaining missing values are replaced
 * @param downshift Perseus downshift, in standard deviations of the sample
 * @param width Perseus width, as a fraction of the standard deviation of the sample
 * @param seed seed of the random draw, so the same file always gives the same imputed values
 * @param fdr largest Benjamini–Hochberg q-value of a significant site
 * @param minAbsLog2FoldChange smallest absolute log2 fold change of a significant site
 */
public record AnalysisSettings(
        Map<String, Group> groups,
        boolean dropContaminants,
        boolean dropReverse,
        boolean dropOnlyBySite,
        double minLocalization,
        int minValidPerGroup,
        ImputationMethod imputation,
        double downshift,
        double width,
        long seed,
        double fdr,
        double minAbsLog2FoldChange) {

    /** Default localization probability used by MaxQuant publications (0.75). */
    public static final double DEFAULT_MIN_LOCALIZATION = 0.75;

    /** Measured values required in each group. Two are needed to estimate a variance. */
    public static final int DEFAULT_MIN_VALID = 2;

    /** Perseus downshift: 1.8 standard deviations below the sample mean. */
    public static final double DEFAULT_DOWNSHIFT = 1.8;

    /** Perseus width: the imputed normal law is 0.3 times as wide as the sample. */
    public static final double DEFAULT_WIDTH = 0.3;

    /** Seed used when the user does not choose one. */
    public static final long DEFAULT_SEED = 1L;

    /** Default false-discovery rate. */
    public static final double DEFAULT_FDR = 0.05;

    /** Default absolute log2 fold change, a doubling or a halving. */
    public static final double DEFAULT_MIN_ABS_LOG2_FC = 1.0;

    /** Checks the numeric ranges and copies the group map. */
    public AnalysisSettings {
        groups = Map.copyOf(groups == null ? Map.of() : groups);
        if (imputation == null) {
            throw new IllegalArgumentException("IMPUTATION");
        }
        if (minLocalization < 0 || minLocalization > 1) {
            throw new IllegalArgumentException("LOCALIZATION_RANGE");
        }
        if (minValidPerGroup < 2 || minValidPerGroup > 100) {
            throw new IllegalArgumentException("MIN_VALID_RANGE");
        }
        if (downshift < 0 || downshift > 5) {
            throw new IllegalArgumentException("DOWNSHIFT_RANGE");
        }
        if (width <= 0 || width > 1) {
            throw new IllegalArgumentException("WIDTH_RANGE");
        }
        if (fdr <= 0 || fdr > 1) {
            throw new IllegalArgumentException("FDR_RANGE");
        }
        if (minAbsLog2FoldChange < 0 || minAbsLog2FoldChange > 20) {
            throw new IllegalArgumentException("FOLD_CHANGE_RANGE");
        }
    }

    /**
     * Settings with the usual filters, Perseus imputation and a 5% false-discovery rate. Groups are
     * guessed from the sample names.
     *
     * @param sampleNames intensity columns of the table
     * @return valid settings
     */
    public static AnalysisSettings defaultsFor(List<String> sampleNames) {
        Map<String, Group> guessed = new LinkedHashMap<>();
        if (sampleNames != null) {
            for (String name : sampleNames) {
                guessed.put(name, GroupGuesser.guess(name));
            }
        }
        return new Builder().groups(guessed).build();
    }

    /**
     * Opens a builder already filled with these settings.
     *
     * @return a builder
     */
    public Builder toBuilder() {
        return new Builder()
                .groups(groups)
                .dropContaminants(dropContaminants)
                .dropReverse(dropReverse)
                .dropOnlyBySite(dropOnlyBySite)
                .minLocalization(minLocalization)
                .minValidPerGroup(minValidPerGroup)
                .imputation(imputation)
                .downshift(downshift)
                .width(width)
                .seed(seed)
                .fdr(fdr)
                .minAbsLog2FoldChange(minAbsLog2FoldChange);
    }

    /**
     * Assembles {@link AnalysisSettings} one choice at a time. Omitted choices keep the defaults
     * used by {@link AnalysisSettings#defaultsFor}.
     */
    public static final class Builder {
        private Map<String, Group> groups = Map.of();
        private boolean dropContaminants = true;
        private boolean dropReverse = true;
        private boolean dropOnlyBySite = true;
        private double minLocalization = DEFAULT_MIN_LOCALIZATION;
        private int minValidPerGroup = DEFAULT_MIN_VALID;
        private ImputationMethod imputation = ImputationMethod.PERSEUS;
        private double downshift = DEFAULT_DOWNSHIFT;
        private double width = DEFAULT_WIDTH;
        private long seed = DEFAULT_SEED;
        private double fdr = DEFAULT_FDR;
        private double minAbsLog2FoldChange = DEFAULT_MIN_ABS_LOG2_FC;

        /** Replaces the sample groups. */
        public Builder groups(Map<String, Group> groups) {
            this.groups = groups;
            return this;
        }

        /** Sets whether contaminant rows are dropped. */
        public Builder dropContaminants(boolean dropContaminants) {
            this.dropContaminants = dropContaminants;
            return this;
        }

        /** Sets whether reversed decoy hits are dropped. */
        public Builder dropReverse(boolean dropReverse) {
            this.dropReverse = dropReverse;
            return this;
        }

        /** Sets whether sites identified only by a modification are dropped. */
        public Builder dropOnlyBySite(boolean dropOnlyBySite) {
            this.dropOnlyBySite = dropOnlyBySite;
            return this;
        }

        /** Sets the minimum localization probability. */
        public Builder minLocalization(double minLocalization) {
            this.minLocalization = minLocalization;
            return this;
        }

        /** Sets how many measured values each group must have. */
        public Builder minValidPerGroup(int minValidPerGroup) {
            this.minValidPerGroup = minValidPerGroup;
            return this;
        }

        /** Sets the imputation strategy. */
        public Builder imputation(ImputationMethod imputation) {
            this.imputation = imputation;
            return this;
        }

        /** Sets the Perseus downshift. */
        public Builder downshift(double downshift) {
            this.downshift = downshift;
            return this;
        }

        /** Sets the Perseus width. */
        public Builder width(double width) {
            this.width = width;
            return this;
        }

        /** Sets the random seed. */
        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }

        /** Sets the false-discovery rate. */
        public Builder fdr(double fdr) {
            this.fdr = fdr;
            return this;
        }

        /** Sets the minimum absolute log2 fold change. */
        public Builder minAbsLog2FoldChange(double minAbsLog2FoldChange) {
            this.minAbsLog2FoldChange = minAbsLog2FoldChange;
            return this;
        }

        /**
         * Builds the settings, checking the numeric ranges.
         *
         * @return valid settings
         */
        public AnalysisSettings build() {
            return new AnalysisSettings(
                    groups,
                    dropContaminants,
                    dropReverse,
                    dropOnlyBySite,
                    minLocalization,
                    minValidPerGroup,
                    imputation,
                    downshift,
                    width,
                    seed,
                    fdr,
                    minAbsLog2FoldChange);
        }
    }
}

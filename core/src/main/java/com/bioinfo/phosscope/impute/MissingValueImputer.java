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

package com.bioinfo.phosscope.impute;

import com.bioinfo.phosscope.model.AnalysisSettings;
import com.bioinfo.phosscope.model.ImputationMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Replaces missing log2 intensities (Strategy chosen by {@link ImputationMethod}).
 *
 * <p>Each sample is imputed from its own observed values, which is what Perseus does. The random
 * draw walks columns from left to right and missing rows from top to bottom, so a seed always
 * rebuilds the same matrix. A sample with no observed value is left missing. A sample whose
 * observed values are all equal is filled with that value, without a random draw.
 */
public final class MissingValueImputer {

    /** Standard deviation used when a sample has a single observed value. */
    static final double SINGLE_VALUE_SD = 0.5;

    private MissingValueImputer() {}

    /**
     * Fills missing values in place.
     *
     * @param log2 rows of log2 intensities; missing entries are {@link Double#NaN}
     * @param settings method, Perseus parameters and seed
     * @return how many values were replaced
     */
    public static int impute(double[][] log2, AnalysisSettings settings) {
        return switch (settings.imputation()) {
            case NONE -> 0;
            case HALF_MINIMUM -> halfMinimum(log2);
            case PERSEUS -> perseus(log2, settings);
            default -> throw new IllegalStateException(settings.imputation().name());
        };
    }

    private static int halfMinimum(double[][] log2) {
        if (log2.length == 0) {
            return 0;
        }
        int filled = 0;
        int columns = log2[0].length;
        for (int column = 0; column < columns; column++) {
            double lowest = Double.POSITIVE_INFINITY;
            List<Integer> missing = new ArrayList<>();
            for (int row = 0; row < log2.length; row++) {
                double value = log2[row][column];
                if (Double.isFinite(value)) {
                    lowest = Math.min(lowest, value);
                } else {
                    missing.add(row);
                }
            }
            if (missing.isEmpty() || lowest == Double.POSITIVE_INFINITY) {
                continue;
            }
            for (int row : missing) {
                log2[row][column] = lowest - 1;
                filled++;
            }
        }
        return filled;
    }

    private static int perseus(double[][] log2, AnalysisSettings settings) {
        if (log2.length == 0) {
            return 0;
        }
        Random random = new Random(settings.seed());
        int filled = 0;
        int columns = log2[0].length;
        for (int column = 0; column < columns; column++) {
            filled += fillColumn(log2, column, settings, random);
        }
        return filled;
    }

    private static int fillColumn(
            double[][] log2, int column, AnalysisSettings settings, Random random) {
        List<Integer> missing = new ArrayList<>();
        double sum = 0;
        int observed = 0;
        for (int row = 0; row < log2.length; row++) {
            double value = log2[row][column];
            if (Double.isFinite(value)) {
                sum += value;
                observed++;
            } else {
                missing.add(row);
            }
        }
        if (missing.isEmpty() || observed == 0) {
            return 0;
        }
        double mean = sum / observed;
        double deviation = observed == 1 ? SINGLE_VALUE_SD : sampleDeviation(log2, column, mean);
        boolean draw = observed == 1 || deviation > 0;
        int filled = 0;
        for (int row : missing) {
            double noise = 0;
            if (draw) {
                noise = settings.width() * deviation * random.nextGaussian();
            }
            log2[row][column] = mean - settings.downshift() * deviation + noise;
            filled++;
        }
        return filled;
    }

    private static double sampleDeviation(double[][] log2, int column, double mean) {
        double sum = 0;
        int observed = 0;
        for (double[] row : log2) {
            double value = row[column];
            if (Double.isFinite(value)) {
                double delta = value - mean;
                sum += delta * delta;
                observed++;
            }
        }
        return Math.sqrt(sum / (observed - 1));
    }
}

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

package com.bioinfo.phosscope.stats;

import java.util.Arrays;

/**
 * Benjamini–Hochberg false-discovery rate.
 *
 * <p>Adjusted values are forced to be non-decreasing when the p-values are sorted, and they never
 * exceed 1. The result is aligned with the input: {@code adjusted[i]} belongs to {@code
 * pValues[i]}.
 */
public final class BenjaminiHochberg {

    private BenjaminiHochberg() {}

    /**
     * Adjusts a list of p-values.
     *
     * @param pValues raw two-sided p-values, between 0 and 1
     * @return adjusted q-values in the same order; an empty input gives an empty array
     */
    public static double[] adjust(double[] pValues) {
        if (pValues == null || pValues.length == 0) {
            return new double[0];
        }
        int count = pValues.length;
        Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (left, right) -> Double.compare(pValues[left], pValues[right]));
        double[] adjusted = new double[count];
        double running = 1;
        for (int rankFromEnd = count - 1; rankFromEnd >= 0; rankFromEnd--) {
            int index = order[rankFromEnd];
            double raw = pValues[index] * count / (rankFromEnd + 1.0);
            if (!Double.isFinite(raw) || raw < 0) {
                raw = 1;
            }
            running = Math.min(running, raw);
            adjusted[index] = Math.min(1, running);
        }
        return adjusted;
    }
}

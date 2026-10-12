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

/**
 * Two-sample Welch t-test, which does not assume that the two groups share a variance.
 *
 * <p>Each group needs at least two values. When both variances are zero, the p-value is 0 if the
 * means differ and 1 if they are equal: there is no noise left to explain the difference.
 */
public final class WelchTest {

    private WelchTest() {}

    /**
     * Comparison of one site.
     *
     * @param controlMean mean of the control group
     * @param diseaseMean mean of the disease group
     * @param log2FoldChange disease mean minus control mean
     * @param tStatistic Welch statistic, positive when the disease mean is higher
     * @param degreesOfFreedom Welch–Satterthwaite degrees of freedom
     * @param pValue two-sided p-value
     */
    public record Result(
            double controlMean,
            double diseaseMean,
            double log2FoldChange,
            double tStatistic,
            double degreesOfFreedom,
            double pValue) {}

    /**
     * Compares two groups of log2 intensities.
     *
     * @param control at least two finite values
     * @param disease at least two finite values
     * @return the fold change, the statistic and the two-sided p-value
     */
    public static Result compare(double[] control, double[] disease) {
        if (control == null || disease == null || control.length < 2 || disease.length < 2) {
            throw new IllegalArgumentException("NEED_REPLICATES");
        }
        double meanControl = mean(control);
        double meanDisease = mean(disease);
        double foldChange = meanDisease - meanControl;
        double varControl = variance(control, meanControl);
        double varDisease = variance(disease, meanDisease);
        double controlTerm = varControl / control.length;
        double diseaseTerm = varDisease / disease.length;
        double standardError2 = controlTerm + diseaseTerm;
        if (standardError2 == 0) {
            double pValue = foldChange == 0 ? 1 : 0;
            double statistic =
                    foldChange == 0 ? 0 : Math.copySign(Double.POSITIVE_INFINITY, foldChange);
            return new Result(
                    meanControl,
                    meanDisease,
                    foldChange,
                    statistic,
                    control.length + disease.length - 2.0,
                    pValue);
        }
        double statistic = foldChange / Math.sqrt(standardError2);
        double freedom =
                (standardError2 * standardError2)
                        / (square(controlTerm) / (control.length - 1)
                                + square(diseaseTerm) / (disease.length - 1));
        double pValue = Distributions.studentTwoTailed(Math.abs(statistic), freedom);
        return new Result(meanControl, meanDisease, foldChange, statistic, freedom, pValue);
    }

    private static double mean(double[] values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private static double variance(double[] values, double mean) {
        double sum = 0;
        for (double value : values) {
            double delta = value - mean;
            sum += delta * delta;
        }
        return sum / (values.length - 1);
    }

    private static double square(double value) {
        return value * value;
    }
}

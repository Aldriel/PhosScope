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
 * Log-gamma, regularized incomplete beta and the two-sided Student t probability.
 *
 * <p>The log-gamma uses the Lanczos approximation. The incomplete beta uses the continued fraction
 * of Numerical Recipes. Both are checked in the tests against known values (for example the Student
 * critical value 2.228 for 10 degrees of freedom).
 */
public final class Distributions {

    private static final double[] LANCZOS = {
        0.99999999999980993,
        676.5203681218851,
        -1259.1392167224028,
        771.32342877765313,
        -176.61502916214059,
        12.507343278686905,
        -0.13857109526572012,
        9.9843695780195716e-6,
        1.5056327351493116e-7
    };

    private static final int LANCZOS_G = 7;

    private Distributions() {}

    /**
     * Natural logarithm of the gamma function.
     *
     * @param z a positive number
     * @return {@code ln Γ(z)}
     */
    public static double logGamma(double z) {
        if (z < 0.5) {
            return Math.log(Math.PI / Math.sin(Math.PI * z)) - logGamma(1 - z);
        }
        z -= 1;
        double x = LANCZOS[0];
        for (int i = 1; i < LANCZOS.length; i++) {
            x += LANCZOS[i] / (z + i);
        }
        double t = z + LANCZOS_G + 0.5;
        return 0.5 * Math.log(2 * Math.PI) + (z + 0.5) * Math.log(t) - t + Math.log(x);
    }

    /**
     * Regularized incomplete beta {@code I_x(a, b)}.
     *
     * @param x argument between 0 and 1
     * @param a first shape, positive
     * @param b second shape, positive
     * @return the probability
     */
    public static double regularizedBeta(double x, double a, double b) {
        if (x <= 0) {
            return 0;
        }
        if (x >= 1) {
            return 1;
        }
        double logFront = a * Math.log(x) + b * Math.log(1 - x) - logBeta(a, b);
        double front = Math.exp(logFront);
        if (x < (a + 1) / (a + b + 2)) {
            return front * continuedFraction(a, b, x) / a;
        }
        return 1 - front * continuedFraction(b, a, 1 - x) / b;
    }

    /**
     * Two-sided tail probability of a Student t variable.
     *
     * @param tAbs absolute value of the statistic
     * @param degreesOfFreedom degrees of freedom, positive
     * @return {@code P(|T| >= t)}
     */
    public static double studentTwoTailed(double tAbs, double degreesOfFreedom) {
        if (tAbs <= 0 || !Double.isFinite(tAbs)) {
            return tAbs <= 0 ? 1 : 0;
        }
        if (!Double.isFinite(degreesOfFreedom) || degreesOfFreedom <= 0) {
            return 1;
        }
        double x = degreesOfFreedom / (degreesOfFreedom + tAbs * tAbs);
        return regularizedBeta(x, degreesOfFreedom / 2.0, 0.5);
    }

    private static double logBeta(double a, double b) {
        return logGamma(a) + logGamma(b) - logGamma(a + b);
    }

    /** Continued fraction of the incomplete beta. The caller multiplies it by the front factor. */
    private static double continuedFraction(double a, double b, double x) {
        double qab = a + b;
        double qap = a + 1;
        double qam = a - 1;
        double c = 1;
        double d = 1 - qab * x / qap;
        if (Math.abs(d) < 1e-30) {
            d = 1e-30;
        }
        d = 1 / d;
        double h = d;
        for (int m = 1; m <= 200; m++) {
            int twice = 2 * m;
            double aa = m * (b - m) * x / ((qam + twice) * (a + twice));
            d = 1 + aa * d;
            if (Math.abs(d) < 1e-30) {
                d = 1e-30;
            }
            c = 1 + aa / c;
            if (Math.abs(c) < 1e-30) {
                c = 1e-30;
            }
            d = 1 / d;
            h *= d * c;
            aa = -(a + m) * (qab + m) * x / ((a + twice) * (qap + twice));
            d = 1 + aa * d;
            if (Math.abs(d) < 1e-30) {
                d = 1e-30;
            }
            c = 1 + aa / c;
            if (Math.abs(c) < 1e-30) {
                c = 1e-30;
            }
            d = 1 / d;
            double delta = d * c;
            h *= delta;
            if (Math.abs(delta - 1) < 3e-12) {
                break;
            }
        }
        return h;
    }
}

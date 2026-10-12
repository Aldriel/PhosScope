package com.bioinfo.phosscope.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Reference values for the gamma function, the Student law, Welch and Benjamini–Hochberg. */
class StatsTest {

    @Test
    void logGammaMatchesKnownFactorials() {
        assertEquals(0, Distributions.logGamma(1), 1e-9);
        assertEquals(0, Distributions.logGamma(2), 1e-9);
        assertEquals(Math.log(24), Distributions.logGamma(5), 1e-8);
        assertTrue(Double.isFinite(Distributions.logGamma(0.3)));
    }

    @Test
    void regularizedBetaOfOneAndOneIsTheArgument() {
        assertEquals(0, Distributions.regularizedBeta(0, 1, 1), 0);
        assertEquals(1, Distributions.regularizedBeta(1, 1, 1), 0);
        assertEquals(0.5, Distributions.regularizedBeta(0.5, 1, 1), 1e-9);
        assertEquals(0, Distributions.regularizedBeta(-1, 2, 2), 0);
    }

    @Test
    void studentCriticalValueForTenDegreesIsAboutFivePercent() {
        assertEquals(1, Distributions.studentTwoTailed(0, 10), 0);
        assertEquals(1, Distributions.studentTwoTailed(-1, 10), 0);
        assertEquals(0, Distributions.studentTwoTailed(Double.POSITIVE_INFINITY, 10), 0);
        assertEquals(0, Distributions.studentTwoTailed(Double.NaN, 10), 0);
        assertEquals(1, Distributions.studentTwoTailed(2, 0), 0);
        assertEquals(1, Distributions.studentTwoTailed(2, Double.NaN), 0);
        assertEquals(0.05, Distributions.studentTwoTailed(2.228138851986, 10), 2e-4);
    }

    @Test
    void welchOnShiftedTriplicates() {
        WelchTest.Result result = WelchTest.compare(new double[] {1, 2, 3}, new double[] {4, 5, 6});
        assertEquals(2, result.controlMean(), 1e-12);
        assertEquals(5, result.diseaseMean(), 1e-12);
        assertEquals(3, result.log2FoldChange(), 1e-12);
        assertEquals(4, result.degreesOfFreedom(), 1e-9);
        assertEquals(3.674234614, result.tStatistic(), 1e-6);
        assertTrue(result.pValue() > 0.01 && result.pValue() < 0.05);
    }

    @Test
    void welchWithNoVarianceIsDecisive() {
        WelchTest.Result same = WelchTest.compare(new double[] {2, 2}, new double[] {2, 2});
        assertEquals(1, same.pValue(), 0);
        assertEquals(0, same.tStatistic(), 0);
        WelchTest.Result different = WelchTest.compare(new double[] {1, 1}, new double[] {4, 4});
        assertEquals(0, different.pValue(), 0);
        assertEquals(Double.POSITIVE_INFINITY, different.tStatistic());
        WelchTest.Result lower = WelchTest.compare(new double[] {4, 4}, new double[] {1, 1});
        assertEquals(Double.NEGATIVE_INFINITY, lower.tStatistic());
    }

    @Test
    void welchRejectsAGroupWithoutAVariance() {
        assertThrows(
                IllegalArgumentException.class, () -> WelchTest.compare(null, new double[] {1, 2}));
        assertThrows(
                IllegalArgumentException.class,
                () -> WelchTest.compare(new double[] {1}, new double[] {1, 2}));
        assertThrows(
                IllegalArgumentException.class, () -> WelchTest.compare(new double[] {1, 2}, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> WelchTest.compare(new double[] {1, 2}, new double[] {1}));
    }

    @Test
    void benjaminiHochbergKeepsTheOrderAndTheMonotonicity() {
        assertEquals(0, BenjaminiHochberg.adjust(null).length);
        assertEquals(0, BenjaminiHochberg.adjust(new double[0]).length);
        double[] adjusted = BenjaminiHochberg.adjust(new double[] {0.01, 0.04, 0.03, 0.20});
        assertEquals(0.04, adjusted[0], 1e-12);
        assertEquals(0.053333333333, adjusted[1], 1e-9);
        assertEquals(0.053333333333, adjusted[2], 1e-9);
        assertEquals(0.20, adjusted[3], 1e-12);
        double[] equal = BenjaminiHochberg.adjust(new double[] {0.05, 0.05, 0.05, 0.05});
        for (double q : equal) {
            assertEquals(0.05, q, 1e-12);
        }
        assertEquals(0, BenjaminiHochberg.adjust(new double[] {0})[0], 0);
        assertEquals(1, BenjaminiHochberg.adjust(new double[] {-1})[0], 0);
        assertEquals(1, BenjaminiHochberg.adjust(new double[] {Double.NaN})[0], 0);
    }
}

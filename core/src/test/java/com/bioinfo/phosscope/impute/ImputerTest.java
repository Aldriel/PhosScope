package com.bioinfo.phosscope.impute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.phosscope.model.AnalysisSettings;
import com.bioinfo.phosscope.model.ImputationMethod;

import org.junit.jupiter.api.Test;

import java.util.Random;

/** The three imputation strategies, including the reproducible Perseus draw. */
class ImputerTest {

    @Test
    void perseusDrawsBelowTheMeanWithTheGivenSeed() {
        double[][] matrix = {{1}, {2}, {3}, {Double.NaN}};
        AnalysisSettings settings =
                new AnalysisSettings.Builder().imputation(ImputationMethod.PERSEUS).seed(1).build();
        assertEquals(1, MissingValueImputer.impute(matrix, settings));
        Random random = new Random(1);
        double expected = 2 - 1.8 + 0.3 * random.nextGaussian();
        assertEquals(expected, matrix[3][0], 1e-12);
    }

    @Test
    void equalValuesAreCopiedWithoutADraw() {
        double[][] matrix = {{5}, {5}, {Double.NaN}, {Double.NaN}};
        AnalysisSettings settings = new AnalysisSettings.Builder().seed(3).build();
        assertEquals(2, MissingValueImputer.impute(matrix, settings));
        assertEquals(5, matrix[2][0], 0);
        assertEquals(5, matrix[3][0], 0);
    }

    @Test
    void aSingleObservedValueUsesTheFallbackWidth() {
        double[][] matrix = {{5}, {Double.NaN}};
        AnalysisSettings settings =
                new AnalysisSettings.Builder().downshift(1.8).width(0.3).seed(2).build();
        MissingValueImputer.impute(matrix, settings);
        Random random = new Random(2);
        double expected = 5 - 1.8 * 0.5 + 0.3 * 0.5 * random.nextGaussian();
        assertEquals(expected, matrix[1][0], 1e-12);
    }

    @Test
    void halfMinimumAndNoneAndEmptyMatrix() {
        double[][] half = {{4, Double.NaN}, {6, 8}, {Double.NaN, 9}};
        AnalysisSettings halfSettings =
                new AnalysisSettings.Builder().imputation(ImputationMethod.HALF_MINIMUM).build();
        assertEquals(2, MissingValueImputer.impute(half, halfSettings));
        assertEquals(3, half[2][0], 0);
        assertEquals(7, half[0][1], 0);
        double[][] unchanged = {{1}, {Double.NaN}};
        AnalysisSettings none =
                new AnalysisSettings.Builder().imputation(ImputationMethod.NONE).build();
        assertEquals(0, MissingValueImputer.impute(unchanged, none));
        assertTrue(Double.isNaN(unchanged[1][0]));
        double[][] emptyColumn = {{Double.NaN}, {Double.NaN}};
        assertEquals(0, MissingValueImputer.impute(emptyColumn, halfSettings));
        assertEquals(0, MissingValueImputer.impute(new double[0][], halfSettings));
        assertEquals(0, MissingValueImputer.impute(new double[0][], settingsPerseus()));
        double[][] complete = {{1, 2}};
        assertEquals(0, MissingValueImputer.impute(complete, halfSettings));
        double[][] oneHole = {{1, 5}, {2, 5}, {Double.NaN, 5}};
        assertEquals(1, MissingValueImputer.impute(oneHole, settingsPerseus()));
    }

    private static AnalysisSettings settingsPerseus() {
        return new AnalysisSettings.Builder().build();
    }
}

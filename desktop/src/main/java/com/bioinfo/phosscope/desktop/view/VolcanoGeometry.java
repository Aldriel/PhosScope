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

package com.bioinfo.phosscope.desktop.view;

/**
 * Maps a fold change and a {@code -log10(p)} onto a volcano plot of a given size.
 *
 * <p>The horizontal axis runs from {@code -maxFold} to {@code +maxFold}. A point past that range is
 * drawn on the border. The vertical axis runs from 0 to {@code maxHeight}.
 */
public final class VolcanoGeometry {

    /** Largest absolute log2 fold change drawn inside the plot; larger values sit on the border. */
    public static final double MAX_FOLD = 4;

    /** Largest {@code -log10(p)} drawn inside the plot. */
    public static final double MAX_HEIGHT = 8;

    private VolcanoGeometry() {}

    /**
     * Horizontal position, in pixels.
     *
     * @param log2FoldChange fold change
     * @param width plot width
     * @return the x coordinate, between 0 and {@code width}
     */
    public static double x(double log2FoldChange, double width) {
        double limited = Math.clamp(log2FoldChange, -MAX_FOLD, MAX_FOLD);
        return (limited + MAX_FOLD) / (2 * MAX_FOLD) * width;
    }

    /**
     * Vertical position, in pixels, from the top.
     *
     * @param negLog10P {@code -log10(p)}
     * @param height plot height
     * @return the y coordinate, between 0 and {@code height}
     */
    public static double y(double negLog10P, double height) {
        double limited = Math.clamp(negLog10P, 0, MAX_HEIGHT);
        return height - limited / MAX_HEIGHT * height;
    }
}

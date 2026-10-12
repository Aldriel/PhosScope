package com.bioinfo.phosscope.desktop.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The volcano axes keep extreme points inside the plot. */
class VolcanoGeometryTest {

    @Test
    void axesAreClamped() {
        assertEquals(250, VolcanoGeometry.x(0, 500), 1e-9);
        assertEquals(500, VolcanoGeometry.x(100, 500), 1e-9);
        assertEquals(0, VolcanoGeometry.x(-100, 500), 1e-9);
        assertEquals(200, VolcanoGeometry.y(0, 200), 1e-9);
        assertEquals(0, VolcanoGeometry.y(100, 200), 1e-9);
    }
}

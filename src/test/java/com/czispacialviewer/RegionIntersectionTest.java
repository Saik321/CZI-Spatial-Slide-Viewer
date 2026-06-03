package com.czispacialviewer;

import com.czispacialviewer.util.RegionIntersection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionIntersectionTest {

    @Test
    void testIntersectingRegions() {
        assertTrue(RegionIntersection.intersects(0, 0, 10, 10, 5, 5, 10, 10));
        assertFalse(RegionIntersection.intersects(0, 0, 10, 10, 20, 20, 5, 5));
    }

    @Test
    void testTouchingEdgesDoNotIntersect() {
        assertFalse(RegionIntersection.intersects(0, 0, 10, 10, 10, 0, 5, 5));
        assertFalse(RegionIntersection.intersects(0, 0, 10, 10, 0, 10, 5, 5));
    }

    @Test
    void testIntersectionRectangle() {
        var intersection = RegionIntersection.intersection(0, 0, 10, 10, 5, 5, 10, 10);
        assertNotNull(intersection);
        assertTrue(intersection.getWidth() > 0);
        assertTrue(intersection.getHeight() > 0);
    }
}

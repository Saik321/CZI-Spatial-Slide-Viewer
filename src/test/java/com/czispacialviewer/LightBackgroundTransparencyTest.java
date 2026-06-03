package com.czispacialviewer;

import com.czispacialviewer.util.LightBackgroundTransparency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightBackgroundTransparencyTest {

    @Test
    void detectsLightNeutralBackground() {
        assertTrue(LightBackgroundTransparency.isLightNeutralBackground(225, 226, 224, 178, 52));
        assertTrue(LightBackgroundTransparency.isLightNeutralBackground(180, 190, 185, 178, 52));
    }

    @Test
    void keepsBlueTissueOpaque() {
        assertFalse(LightBackgroundTransparency.isLightNeutralBackground(35, 72, 190, 178, 52));
        assertFalse(LightBackgroundTransparency.isLightNeutralBackground(185, 170, 215, 178, 52));
    }
}

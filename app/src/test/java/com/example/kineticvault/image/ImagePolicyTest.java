package com.example.kineticvault.image;

import com.example.kineticvault.core.Constants;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class ImagePolicyTest {
    @Test public void coverFitsBothPixelCountAndLongSide() throws Exception {
        int[] size = ImageMemory.fitCover(8000, 7000, Constants.MAX_PIXELS);
        assertTrue((long) size[0] * size[1] <= Constants.MAX_PIXELS);
        assertTrue(Math.max(size[0], size[1]) <= 3000);
        assertEquals(8.0 / 7.0, size[0] / (double) size[1], 0.002);
    }

    @Test public void smallerBudgetLimitsPixelsEvenBelowLongSideLimit() throws Exception {
        int[] size = ImageMemory.fitCover(2500, 2500, 1_000_000);
        assertArrayEquals(new int[]{1000, 1000}, size);
    }

    @Test public void narrowAndExtremeDimensionsCannotOverflow() throws Exception {
        int[] size = ImageMemory.fitCover(Integer.MAX_VALUE, 1, 100);
        assertTrue((long) size[0] * size[1] <= 100);
        assertTrue(size[0] > 0 && size[1] > 0);
        assertThrows(IOException.class, () -> ImageMemory.pixelCount(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test public void coreLimitIsExact() throws Exception {
        assertEquals(9_000_000, ImageMemory.pixelCount(3000, 3000));
        assertThrows(IOException.class, () -> ImageMemory.pixelCount(3001, 3000));
        assertThrows(IOException.class, () -> ImageMemory.pixelCount(0, 100));
    }

    @Test public void smallCoverIsNotUpscaled() throws Exception {
        assertArrayEquals(new int[]{320, 180}, ImageMemory.fitCover(320, 180, 9_000_000));
    }

    @Test public void ghostBudgetIncludesItsShuffleArray() throws Exception {
        long budget = 96L * 1024 * 1024;
        assertTrue(ImageMemory.maximumPixels(true, budget) < ImageMemory.maximumPixels(false, budget));
        assertEquals(4_000_000L, ImageMemory.requiredBytes(1_000_000, true)
                - ImageMemory.requiredBytes(1_000_000, false));
        assertThrows(IOException.class, () -> ImageMemory.maximumPixels(true, 0));
    }

    @Test public void tinyInvalidPlansAreRejected() {
        assertThrows(IOException.class, () -> ImageMemory.fitCover(0, 5, 100));
        assertThrows(IOException.class, () -> ImageMemory.fitCover(5, 5, 0));
    }

    @Test public void unchangedImageHasInfinitePsnr() {
        ImageStats stats = ImageStats.compare(new int[]{0xff123456}, new int[]{0xff123456});
        assertEquals(Double.POSITIVE_INFINITY, stats.psnrDb, 0);
        assertEquals(0, stats.pixelsChanged);
        assertEquals(0, stats.maxChannelChange);
    }

    @Test public void knownOneLevelChangeHasCorrectPsnr() {
        ImageStats stats = ImageStats.compare(new int[]{0xff000000}, new int[]{0xff010101});
        assertEquals(48.1308036, stats.psnrDb, 0.000001);
        assertEquals(1, stats.maxChannelChange);
        assertEquals(100.0, stats.changedPercent, 0);
    }

    @Test public void changedPixelsCountOnceAcrossThreeChannels() {
        ImageStats stats = ImageStats.compare(new int[]{0xff000000, 0xff000000},
                new int[]{0xff010101, 0xff000000});
        assertEquals(1, stats.pixelsChanged);
        assertEquals(50.0, stats.changedPercent, 0);
    }

    @Test public void squaredErrorAccumulatorDoesNotOverflow() {
        int[] black = new int[100_000];
        int[] white = new int[100_000];
        Arrays.fill(white, 0x00ffffff);
        ImageStats stats = ImageStats.compare(black, white);
        assertEquals(0.0, stats.psnrDb, 0);
        assertEquals(255, stats.maxChannelChange);
    }

    @Test public void unequalPixelArraysAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ImageStats.compare(new int[1], new int[2]));
        assertThrows(IllegalArgumentException.class, () -> ImageStats.compare(new int[0], new int[0]));
    }
}

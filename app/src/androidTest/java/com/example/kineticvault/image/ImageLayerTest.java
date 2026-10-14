package com.example.kineticvault.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import androidx.core.content.FileProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.kineticvault.diagnostics.HarnessReport;
import com.example.kineticvault.diagnostics.HarnessRunner;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ImageLayerTest {
    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test public void allFourPngRoundTripsPass() throws Exception {
        try (HarnessReport report = HarnessRunner.run(context(), null)) {
            assertEquals(4, report.entries.size());
            for (HarnessReport.Entry entry : report.entries) {
                assertEquals(64, entry.sha256.length());
                assertEquals(1, entry.stats.maxChannelChange);
                assertTrue(entry.stats.psnrDb > 40);
            }
        }
    }

    @Test public void alphaIsFlattenedOnlyForCovers() throws Exception {
        Bitmap transparent = Bitmap.createBitmap(32, 16, Bitmap.Config.ARGB_8888);
        transparent.eraseColor(0x80123456);
        byte[] bytes;
        try { bytes = encode(transparent, Bitmap.CompressFormat.PNG); }
        finally { transparent.recycle(); }
        ImageUtils.Source source = () -> new ByteArrayInputStream(bytes);
        Bitmap cover = ImageUtils.decodeCover(source, false);
        try {
            assertEquals(Bitmap.Config.ARGB_8888, cover.getConfig());
            assertEquals(ColorSpace.get(ColorSpace.Named.SRGB), cover.getColorSpace());
            assertTrue(cover.isMutable());
            for (int pixel : ImageUtils.pixels(cover)) { assertEquals(255, pixel >>> 24); }
        } finally { cover.recycle(); }
        assertThrows(IOException.class, () -> ImageUtils.decodeExact(source, false));
    }

    @Test public void oversizedCoverScalesButExactDecodeRejectsIt() throws Exception {
        byte[] bytes = PngFixtures.opaque(4000, 2500);
        ImageUtils.Source source = () -> new ByteArrayInputStream(bytes);
        Bitmap cover = ImageUtils.decodeCover(source, true);
        try {
            assertTrue((long) cover.getWidth() * cover.getHeight() <= 9_000_000);
            assertTrue(Math.max(cover.getWidth(), cover.getHeight()) <= 3000);
        } finally { cover.recycle(); }
        assertThrows(IOException.class, () -> ImageUtils.decodeExact(source, true));
    }

    @Test public void exactDecodeDoesNotApplyCoverLongSideLimit() throws Exception {
        byte[] bytes = PngFixtures.opaque(3001, 1);
        Bitmap exact = ImageUtils.decodeExact(() -> new ByteArrayInputStream(bytes), false);
        try { assertEquals(3001, exact.getWidth()); assertEquals(1, exact.getHeight()); }
        finally { exact.recycle(); }
    }

    @Test public void thumbnailIsBoundedAndSoftware() throws Exception {
        Bitmap cover = CoverGenerator.generate(128, 64, 42);
        Bitmap thumb = null;
        try {
            thumb = ImageUtils.thumbnail(cover, 32);
            assertEquals(32, thumb.getWidth());
            assertEquals(16, thumb.getHeight());
            assertEquals(Bitmap.Config.ARGB_8888, thumb.getConfig());
        } finally {
            cover.recycle();
            if (thumb != null) { thumb.recycle(); }
        }
    }

    @Test public void sha256MatchesKnownBytes() throws Exception {
        File file = File.createTempFile("hash-", ".bin", context().getCacheDir());
        try {
            Files.write(file.toPath(), new byte[]{97, 98, 99});
            assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                    ImageUtils.sha256(file));
        } finally { Files.deleteIfExists(file.toPath()); }
    }

    @Test public void fileProviderOnlyExposesSharedSubdirectory() throws Exception {
        File folder = new File(context().getCacheDir(), "shared");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        File file = File.createTempFile("provider-", ".png", folder);
        try {
            assertEquals("content", FileProvider.getUriForFile(context(),
                    context().getPackageName() + ".files", file).getScheme());
            File privateFile = new File(context().getFilesDir(), "private.bin");
            assertThrows(IllegalArgumentException.class, () -> FileProvider.getUriForFile(
                    context(), context().getPackageName() + ".files", privateFile));
        } finally { Files.deleteIfExists(file.toPath()); }
    }

    @Test public void jpegExifOrientationIsNotApplied() throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(31, 17, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(0xff123456);
        byte[] jpeg;
        try { jpeg = encode(bitmap, Bitmap.CompressFormat.JPEG); }
        finally { bitmap.recycle(); }
        byte[] exif = { (byte) 255, (byte) 225, 0, 34, 69, 120, 105, 102, 0, 0,
                73, 73, 42, 0, 8, 0, 0, 0, 1, 0, 18, 1, 3, 0, 1, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0 };
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        encoded.write(jpeg, 0, 2);
        encoded.write(exif);
        encoded.write(jpeg, 2, jpeg.length - 2);
        byte[] bytes = encoded.toByteArray();
        Bitmap decoded = ImageUtils.decodeExact(() -> new ByteArrayInputStream(bytes), false);
        try { assertEquals(31, decoded.getWidth()); assertEquals(17, decoded.getHeight()); }
        finally { decoded.recycle(); }
    }

    @Test public void malformedInputFailsCleanly() {
        assertThrows(IOException.class, () -> ImageUtils.decodeExact(
                () -> new ByteArrayInputStream(new byte[]{1, 2, 3}), false));
    }

    @Test public void mainThreadImageWorkIsRejected() {
        java.util.concurrent.atomic.AtomicReference<Throwable> result =
                new java.util.concurrent.atomic.AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try { ImageUtils.requireWorker(); }
            catch (Throwable failure) { result.set(failure); }
        });
        assertTrue(result.get() instanceof IllegalStateException);
    }

    private byte[] encode(Bitmap bitmap, Bitmap.CompressFormat format) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!bitmap.compress(format, 100, output)) { throw new IOException("Fixture encoding failed"); }
        return output.toByteArray();
    }
}

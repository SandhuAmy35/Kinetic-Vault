package com.example.kineticvault.image;

public final class ImageStats {
    public final int pixelsChanged;
    public final double changedPercent;
    public final int maxChannelChange;
    public final double psnrDb;

    private ImageStats(int changed, int count, int maximum, long squaredError) {
        pixelsChanged = changed;
        changedPercent = 100.0 * changed / count;
        maxChannelChange = maximum;
        double meanSquaredError = squaredError / (count * 3.0);
        psnrDb = squaredError == 0 ? Double.POSITIVE_INFINITY
                : 10.0 * Math.log10(255.0 * 255.0 / meanSquaredError);
    }

    public static ImageStats compare(int[] before, int[] after) {
        if (before == null || after == null || before.length == 0 || before.length != after.length) {
            throw new IllegalArgumentException("Pixel arrays must have equal nonzero length");
        }
        int changed = 0;
        int maximum = 0;
        long squaredError = 0;
        for (int i = 0; i < before.length; i++) {
            if (((before[i] ^ after[i]) & 0x00ffffff) != 0) { changed++; }
            for (int shift = 0; shift <= 16; shift += 8) {
                int difference = ((before[i] >>> shift) & 255) - ((after[i] >>> shift) & 255);
                maximum = Math.max(maximum, Math.abs(difference));
                squaredError += (long) difference * difference;
            }
        }
        return new ImageStats(changed, before.length, maximum, squaredError);
    }
}

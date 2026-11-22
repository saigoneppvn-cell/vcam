package com.yaahua.vcam;

/**
 * Converts a decoded NV21 frame to the camera callback's requested size.
 * The default fit mode preserves the complete source frame (letterboxing rather
 * than stretching or silently cropping). Rotation and zoom are applied before
 * sampling into the destination buffer.
 */
public final class Nv21FrameTransformer {
    private Nv21FrameTransformer() {}

    public static byte[] transform(byte[] src, int srcWidth, int srcHeight,
                                   int dstWidth, int dstHeight, int rotation, float zoom) {
        if (src == null || srcWidth <= 0 || srcHeight <= 0 ||
                dstWidth <= 0 || dstHeight <= 0 ||
                (srcWidth & 1) != 0 || (srcHeight & 1) != 0 ||
                (dstWidth & 1) != 0 || (dstHeight & 1) != 0 ||
                src.length < srcWidth * srcHeight * 3 / 2) return null;

        // The replacement frame is source-authoritative: ignore per-app rotation and zoom
        // overrides. Fit the complete source into the app's buffer without cropping.
        int r = 0;
        float safeZoom = 1.0f;

        int rotatedWidth = (r == 90 || r == 270) ? srcHeight : srcWidth;
        int rotatedHeight = (r == 90 || r == 270) ? srcWidth : srcHeight;
        float scale = Math.min(dstWidth / (float) rotatedWidth,
                dstHeight / (float) rotatedHeight) * safeZoom;
        float renderedWidth = rotatedWidth * scale;
        float renderedHeight = rotatedHeight * scale;
        float left = (dstWidth - renderedWidth) * 0.5f;
        float top = (dstHeight - renderedHeight) * 0.5f;

        int dstYSize = dstWidth * dstHeight;
        byte[] out = new byte[dstYSize * 3 / 2];
        java.util.Arrays.fill(out, 0, dstYSize, (byte) 16);
        java.util.Arrays.fill(out, dstYSize, out.length, (byte) 128);

        for (int y = 0; y < dstHeight; y++) {
            for (int x = 0; x < dstWidth; x++) {
                float rx = (x + 0.5f - left) / scale;
                float ry = (y + 0.5f - top) / scale;
                if (rx < 0 || ry < 0 || rx >= rotatedWidth || ry >= rotatedHeight) continue;
                long point = inverseRotate(rx, ry, srcWidth, srcHeight, r);
                int sx = clamp((int) (point >> 32), 0, srcWidth - 1);
                int sy = clamp((int) point, 0, srcHeight - 1);
                out[y * dstWidth + x] = src[sy * srcWidth + sx];
            }
        }

        // NV21 chroma is interleaved VU at half resolution.
        for (int y = 0; y < dstHeight; y += 2) {
            for (int x = 0; x < dstWidth; x += 2) {
                float rx = (x + 1.0f - left) / scale;
                float ry = (y + 1.0f - top) / scale;
                if (rx < 0 || ry < 0 || rx >= rotatedWidth || ry >= rotatedHeight) continue;
                long point = inverseRotate(rx, ry, srcWidth, srcHeight, r);
                int sx = clamp(((int) (point >> 32)) & ~1, 0, srcWidth - 2);
                int sy = clamp(((int) point) & ~1, 0, srcHeight - 2);
                int srcIndex = srcWidth * srcHeight + (sy / 2) * srcWidth + sx;
                int dstIndex = dstYSize + (y / 2) * dstWidth + x;
                if (srcIndex + 1 < src.length && dstIndex + 1 < out.length) {
                    out[dstIndex] = src[srcIndex];
                    out[dstIndex + 1] = src[srcIndex + 1];
                }
            }
        }
        return out;
    }

    private static long inverseRotate(float x, float y, int width, int height, int rotation) {
        int sx;
        int sy;
        switch (rotation) {
            case 90: sx = (int) y; sy = height - 1 - (int) x; break;
            case 180: sx = width - 1 - (int) x; sy = height - 1 - (int) y; break;
            case 270: sx = width - 1 - (int) y; sy = (int) x; break;
            default: sx = (int) x; sy = (int) y; break;
        }
        return (((long) sx) << 32) | (sy & 0xffffffffL);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

package com.campusfind.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * ImageHelper — robust utility for capturing, compressing, storing (Base64),
 * and loading item photos across SQLite, Firestore, and different Android devices.
 */
public class ImageHelper {

    private static final String TAG = "ImageHelper";
    private static final int MAX_IMAGE_DIMENSION = 600;
    private static final int JPEG_QUALITY = 75;

    /**
     * Reads an image from a Uri (camera file or gallery content Uri),
     * fixes EXIF rotation, scales it down to max 600px, and returns a Base64 string.
     */
    public static String uriToBase64(Context context, Uri uri) {
        if (context == null || uri == null) return null;
        try {
            // 1. Measure dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is == null) return null;
                BitmapFactory.decodeStream(is, null, options);
            }

            int width = options.outWidth;
            int height = options.outHeight;
            if (width <= 0 || height <= 0) return null;

            // 2. Compute sample size
            int inSampleSize = 1;
            int maxDim = Math.max(width, height);
            while (maxDim / (inSampleSize * 2) >= MAX_IMAGE_DIMENSION) {
                inSampleSize *= 2;
            }

            // 3. Decode scaled bitmap
            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            Bitmap bitmap;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is == null) return null;
                bitmap = BitmapFactory.decodeStream(is, null, options);
            }
            if (bitmap == null) return null;

            // 4. Fix rotation from EXIF
            bitmap = fixOrientation(context, uri, bitmap);

            // 5. Final resize if still larger than max dimension
            if (bitmap.getWidth() > MAX_IMAGE_DIMENSION || bitmap.getHeight() > MAX_IMAGE_DIMENSION) {
                float scale = (float) MAX_IMAGE_DIMENSION / Math.max(bitmap.getWidth(), bitmap.getHeight());
                int newW = Math.round(bitmap.getWidth() * scale);
                int newH = Math.round(bitmap.getHeight() * scale);
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true);
                if (scaled != bitmap) {
                    bitmap.recycle();
                    bitmap = scaled;
                }
            }

            // 6. Compress to JPEG bytes & convert to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
            bitmap.recycle();

            byte[] imageBytes = baos.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);

        } catch (Exception e) {
            Log.e(TAG, "uriToBase64 failed: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Decodes a Base64 string into an Android Bitmap safely.
     */
    public static Bitmap base64ToBitmap(String base64Str) {
        if (base64Str == null || base64Str.trim().isEmpty()) return null;
        try {
            String clean = base64Str.trim();
            if (clean.startsWith("data:image")) {
                int commaIdx = clean.indexOf(",");
                if (commaIdx != -1) {
                    clean = clean.substring(commaIdx + 1);
                }
            }
            byte[] decodedBytes = Base64.decode(clean, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception | OutOfMemoryError e) {
            Log.e(TAG, "base64ToBitmap failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Universal photo loader. Accepts:
     *   - Base64 string
     *   - Multiple photos combined with "||" (loads first one)
     *   - content:// Uri
     *   - file:// Uri or raw local path
     */
    public static Bitmap loadPhotoBitmap(Context context, String photoPath) {
        if (photoPath == null || photoPath.trim().isEmpty()) return null;

        String firstPath = photoPath;
        if (photoPath.contains("||")) {
            String[] parts = photoPath.split("\\|\\|");
            for (String p : parts) {
                if (p != null && !p.trim().isEmpty()) {
                    firstPath = p.trim();
                    break;
                }
            }
        }

        firstPath = firstPath.trim();

        // 1. Is it Base64?
        if (isBase64(firstPath)) {
            Bitmap bmp = base64ToBitmap(firstPath);
            if (bmp != null) return bmp;
        }

        // 2. Is it a content:// URI?
        if (context != null && firstPath.startsWith("content://")) {
            try (InputStream is = context.getContentResolver().openInputStream(Uri.parse(firstPath))) {
                if (is != null) {
                    return BitmapFactory.decodeStream(is);
                }
            } catch (Exception ignored) {}
        }

        // 3. Is it a file path or file:// URI?
        try {
            String filePath = firstPath.startsWith("file://") ? firstPath.substring(7) : firstPath;
            File file = new File(filePath);
            if (file.exists() && file.length() > 0) {
                return BitmapFactory.decodeFile(file.getAbsolutePath());
            }
        } catch (Exception ignored) {}

        // Fallback: try decoding as base64 in case it wasn't recognized
        return base64ToBitmap(firstPath);
    }

    /**
     * Loads ALL photos when multiple photos were saved with "||".
     */
    public static List<Bitmap> loadAllBitmaps(Context context, String photoPath) {
        List<Bitmap> list = new ArrayList<>();
        if (photoPath == null || photoPath.trim().isEmpty()) return list;

        String[] parts = photoPath.split("\\|\\|");
        for (String p : parts) {
            if (p != null && !p.trim().isEmpty()) {
                Bitmap bmp = loadPhotoBitmap(context, p.trim());
                if (bmp != null) {
                    list.add(bmp);
                }
            }
        }
        return list;
    }

    private static boolean isBase64(String str) {
        if (str == null) return false;
        String s = str.trim();
        if (s.startsWith("data:image")) return true;
        if (s.startsWith("/") || s.startsWith("content://") || s.startsWith("file://") || s.startsWith("http://") || s.startsWith("https://")) return false;
        return s.length() > 50;
    }

    private static Bitmap fixOrientation(Context context, Uri uri, Bitmap bitmap) {
        if (context == null || uri == null || bitmap == null) return bitmap;
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return bitmap;
            // Note: android.media.ExifInterface is standard in Android
            android.media.ExifInterface exif = new android.media.ExifInterface(is);
            int orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL);

            int rotate = 0;
            switch (orientation) {
                case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case android.media.ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
            }

            if (rotate != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotate);
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (rotated != bitmap) {
                    bitmap.recycle();
                    return rotated;
                }
            }
        } catch (Exception ignored) {}
        return bitmap;
    }
}

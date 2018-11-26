package com.apptreesoftware.barcodescan;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;


class FileUtils {
    public static final List<BarcodeFormat> ALL_FORMATS = new ArrayList();

    static {
        ALL_FORMATS.add(BarcodeFormat.AZTEC);
        ALL_FORMATS.add(BarcodeFormat.CODABAR);
        ALL_FORMATS.add(BarcodeFormat.CODE_39);
        ALL_FORMATS.add(BarcodeFormat.CODE_93);
        ALL_FORMATS.add(BarcodeFormat.CODE_128);
        ALL_FORMATS.add(BarcodeFormat.DATA_MATRIX);
        ALL_FORMATS.add(BarcodeFormat.EAN_8);
        ALL_FORMATS.add(BarcodeFormat.EAN_13);
        ALL_FORMATS.add(BarcodeFormat.ITF);
        ALL_FORMATS.add(BarcodeFormat.MAXICODE);
        ALL_FORMATS.add(BarcodeFormat.PDF_417);
        ALL_FORMATS.add(BarcodeFormat.QR_CODE);
        ALL_FORMATS.add(BarcodeFormat.RSS_14);
        ALL_FORMATS.add(BarcodeFormat.RSS_EXPANDED);
        ALL_FORMATS.add(BarcodeFormat.UPC_A);
        ALL_FORMATS.add(BarcodeFormat.UPC_E);
        ALL_FORMATS.add(BarcodeFormat.UPC_EAN_EXTENSION);
    }

    public static Result analyzeBitmap(String path) {

        /**
         * 首先判断图片的大小,若图片过大,则执行图片的裁剪操作,防止OOM
         */
        //BitmapFactory.Options options = new BitmapFactory.Options();
        //options.inJustDecodeBounds = true; // 先获取原大小
        // Bitmap mBitmap = BitmapFactory.decodeFile(path, options);
        Bitmap mBitmap = BitmapFactory.decodeFile(path);
        //options.inJustDecodeBounds = false; // 获取新的大小

//        int sampleSize = (int) (options.outHeight / (float) 400);
//
//        if (sampleSize <= 0)
//            sampleSize = 1;
        //options.inSampleSize = sampleSize;
        //mBitmap = BitmapFactory.decodeFile(path, options);
        int width = mBitmap.getWidth(), height = mBitmap.getHeight();
        float scaleWidth = 250.0f / width;
        float scaleHeight = (250.0f / width) / width * height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, width, height, matrix, false);
        MultiFormatReader multiFormatReader = new MultiFormatReader();

        // 解码的参数
        //Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(2);
        Map<DecodeHintType, Object> hints = new EnumMap(DecodeHintType.class);
        // 可以解析的编码类型
        Vector<BarcodeFormat> decodeFormats = new Vector<BarcodeFormat>(EnumSet.allOf(BarcodeFormat.class));

        hints.put(DecodeHintType.POSSIBLE_FORMATS, ALL_FORMATS);
        //hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
        // 设置继续的字符编码格式为UTF8
        //hints.put(DecodeHintType.CHARACTER_SET, "UTF8");
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        //hints.put(DecodeHintType.PURE_BARCODE, Boolean.FALSE);
        // 设置解析配置参数
        multiFormatReader.setHints(hints);

        // 开始对图像资源解码
        Result rawResult = null;
        BitmapLuminanceSource source = new BitmapLuminanceSource(mBitmap);

        try {
            rawResult = multiFormatReader.decodeWithState(new BinaryBitmap(new HybridBinarizer(source)));
        } catch (Throwable e) {
            e.printStackTrace();
        }


        if (rawResult == null) {
            try {
                rawResult = multiFormatReader.decodeWithState(new BinaryBitmap(new HybridBinarizer(source.invert())));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        return rawResult;
    }


    String getPathFromUri(final Context context, final Uri uri) {
        String path = getPathFromLocalUri(context, uri);
        if (path == null) {
            path = getPathFromRemoteUri(context, uri);
        }
        return path;
    }

    @SuppressLint("NewApi")
    private String getPathFromLocalUri(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);

                if (!TextUtils.isEmpty(id)) {
                    try {
                        final Uri contentUri =
                                ContentUris.withAppendedId(
                                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                        return getDataColumn(context, contentUri, null, null);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }

            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }

            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    private static String getDataColumn(
            Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;

        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private static String getPathFromRemoteUri(final Context context, final Uri uri) {
        // The code below is why Java now has try-with-resources and the Files utility.
        File file = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        boolean success = false;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            file = File.createTempFile("image_picker", "jpg", context.getCacheDir());
            outputStream = new FileOutputStream(file);
            if (inputStream != null) {
                copy(inputStream, outputStream);
                success = true;
            }
        } catch (IOException ignored) {
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException ignored) {
            }
            try {
                if (outputStream != null) outputStream.close();
            } catch (IOException ignored) {
                // If closing the output stream fails, we cannot be sure that the
                // target file was written in full. Flushing the stream merely moves
                // the bytes into the OS, not necessarily to the file.
                success = false;
            }
        }
        return success ? file.getPath() : null;
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        final byte[] buffer = new byte[4 * 1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        out.flush();
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}

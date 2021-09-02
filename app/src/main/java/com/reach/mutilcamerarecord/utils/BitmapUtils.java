package com.reach.mutilcamerarecord.utils;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * Author：lzh on 2021/7/8 10:42
 */
public class BitmapUtils {

    private static final String TAG = "BitmapUtils";

    public static final String[] EXIF_TAGS = {
            "FNumber",
            ExifInterface.TAG_DATETIME,
            "ExposureTime",
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_FOCAL_LENGTH,
            "GPSAltitude", "GPSAltitudeRef",
            ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_PROCESSING_METHOD,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_IMAGE_LENGTH,
            ExifInterface.TAG_IMAGE_WIDTH, "ISOSpeedRatings",
            ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL,
            ExifInterface.TAG_WHITE_BALANCE,
    };

    public static Bitmap loadBitmapFromAssets(Context context, String filePath) {
        InputStream inputStream = null;
        try {
            inputStream = context.getResources().getAssets().open(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (inputStream == null) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        return bitmap;
    }
    public static Bitmap loadBitmapFromDisk(Context context, String filePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (inputStream == null) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        return bitmap;
    }

    public static Bitmap loadBitmapFromRaw(Context context, int resourceId) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
        return bitmap;
    }

    /**
     * 从Buffer中创建Bitmap
     * @param buffer
     * @param width
     * @param height
     * @return
     */
    public static Bitmap getBitmapFromBuffer(ByteBuffer buffer, int width, int height) {
        return getBitmapFromBuffer(buffer, width, height, false, false);
    }

    /**
     * 从Buffer中创建Bitmap
     * @param buffer
     * @param width
     * @param height
     * @param flipX
     * @param flipY
     * @return
     */
    public static Bitmap getBitmapFromBuffer(ByteBuffer buffer, int width, int height,
                                             boolean flipX, boolean flipY) {
        if (buffer == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        if (flipX || flipY) {
            Bitmap result = flipBitmap(bitmap, flipX, flipY, true);
            return result;
        } else {
            return bitmap;
        }
    }

    /**
     * 从普通文件中读入图片
     * @param fileName
     * @return
     */
    public static Bitmap getBitmapFromFile(String fileName) {
        Bitmap bitmap;
        File file = new File(fileName);
        if (!file.exists()) {
            return null;
        }
        try {
            bitmap = BitmapFactory.decodeFile(fileName);
        } catch (Exception e) {
            Log.e(TAG, "getBitmapFromFile: ", e);
            bitmap = null;
        }
        return bitmap;
    }

    /**
     * 加载Assets文件夹下的图片
     * @param context
     * @param fileName
     * @return
     */
    public static Bitmap getImageFromAssetsFile(Context context, String fileName) {
        Bitmap bitmap = null;
        AssetManager manager = context.getResources().getAssets();
        try {
            InputStream is = manager.open(fileName);
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    /**
     * 加载Assets文件夹下的图片
     * @param context
     * @param fileName
     * @return
     */
    public static Bitmap getImageFromAssetsFile(Context context, String fileName, Bitmap inBitmap) {
        Bitmap bitmap = null;
        AssetManager manager = context.getResources().getAssets();
        try {
            InputStream is = manager.open(fileName);
            if (inBitmap != null && !inBitmap.isRecycled()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                // 使用inBitmap时，inSampleSize得设置为1
                options.inSampleSize = 1;
                // 这个属性一定要在inBitmap之前使用，否则会弹出一下异常
                // BitmapFactory: Unable to reuse an immutable bitmap as an image decoder target.
                options.inMutable = true;
                options.inBitmap = inBitmap;
                bitmap = BitmapFactory.decodeStream(is, null, options);
            } else {
                bitmap = BitmapFactory.decodeStream(is);
            }
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    /**
     * 计算 inSampleSize的值
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }

            long totalPixels = width * height / inSampleSize;
            final long totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels > totalReqPixelsCap) {
                inSampleSize *= 2;
                totalPixels /= 2;
            }
        }
        return inSampleSize;
    }


    /**
     * 从文件读取Bitmap
     * @param dst       目标路径
     * @param maxWidth  读入最大宽度, 为0时，直接读入原图
     * @param maxHeight 读入最大高度，为0时，直接读入原图
     * @return
     */
    public static Bitmap getBitmapFromFile(File dst, int maxWidth, int maxHeight) {
        if (null != dst && dst.exists()) {
            BitmapFactory.Options opts = null;
            if (maxWidth > 0 && maxHeight > 0) {
                opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(dst.getPath(), opts);
                // 计算图片缩放比例
                opts.inSampleSize = calculateInSampleSize(opts, maxWidth, maxHeight);
                opts.inJustDecodeBounds = false;
                opts.inInputShareable = true;
                opts.inPurgeable = true;
            }
            try {
                return BitmapFactory.decodeFile(dst.getPath(), opts);
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 从文件读取Bitmap
     * @param dst                   目标路径
     * @param maxWidth              读入最大宽度，为0时，直接读入原图
     * @param maxHeight             读入最大高度，为0时，直接读入原图
     * @param processOrientation    是否处理图片旋转角度
     * @return
     */
    public static Bitmap getBitmapFromFile(File dst, int maxWidth, int maxHeight, boolean processOrientation) {
        if (null != dst && dst.exists()) {
            BitmapFactory.Options opts = null;
            if (maxWidth > 0 && maxHeight > 0) {
                opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(dst.getPath(), opts);
                // 计算图片缩放比例
                opts.inSampleSize = calculateInSampleSize(opts, maxWidth, maxHeight);
                opts.inJustDecodeBounds = false;
                opts.inInputShareable = true;
                opts.inPurgeable = true;
            }
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(dst.getPath(), opts);
                if (!processOrientation) {
                    return bitmap;
                }
                int orientation = getOrientation(dst.getPath());
                if (orientation == 0) {
                    return bitmap;
                } else {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(orientation);
                    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 从Drawable中获取Bitmap图片
     * @param drawable
     * @return
     */
    public static Bitmap getBitmapFromDrawable(Drawable drawable) {
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        Bitmap.Config config =
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                        : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(w, h, config);
        // 在View或者SurfaceView里的canvas.drawBitmap会看不到图，需要用以下方式处理
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     * 图片等比缩放
     * @param bitmap
     * @param newWidth
     * @param newHeight
     * @param isRecycled
     * @return
     */
    public static Bitmap zoomBitmap(Bitmap bitmap, int newWidth, int newHeight, boolean isRecycled) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        if (scaleWidth < scaleHeight) {
            matrix.postScale(scaleWidth, scaleWidth);
        } else {
            matrix.postScale(scaleHeight, scaleHeight);
        }
        Bitmap result = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        if (!bitmap.isRecycled() && isRecycled) {
            bitmap.recycle();
            bitmap = null;
        }
        return result;
    }

    /**
     * 保存图片
     * @param context
     * @param path
     * @param bitmap
     */
    public static void saveBitmap(Context context, String path, Bitmap bitmap) {
        saveBitmap(context, path, bitmap, true);
    }

    /**
     * 保存图片
     * @param context
     * @param path
     * @param bitmap
     * @param addToMediaStore
     */
    public static void saveBitmap(Context context, String path, Bitmap bitmap,
                                  boolean addToMediaStore) {
        final File file = new File(path);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean compress = true;
        if (path.endsWith(".png")) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        } else if (path.endsWith(".jpeg") || path.endsWith(".jpg")) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
        } else { // 除了png和jpeg之外的图片格式暂时不支持
            compress = false;
        }
        try {
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 添加到媒体库
        if (addToMediaStore && compress) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATA, path);
            values.put(MediaStore.Images.Media.DISPLAY_NAME, file.getName());
            context.getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        }
    }

    /**
     * 保存图片
     * @param filePath
     * @param buffer
     * @param width
     * @param height
     */
    public static void saveBitmap(String filePath, ByteBuffer buffer, int width, int height) {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(filePath));
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            bitmap = BitmapUtils.rotateBitmap(bitmap, 180, true);
            bitmap = BitmapUtils.flipBitmap(bitmap, true);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bitmap.recycle();
            bitmap = null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) try {
                bos.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    /**
     * 保存图片
     * @param filePath
     * @param bitmap
     */
    public static void saveBitmap(String filePath, Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(filePath));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bitmap.recycle();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) try {
                bos.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    /**
     * 获取图片旋转角度
     * @param path
     * @return
     */
    public static int getOrientation(final String path) {
        int rotation = 0;
        try {
            File file = new File(path);
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotation = 90;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotation = 180;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotation = 270;
                    break;

                default:
                    rotation = 0;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rotation;
    }

    /**
     * 获取Uri路径图片的旋转角度
     * @param context
     * @param uri
     * @return
     */
    public static int getOrientation(Context context, Uri uri) {
        final String scheme = uri.getScheme();
        ContentProviderClient provider = null;
        if (scheme == null || ContentResolver.SCHEME_FILE.equals(scheme)) {
            return getOrientation(uri.getPath());
        } else if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            try {
                provider = context.getContentResolver().acquireContentProviderClient(uri);
            } catch (SecurityException e) {
                return 0;
            }
            if (provider != null) {
                Cursor cursor;
                try {
                    cursor = provider.query(uri, new String[] {
                                    MediaStore.Images.ImageColumns.ORIENTATION,
                                    MediaStore.Images.ImageColumns.DATA},
                            null, null, null);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    return 0;
                }
                if (cursor == null) {
                    return 0;
                }

                int orientationIndex = cursor
                        .getColumnIndex(MediaStore.Images.ImageColumns.ORIENTATION);
                int dataIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);

                try {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();

                        int rotation = 0;

                        if (orientationIndex > -1) {
                            rotation = cursor.getInt(orientationIndex);
                        }

                        if (dataIndex > -1) {
                            String path = cursor.getString(dataIndex);
                            rotation |= getOrientation(path);
                        }
                        return rotation;
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return 0;
    }

    /**
     * 获取图片大小
     * @param path
     * @return
     */
    public static Point getBitmapSize(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        return new Point(options.outWidth, options.outHeight);
    }

    /**
     * 将Bitmap图片旋转90度
     * @param data
     * @return
     */
    public static Bitmap rotateBitmap(byte[] data) {
        return rotateBitmap(data, 90);
    }

    /**
     * 将Bitmap图片旋转一定角度
     * @param data
     * @param rotate
     * @return
     */
    public static Bitmap rotateBitmap(byte[] data, int rotate) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.postRotate(rotate);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
        bitmap.recycle();
        System.gc();
        return rotatedBitmap;
    }

    /**
     * 将Bitmap图片旋转90度
     * @param bitmap
     * @param isRecycled
     * @return
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, boolean isRecycled) {
        return rotateBitmap(bitmap, 90, isRecycled);
    }

    /**
     * 将Bitmap图片旋转一定角度
     * @param bitmap
     * @param rotate
     * @param isRecycled
     * @return
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, int rotate, boolean isRecycled) {
        if (bitmap == null) {
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.postRotate(rotate);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
        if (!bitmap.isRecycled() && isRecycled) {
            bitmap.recycle();
            bitmap = null;
        }
        return rotatedBitmap;
    }

    /**
     * 镜像翻转图片
     * @param bitmap
     * @param isRecycled
     * @return
     */
    public static Bitmap flipBitmap(Bitmap bitmap, boolean isRecycled) {
        return flipBitmap(bitmap, true, false, isRecycled);
    }

    /**
     * 翻转图片
     * @param bitmap
     * @param flipX
     * @param flipY
     * @param isRecycled
     * @return
     */
    public static Bitmap flipBitmap(Bitmap bitmap, boolean flipX, boolean flipY, boolean isRecycled) {
        if (bitmap == null) {
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.setScale(flipX ? -1 : 1, flipY ? -1 : 1);
        matrix.postTranslate(bitmap.getWidth(), 0);
        Bitmap result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, false);
        if (isRecycled && bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
        return result;
    }

    /**
     * 裁剪
     * @param bitmap
     * @param x
     * @param y
     * @param width
     * @param height
     * @param isRecycled
     * @return
     */
    public static Bitmap cropBitmap(Bitmap bitmap, int x, int y, int width, int height, boolean isRecycled) {

        if (bitmap == null) {
            return null;
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        // 保证裁剪区域
        if ((w - x) < width || (h - y) < height) {
            return null;
        }
        Bitmap result = Bitmap.createBitmap(bitmap, x, y, width, height, null, false);
        if (!bitmap.isRecycled() && isRecycled) {
            bitmap.recycle();
            bitmap = null;
        }
        return result;
    }

    /**
     * 获取Exif参数
     * @param path
     * @param bundle
     * @return
     */
    public static boolean loadExifAttributes(String path, Bundle bundle) {
        ExifInterface exifInterface;
        try {
            exifInterface = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        for (String tag : EXIF_TAGS) {
            bundle.putString(tag, exifInterface.getAttribute(tag));
        }
        return true;
    }

    /**
     * 保存Exif属性
     * @param path
     * @param bundle
     * @return 是否保存成功
     */
    public static boolean saveExifAttributes(String path, Bundle bundle) {
        ExifInterface exif;
        try {
            exif = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        for (String tag : EXIF_TAGS) {
            if (bundle.containsKey(tag)) {
                exif.setAttribute(tag, bundle.getString(tag));
            }
        }
        try {
            exif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * 替换背景颜色
     * @param color
     * @param orginBitmap
     * @return
     */
    public static Bitmap drawBg4Bitmap(int color, Bitmap orginBitmap) {
        Paint paint = new Paint();
        paint.setColor(color);
        Bitmap bitmap = Bitmap.createBitmap(orginBitmap.getWidth(),
                orginBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawRect(0, 0, orginBitmap.getWidth(), orginBitmap.getHeight(), paint);
        canvas.drawBitmap(orginBitmap, 0, 0, paint);
        return bitmap;
    }

    /**
     * 绘制纯色的背景
     * @param color 绘制的颜色
     * @param w bitmap的宽
     * @param h bitmap的高
     * @return Bitmap
     */
    public static Bitmap drawBg4Bitmap(String color, int w, int h){
        Bitmap bitmap = Bitmap.createBitmap(w, h,
                Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.parseColor(color));//填充颜色
        return bitmap;
    }



    /**
     * Return bitmap.
     *
     * @param filePath The path of file.
     * @return bitmap
     */
    public static Bitmap getBitmap(final String filePath) {
        //if (isSpace(filePath)) return null;
        return BitmapFactory.decodeFile(filePath);
    }

    /**
     * Return the scaled bitmap.
     *
     * @param src       The source of bitmap.
     * @param newWidth  The new width.
     * @param newHeight The new height.
     * @param recycle   True to recycle the source of bitmap, false otherwise.
     * @return the scaled bitmap
     */
    public static Bitmap scale(final Bitmap src,
                               final int newWidth,
                               final int newHeight,
                               final boolean recycle) {
        if (isEmptyBitmap(src)) return null;
        Bitmap ret = Bitmap.createScaledBitmap(src, newWidth, newHeight, true);
        if (recycle && !src.isRecycled()) src.recycle();
        return ret;
    }

    private static boolean isEmptyBitmap(final Bitmap src) {
        return src == null || src.getWidth() == 0 || src.getHeight() == 0;
    }

    //文字转图片
    public static Bitmap textToBitmap(String msg){
        Bitmap bmp;
        Canvas canvasTemp;
        Paint p = new Paint();
        Typeface font = Typeface.create("monospace", Typeface.BOLD);
        Rect r = new Rect();

        p.setColor(Color.WHITE);
        p.setTypeface(font);
        p.setTextSize(25);
        //p.getTextBounds(msg, 0, msg.length(), r);
        bmp = Bitmap.createBitmap(320,40, Bitmap.Config.ARGB_8888);
        canvasTemp = new Canvas(bmp);
        canvasTemp.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);//设置透明画布，无背景色
        canvasTemp.drawText(msg,0,30,p);
        return bmp;
    }

    public static Bitmap textToBitmap(String msg, int fontSize){
        Bitmap bmp;
        Canvas canvasTemp;
        Paint p = new Paint();
        Typeface font = Typeface.create("monospace", Typeface.BOLD);
        Rect r = new Rect();

        p.setColor(Color.WHITE);
        p.setTypeface(font);
        p.setTextSize(fontSize);
        p.getTextBounds(msg, 0, msg.length(), r);
        bmp = Bitmap.createBitmap(r.width(),r.height(), Bitmap.Config.ARGB_8888);
        canvasTemp = new Canvas(bmp);
        canvasTemp.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);//设置透明画布，无背景色
        canvasTemp.drawText(msg,0,10,p);
        return bmp;
    }

    /**
     * 压缩图片--压缩到100KB以下
     * @param image
     * @return
     */
    public static Bitmap compressBitmap(Bitmap image){
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            int options = 90;
            while (baos.toByteArray().length / 1024 > 100) { //循环判断如果压缩后图片是否大于100kb,大于继续压缩
                baos.reset();//重置baos即清空baos
                image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
                options -= 10;//每次都减少10
                Log.e("TAG", ">>>>>>>>>>>>>>>>>>>"+baos.toByteArray().length);
            }
            //把压缩后的数据baos存放到ByteArrayInputStream中
            ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
            Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null); //把ByteArrayInputStream数据生成图片
            Log.e("TAG", ">>>>>>>>>>rrr>>>>>>>>>"+bitmap.getByteCount());
            return bitmap;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 采用缩放压缩Bitmap
     * @param bm 原始Bitmap
     * @param scale 缩放比例，0~1
     * @return
     */
    public static Bitmap compressScaleBitmap(Bitmap bm, float scale) {
        try {
            Bitmap mSrcBitmap = Bitmap.createScaledBitmap(bm, (int) (bm.getWidth() * scale), (int) (bm.getHeight() * scale), true);
            return mSrcBitmap;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 保存网络图片至本地
     * @param url 网络图片址
     * @param savePath 本地图片地址，包括文件名及后缀
     * @return true表示保存成功，false表示保存失败
     */
    public static boolean saveImageFromNetwork(String url, String savePath){
        try{
            URL urls = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urls.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5 * 1000);
            InputStream inputStream = conn.getInputStream();
            byte[] bytes = ReadInputStream(inputStream);
            File file = new File(savePath);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bytes);
            fileOutputStream.close();
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static byte[] ReadInputStream(InputStream inputStream) throws Exception
    {
        ByteArrayOutputStream outstream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len=inputStream.read(buffer)) != -1)
        {
            outstream.write(buffer, 0, len);
        }
        inputStream.close();
        return outstream.toByteArray();
    }

}

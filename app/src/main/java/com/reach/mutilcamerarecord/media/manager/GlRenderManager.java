package com.reach.mutilcamerarecord.media.manager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.AsyncTask;
import android.view.Surface;

import com.reach.mutilcamerarecord.media.opengl.EglCore;
import com.reach.mutilcamerarecord.media.opengl.WindowSurface;
import com.reach.mutilcamerarecord.media.render.GlDisplayGroup;
import com.reach.mutilcamerarecord.media.render.GlRecordGroup;
import com.reach.mutilcamerarecord.media.render.GlRenderImgList;
import com.reach.mutilcamerarecord.medialocal.drawer.TextureHelper;
import com.reach.mutilcamerarecord.medialocal.drawer.WaterSignSProgram;
import com.reach.mutilcamerarecord.medialocal.drawer.WaterSignature;
import com.reach.mutilcamerarecord.utils.BitmapUtils;
import com.reach.mutilcamerarecord.utils.GlUtil;

import java.lang.ref.WeakReference;
import java.nio.Buffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Lzc on 2018/3/12 0012.
 * 绘图控制
 */

public class GlRenderManager {
    //gl 核心
    private EglCore mEglCore;
    //展示的surface
    private WindowSurface mDisplaySurface;
    //编码的surface
    private WindowSurface mEncoderSurface;
    private final Object mSyncObject = new Object();

    private boolean beautyEnable;

    private GlDisplayGroup displayRenderGroup;
    private GlRecordGroup recordRenderGroup;

    // 输入流大小
    private int mTextureWidth;
    private int mTextureHeight;
    // 显示大小
    private int mDisplayWidth;
    private int mDisplayHeight;

    // 显示大小
    private int mRecordWidth;
    private int mRecordHeight;

    private Context context;

    private int texture;
    private SurfaceTexture surfaceTexture;

    private GlBackBitmap glBackBitmap;


    //是否拍照状态
    private boolean takePhoto = false;

    private final static SimpleDateFormat formatter   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private WaterSignature mWaterSign;//水印
    private int[] mWaterTexId = new int[12];
    private int[] textureObjectIds = new int[1];


    private static class TakePhotoTask extends AsyncTask<Object, Object, Bitmap> {
        private WeakReference<GlRenderManager> glRenderManagerRef;

        TakePhotoTask(GlRenderManager glRenderManager) {
            glRenderManagerRef = new WeakReference<>(glRenderManager);
        }

        @Override
        protected Bitmap doInBackground(Object[] objects) {
            Buffer buffer = (Buffer) objects[0];
            int width = (int) objects[1];
            int height = (int) objects[2];
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(buffer);
            Matrix matrix = new Matrix();
            matrix.postRotate(180); /*翻转90度*/
            matrix.postScale(-1,1);
            bmp = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
            return bmp;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (glRenderManagerRef != null && glRenderManagerRef.get() != null && glRenderManagerRef.get().glBackBitmap != null) {
                glRenderManagerRef.get().glBackBitmap.onFinish(bitmap);
            } else
                bitmap.recycle();
        }
    }

    public interface GlBackBitmap {
        void onFinish(Bitmap bitmap);
    }


    public GlRenderManager(Context context, int texture, Surface disPlaySurface, SurfaceTexture surfaceTexture) throws GlUtil.OpenGlException {
        this.context = context;
        this.texture = texture;
        this.surfaceTexture = surfaceTexture;
        mEglCore = new EglCore(null, EglCore.FLAG_TRY_GLES3);
        setDisPlaySurface(disPlaySurface);
        mDisplaySurface.makeCurrent();
        //关闭深度测试和绘制背面
        GLES20.glDisable(GL10.GL_DEPTH_TEST);
        GLES20.glDisable(GL10.GL_CULL_FACE);
        init();
    }


    public void init() {
        //显示渲染组
        displayRenderGroup = new GlDisplayGroup(context);
        //录制渲染组
        recordRenderGroup = new GlRecordGroup(context);
        //设置水印
        mWaterSign = new WaterSignature();
        mWaterSign.setShaderProgram(new WaterSignSProgram());
        //mSignTexId = TextureHelper.loadTexture(context, R.mipmap.watermark);静态水印
        //初始化文字转图片所需的对象，避免多次生成新对象消耗过多内存
        //将字符图片与纹理绑定，返回纹理id
        for (int i = 0; i < 12; i++) {
            if (i == 10) {
                mWaterTexId[i] = TextureHelper.loadTexture(BitmapUtils.textToBitmap("-"), textureObjectIds);
            } else if (i == 11) {
                mWaterTexId[i] = TextureHelper.loadTexture(BitmapUtils.textToBitmap(":"), textureObjectIds);
            } else {
                mWaterTexId[i] = TextureHelper.loadTexture(BitmapUtils.textToBitmap(i + ""), textureObjectIds);
            }
        }
    }


    /**
     * 销毁
     */
    public void release() {
        if (displayRenderGroup != null) {
            displayRenderGroup.release();
            displayRenderGroup = null;
        }
        if (recordRenderGroup != null) {
            recordRenderGroup.release();
            recordRenderGroup = null;
        }
        if (mEncoderSurface != null) {
            mEncoderSurface.release();
            mEncoderSurface = null;
        }
        if (mDisplaySurface != null) {
            mDisplaySurface.release();
            mDisplaySurface = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        if (mWaterSign != null) {
            mWaterSign.release();
            mWaterSign = null;
        }
    }


    public void setDisPlaySurface(Surface displaySurface) throws GlUtil.OpenGlException {
        mDisplaySurface = new WindowSurface(mEglCore, displaySurface, false);
    }

    public void setEncoderSurface(Surface encodeSurface) throws GlUtil.OpenGlException {
        mEncoderSurface = new WindowSurface(mEglCore, encodeSurface, true);
    }

    //绘制
    public void drawFrame(boolean is_record, int recordRotate, boolean mirroring) throws Exception {
        int currentTexture = texture;
        if (mEglCore == null || mDisplaySurface == null) {
            return;
        }
        mDisplaySurface.makeCurrent();
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        try {
            surfaceTexture.updateTexImage();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (displayRenderGroup != null) {
            displayRenderGroup.setMirroring(mirroring);
            currentTexture = displayRenderGroup.drawFrame(currentTexture);
        }
        //拍照状态
        if (takePhoto) {
            takePhoto = false;
            new TakePhotoTask(this).execute(mDisplaySurface.getCurrentFrame(), mDisplayWidth, mDisplayHeight);
        }
        drawWaterSign();
        mDisplaySurface.swapBuffers();
        if (is_record && mEncoderSurface != null && recordRenderGroup != null) {
            mEncoderSurface.makeCurrent();
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            GLES20.glEnable(GLES20.GL_BLEND);
            //开启GL的混合模式，即图像叠加
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            recordRenderGroup.onInputSizeChanged(mRecordWidth, mRecordHeight);
            recordRenderGroup.onDisplayChanged(mRecordWidth, mRecordHeight);
            recordRenderGroup.setRotate(recordRotate);
            recordRenderGroup.drawFrame(currentTexture);
            drawWaterSign();
            mEncoderSurface.setPresentationTime(surfaceTexture.getTimestamp());
            mEncoderSurface.swapBuffers();
        }
        FrameRateMeter.getInstance().drawFrameCount();
    }


    /**
     * 渲染Texture的大小
     *
     * @param width
     * @param height
     */
    public void onInputSizeChanged(int width, int height) {
        mTextureWidth = width;
        mTextureHeight = height;
        if (displayRenderGroup != null)
            displayRenderGroup.onInputSizeChanged(width, height);
        if (recordRenderGroup != null)
            recordRenderGroup.onInputSizeChanged(width, height);
    }

    /**
     * Surface显示的大小
     *
     * @param width
     * @param height
     */
    public void onDisplaySizeChanged(int width, int height) {
        mDisplayWidth = width;
        mDisplayHeight = height;
        if (displayRenderGroup != null)
            displayRenderGroup.onDisplayChanged(width, height);
        if (recordRenderGroup != null)
            recordRenderGroup.onDisplayChanged(width, height);
    }


    public int getmDisplayWidth() {
        return mDisplayWidth;
    }

    public int getmDisplayHeight() {
        return mDisplayHeight;
    }

    public int getmTextureWidth() {
        return mTextureWidth;
    }

    public int getmTextureHeight() {
        return mTextureHeight;
    }

    public boolean isBeautyEnable() {
        return beautyEnable;
    }

    public void setBeautyEnable(boolean beautyEnable) {
        this.beautyEnable = beautyEnable;
        displayRenderGroup.enableBeauty(beautyEnable);
    }


    public void setmRecordWidth(int mRecordWidth) {
        this.mRecordWidth = mRecordWidth;
    }

    public void setmRecordHeight(int mRecordHeight) {
        this.mRecordHeight = mRecordHeight;
    }

    public GlRenderImgList getRenderList()
    {
        return (GlRenderImgList) displayRenderGroup.getmFilters().get(2);
    }

    public GlBackBitmap getGlBackBitmap() {
        return glBackBitmap;
    }

    public void setGlBackBitmap(GlBackBitmap glBackBitmap) {
        this.glBackBitmap = glBackBitmap;
    }

    public void setTakePhoto(boolean takePhoto) {
        this.takePhoto = takePhoto;
    }

    public void setCameraRotate(int cameraRotate) {
        displayRenderGroup.setCameraRotate(cameraRotate);
    }

    private void drawWaterSign(){
        String time = formatter.format(new Date());
        int x = 20;
        int y = 0;
        if ("".equals(time)) {
            return;
        }
        GLES20.glEnable(GLES20.GL_BLEND);
        //开启GL的混合模式，即图像叠加
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        //画水印
        GLES20.glViewport(x, y, 220, 40);//60
        mWaterSign.drawFrame(mWaterTexId[Integer.parseInt(time.substring(0, 1))]);
        GLES20.glViewport(x + 15, y, 220, 40);
        mWaterSign.drawFrame(mWaterTexId[Integer.parseInt(time.substring(1, 2))]);
        GLES20.glViewport(x + 15 * 2, y, 220, 40);
        mWaterSign.drawFrame(mWaterTexId[Integer.parseInt(time.substring(2, 3))]);
        GLES20.glViewport(x + 15 * 3, y, 220, 40);
        mWaterSign.drawFrame(mWaterTexId[Integer.parseInt(time.substring(3, 4))]);
        GLES20.glViewport(x + 15 * 4, y, 220, 40);
        mWaterSign.drawFrame(mWaterTexId[10]); // -
        GLES20.glViewport(x + 15 * 5, y, 220, 40);
        mWaterSign.drawFrame(mWaterTexId[Integer.parseInt(time.substring(5, 6))]);
        GLES20.glViewport(x + 15 * 6, y, 220, 40);
        mWaterSign.drawFrame(mWaterTexId[Integer.parseInt(time.substring(6, 7))]);
        GLES20.glViewport(x + 15 * 7, y, 220, 40);
        mWaterSign.drawFrame(mWaterTexId[10]); // -
        GLES20.glViewport(x + 15 * 8, y, 220, 40);
        mWaterSign.drawFrame(mWaterTexId[Integer.parseInt(time.substring(8, 9))]);
        GLES20.glViewport(x + 15 * 9, y, 220, 40);
        mWaterSign.drawFrame(mWaterTexId[Integer.parseInt(time.substring(9, 10))]);
        GLES20.glViewport(x + 15 * 11, y, 220, 40);
        mWaterSign.drawFrame(mWaterTexId[Integer.parseInt(time.substring(11, 12))]);
        GLES20.glViewport(x + 15 * 12, y, 220, 40);
        mWaterSign.drawFrame(mWaterTexId[Integer.parseInt(time.substring(12, 13))]);
        GLES20.glViewport(x + 15 * 13, y, 220, 40);
        mWaterSign.drawFrame(mWaterTexId[11]); // :
        GLES20.glViewport(x + 15 * 14, y, 220, 40);
        mWaterSign.drawFrame(mWaterTexId[Integer.parseInt(time.substring(14, 15))]);
        GLES20.glViewport(x + 15 * 15, y, 220, 40);
        mWaterSign.drawFrame(mWaterTexId[Integer.parseInt(time.substring(15, 16))]);
        GLES20.glViewport(x + 15 * 16, y, 220, 40);
        mWaterSign.drawFrame(mWaterTexId[11]); // :
        GLES20.glViewport(x + 15 * 17, y, 220, 40);
        mWaterSign.drawFrame(mWaterTexId[Integer.parseInt(time.substring(17, 18))]);
        GLES20.glViewport(x + 15 * 18, y, 220, 40);
        mWaterSign.drawFrame(mWaterTexId[Integer.parseInt(time.substring(18, 19))]);
        //GLES30.glDisable(GLES30.GL_BLEND);
    }
}

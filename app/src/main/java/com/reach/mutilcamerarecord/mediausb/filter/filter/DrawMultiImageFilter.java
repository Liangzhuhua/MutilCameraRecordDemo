package com.reach.mutilcamerarecord.mediausb.filter.filter;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import com.reach.mutilcamerarecord.mediausb.filter.hardvideofilter.BaseHardVideoFilter;
import com.reach.mutilcamerarecord.mediausb.tools.GLESTools;
import com.reach.mutilcamerarecord.utils.BitmapUtils;

import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Author：lzh on 2021/7/14 17:34
 */
public class DrawMultiImageFilter extends BaseHardVideoFilter {
    protected int glProgram;
    protected int glCamTextureLoc;
    protected int glCamPostionLoc;
    protected int glCamTextureCoordLoc;
    protected int glImageTextureLoc;
    protected int glImageRectLoc;
    protected int glImageAngelLoc;

    protected Context mContext;
    //private ArrayList<ImageDrawData> mImageInfos = new ArrayList<>();
    private ArrayList<ImageTexture> imageTextures = new ArrayList<>();
    private int mSize = 12;

    int textureId;
    int frameBuffer;
    Rect rect;
    int x;
    int y;
    String str;

    private final static SimpleDateFormat formatter   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public DrawMultiImageFilter(Context context) {
        super();
        mContext = context;
    }

    @Override
    public void onInit(int videoWidth, int videoHeight) {
        super.onInit(videoWidth, videoHeight);
        glProgram = GLESTools.createProgram(GLESTools.uRes(mContext.getResources(), "drawimage_vertex.sh"),
                GLESTools.uRes(mContext.getResources(), "drawimage_fragment.sh"));
        GLES20.glUseProgram(glProgram);
        glCamTextureLoc = GLES20.glGetUniformLocation(glProgram, "uCamTexture");
        glImageTextureLoc = GLES20.glGetUniformLocation(glProgram, "uImageTexture");
        glCamPostionLoc = GLES20.glGetAttribLocation(glProgram, "aCamPosition");
        glCamTextureCoordLoc = GLES20.glGetAttribLocation(glProgram, "aCamTextureCoord");
        glImageRectLoc = GLES20.glGetUniformLocation(glProgram, "imageRect");
        glImageAngelLoc = GLES20.glGetUniformLocation(glProgram, "imageAngel");

        x = 20;
        y = videoHeight - 50;//时间水印设在左下角
Log.e("water", "usb water title,y pos:" + y + ",h=" + videoHeight);
        initImageTexture();
    }

    protected void initImageTexture() {
        imageTextures = new ArrayList<>();
       ImageTexture imageTexture;
        for (int i = 0; i < 19; i++) {
            imageTexture = new ImageTexture(outVideoWidth, outVideoHeight);
            if (i == 10) {
                imageTexture.loadBitmap(BitmapUtils.textToBitmap("-"));
            } else if (i == 11) {
                imageTexture.loadBitmap(BitmapUtils.textToBitmap(":"));
            } else if (i >=0 && i <= 9){
                imageTexture.loadBitmap(BitmapUtils.textToBitmap(i + ""));
            } else {
                imageTexture.loadBitmap(BitmapUtils.textToBitmap("*"));
            }
            imageTextures.add(imageTexture);
        }
        mSize = imageTextures.size();
    }

    @Override
    public void onDraw(int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textureBuffer) {
        GLES20.glViewport(0, 0, outVideoWidth, outVideoHeight);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        ImageTexture preImageTexture = null;
        String time = formatter.format(new Date());

        if ("".equals(time)) {
            return;
        }
        for (int i = 0; i < mSize; i++) {
            if (preImageTexture == null) {
                textureId = cameraTexture;
            } else {
                textureId = preImageTexture.getTextureId();
            }
            if (i == mSize - 1) {
                frameBuffer = targetFrameBuffer;
            } else {
                frameBuffer = imageTextures.get(i).getFrameBuffer();
            }
            rect = new Rect(x+15*i, y, x+15*i+220, y+40);
            if (rect.left == rect.right || rect.top == rect.bottom) {
                continue;
            }
            str = time.substring(i, i+1);
            if (str.equals("-")){
                str = "10";
            } else if (str.equals(":")){
                str = "11";
            } else if (str.equals(" ") || str.equals("*")){
                continue;
            }
            drawImage(convertToRectF(rect), imageTextures.get(Integer.parseInt(str)).getImageTextureId(), textureId, frameBuffer, shapeBuffer, textureBuffer);
            preImageTexture = imageTextures.get(i);
        }
        GLES20.glFinish();
    }

    protected void drawImage(RectF rectF, int imageTextureId, int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textureBuffer) {
        GLES20.glEnableVertexAttribArray(glCamPostionLoc);
        GLES20.glEnableVertexAttribArray(glCamTextureCoordLoc);
        shapeBuffer.position(0);
        GLES20.glVertexAttribPointer(glCamPostionLoc, 2,
                GLES20.GL_FLOAT, false,
                2 * 4, shapeBuffer);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(glCamTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false,
                2 * 4, textureBuffer);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFrameBuffer);
        GLES20.glUseProgram(glProgram);
        GLES20.glUniform4f(glImageRectLoc, rectF.left, rectF.top, rectF.right, rectF.bottom);
//        GLES20.glUniform1f(glImageAngelLoc, (float)(30.0f*Math.PI/180));//用来更新旋转角度的
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, cameraTexture);
        GLES20.glUniform1i(glCamTextureLoc, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, imageTextureId);
        GLES20.glUniform1i(glImageTextureLoc, 1);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawIndecesBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, drawIndecesBuffer);
        GLES20.glDisableVertexAttribArray(glCamPostionLoc);
        GLES20.glDisableVertexAttribArray(glCamTextureCoordLoc);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glUseProgram(0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteProgram(glProgram);
        destroyImageTexture();
    }

    protected void destroyImageTexture() {
        for (ImageTexture imageTexture : imageTextures) {
            imageTexture.destroy();
        }
    }

    private RectF convertToRectF(Rect iconRect) {
        RectF iconRectF = new RectF();
        iconRectF.top = iconRect.top / (float) outVideoHeight;
        iconRectF.bottom = iconRect.bottom / (float) outVideoHeight;
        iconRectF.left = iconRect.left / (float) outVideoWidth;
        iconRectF.right = iconRect.right / (float) outVideoWidth;
        return iconRectF;
    }

    public static class ImageDrawData {
        public int resId = 0;
        public Rect rect;
    }
}

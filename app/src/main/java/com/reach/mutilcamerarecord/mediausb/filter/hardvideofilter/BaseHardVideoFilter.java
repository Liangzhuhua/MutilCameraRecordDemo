package com.reach.mutilcamerarecord.mediausb.filter.hardvideofilter;

import com.reach.mutilcamerarecord.mediausb.code.GLHelper;
import com.reach.mutilcamerarecord.mediausb.model.Size;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Author：lzh on 2021/6/10 10:56
 */
public class BaseHardVideoFilter {
    protected int outVideoWidth;
    protected int outVideoHeight;
    protected int directionFlag=-1;
    protected ShortBuffer drawIndecesBuffer;

    public void onInit(int videoWidth, int videoHeight) {
        outVideoWidth = videoWidth;
        outVideoHeight = videoHeight;
        drawIndecesBuffer = GLHelper.getDrawIndecesBuffer();
    }

    public void onDraw(final int cameraTexture, final int targetFrameBuffer, final FloatBuffer shapeBuffer, final FloatBuffer textrueBuffer) {
    }

    public void onDestroy() {

    }

    public void onDirectionUpdate(int _directionFlag) {
        this.directionFlag = _directionFlag;
    }

    protected int previewWidth;//横屏
    protected int previewHeight;//横屏
    protected Size previewSize;
    public void updatePreviewSize(int width, int height) {
        previewWidth = width;
        previewHeight = height;
        previewSize = new Size(width, height);
    }

    protected boolean isSquare;
    public void updateSquareFlag(boolean isSquare) {
        this.isSquare = isSquare;
    }

    protected float mCropRatio = 0;
    public void updateCropRatio(float cropRatio) {
        mCropRatio = cropRatio;
    }

}

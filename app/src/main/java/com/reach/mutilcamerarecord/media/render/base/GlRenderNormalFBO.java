package com.reach.mutilcamerarecord.media.render.base;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;

import com.reach.mutilcamerarecord.utils.GlUtil;
import com.reach.mutilcamerarecord.utils.TexturePositionUtil;

import java.nio.FloatBuffer;

/**
 * Created by Lzc on 2018/3/15 0015.
 */

public abstract class GlRenderNormalFBO extends GlRenderNormal {
    // FBO属性
    protected int[] mFramebuffers;
    protected int[] mFramebufferTextures;
    protected int mFrameWidth = -1;
    protected int mFrameHeight = -1;
    protected int rotate;

    public GlRenderNormalFBO(Context context) {
        super(context);
        textureBuffer = TexturePositionUtil.FBOFragmentFloatBuffer;
    }


    @Override
    public void onInputSizeChanged(int width, int height) {
        super.onInputSizeChanged(width, height);
        initFramebuffer(width, height);
    }

    protected void initFramebuffer(int width, int height) {
        if (mFramebuffers != null && (mFrameWidth != width || mFrameHeight != height)) {
            destroyFramebuffer();
        }
        if (mFramebuffers == null) {
            mFrameWidth = width;
            mFrameHeight = height;
            mFramebuffers = new int[1];
            mFramebufferTextures = new int[1];
            try {
                GlUtil.createSampler2DFrameBuff(mFramebuffers, mFramebufferTextures, width, height, 0);
            } catch (GlUtil.OpenGlException e) {
                e.printStackTrace();
            }
        }
    }

    protected void destroyFramebuffer() {
        if (mFramebufferTextures != null) {
            GLES30.glDeleteTextures(1, mFramebufferTextures, 0);
            mFramebufferTextures = null;
        }

        if (mFramebuffers != null) {
            GLES30.glDeleteFramebuffers(1, mFramebuffers, 0);
            mFramebuffers = null;
        }
        mImageWidth = -1;
        mImageHeight = -1;
    }

    @Override
    public void release() {
        destroyFramebuffer();
        super.release();
    }

    @Override
    public int drawFrame(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        if (rotate != 0) {
            runOnDraw(new Runnable() {
                @Override
                public void run() {
                    Matrix.setIdentityM(mMVPMatrix, 0);
                    Matrix.rotateM(mMVPMatrix, 0, rotate, 0, 0, 1f);
                }
            });
        }
        if (mFramebuffers == null)
            return textureId;
        GLES30.glViewport(0, 0, mFrameWidth, mFrameHeight);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFramebuffers[0]);
        super.drawFrame(textureId, vertexBuffer, textureBuffer);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        return mFramebufferTextures[0];
    }

    public int drawNormalFrame(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        return super.drawFrame(textureId, vertexBuffer, textureBuffer);
    }

    public int getRotate() {
        return rotate;
    }

    public void setRotate(int rotate) {
        this.rotate = rotate;
    }
}

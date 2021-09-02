package com.reach.mutilcamerarecord.media.render;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;

import com.reach.mutilcamerarecord.R;
import com.reach.mutilcamerarecord.media.render.base.GlRenderNormal;
import com.reach.mutilcamerarecord.utils.StringManagerUtil;

import java.nio.FloatBuffer;

/**
 * Created by Lzc on 2018/3/12 0012.
 */

public class GlRenderOutput extends GlRenderNormal {

    private int rotate;

    public GlRenderOutput(Context context) {
        super(context);
    }

    @Override
    public String getFragmentShaderCode() {

        return StringManagerUtil.getStringFromRaw(context,
                R.raw.normal_fragment_shader);
    }


    @Override
    public String getVertexShaderCode() {
        return StringManagerUtil.getStringFromRaw(context, R.raw.camera_vertex_shader);
    }


    @Override
    public void onDrawArraysBegin() {

    }

    @Override
    public void onDrawArraysAfter() {

    }

    @Override
    public int drawFrame(int textureId) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                Matrix.setIdentityM(mMVPMatrix, 0);
                Matrix.rotateM(mMVPMatrix, 0, rotate, 0, 0, 1f);
            }
        });
        return super.drawFrame(textureId);
    }

    @Override
    public int drawFrame(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        GLES30.glViewport(0, 0, mDisplayWidth, mDisplayHeight);
        return super.drawFrame(textureId, vertexBuffer, textureBuffer);
    }

    @Override
    public int getTextureType() {
        return GLES30.GL_TEXTURE_2D;
    }

    public int getRotate() {
        return rotate;
    }

    public void setRotate(int rotate) {
        this.rotate = rotate;
    }
}

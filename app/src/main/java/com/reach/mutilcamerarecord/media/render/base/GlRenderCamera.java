package com.reach.mutilcamerarecord.media.render.base;

import android.content.Context;
import android.opengl.GLES11Ext;

import com.reach.mutilcamerarecord.R;
import com.reach.mutilcamerarecord.utils.StringManagerUtil;
import com.reach.mutilcamerarecord.utils.TexturePositionUtil;

import java.nio.FloatBuffer;

/**
 * Created by Lzc on 2018/3/10 0010.
 */

public class GlRenderCamera extends GlRenderNormalFBO {

    protected FloatBuffer textureBufferMirror = TexturePositionUtil.FBOMirrorFragmentFloatBuffer;
    //是否是镜像
    private boolean isMirroring = false;


    public GlRenderCamera(Context context) {
        super(context);
    }


    @Override
    public String getFragmentShaderCode() {

        return StringManagerUtil.getStringFromRaw(context, R.raw.camera_fragment_shader);
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
    public int getTextureType() {
        return GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
    }


    @Override
    public int drawFrame(int textureId) {
        return drawFrame(textureId, vertexBuffer, isMirroring ? textureBufferMirror : textureBuffer);
    }


    public boolean isMirroring() {
        return isMirroring;
    }

    public void setMirroring(boolean mirroring) {
        isMirroring = mirroring;
    }

}

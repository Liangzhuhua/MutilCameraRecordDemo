package com.reach.mutilcamerarecord.media.render;

import android.content.Context;
import android.opengl.GLES30;

import com.reach.mutilcamerarecord.R;
import com.reach.mutilcamerarecord.media.render.base.GlRenderNormalFBO;
import com.reach.mutilcamerarecord.utils.StringManagerUtil;


/**
 * Created by Lzc on 2018/3/12 0012.
 */

public class GlRenderRealTimeBeauty extends GlRenderNormalFBO {
    private int mWidthLoc;
    private int mHeightLoc;
    private int mOpacityLoc;

    public GlRenderRealTimeBeauty(Context context) {
        super(context);
        mWidthLoc = GLES30.glGetUniformLocation(mProgram, "width");
        mHeightLoc = GLES30.glGetUniformLocation(mProgram, "height");
        mOpacityLoc = GLES30.glGetUniformLocation(mProgram, "opacity");
        setSmoothOpacity(1.0f);
    }

    @Override
    public String getFragmentShaderCode() {
        return StringManagerUtil.getStringFromRaw(context, R.raw.real_time_beauty_fragment);
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
        return GLES30.GL_TEXTURE_2D;
    }

    @Override
    public void onInputSizeChanged(int width, int height) {
        super.onInputSizeChanged(width, height);
        // 宽高变更时需要重新设置宽高值
        setInteger(mWidthLoc, width);
        setInteger(mHeightLoc, height);
    }

    /**
     * 设置磨皮程度
     *
     * @param percent 0.0 ~ 1.0
     */
    public void setSmoothOpacity(float percent) {
        float opacity;
        if (percent <= 0) {
            opacity = 0.0f;
        } else {
            opacity = calculateOpacity(percent);
        }
        setFloat(mOpacityLoc, opacity);
    }

    /**
     * 根据百分比计算出实际的磨皮程度
     *
     * @param percent 0% ~ 100%
     * @return
     */
    private float calculateOpacity(float percent) {
        if (percent > 1.0f) {
            percent = 1.0f;
        }
        float result = 0.0f;
        result = (float) (1.0f - (1.0f - percent + 0.02) / 2.0f);

        return result;
    }
}

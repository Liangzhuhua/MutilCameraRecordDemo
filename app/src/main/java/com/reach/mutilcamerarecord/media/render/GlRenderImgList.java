package com.reach.mutilcamerarecord.media.render;

import android.content.Context;
import android.opengl.GLES30;

import com.reach.mutilcamerarecord.R;
import com.reach.mutilcamerarecord.media.render.base.GlRenderNormalFBO;
import com.reach.mutilcamerarecord.utils.StringManagerUtil;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Lzc on 2018/3/13 0013.
 */

public class GlRenderImgList extends GlRenderNormalFBO {

    // 水印
    private HashMap<String, GlRenderImg> glRenderImgArrayList = new HashMap<>(10);

    private boolean vertical = true;

    public GlRenderImgList(Context context) {
        super(context);
    }

    @Override
    public String getFragmentShaderCode() {

        return StringManagerUtil.getStringFromRaw(context, R.raw.normal_fragment_shader);
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
    protected void unBindValue() {
        super.unBindValue();
        //绘制原图完成后利用同一个program进行处理
        GLES30.glUseProgram(mProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        for (HashMap.Entry<String, GlRenderImg> entry : glRenderImgArrayList.entrySet()) {

            FloatBuffer buffer;
            GlRenderImg value = entry.getValue();

            if (vertical) {
                buffer = value.getVertexPosition();
            } else {
                buffer = value.getVertexPositionHorizontal();
            }

            // 绑定数据
            bindValue(value.getTexture(), buffer, value.getFragmentPosition());
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
            // GLES30.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
            GLES30.glDisableVertexAttribArray(maPositionLoc);
            GLES30.glDisableVertexAttribArray(maTextureCoordLoc);
        }

        GLES30.glDisable(GLES30.GL_BLEND);
    }

    @Override
    public int getTextureType() {
        return GLES30.GL_TEXTURE_2D;
    }

    public void add(String key, GlRenderImg openGlImgTexture) {
        glRenderImgArrayList.put(key, openGlImgTexture);
    }

    public void remove(int position) {
        GlRenderImg openGlImgTexture = glRenderImgArrayList.remove(position);
        if (openGlImgTexture != null)
            openGlImgTexture.release();
    }

    public boolean contains(String key) {
        return glRenderImgArrayList.containsKey(key);
    }

    public void clear(String key) {

        glRenderImgArrayList.remove(key);
    }

    public void clear() {

        Iterator<Map.Entry<String, GlRenderImg>> iterator = glRenderImgArrayList.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, GlRenderImg> next = iterator.next();
            iterator.remove();
            if (next != null) {
                next.getValue().release();
            }
        }
    }

    public void replace(String key, GlRenderImg openGlImgTexture) {
        GlRenderImg last = glRenderImgArrayList.get(key);
        if (null != last) {
            last.release();
            glRenderImgArrayList.remove(key);
        }
        glRenderImgArrayList.put(key, openGlImgTexture);
    }

    public void remove(GlRenderImg openGlImgTexture) {
        glRenderImgArrayList.remove(openGlImgTexture);
        openGlImgTexture.release();
    }

    public int getSize() {
        return glRenderImgArrayList.size();
    }

    public boolean isVertical() {
        return vertical;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    @Override
    public void release() {
        super.release();
        if (glRenderImgArrayList != null) {
            for (HashMap.Entry<String, GlRenderImg> entry : glRenderImgArrayList.entrySet()) {
                entry.getValue().release();
            }
            glRenderImgArrayList.clear();
        }
        glRenderImgArrayList = null;
    }
}

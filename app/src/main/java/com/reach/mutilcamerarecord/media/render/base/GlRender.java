package com.reach.mutilcamerarecord.media.render.base;

/**
 * description: gl
 * create by kalu on 2019/03/14
 */
public interface GlRender {

    void onInputSizeChanged(int width, int height);

    void onDisplayChanged(int width, int height);

    int drawFrame(int textureId);

    void release();
}

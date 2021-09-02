package com.reach.mutilcamerarecord.media.render.base;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lzc on 2018/3/10 0010.
 */

public abstract class GlRenderGroup implements GlRender {
    private int mCurrentTextureId;
    // 渲染的Image的宽高
    protected int mImageWidth;
    protected int mImageHeight;
    // 显示输出的宽高
    protected int mDisplayWidth;
    protected int mDisplayHeight;
    protected List<GlRenderNormal> mFilters = new ArrayList<>();
    protected Context context;


    public GlRenderGroup(Context context) {
        this.context = context;
    }

    @Override
    public void onInputSizeChanged(int width, int height) {
        if (mFilters.size() <= 0) {
            return;
        }
        int size = mFilters.size();
        for (int i = 0; i < size; i++) {
            if (mFilters.get(i) != null) {
                mFilters.get(i).onInputSizeChanged(width, height);
            }
        }
        mImageWidth = width;
        mImageHeight = height;
    }


    @Override
    public void release() {
        if (mFilters != null) {
            for (GlRenderNormal mFilter : mFilters) {
                if (mFilter != null)
                    mFilter.release();
            }
            mFilters.clear();
        }
        mFilters = null;
    }


    @Override
    public void onDisplayChanged(int width, int height) {
        mDisplayWidth = width;
        mDisplayHeight = height;
        // 更新显示的的视图大小
        if (mFilters == null || mFilters.size() <= 0) {
            return;
        }
        int size = mFilters.size();
        for (int i = 0; i < size; i++) {
            if (mFilters.get(i) != null)
                mFilters.get(i).onDisplayChanged(width, height);
        }
    }


    @Override
    public int drawFrame(int textureId) {
        if (mFilters.size() <= 0) {
            return textureId;
        }
        int size = mFilters.size();
        mCurrentTextureId = textureId;
        for (int i = 0; i < size; i++) {
            if (mFilters.get(i) != null)
                mCurrentTextureId = mFilters.get(i).drawFrame(mCurrentTextureId);
        }
        return mCurrentTextureId;
    }

    public void replace(int position, GlRenderNormal glRenderNormal) {
        GlRenderNormal remove = mFilters.get(position);
        mFilters.set(position, glRenderNormal);
        if (remove != null)
            remove.release();
        if (glRenderNormal != null) {
            glRenderNormal.onInputSizeChanged(mImageWidth, mImageHeight);
            glRenderNormal.onDisplayChanged(mDisplayWidth, mDisplayHeight);
        }
    }


    /**
     * 获取当前滤镜TextureId
     *
     * @return
     */
    public int getCurrentTextureId() {
        return mCurrentTextureId;
    }

    public List<GlRenderNormal> getmFilters() {
        return mFilters;
    }

}

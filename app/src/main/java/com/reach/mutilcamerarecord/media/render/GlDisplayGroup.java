package com.reach.mutilcamerarecord.media.render;

import android.content.Context;

import com.reach.mutilcamerarecord.media.render.base.GlRenderCamera;
import com.reach.mutilcamerarecord.media.render.base.GlRenderGroup;

/**
 * Created by Lzc on 2018/3/15 0015.
 */

public class GlDisplayGroup extends GlRenderGroup {
    private GlRenderCamera glRenderCamera;
    private boolean enableBeauty = false;

    public GlDisplayGroup(Context context) {
        super(context);
        glRenderCamera = new GlRenderCamera(context);
        mFilters.add(glRenderCamera);
        mFilters.add(null);
        mFilters.add(new GlRenderImgList(context));
        mFilters.add(new GlRenderOutput(context));
    }

    public void setMirroring(boolean mirroring) {
        glRenderCamera.setMirroring(mirroring);
    }

    public void enableBeauty(boolean enable) {
        if (this.enableBeauty != enable) {
            enableBeauty = enable;
            replace(1, enableBeauty ? new GlRenderRealTimeBeauty(context) : null);
        }
    }

    public void setCameraRotate(int cameraRotate) {
        glRenderCamera.setRotate(cameraRotate);
    }
}

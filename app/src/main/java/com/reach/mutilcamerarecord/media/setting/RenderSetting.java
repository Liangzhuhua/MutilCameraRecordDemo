package com.reach.mutilcamerarecord.media.setting;

/**
 * Created by Lzc on 2018/3/27 0027.
 */

public class RenderSetting {
    //渲染宽度
    private int renderWidth = 1080;
    //渲染高度
    private int renderHeight = 1920;

    //输出到屏幕宽度
    private int displayWidth = 1080;
    //输出到屏幕高度
    private int displayHeight = 1920;
    //是否开启美颜
    private boolean enableBeauty = false;

    public void setRenderSize(int renderWidth, int renderHeight) {
        this.renderWidth = renderWidth;
        this.renderHeight = renderHeight;
    }

    public void setDisplaySize(int displayWidth, int displayHeight) {
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
    }

    public int getRenderWidth() {
        return renderWidth;
    }


    public int getRenderHeight() {
        return renderHeight;
    }


    public boolean isEnableBeauty() {
        return enableBeauty;
    }

    public void setEnableBeauty(boolean enableBeauty) {
        this.enableBeauty = enableBeauty;
    }

    public void setRenderWidth(int renderWidth) {
        this.renderWidth = renderWidth;
    }

    public void setRenderHeight(int renderHeight) {
        this.renderHeight = renderHeight;
    }

    public int getDisplayWidth() {
        return displayWidth;
    }

    public void setDisplayWidth(int displayWidth) {
        this.displayWidth = displayWidth;
    }

    public int getDisplayHeight() {
        return displayHeight;
    }

    public void setDisplayHeight(int displayHeight) {
        this.displayHeight = displayHeight;
    }
}

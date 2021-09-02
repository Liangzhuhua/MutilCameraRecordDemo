package com.reach.mutilcamerarecord.media.setting;

import android.graphics.SurfaceTexture;
import android.view.SurfaceHolder;

/**
 * Created by Lzc on 2018/3/22 0022.
 */

public class CameraSetting {
    private static final int TargetWidthDefault = 1080;
    private static final int TargetHeightDefault = 1920;
    private static final int PreviewViewFpsDefault = 30;
    //后置摄像头
    public static final int RearCamera = 0;
    //前置摄像头
    public static final int FrontCamera = 1;

    public CameraSetting() {
    }

    //相机图像宽
    public int width = TargetWidthDefault;
    //相机图像高
    public int height = TargetHeightDefault;
    //fps
    public int fps = PreviewViewFpsDefault;
    //摄像头选择
    public int cameraPosition = RearCamera;//FrontCamera;//RearCamera;
    //外置贴图
    public SurfaceTexture surfaceTexture;
    //显示区域holder
    public SurfaceHolder surfaceHolder;
    //旋转角度
    public int cameraRotate=0;//270;//0;
    //摄像头原始出来的数据宽度
    public int cameraW = 640;
    //摄像头原始出来的数据高度
    public int cameraH = 480;
}

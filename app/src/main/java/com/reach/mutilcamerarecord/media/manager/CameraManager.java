package com.reach.mutilcamerarecord.media.manager;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;

import androidx.annotation.Size;

import com.reach.mutilcamerarecord.media.setting.CameraSetting;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lzc on 2017/5/3 0003.
 */

public class CameraManager {
    private String TAG = "CameraManager";
    //相机对象
    private Camera mCamera;
    private boolean isOpenCamera = false;
    //回调信息
    private CallBackEvent event;

    //相机配置
    private CameraSetting cameraSetting;
    //照片分辨率
    private int photoWidth;
    private int photoHeight;
    //录制分辨率
    private int videoWidth;
    private int videoHeight;
    //真实fps
    private int realFps;

    //当前放大倍率
    private int currentZoom = 0;


    public Camera getmCamera() {
        return mCamera;
    }


    public CameraManager(CameraSetting cameraSetting) {
        this.cameraSetting = cameraSetting;
    }


    /**
     * 初始化摄像头同时开启预览
     */
    public void initCamera() {
        if (openCamera()) {
            changeCameraParams();
            //保存和回调真实属性
            saveCameraRealParams();
            mCamera.startPreview();
            isOpenCamera = true;
            if (event != null)
                event.openCameraSuccess(cameraSetting.cameraPosition);
        }
    }

    //保存真实属性
    private void saveCameraRealParams() {
        Camera.Size size = mCamera.getParameters().getPictureSize();
        if (event != null)
            event.onPhotoSizeChange(size.height, size.width);
        photoWidth = size.height;
        photoHeight = size.width;
        size = mCamera.getParameters().getPreviewSize();
        if (event != null)
            event.onVideoSizeChange(size.height, size.width);
        videoWidth = size.height;
        videoHeight = size.width;
        int fpsRange[] = new int[2];
        mCamera.getParameters().getPreviewFpsRange(fpsRange);
        realFps = fpsRange[0];
        Log.e(TAG, "photoWidth=" + photoWidth + ",photoHeight=" + photoHeight + ",videoWidth=" + videoWidth + ",videoHeight=" + videoHeight + ",realFps=" + realFps);
    }

    /**
     * 打开相机
     *
     * @return 是否成功
     */
    private boolean openCamera() {
        if (mCamera != null) {
            freeCameraResource();
        }
        try {
            mCamera = Camera.open(cameraSetting.cameraPosition);
            if (cameraSetting.surfaceTexture != null)
                mCamera.setPreviewTexture(cameraSetting.surfaceTexture);
            else
                mCamera.setPreviewDisplay(cameraSetting.surfaceHolder);
        } catch (Exception e) {
            e.printStackTrace();
            freeCameraResource();
            mCamera = null;
            if (event != null)
                event.openCameraFailure(cameraSetting.cameraPosition);
            return false;
        }
        return true;
    }

    /**
     * 释放摄像头资源
     */
    public void freeCameraResource() {
        if (mCamera != null && isOpenCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
//            mCamera.lock();
            mCamera.release();
            mCamera = null;
            isOpenCamera = false;
        }
    }

    private Camera.Size calcMaxSize(List<Camera.Size> pictureSizeList) {
        if (pictureSizeList.size() == 0) {
            return null;
        }
        return (pictureSizeList.get(0).width * pictureSizeList.get(0).height
                > pictureSizeList.get(pictureSizeList.size() - 1).width * pictureSizeList.get(pictureSizeList.size() - 1).height) ? pictureSizeList.get(0)
                : pictureSizeList.get(pictureSizeList.size() - 1);
    }

    private Camera.Size calcBestSize(List<Camera.Size> pictureSizeList, int targetWidth, int targetHeight) {
        Camera.Size result = null;
        for (Camera.Size size : pictureSizeList) {
            if (targetWidth == size.height && size.width == targetHeight) {
                Log.e(TAG, "targetWidth == size.height && size.width == targetHeight" + ",w=" + targetWidth + ",h=" + targetHeight);
                return size;
            }
        }
        int lastPxDx = Integer.MAX_VALUE;
        Size other;
        for (int i = pictureSizeList.size() - 1; i >= 0; i--) {
            Camera.Size size = pictureSizeList.get(i);
            Log.e(TAG, "camera list[" + i + "]----" + "w=" + size.width + ",h=" + size.height);
            if (size.width == cameraSetting.cameraW && size.height == cameraSetting.cameraH){//-----------------------------------------------------------
                return size;
            }
            if ((targetWidth - targetHeight) * (size.height - size.width) >= 0) {
                int current = Math.abs(size.width * size.height - targetHeight * targetWidth);
                if (current < lastPxDx) {
                    result = size;
                    lastPxDx = current;
                }
            }

        }

        return result;
    }

    public void switchCamera() {
        if (Camera.getNumberOfCameras() <= 1)
            return;
        cameraSetting.cameraPosition = cameraSetting.cameraPosition == 1 ? 0 : 1;
        if (!isOpenCamera)
            return;
        initCamera();
    }

//    public void changeCameraRotate(int cameraRotate) {
//        cameraSetting.cameraRotate = cameraRotate;
//        mCamera.setDisplayOrientation(cameraRotate);
//    }


    /**
     * 选取拍照最佳分辨率
     */
    private void initCameraPicSize(Camera.Parameters parameters) {
        // 获取摄像头支持的PictureSize列表
        List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
        //从列表中选取合适的分辨率
        Camera.Size picSize = calcBestSize(pictureSizeList, cameraSetting.width, cameraSetting.height);
        if (null == picSize) {
            picSize = parameters.getPictureSize();
        }
        // 根据选出的PictureSize重新设置SurfaceView大小
        parameters.setPictureSize(picSize.width, picSize.height);
    }


    /**
     * 设置预览分辨率
     */
    private void initCameraPreviewSize(Camera.Parameters parameters) {
        // 获取摄像头支持的PreviewSize列表
        List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();
        Camera.Size preSize = calcBestSize(previewSizeList, cameraSetting.width, cameraSetting.height);
        if (null == preSize) {
            preSize = parameters.getPreferredPreviewSizeForVideo();
        }
        parameters.setPreviewSize(preSize.width, preSize.height);
        Log.e(TAG , "preview width=" + preSize.width + ",preview height=" + preSize.height);
    }

    //设置最佳FPS
    private void initPreviewFps(Camera.Parameters parms) {
        List<int[]> supported = parms.getSupportedPreviewFpsRange();
        for (int[] entry : supported) {
            if ((entry[0] == entry[1]) && (entry[0] == cameraSetting.fps * 1000)) {
                parms.setPreviewFpsRange(entry[0], entry[1]);
                Log.e(TAG , "entry[0]=" + entry[0] + ",entry[1]=" + entry[1]);
                return;
            }
        }
    }

    //设置对焦模式
    private void initCameraFocusMode(Camera.Parameters parameters) {
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
    }


    //设置相机参数
    private void changeCameraParams() {
        Camera.Parameters parameters = mCamera.getParameters();
        //修改拍照分辨率
        initCameraPicSize(parameters);
        //修改预览分辨率
        initCameraPreviewSize(parameters);
        //修改预览fps
        initPreviewFps(parameters);
        // 设置照片质量
        parameters.setJpegQuality(100);
        //设置对焦模式
        initCameraFocusMode(parameters);
        parameters.set("orientation", "portrait");
        mCamera.setDisplayOrientation(cameraSetting.cameraRotate);
        mCamera.setParameters(parameters);
    }

    /**
     * @param zoomRatio 手势放大倍率
     */
    public void zoom(float zoomRatio) {
        final float maxScaleRatio = 10f;
        Camera.Parameters parameters = mCamera.getParameters();
        if (!parameters.isZoomSupported())
            return;
        int newZoom = 0;
        newZoom = (int) (currentZoom + (zoomRatio > 1 ? 1 : -1) * (zoomRatio / maxScaleRatio * parameters.getMaxZoom()));
        if (newZoom > parameters.getMaxZoom()) {
            newZoom = parameters.getMaxZoom();
        } else if (newZoom < 0) {
            newZoom = 0;
        }
        if (newZoom == currentZoom)
            return;
        if (parameters.isSmoothZoomSupported()) {
            mCamera.startSmoothZoom(newZoom);
        } else
            parameters.setZoom(newZoom);
        currentZoom = newZoom;
        mCamera.setParameters(parameters);
    }


    /**
     * @return 是否自动对焦
     */
    public boolean isAutoFocus() {
        return mCamera.getParameters().getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO);
    }


    /**
     * 对焦到该区域
     *
     * @param rect   对焦区域（页面坐标）
     * @param width  页面宽度
     * @param height 页面高度
     */
    public void focusToRect(Rect rect, int width, int height) {
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Area> areaList = new ArrayList<>();
        rect = calcFocus(rect, videoWidth, videoHeight, width, height);
        areaList.clear();
        areaList.add(new Camera.Area(rect, 800));
        int maxMeteringAreas = mCamera.getParameters().getMaxNumMeteringAreas();
        int maxFocusAreas = mCamera.getParameters().getMaxNumFocusAreas();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        parameters.setFocusAreas(areaList);
        parameters.setMeteringAreas(areaList);

        try {
            mCamera.setParameters(parameters);
            mCamera.autoFocus((boolean success, Camera camera) -> {
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //计算对焦区域  坐标映射
    private Rect calcFocus(Rect rectOr, int cameraVideoWidth, int cameraVideoHeight, int screenWidth, int screenHeight) {

        if (rectOr.left < 0)
            rectOr.left = 0;
        if (rectOr.right > screenWidth)
            rectOr.right = screenWidth;
        if (rectOr.top < 0)
            rectOr.top = 0;
        if (rectOr.bottom > screenHeight)
            rectOr.bottom = screenHeight;

        if (screenWidth > screenHeight) {
            int temp = cameraVideoWidth;
            cameraVideoWidth = cameraVideoHeight;
            cameraVideoHeight = temp;
        }

        //映射为实际视频坐标
        rectOr.left *= cameraVideoWidth / screenWidth;
        rectOr.right *= cameraVideoWidth / screenWidth;
        rectOr.top *= cameraVideoHeight / screenHeight;
        rectOr.bottom *= cameraVideoHeight / screenHeight;

        //映射为对焦坐标
        rectOr.left = (int) (2000f / cameraVideoWidth * rectOr.left - 1000f);
        rectOr.right = (int) (2000f / cameraVideoWidth * rectOr.right - 1000f);
        rectOr.bottom = (int) (2000f / cameraVideoHeight * rectOr.bottom - 1000f);
        rectOr.top = (int) (2000f / cameraVideoHeight * rectOr.top - 1000f);

        if (screenWidth < screenHeight) {
//            rectOr = new Rect(-rectOr.bottom, rectOr.left, -rectOr.top, rectOr.right);
            rectOr = new Rect(rectOr.top, -rectOr.right, rectOr.bottom, -rectOr.left);

        }

        //校验是否超过最大值或小于最小值
        if (rectOr.left <= -1000)
            rectOr.left = -1000;
        if (rectOr.right >= 1000)
            rectOr.right = 1000;
        if (rectOr.top <= -1000)
            rectOr.top = -1000;
        if (rectOr.bottom >= 1000)
            rectOr.bottom = 1000;
        return rectOr;
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }


    public int getCameraPosition() {
        return cameraSetting.cameraPosition;
    }

    public void setCameraPosition(int cameraPosition) {
        this.cameraSetting.cameraPosition = cameraPosition;
    }

    public boolean isOpenCamera() {
        return isOpenCamera;
    }

    public void setOpenCamera(boolean openCamera) {
        isOpenCamera = openCamera;
    }


    public CallBackEvent getEvent() {
        return event;
    }

    public void setEvent(CallBackEvent event) {
        this.event = event;
    }


    public int getPhotoWidth() {
        return photoWidth;
    }


    public int getPhotoHeight() {
        return photoHeight;
    }


    public int getVideoWidth() {
        return videoWidth;
    }


    public int getVideoHeight() {
        return videoHeight;
    }


    public int getRealFps() {
        return realFps;
    }

    public CameraSetting getCameraSetting() {
        return cameraSetting;
    }

    /**
     * 拍摄的所有事件
     */
    public interface CallBackEvent {

        void openCameraSuccess(int cameraPosition);

        void openCameraFailure(int cameraPosition);

        void onVideoSizeChange(int width, int height);

        void onPhotoSizeChange(int width, int height);
    }
}

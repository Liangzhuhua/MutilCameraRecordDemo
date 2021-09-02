package com.reach.mutilcamerarecord.mediausb;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;

import com.reach.mutilcamerarecord.MainActivity;
import com.reach.mutilcamerarecord.R;
import com.reach.mutilcamerarecord.mediausb.client.RecorderClient;
import com.reach.mutilcamerarecord.mediausb.code.listener.IVideoChange;
import com.reach.mutilcamerarecord.mediausb.code.listener.UvcPcmDataCallBack;
import com.reach.mutilcamerarecord.mediausb.filter.filter.DrawMultiImageFilter;
import com.reach.mutilcamerarecord.mediausb.model.MediaConfig;
import com.reach.mutilcamerarecord.mediausb.model.RecordConfig;
import com.reach.mutilcamerarecord.mediausb.model.Size;
import com.reach.mutilcamerarecord.mediausb.view.AspectTextureView;

/**
 * Author：lzh on 2021/6/10 10:04
 */
public class GLUsbManager {
    private MainActivity mActivity;
    private Context mContext;

    protected RecorderClient mRecorderClient;
    protected AspectTextureView mTextureView;
    RecordConfig recordConfig;

    boolean isStart = false;

    public GLUsbManager(MainActivity activity, Context context){
        mActivity = activity;
        mContext = context;

        initView();
    }

    private void initView(){

        mTextureView = mActivity.findViewById(R.id.camera_usb);
        mTextureView.setVisibility(View.VISIBLE);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                if (mRecorderClient != null) {
                    mRecorderClient.startPreview(surface, width, height);
                }
                updateOutsideCamState(1);
                Log.i("textureAvailable", "usb camera start to preview...");
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                if (mRecorderClient != null) {
                    mRecorderClient.updatePreview(width, height);
                }
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                if (mRecorderClient != null) {
                    mRecorderClient.stopPreview(true);
                }
                updateOutsideCamState(0);
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        });
        prepareStreamingClient();
        onSetFilters();
    }

    private void prepareStreamingClient() {
        try {
            mRecorderClient = new RecorderClient(this);

            recordConfig = RecordConfig.obtain();
            recordConfig.setTargetVideoSize(new Size(1280, 720));
            int bitRate = 1280 * 720;
            recordConfig.setSquare(true);//方正，不圆角
            recordConfig.setBitRate(bitRate);
            recordConfig.setVideoFPS(15);
            recordConfig.setVideoGOP(1);
            recordConfig.setRenderingMode(MediaConfig.Rending_Model_OpenGLES);
            //camera
            recordConfig.setDefaultCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
            int frontDirection, backDirection;
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, cameraInfo);
            frontDirection = cameraInfo.orientation;
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, cameraInfo);
            backDirection = cameraInfo.orientation;
            if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                recordConfig.setFrontCameraDirectionMode((frontDirection == 90 ? MediaConfig.DirectionMode.FLAG_DIRECTION_ROATATION_270 : MediaConfig.DirectionMode.FLAG_DIRECTION_ROATATION_90) | MediaConfig.DirectionMode.FLAG_DIRECTION_FLIP_HORIZONTAL);
                recordConfig.setBackCameraDirectionMode((backDirection == 90 ? MediaConfig.DirectionMode.FLAG_DIRECTION_ROATATION_90 : MediaConfig.DirectionMode.FLAG_DIRECTION_ROATATION_270));
            } else {
                recordConfig.setBackCameraDirectionMode((backDirection == 90 ? MediaConfig.DirectionMode.FLAG_DIRECTION_ROATATION_0 : MediaConfig.DirectionMode.FLAG_DIRECTION_ROATATION_180));
                recordConfig.setFrontCameraDirectionMode((frontDirection == 90 ? MediaConfig.DirectionMode.FLAG_DIRECTION_ROATATION_180 : MediaConfig.DirectionMode.FLAG_DIRECTION_ROATATION_0) | MediaConfig.DirectionMode.FLAG_DIRECTION_FLIP_HORIZONTAL);
            }
            //save video
            String mSaveVideoPath = Environment.getExternalStorageDirectory().getPath() + "/usb_video" + System.currentTimeMillis() + ".mp4";
            recordConfig.setSaveVideoPath(mSaveVideoPath);

            if (!mRecorderClient.prepare(mContext, recordConfig)) {
                mRecorderClient = null;
                Log.e("RecordingActivity", "prepare,failed!!");
                updateOutsideCamState(0);
                return;
            }

            Size s = mRecorderClient.getVideoSize();
            mTextureView.setAspectRatio(AspectTextureView.MODE_FITXY, ((double) s.getWidth()) / s.getHeight());

            mRecorderClient.setVideoChangeListener(new IVideoChange() {
                @Override
                public void onVideoSizeChanged(int width, int height) {
                    mTextureView.setAspectRatio(AspectTextureView.MODE_FITXY, ((double) width) / height);
                }
            });

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    protected void onSetFilters() {
        mRecorderClient.setHardVideoFilter(new DrawMultiImageFilter(mContext));
    }

    public void recordVideo(){
        if (!isStart) {
            updateOutsideCamState(1);
            String path = "/sdcard/test_B.mp4";
            mRecorderClient.updatePath(path);
            mRecorderClient.startRecording();
        } else{
            mRecorderClient.stopRecording();
        }
        isStart = !isStart;
    }

    public void setUsbAudioCallback(UvcPcmDataCallBack l){
        mActivity.setUsbAudioCallback(l);
    }

    public void updateOutsideCamState(int status){
        try {
           // mActivity.updateOutsideCamState(status);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


}

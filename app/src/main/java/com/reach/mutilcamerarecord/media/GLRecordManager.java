package com.reach.mutilcamerarecord.media;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.reach.mutilcamerarecord.MainActivity;
import com.reach.mutilcamerarecord.media.base.RecordManageBase;
import com.reach.mutilcamerarecord.media.encode.IAudioListener;
import com.reach.mutilcamerarecord.media.manager.RecordVideoAndAudioManager;
import com.reach.mutilcamerarecord.media.setting.CameraSetting;
import com.reach.mutilcamerarecord.media.setting.RecordSetting;
import com.reach.mutilcamerarecord.media.setting.RenderSetting;

import java.io.File;
import java.io.IOException;

/**
 * 本地内置摄像头
 */
public class GLRecordManager {
    private MainActivity mActivity;
    private Context mContext;
    //view
    private SurfaceView surfaceView;
    private SurfaceHolder holder;
    private RecordVideoAndAudioManager recorder;

    private final static int TargetLongWidth = 1280;//480;//640;//1920;   这个是录像分辨率
    private int TargetShortWidth = 720;//320;//480;//1080;

    private int cameraWidth = 1280;//320;//640;//这个是摄像头原始数据
    private int cameraHeight = 720;//240;//480;


    public GLRecordManager(MainActivity activity, Context context, SurfaceView sv){
        mActivity = activity;
        mContext = context;
        surfaceView = sv;
        RecordSetting recordSetting = new RecordSetting();
        CameraSetting cameraSetting = new CameraSetting();
        cameraSetting.fps = 15;//30;
        cameraSetting.cameraW = cameraWidth;
        cameraSetting.cameraH = cameraHeight;
        cameraSetting.cameraPosition = 0;//1;
        RenderSetting renderSetting = new RenderSetting();

        /*String s = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + System.currentTimeMillis() + ".mp4";
        File file = new File(s);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        recorder = new RecordVideoAndAudioManager(mActivity, null, recordSetting, cameraSetting, renderSetting, surfaceView);
        recorder.setCallBackEvent(new RecordManageBase.CallBackEvent() {
            @Override
            public void startRecordSuccess() {
                Log.e("main", "startRecordSuccess");
            }

            @Override
            public void onDuringUpdate(float time) {
                Log.e("main", "onDuringUpdate => time = " + time);
            }

            @Override
            public void stopRecordFinish(File file) {
                Log.e("main", "stopRecordFinish => path = " + file.getPath());
            }

            @Override
            public void recordError(String errorMsg) {
                Log.e("main", "recordError => msg = " + errorMsg);
            }

            @Override
            public void openCameraSuccess(int cameraPosition) {

                recorder.getRecordSetting().setVideoSetting(TargetLongWidth,TargetShortWidth,
                        recorder.getCameraManager().getRealFps() / 1000, RecordSetting.ColorFormatDefault);
                recorder.getRecordSetting().setVideoBitRate(3000 * 1024);
                recorder.switchOnBeauty(cameraPosition == 1);
                Log.e("main", "openCameraSuccess => cameraPosition = " + cameraPosition);
            }

            @Override
            public void openCameraFailure(int cameraPosition) {
                Log.e("main", "openCameraFailure => cameraPosition = " + cameraPosition);
            }

            @Override
            public void onVideoSizeChange(int width, int height) {
                Log.e("main", "onVideoSizeChange => width = " + width + ", height = " + height);
            }

            @Override
            public void onPhotoSizeChange(int width, int height) {
                Log.e("main", "onPhotoSizeChange => width = " + width + ", height = " + height);
            }
        });

        holder = surfaceView.getHolder();
        holder.addCallback(new CustomCallBack());
    }

    public void onDestroy() {
        if (recorder != null)
            recorder.destroy();
    }

    public void recordVideo(){
        if (recorder.isRecord){
            stopRecord();
        } else {
            startRecord();
        }
    }

    /**
     * 录像前先创建个文件先
     */
    public void setVideoFileName() {
        String path = "/sdcard/test_A.mp4";
        String pathMp3 = "/sdcard/test.mp3";
        File file = new File(path);
        File mp3 = new File(pathMp3);
        try {
            file.createNewFile();
            mp3.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        recorder.setFileName(file, mp3);
    }

    public void startRecord(){
        recorder.startRecord();
    }

    public void stopRecord(){
        recorder.stopRecord();
    }

    public IAudioListener getGLAudioListener(){
        return recorder.getGLAudioListener();
    }

    private class CustomCallBack implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            recorder.init();
            //recorder.getCameraManager().getEvent().openCameraFailure(0);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            recorder.getRenderSetting().setRenderSize(width < height ? TargetShortWidth : TargetLongWidth, width < height ? TargetLongWidth : TargetShortWidth);
            recorder.getRenderSetting().setDisplaySize(width, height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            recorder.destroy();
        }

    }
}

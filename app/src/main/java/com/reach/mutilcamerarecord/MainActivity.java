package com.reach.mutilcamerarecord;

import android.hardware.Camera;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.reach.mutilcamerarecord.media.GLRecordManager;
import com.reach.mutilcamerarecord.media.encode.IAudioListener;
import com.reach.mutilcamerarecord.media.manager.AudioManager;
import com.reach.mutilcamerarecord.mediausb.GLUsbManager;
import com.reach.mutilcamerarecord.mediausb.code.listener.UvcPcmDataCallBack;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private GLRecordManager mGLRecord;
    private AudioManager mAudioManager;
    private GLUsbManager glUsbManager;//双屏直接使用USB连接
    private UvcPcmDataCallBack uvcPcmDataCallBack;
    private IAudioListener glAudioCallback;

    private boolean isRecording = false;
    private TextView mRecordBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUVCCamera();
        initLocalCamera();
        initAudioManager();
        initView();
    }

    private void initUVCCamera(){
        {//USB摄像头，双屏使用
            int cameraCount = Camera.getNumberOfCameras();
            Log.i("usbCamera", "get total camera count:" + cameraCount);
            if (cameraCount >= 2) {
                glUsbManager = new GLUsbManager(this, this);
            } else{
                Log.e("usbCamera", "get total camera count:" + cameraCount + ",so give up open usb camera...");
            }
        }
    }

    private void initLocalCamera(){
        mGLRecord = new GLRecordManager(this, this, findViewById(R.id.surface_view));
    }

    private void initAudioManager(){
        //音频数据回调到这个接口，再分发给有需要的线程
        mAudioManager = new AudioManager(this, new AudioManager.OnPCmCallbackListener() {
            @Override
            public void onPcmCallback(ByteBuffer buffer, int readBytes) {
                onFramePcm(buffer, readBytes);
            }

            @Override
            public void onPcmStatusCallback(int status) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ;
                    }
                });
            }
        });
    }

    private void initView(){
        mRecordBtn = findViewById(R.id.record);
        mRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordFunc();
            }
        });
    }


    /**
     * 开始/结束录像按钮
     */
    private void recordFunc(){
        if (isRecording) {
            mRecordBtn.setText("开始录像");
        }else {
            mRecordBtn.setText("结束录像");
        }
        recordVideo();
        isRecording = !isRecording;
    }

    private void recordVideo(){
        mAudioManager.record();
        if (glUsbManager != null) {
            glUsbManager.recordVideo();
        } else {
            Log.e("usbCamera", "start usb camera record fail,gl usb manager is null...");
        }
        mGLRecord.setVideoFileName();
        mGLRecord.recordVideo();

        getGLAudioListener();
    }

    /**
     * 绑定USB音频线程回调
     *
     * 这个是直接插USB接口的回调，一般用在双屏上
     * @param l
     */
    public void setUsbAudioCallback(UvcPcmDataCallBack l){
        uvcPcmDataCallBack = l;
    }

    private void getGLAudioListener() {
        glAudioCallback = mGLRecord.getGLAudioListener();
    }

    /**
     * 原始音频来了转发给各线程分享
     * @param buffer
     * @param readBytes
     */
    public void onFramePcm(ByteBuffer buffer, int readBytes) {
        if (uvcPcmDataCallBack != null){
            uvcPcmDataCallBack.onFramePcm(buffer, readBytes);//UVC
            buffer.flip();
            //buffer.clear();
        }
        if (glAudioCallback != null) {
            glAudioCallback.onPcm(buffer, readBytes);//本地
            buffer.flip();
        }
    }
}
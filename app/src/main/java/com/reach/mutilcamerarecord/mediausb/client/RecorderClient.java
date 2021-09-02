package com.reach.mutilcamerarecord.mediausb.client;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;

import com.reach.mutilcamerarecord.mediausb.GLUsbManager;
import com.reach.mutilcamerarecord.mediausb.code.listener.IVideoChange;
import com.reach.mutilcamerarecord.mediausb.encoder.MediaMuxerWrapper;
import com.reach.mutilcamerarecord.mediausb.filter.hardvideofilter.BaseHardVideoFilter;
import com.reach.mutilcamerarecord.mediausb.filter.softaudiofilter.BaseSoftAudioFilter;
import com.reach.mutilcamerarecord.mediausb.model.MediaMakerConfig;
import com.reach.mutilcamerarecord.mediausb.model.RecordConfig;
import com.reach.mutilcamerarecord.mediausb.model.Size;

import java.io.IOException;

/**
 * Author：lzh on 2021/6/10 10:42
 */
public class RecorderClient {
    private VideoClient videoClient;
    private AudioClient audioClient;
    private final Object SyncOp;
    private MediaMakerConfig mediaMakerConfig;

    private GLUsbManager manager;

    public RecorderClient(GLUsbManager manager) {
        SyncOp = new Object();
        mediaMakerConfig = new MediaMakerConfig();
        this.manager = manager;
        CallbackDelivery.i();
    }

    /**
     * prepare to stream
     *
     * @param config config
     * @return true if prepare success
     */
    public boolean prepare(Context context, RecordConfig config) {
        synchronized (SyncOp) {
            try {
                checkDirection(config);
            } catch (RuntimeException e) {
                e.printStackTrace();
                return false;
            }
            mediaMakerConfig.printDetailMsg = config.isPrintDetailMsg();
            mediaMakerConfig.isSquare = config.isSquare();
            mediaMakerConfig.saveVideoEnable = config.isSaveVideoEnable();
            mediaMakerConfig.saveVideoPath = config.getSaveVideoPath();
            mediaMakerConfig.mediacodecAACSampleRate = 16000;
            videoClient = new VideoClient(context, mediaMakerConfig);
            audioClient = new AudioClient(mediaMakerConfig, manager);
            if (!videoClient.prepare(config)) {
                Log.d("prepare","!!!!!videoClient.prepare()failed");
                Log.d("prepare", mediaMakerConfig.toString());
                return false;
            }
            if (!audioClient.prepare(config)) {
                Log.d("prepare","!!!!!audioClient.prepare()failed");
                Log.d("prepare", mediaMakerConfig.toString());
                return false;
            }
            mediaMakerConfig.done = true;
            Log.d("prepare","===INFO===coreParametersReady:");
            Log.d("prepare", mediaMakerConfig.toString());
            return true;
        }
    }


    public void updatePath(String path) {
        mediaMakerConfig.saveVideoPath = path;
    }

    public String getFilePath() {
        return mediaMakerConfig.saveVideoEnable ? mediaMakerConfig.saveVideoPath : null;
    }

    /**
     * start recording
     */
    public void startRecording() {
        synchronized (SyncOp) {
            prepareMuxer();
            boolean ve = videoClient.startRecording(mMuxer);
            boolean ae = audioClient.startRecording(mMuxer);
            if (!ve && !ae){
                manager.updateOutsideCamState(0);
            }
            Log.d("recorder","RecorderClient,startRecording,ve=" + ve + ",ae=" + ae);
        }
    }

    /**
     * stop recording
     */
    public void stopRecording() {
        synchronized (SyncOp) {
            boolean ve = videoClient.stopRecording();
            boolean ae = audioClient.stopRecording();
            if (!ve && !ae){
                manager.updateOutsideCamState(0);
            }
            Log.d("recorder","RecorderClient,stopRecording,ve=" + ve + ",ae=" + ae);
        }
    }

    /**
     * clean up
     */
    public void destroy() {
        synchronized (SyncOp) {
            videoClient.destroy();
            audioClient.destroy();
            videoClient = null;
            audioClient = null;
            Log.d("recorder","RecorderClient,destroy()");
        }
    }

    /**
     * call it AFTER {@link #prepare}
     *
     * @param surfaceTexture to rendering preview
     */
    public void startPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight) {
        videoClient.startPreview(surfaceTexture, visualWidth, visualHeight);
        Log.d("recorder","RecorderClient,startPreview()");
    }

    public void updatePreview(int visualWidth, int visualHeight) {
        videoClient.updatePreview(visualWidth, visualHeight);
        Log.d("recorder","RecorderClient,updatePreview()");
    }

    /**
     * @param releaseTexture true if you won`t reuse this surfaceTexture later
     */
    public void stopPreview(boolean releaseTexture) {
        videoClient.stopPreview(releaseTexture);
        Log.d("recorder","RecorderClient,stopPreview()");
    }

    /**
     * change camera on running.<br/>
     */
    public boolean swapCamera() {
        synchronized (SyncOp) {
            Log.d("recorder","RecorderClient,swapCamera()");
            return videoClient.swapCamera();
        }
    }

    public boolean isFrontCamera() {
        return videoClient.isFrontCamera();
    }

    /**
     * get the real video size,call after prepare()
     *
     * @return
     */
    public Size getVideoSize() {
        return new Size(mediaMakerConfig.videoWidth, mediaMakerConfig.videoHeight);
    }

    /**
     * only for hard filter mode.<br/>
     * set videofilter.<br/>
     * can be called Repeatedly.<br/>
     *
     * @param baseHardVideoFilter videofilter to apply
     */
    public void setHardVideoFilter(BaseHardVideoFilter baseHardVideoFilter) {
        videoClient.setHardVideoFilter(baseHardVideoFilter);
    }

    /**
     * set audiofilter.<br/>
     * can be called Repeatedly.<br/>
     *
     * @param baseSoftAudioFilter audiofilter to apply
     */
    public void setSoftAudioFilter(BaseSoftAudioFilter baseSoftAudioFilter) {
     //   audioClient.setSoftAudioFilter(baseSoftAudioFilter);
    }

    /**
     * listener for video size change
     *
     * @param videoChangeListener
     */
    public void setVideoChangeListener(IVideoChange videoChangeListener) {
        videoClient.setVideoChangeListener(videoChangeListener);
    }

    /**
     * toggle flash light
     *
     * @return true if operation success
     */
    public boolean toggleFlashLight() {
        return videoClient.toggleFlashLight();
    }
    public boolean toggleFlashLight(boolean on) {
        return videoClient.toggleFlashLight(on);
    }

    /**
     * =====================PRIVATE=================
     **/
    private void checkDirection(RecordConfig config) {
        int frontFlag = config.getFrontCameraDirectionMode();
        int backFlag = config.getBackCameraDirectionMode();
        int fbit = 0;
        int bbit = 0;
        //check or set default value
        if ((frontFlag >> 4) == 0) {
            frontFlag |= MediaMakerConfig.FLAG_DIRECTION_ROATATION_0;
        }
        if ((backFlag >> 4) == 0) {
            backFlag |= MediaMakerConfig.FLAG_DIRECTION_ROATATION_0;
        }
        //make sure only one direction
        for (int i = 4; i <= 8; ++i) {
            if (((frontFlag >> i) & 0x1) == 1) {
                fbit++;
            }
            if (((backFlag >> i) & 0x1) == 1) {
                bbit++;
            }
        }
        if (fbit != 1 || bbit != 1) {
            throw new RuntimeException("invalid direction rotation flag:frontFlagNum=" + fbit + ",backFlagNum=" + bbit);
        }
        if (((frontFlag & MediaMakerConfig.FLAG_DIRECTION_ROATATION_0) != 0) || ((frontFlag & MediaMakerConfig.FLAG_DIRECTION_ROATATION_180) != 0)) {
            fbit = 0;
        } else {
            fbit = 1;
        }
        if (((backFlag & MediaMakerConfig.FLAG_DIRECTION_ROATATION_0) != 0) || ((backFlag & MediaMakerConfig.FLAG_DIRECTION_ROATATION_180) != 0)) {
            bbit = 0;
        } else {
            bbit = 1;
        }
        if (bbit != fbit) {
            if (bbit == 0) {
                throw new RuntimeException("invalid direction rotation flag:back camera is landscape but front camera is portrait");
            } else {
                throw new RuntimeException("invalid direction rotation flag:back camera is portrait but front camera is landscape");
            }
        }
        if (fbit == 1) {
            mediaMakerConfig.isPortrait = true;
        } else {
            mediaMakerConfig.isPortrait = false;
        }
        mediaMakerConfig.backCameraDirectionMode = backFlag;
        mediaMakerConfig.frontCameraDirectionMode = frontFlag;
    }

    private MediaMuxerWrapper mMuxer = null;

    private void prepareMuxer() {
        if (!mediaMakerConfig.saveVideoEnable) {
            return;
        }
        try {
            mMuxer = new MediaMuxerWrapper(mediaMakerConfig.saveVideoPath);
            mMuxer.setTrackCount(2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

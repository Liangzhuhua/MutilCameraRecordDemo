package com.reach.mutilcamerarecord.mediausb.code.video;

import android.graphics.SurfaceTexture;

import com.reach.mutilcamerarecord.mediausb.code.listener.IVideoChange;
import com.reach.mutilcamerarecord.mediausb.encoder.MediaMuxerWrapper;
import com.reach.mutilcamerarecord.mediausb.model.RecordConfig;

/**
 * Author：lzh on 2021/6/10 10:46
 */
public interface IVideoCore {
    int OVERWATCH_TEXTURE_ID = 10;
    boolean prepare(RecordConfig resConfig);

    void updateCamTexture(SurfaceTexture camTex);

    void startPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight);

    void updatePreview(int visualWidth, int visualHeight);

    void stopPreview(boolean releaseTexture);

    boolean startRecording(MediaMuxerWrapper muxer);

    boolean stopRecording();

    boolean destroy();

    void setCurrentCamera(int cameraIndex);

    void setVideoChangeListener(IVideoChange listener);
}
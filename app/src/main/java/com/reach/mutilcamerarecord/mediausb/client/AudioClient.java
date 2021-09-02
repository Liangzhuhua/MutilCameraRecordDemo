package com.reach.mutilcamerarecord.mediausb.client;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.util.Log;

import com.reach.mutilcamerarecord.mediausb.GLUsbManager;
import com.reach.mutilcamerarecord.mediausb.code.audio.AudioCore;
import com.reach.mutilcamerarecord.mediausb.code.listener.UvcPcmDataCallBack;
import com.reach.mutilcamerarecord.mediausb.encoder.MediaMuxerWrapper;
import com.reach.mutilcamerarecord.mediausb.filter.softaudiofilter.BaseSoftAudioFilter;
import com.reach.mutilcamerarecord.mediausb.model.MediaMakerConfig;
import com.reach.mutilcamerarecord.mediausb.model.RecordConfig;

import java.nio.ByteBuffer;

/**
 * Author：lzh on 2021/6/10 11:08
 */
public class AudioClient {
    MediaMakerConfig mediaMakerConfig;
    private final Object syncOp = new Object();
    //private AudioRecordThread audioRecordThread;
    //private AudioRecord audioRecord;
    private byte[] audioBuffer;
    private AudioCore softAudioCore;

    GLUsbManager manager;
    private boolean isRunning = false;

    public AudioClient(MediaMakerConfig parameters, GLUsbManager manager) {
        mediaMakerConfig = parameters;
        this.manager = manager;

        initAudioListener();
    }

    public boolean prepare(RecordConfig recordConfig) {
        synchronized (syncOp) {
            mediaMakerConfig.audioBufferQueueNum = 5;
            softAudioCore = new AudioCore(mediaMakerConfig);
            if (!softAudioCore.prepare(recordConfig)) {
                Log.e("","AudioClient,prepare");
                return false;
            }
            mediaMakerConfig.audioRecoderFormat = AudioFormat.ENCODING_PCM_16BIT;
            mediaMakerConfig.audioRecoderChannelConfig = AudioFormat.CHANNEL_IN_MONO;
            mediaMakerConfig.audioRecoderSliceSize = 320;//mediaMakerConfig.mediacodecAACSampleRate / 10;
            mediaMakerConfig.audioRecoderBufferSize = 640;//mediaMakerConfig.audioRecoderSliceSize * 2;
            mediaMakerConfig.audioRecoderSource = MediaRecorder.AudioSource.DEFAULT;
            mediaMakerConfig.audioRecoderSampleRate = 16000;//mediaMakerConfig.mediacodecAACSampleRate;
            prepareAudio();
            return true;
        }
    }

    public boolean startRecording(MediaMuxerWrapper muxer) {
        synchronized (syncOp) {
            softAudioCore.startRecording(muxer);
            //audioRecord.startRecording();
            //audioRecordThread = new AudioRecordThread();
            //audioRecordThread.start();
            Log.d("","AudioClient,start()");
            isRunning =  true;
            return true;
        }
    }

    public boolean stopRecording() {
        synchronized (syncOp) {
            /*if (audioRecordThread != null) {
                audioRecordThread.quit();
                try {
                    audioRecordThread.join();
                } catch (InterruptedException ignored) {
                }
                audioRecordThread = null;
            }*/
            softAudioCore.stop();
            //audioRecord.stop();
            isRunning = false;
            return true;
        }
    }

    public boolean destroy() {
        synchronized (syncOp) {
            //audioRecord.release();
            isRunning = false;
            return true;
        }
    }
    public void setSoftAudioFilter(BaseSoftAudioFilter baseSoftAudioFilter) {
        softAudioCore.setAudioFilter(baseSoftAudioFilter);
    }
    public BaseSoftAudioFilter acquireSoftAudioFilter() {
        return softAudioCore.acquireAudioFilter();
    }

    public void releaseSoftAudioFilter() {
        softAudioCore.releaseAudioFilter();
    }

    private boolean prepareAudio() {
        /*int minBufferSize = AudioRecord.getMinBufferSize(mediaMakerConfig.audioRecoderSampleRate,
                mediaMakerConfig.audioRecoderChannelConfig,
                mediaMakerConfig.audioRecoderFormat);
        audioRecord = new AudioRecord(mediaMakerConfig.audioRecoderSource,
                mediaMakerConfig.audioRecoderSampleRate,
                mediaMakerConfig.audioRecoderChannelConfig,
                mediaMakerConfig.audioRecoderFormat,
                minBufferSize * 5);
        audioBuffer = new byte[mediaMakerConfig.audioRecoderBufferSize];
        if (AudioRecord.STATE_INITIALIZED != audioRecord.getState()) {
            Log.e("","audioRecord.getState()!=AudioRecord.STATE_INITIALIZED!");
            return false;
        }*/
        /*if (AudioRecord.SUCCESS != audioRecord.setPositionNotificationPeriod(mediaMakerConfig.audioRecoderSliceSize)) {
            Log.e("","AudioRecord.SUCCESS != audioRecord.setPositionNotificationPeriod(" + mediaMakerConfig.audioRecoderSliceSize + ")");
            return false;
        }*/
        return true;
    }

    class AudioRecordThread extends Thread {
        private boolean isRunning = true;

        AudioRecordThread() {
            isRunning = true;
        }

        public void quit() {
            isRunning = false;
        }

        @Override
        public void run() {
            Log.d("","AudioRecordThread,tid=" + Thread.currentThread().getId());
            /*while (isRunning) {
                int size = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                if (isRunning && softAudioCore != null && size > 0) {
                    softAudioCore.queueAudio(audioBuffer);
                }
            }*/
        }
    }

    private void initAudioListener(){
        UvcPcmDataCallBack uvcPcmDataCallBack = new UvcPcmDataCallBack() {
            @Override
            public void onFramePcm(ByteBuffer buffer, int readBytes) {
                if (isRunning && softAudioCore != null && readBytes > 0) {
                    buffer.clear();
                    int len = buffer.capacity();
                    byte[] data = new byte[len];
                    buffer.get(data);
                    softAudioCore.queueAudio(data);
                }
            }
        };
        manager.setUsbAudioCallback(uvcPcmDataCallBack);
    }
}

package com.reach.mutilcamerarecord.media.manager;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioManager {
    private Context context;
    private AudioThread mAudioThread = null;
    private static final String TAG = "AudioManager";

    private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int SAMPLE_RATE = 16000;//44100;	// 44.1[KHz] is only setting guaranteed to be available on all devices.
    private static final int BIT_RATE = 64000;
    public static final int SAMPLES_PER_FRAME = 640;//1280;//1024;	// AAC, bytes/frame/channel
    public static final int FRAMES_PER_BUFFER = 25; 	// AAC, frame/buffer/sec
    /**
     * Flag that indicate this encoder is capturing now.
     */
    protected volatile boolean mIsCapturing;
    protected OnPCmCallbackListener onPCmCallbackListener;

    private static final int[] AUDIO_SOURCES = new int[] {
         //   MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.MIC,          //本地
            MediaRecorder.AudioSource.CAMCORDER,
    };

    public AudioManager(Context context, OnPCmCallbackListener onPCmCallbackListener){
        this.context = context;
        this.onPCmCallbackListener = onPCmCallbackListener;
        Log.e(TAG, "audio manager is initiation...");
    }

    public void record(){
        if (mIsCapturing){
            stopRecord();
        }else{
            startRecord();
        }
    }

    public void startRecord(){
        if (mAudioThread == null) {
            mAudioThread = new AudioThread();
            mIsCapturing = true;
            mAudioThread.start();
        }
    }

    public void stopRecord(){
        try {
            mIsCapturing = false;
            mAudioThread = null;
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * Thread to capture audio data from internal mic as uncompressed 16bit PCM data
     * and write them to the MediaCodec encoder
     */
    private class AudioThread extends Thread {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO); // THREAD_PRIORITY_URGENT_AUDIO
            //int cnt = 0;
      //      final int min_buffer_size = AudioRecord.getMinBufferSize(
      //              SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
      //      int buffer_size = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
      //      if (buffer_size < min_buffer_size)
      //          buffer_size = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;
            final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME).order(ByteOrder.nativeOrder());
            AudioRecord audioRecord = null;
            for ( int src: AUDIO_SOURCES) {
                try {
                    if (Build.MODEL.equals("rk3288")) {
                        src = MediaRecorder.AudioSource.CAMCORDER;
                        Log.e(TAG, "this device is rk3288,so mic choice MediaRecorder.AudioSource.CAMCORDER");
                    }
                    audioRecord = new AudioRecord(src,
                            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 640*4/*buffer_size*/);
                    if (audioRecord != null) {
                        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                            audioRecord.release();
                            audioRecord = null;
                        }
                    }
                } catch (final Exception e) {
                    audioRecord = null;
                }
                if (audioRecord != null) {
                    break;
                }
            }
            if (audioRecord != null) {
                try {
                    if (mIsCapturing) {
                        Log.v(TAG, "AudioThread:start audio recording");
                        int readBytes;
                        audioRecord.startRecording();
                        if (onPCmCallbackListener != null){
                            onPCmCallbackListener.onPcmStatusCallback(1);
                        }
                        try {
                            for ( ; mIsCapturing  ; ) {
                                // read audio data from internal mic
                                buf.clear();
                                try {
                                    readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME);
                                } catch (final Exception e) {
                                    break;
                                }
                                if (readBytes > 0) {
                                    // set audio data to encoder
                                    buf.position(readBytes);
                                    buf.flip();
                                    if (onPCmCallbackListener != null){
                                        onPCmCallbackListener.onPcmCallback(buf, readBytes);
                                    }
                                }
                            }
                        } finally {
                            audioRecord.stop();
                        }
                    }
                } catch (final Exception e) {
                    Log.e(TAG, "AudioThread#run", e);
                    if (onPCmCallbackListener != null){
                        onPCmCallbackListener.onPcmStatusCallback(0);
                    }
                } finally {
                    audioRecord.release();
                }
            }
            Log.v(TAG, "AudioThread:finished");
        }
    }

    public interface OnPCmCallbackListener{
        void onPcmCallback(ByteBuffer buffer, int readBytes);
        void onPcmStatusCallback(int status);
    }
}

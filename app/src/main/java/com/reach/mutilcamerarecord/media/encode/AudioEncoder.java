package com.reach.mutilcamerarecord.media.encode;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.reach.mutilcamerarecord.media.setting.MediaFrameData;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * 音频编码
 */
public class AudioEncoder {
    private static final String TAG = "AudioEncoder";
    private static final boolean VERBOSE = false;
    private static final String MIME_TYPE = "audio/mp4a-latm";    // H.264 Advanced Video Coding

    //数据处理线程
    private EncoderThread mEncoderThread;


    private AudioRecordCallBack audioRecordCallBack;

    //回调handler
    private AudioCallBackHandler audioCallbackHandler = new AudioCallBackHandler(this);


    public static final int MSG_Finish = 0;
    public static final int MSG_Fail = 1;
    public static final int MSG_Format_Confirm = 2;
    public static final int MSG_Frame_Available = 3;
    public static final int MSG_format_mp3 = 4;

    public void setSoundOff(boolean off) {
        if (mEncoderThread != null) {
            mEncoderThread.soundOff = off;
        }
    }

    public boolean isSoundOff() {
        return mEncoderThread != null && mEncoderThread.soundOff;
    }


    private static class AudioCallBackHandler extends Handler {
        private WeakReference<AudioEncoder> audioEncoderWeakReference;

        public AudioCallBackHandler(AudioEncoder audioEncoder) {
            this.audioEncoderWeakReference = new WeakReference<AudioEncoder>(audioEncoder);
        }

        @Override
        public void handleMessage(Message msg) {
            if (audioEncoderWeakReference.get() == null)
                return;
            if (msg.what == MSG_Finish) {
                audioEncoderWeakReference.get().audioRecordCallBack.finish();
            } else if (msg.what == MSG_Fail)
                audioEncoderWeakReference.get().audioRecordCallBack.failure((String) msg.obj);
            else if (msg.what == MSG_Format_Confirm) {
                audioEncoderWeakReference.get().audioRecordCallBack.formatConfirm((MediaFormat) msg.obj);
            } else if (msg.what == MSG_Frame_Available) {
                audioEncoderWeakReference.get().audioRecordCallBack.frameAvailable((MediaFrameData) msg.obj);
            } else if (msg.what == MSG_format_mp3){
                audioEncoderWeakReference.get().audioRecordCallBack.mp3FormatConfirm((MediaFormat) msg.obj);
            }
            super.handleMessage(msg);
        }
    }


    public interface AudioRecordCallBack {
        void finish();

        void failure(String msg);

        int formatConfirm(MediaFormat mediaFormat);

        void frameAvailable(MediaFrameData frameData);

        int mp3FormatConfirm(MediaFormat mediaFormat);
    }


    public AudioEncoder(int sampleRate, int bit_rate, int bufferSizeInBytes, AudioRecordCallBack AudioRecordCallBack) throws IOException {
        this.audioRecordCallBack = AudioRecordCallBack;
        MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, sampleRate, 1);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);//CHANNEL_IN_STEREO 立体声
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, bit_rate);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);

        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSizeInBytes * 2);


        MediaCodec mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncoder.start();

        mEncoderThread = new EncoderThread(mEncoder, bufferSizeInBytes, audioCallbackHandler);
        mEncoderThread.start();
        mEncoderThread.waitUntilReady();
    }

    public void setAudioRecord(AudioRecord audioRecord) {
        if (mEncoderThread != null)
            mEncoderThread.setAudioRecord(audioRecord);
    }

    public IAudioListener getAudioListener(){
        if (mEncoderThread != null)
            return mEncoderThread.audioListener;
        else
            return null;
    }


    /**
     * 结束音频编码
     */
    public void shutdown(boolean succeed) {
        if (mEncoderThread == null)
            return;
        mEncoderThread.record = false;
        mEncoderThread.audioListener = null;
        mEncoderThread.canSend = false;
        Handler handler = mEncoderThread.getHandler();

        if (handler != null)
            handler.sendMessage(handler.obtainMessage(EncoderThread.EncoderHandler.MSG_SHUTDOWN, succeed));
    }

    /**
     * 开始音频编码
     */
    public void startRecordAudioLoop() {
        Handler handler = mEncoderThread.getHandler();
        if (handler != null)
            handler.sendMessage(handler.obtainMessage(
                    EncoderThread.EncoderHandler.MSG_FRAME_START));
    }


    /**
     * 音频编码
     */
    private static class EncoderThread extends Thread {
        private MediaCodec mEncoder;
        public MediaFormat mEncodedFormat;
        private MediaCodec.BufferInfo mBufferInfo;

        private EncoderHandler mHandler;
        private final Object mLock = new Object();
        private volatile boolean mReady = false;
    //    private AudioRecord audioRecord;
        private int bufferSizeInBytes;

        public AudioCallBackHandler callBackHandler;

        //是否录制
        public volatile boolean record = false;
        //静音
        public volatile boolean soundOff = false;
        private boolean succeed = false;

        private boolean canSend = false;//uvc的音频是否能发给此线程
        private IAudioListener audioListener;


        private EncoderThread(MediaCodec mediaCodec, int bufferSizeInBytes, AudioCallBackHandler handler) {
            this.callBackHandler = handler;
            mEncoder = mediaCodec;
            mBufferInfo = new MediaCodec.BufferInfo();
            this.bufferSizeInBytes = bufferSizeInBytes;
            record = true;
            initAudioListener();
        }

        public void release() {
            if (mEncoder != null) {
                mEncoder.stop();
                mEncoder.release();
                mEncoder = null;
            }
            /*if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }*/
            audioListener = null;
            canSend = false;
        }

        public void setAudioRecord(AudioRecord audioRecord) {
           // this.audioRecord = audioRecord;
        }

        @Override
        public void run() {
            Looper.prepare();
            mHandler = new EncoderHandler(this);    // must create on encoder thread
            synchronized (mLock) {
                mReady = true;
                mLock.notify();    // signal waitUntilReady()
            }
            Looper.loop();
            synchronized (mLock) {
                mReady = false;
                mHandler = null;
            }
            Log.d(TAG, "looper quit  " + succeed);
            if (succeed)
                callBackHandler.sendEmptyMessage(MSG_Finish);
            release();
        }

        public void waitUntilReady() {
            synchronized (mLock) {
                while (!mReady) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ie) { /* not expected */ }
                }
            }
        }

        public EncoderHandler getHandler() {
            synchronized (mLock) {
                // Confirm ready state.
                if (!mReady) {
                    return null;
                }
            }
            return mHandler;
        }


        private void writeTo(ByteBuffer buf, int length, long timeStamp) throws Exception {
            int inputBufferIndex = mEncoder.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                ByteBuffer[] byteBuffers = mEncoder.getInputBuffers();
                ByteBuffer byteBuffer = byteBuffers[inputBufferIndex];
                byteBuffer.clear();
                byteBuffer.position(0);
                byteBuffer.put(buf);
                if (length <= 0) {
                    mEncoder.queueInputBuffer(inputBufferIndex, 0, length, timeStamp, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    mEncoder.queueInputBuffer(inputBufferIndex, 0, length, timeStamp, 0);
                }
            }

        }

        private long prevOutputPTSUs = 0;

        long getPTSUs() {
            long result = System.nanoTime() / 1000L;
            if (result < prevOutputPTSUs)
                result = (prevOutputPTSUs - result) + result;
            return result;
        }

        //写入编码器
        private boolean writeToEncoder(ByteBuffer buf, boolean isRecord) throws Exception {
            if (isRecord) {
                int readSize = 0;//audioRecord.read(buf, bufferSizeInBytes);
                if (readSize > 0) {
                    prevOutputPTSUs = getPTSUs();
                    handleAudio(buf, soundOff);
                    writeTo(buf, readSize, prevOutputPTSUs);
                } else {
                    return false;
                }

            } else {
                //写最后一帧
                writeTo(buf, 0, prevOutputPTSUs);
            }
            return true;
        }

        //读出被编码的信息
        @SuppressLint("WrongConstant")
        private void readFromEncoder() throws Exception {
            final int TIMEOUT_USEC = 0;     // no timeout -- check for buffers, bail if none

            ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
            while (true) {
                int encoderStatus = 0;
                encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break;
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    encoderOutputBuffers = mEncoder.getOutputBuffers();
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    mEncodedFormat = mEncoder.getOutputFormat();
                    Log.e("readFromEncoder", "get mEncodedFormat:" + mEncodedFormat);
                    callBackHandler.sendMessage(Message.obtain(callBackHandler, MSG_Format_Confirm, mEncodedFormat));
                    callBackHandler.sendMessage(Message.obtain(callBackHandler, MSG_format_mp3, mEncoder.getOutputFormat()));
                } else if (encoderStatus < 0) {
                    Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                            encoderStatus);
                } else {
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        return;
                    }

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        mBufferInfo.size = 0;
                    }

                    if (mBufferInfo.size != 0) {
                        MediaFrameData mediaFrameData = new MediaFrameData(encodedData, mBufferInfo);
                        callBackHandler.sendMessage(Message.obtain(callBackHandler, MSG_Frame_Available, mediaFrameData));
                    }
                    mEncoder.releaseOutputBuffer(encoderStatus, false);
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                        break;      // out of while
                    }
                }
            }
        }

        /**
         * 读取PCM并编码音频
         */
        private boolean readAndEncode(boolean isRecord) {
            /*if (audioRecord == null)
                return true;*/
            ByteBuffer buf = ByteBuffer.allocateDirect(bufferSizeInBytes);
            try {
                if (!writeToEncoder(buf, isRecord))
                    return true;
                readFromEncoder();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        //调整声音大小
        private void handleAudio(ByteBuffer buf, boolean soundOff) {
            short data[] = new short[buf.capacity() / 2];
            buf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(data);
            final float radio = 5f;
            for (int i = 0; i < data.length; i++) {
                short aShort = data[i];
                if (aShort > 0) {
                    if (aShort * radio < 32768)
                        aShort = (short) (radio * aShort);
                    else
                        aShort = 32767;
                } else if (aShort < 0) {
                    if (aShort * radio >= -32768)
                        aShort = (short) (radio * aShort);
                    else
                        aShort = -32768;
                }
                data[i] = soundOff ? 0 : aShort;
            }
            buf.clear();
            buf.position(0);
            buf.asShortBuffer().put(data);
        }

        /**
         * 开始处理-----PCM线程------------------------------------------------------------------------------
         * <p>
         * See notes for {@link AudioEncoder}.
         */
        void startVideoFrameLoop() {
            while (record) {
                synchronized (AudioEncoder.class) {
                    if (!readAndEncode(true)) {
                        succeed = false;
                        callBackHandler.sendMessage(Message.obtain(callBackHandler, MSG_Fail, "音频编码错误！"));
                        shutdown();
                        return;
                    }
                }

            }
            //写入最后一帧
            readAndEncode(false);
        }

        private void initAudioListener(){
            audioListener = new IAudioListener() {
                @Override
                public void onPcm(ByteBuffer buf, int readSize) {
                    try {
                        if (canSend) {
                            if (record) {
                                if (readSize > 0) {
                                    prevOutputPTSUs = getPTSUs();
                                    //handleAudio(buf, soundOff);
                                    writeTo(buf, readSize, prevOutputPTSUs);
                                    readFromEncoder();
                                }
                            } else {
                                //写入最后一帧
                                readAndEncode(false);
                            }
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            };
        }


        /**
         * Tells the Looper to quit.
         */
        private void shutdown() {
            getHandler().getLooper().quit();
        }


        /**
         * 事件消息管理
         */
        private static class EncoderHandler extends Handler {
            //数据有更新
            public static final int MSG_FRAME_START = 1;
            //结束录制
            public static final int MSG_SHUTDOWN = 3;

            // This shouldn't need to be a weak ref, since we'll go away when the Looper quits,
            // but no real harm in it.
            private WeakReference<EncoderThread> mWeakEncoderThread;

            /**
             * Constructor.  Instantiate object from encoder thread.
             */
            public EncoderHandler(EncoderThread et) {
                mWeakEncoderThread = new WeakReference<EncoderThread>(et);
            }

            @Override  // runs on encoder thread
            public void handleMessage(Message msg) {
                int what = msg.what;
                if (VERBOSE) {
                    Log.v(TAG, "EncoderHandler: what=" + what);
                }

                EncoderThread encoderThread = mWeakEncoderThread.get();
                if (encoderThread == null) {
                    Log.w(TAG, "EncoderHandler.handleMessage: weak ref is null");
                    return;
                }

                switch (what) {
                    case MSG_FRAME_START:
                       // encoderThread.audioRecord.startRecording();
                      //  encoderThread.startVideoFrameLoop();
                        encoderThread.canSend = true;
                        break;
                    case MSG_SHUTDOWN:
                        encoderThread.succeed = (boolean) msg.obj;
                        encoderThread.shutdown();
                        break;
                    default:
                        throw new RuntimeException("unknown message " + what);
                }
            }
        }
    }


    public MediaFormat getFormat() {
        if (mEncoderThread != null)
            return mEncoderThread.mEncodedFormat;
        return null;
    }

}

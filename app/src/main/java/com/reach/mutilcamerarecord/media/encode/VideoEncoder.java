package com.reach.mutilcamerarecord.media.encode;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.reach.mutilcamerarecord.media.setting.MediaFrameData;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import static android.media.MediaFormat.KEY_FRAME_RATE;


/**
 * 视频编码
 */
public class VideoEncoder {
    private static final String TAG = "VideoEncoder";
    private static final boolean VERBOSE = false;

    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int IFRAME_INTERVAL = 1;           // sync frame every second

    //数据处理线程
    private EncoderThread mEncoderThread;
    //硬编码显存
    private Surface mInputSurface;


    public static final int MSG_BUFFER_STATUS = 3;
    public static final int MSG_Format_Confirm = 4;
    public static final int MSG_Frame_Available = 5;
    public static final int MSG_Thread_Finish = 0;
    public static final int MSG_Thread_Error = 1;


    /**
     * Configures encoder, and prepares the input Surface.
     *
     * @param width                宽
     * @param height               高
     * @param bitRate              比特率
     * @param frameRate            帧率
     * @param videoCallBackHandler 回调信息
     * @param colorFormat          色彩格式
     */
    public VideoEncoder(int width, int height, int bitRate, int frameRate,
                        Handler videoCallBackHandler, int colorFormat) throws IOException {

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

        MediaCodec mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mEncoder.createInputSurface();
        mEncoder.start();


        mEncoderThread = new EncoderThread(mEncoder, videoCallBackHandler);
        mEncoderThread.start();
        mEncoderThread.waitUntilReady();
    }


    /**
     * Returns the encoder's input surface.
     */
    public Surface getInputSurface() {
        return mInputSurface;
    }

    /**
     * 结束
     *
     * @param succeed
     */
    public void shutdown(boolean succeed) {
        if (mEncoderThread == null)
            return;
        Handler handler = mEncoderThread.getHandler();
        handler.sendMessage(handler.obtainMessage(EncoderThread.EncoderHandler.MSG_SHUTDOWN, succeed));
    }


    /**
     * 帧绘制完毕调用
     */
    public void frameAvailableSoon() {
        Handler handler = mEncoderThread.getHandler();
        handler.sendMessage(handler.obtainMessage(
                EncoderThread.EncoderHandler.MSG_FRAME_AVAILABLE_SOON));
    }


    /**
     * 编码线程
     */
    private static class EncoderThread extends Thread {
        private MediaCodec mEncoder;
        public MediaFormat mEncodedFormat;
        private MediaCodec.BufferInfo mBufferInfo;

        private EncoderHandler mHandler;
        private Handler videoCallBackHandler;
        private int mFrameNum;
        private final Object mLock = new Object();
        private volatile boolean mReady = false;
        private boolean isCallTimeOut = false;
        private long firstTime = -1;
        private long currentTime = 0;
        private boolean succeed = false;

        public void release() {
            if (mEncoder != null) {
                mEncoder.stop();
                mEncoder.release();
                mEncoder = null;
            }
        }

        private EncoderThread(MediaCodec mediaCodec, Handler videoCallBackHandler) {
            mEncoder = mediaCodec;
            this.videoCallBackHandler = videoCallBackHandler;

            mBufferInfo = new MediaCodec.BufferInfo();
        }

        /**
         * Thread entry point.
         * <p>
         * Prepares the Looper, Handler, and signals anybody watching that we're ready to go.
         */
        @Override
        public void run() {
            Looper.prepare();
            mHandler = new EncoderHandler(this);    // must create on encoder thread
            Log.d(TAG, "encoder thread ready");
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
                finish();
            release();

        }

        /**
         * Waits until the encoder thread is ready to receive messages.
         * <p>
         * Call from non-encoder thread.
         */
        public void waitUntilReady() {
            synchronized (mLock) {
                while (!mReady) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ie) { /* not expected */ }
                }
            }
        }

        /**
         * Returns the Handler used to send messages to the encoder thread.
         */
        public EncoderHandler getHandler() {
            synchronized (mLock) {
                if (!mReady) {
                    throw new RuntimeException("not ready");
                }
            }
            return mHandler;
        }

        /**
         *
         */

        public void encodeVideo() {
            ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
            while (true) {
                try {
                    if (!doVideoEncodeLoop(encoderOutputBuffers))
                        break;
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        @SuppressLint("WrongConstant")
        private boolean doVideoEncodeLoop(ByteBuffer[] encoderOutputBuffers) throws Exception {
            final int TIMEOUT_USEC = 0;
            int encoderStatus = 0;
            encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                return false;
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                encoderOutputBuffers = mEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                mEncodedFormat = mEncoder.getOutputFormat();
                formatConfirm(mEncodedFormat);
                waitUntilReady();
            } else if (encoderStatus < 0) {
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0) {
                    currentTime = mBufferInfo.presentationTimeUs;
                    if (firstTime == -1)
                        firstTime = currentTime;
                    frameAvailable(new MediaFrameData(encodedData, mBufferInfo));
                }
                mEncoder.releaseOutputBuffer(encoderStatus, false);
                if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    Log.i("lzc", "视频结束帧");
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    return false;
                }
            }
            return true;
        }

        /**
         * 通知 帧数据处理完毕
         * <p>
         * See notes for {@link VideoEncoder#frameAvailableSoon()}.
         */
        void frameAvailableSoon() throws Exception {
            if (VERBOSE) Log.d(TAG, "startRecordAudioLoop");
            encodeVideo();
            mFrameNum++;
            if ((mFrameNum % 10) == 0) {
                bufferStatus(currentTime - firstTime);
            }
        }


        /**
         * Tells the Looper to quit.
         */
        private void shutdown() {
            if (VERBOSE) Log.d(TAG, "shutdown");
            getHandler().getLooper().quit();
        }


        // CircularEncoder.Callback, called on encoder thread
        //long64位拆为两个32位int的数据
        public void bufferStatus(long totalTimeMsec) {
            videoCallBackHandler.sendMessage(videoCallBackHandler.obtainMessage(MSG_BUFFER_STATUS,
                    (int) (totalTimeMsec >> 32), (int) totalTimeMsec));
        }

        public void formatConfirm(MediaFormat mediaFormat) {
            videoCallBackHandler.sendMessage(videoCallBackHandler.obtainMessage(MSG_Format_Confirm, mediaFormat));
        }

        public void frameAvailable(MediaFrameData mediaFrameData) {
            videoCallBackHandler.sendMessage(videoCallBackHandler.obtainMessage(MSG_Frame_Available, mediaFrameData));
        }

        public void finish() {
            videoCallBackHandler.sendMessage(videoCallBackHandler.obtainMessage(MSG_Thread_Finish));
        }

        public void error(String msg) {
            videoCallBackHandler.sendMessage(videoCallBackHandler.obtainMessage(MSG_Thread_Error, msg));
        }


        /**
         * 事件消息管理
         */
        private static class EncoderHandler extends Handler {
            //数据有更新
            public static final int MSG_FRAME_AVAILABLE_SOON = 1;
            //结束录制
            public static final int MSG_SHUTDOWN = 3;

            private WeakReference<EncoderThread> mWeakEncoderThread;

            public EncoderHandler(EncoderThread et) {
                mWeakEncoderThread = new WeakReference<EncoderThread>(et);
            }

            @Override  // runs on encoder thread
            public void handleMessage(Message msg) {
                int what = msg.what;
                EncoderThread encoderThread = mWeakEncoderThread.get();
                if (encoderThread == null) {
                    return;
                }

                switch (what) {
                    case MSG_FRAME_AVAILABLE_SOON:
                        try {
                            encoderThread.frameAvailableSoon();
                        } catch (Exception e) {
                            e.printStackTrace();
                            encoderThread.error("视频编码失败！");
                            encoderThread.succeed = false;
                            encoderThread.shutdown();
                        }
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

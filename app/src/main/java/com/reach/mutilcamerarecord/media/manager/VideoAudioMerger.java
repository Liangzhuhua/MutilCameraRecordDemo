package com.reach.mutilcamerarecord.media.manager;

import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.reach.mutilcamerarecord.media.setting.MediaFrameData;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by lzc on 2017/8/24 0024.
 * 混合音轨和视频轨
 */

public class VideoAudioMerger {
    private CallBack callBack;
    private volatile boolean isCompound = false;
    private CallBackHandler callBackhandler;
    private File file;
    private File mp3File;
    private CompoundThread compoundThread;
    private MediaMuxer muxer = null;
    private MediaMuxer musicMuxer = null;//合成MP3

    //结果回调
    private static final int MSG_Result_Error = 1;
    private static final int MSG_Result_Succeed = 0;

    //线程工作handler
    private static final int MSG_Work_Start = 1;
    private static final int MSG_Work_End = 2;
    private static final int MSG_Work_Frame = 0;
    private static final int MSG_AAC_Frame = 3;//mp3

    public VideoAudioMerger(File file) {
        this.file = file;
    }

    public VideoAudioMerger(){
        ;//do nothing
    }

    public void setFile(File file, File mp3){
        this.file = file;
        this.mp3File = mp3;
    }



    public boolean initCompoundVideo() {
        try {
            isCompound = false;
            muxer = new MediaMuxer(file.getPath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            musicMuxer = new MediaMuxer(mp3File.getPath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        Log.e("initCompoundVideo", "start init mp4 and mp3 thread...");
        compoundThread = new CompoundThread(this, file);
        compoundThread.start();
        return true;
    }

    //停止合成
    public void shutdownCompoundVideo() {
        isCompound = false;
        if (compoundThread != null && compoundThread.getHandler() != null && compoundThread.isAlive()) {
            compoundThread.getHandler().sendMessage(Message.obtain(compoundThread.getHandler(), MSG_Work_End, true));
        }

    }

    //停止合成
    public void cancelCompoundVideo() {
        if (compoundThread != null && compoundThread.getHandler() != null && compoundThread.isAlive()) {
            compoundThread.getHandler().sendMessage(Message.obtain(compoundThread.getHandler(), MSG_Work_End, false));
        }

    }

    public int addTrack(MediaFormat mediaFormat) throws Exception {

        return muxer.addTrack(mediaFormat);
    }

    public int addAudioTrack(MediaFormat mediaFormat) throws Exception{
        return musicMuxer.addTrack(mediaFormat);
    }

    //开始
    public void start() {
        if (compoundThread != null && compoundThread.getHandler() != null && compoundThread.isAlive()) {
            compoundThread.getHandler().sendMessage(Message.obtain(compoundThread.getHandler(), MSG_Work_Start));
        }
    }

    //传递一帧
    public void frameAvailable(MediaFrameData mediaFrameData, int track) {
        if (compoundThread != null && compoundThread.isAlive()) {
            compoundThread.getHandler().sendMessage(Message.obtain(compoundThread.getHandler(), MSG_Work_Frame, track, 0, mediaFrameData));
        }

    }

    //传递一帧aac，合并为mp3专用
    public void frameAacAvailable(MediaFrameData mediaFrameData, int track){
        if (compoundThread != null && compoundThread.isAlive()) {
            compoundThread.getHandler().sendMessage(Message.obtain(compoundThread.getHandler(), MSG_AAC_Frame, track, 0, mediaFrameData));
        }
    }


    //混合线程
    private static class CompoundThread extends Thread {
        private File file;
        private WeakReference<VideoAudioMerger> helper;
        private CompoundHandler compoundHandler;
        private volatile boolean isSucceed = true;
        private Queue<WrapperData> mediaFrameDataQueue = new ArrayBlockingQueue<WrapperData>(100);
        private Queue<WrapperData> mediaFrameAacQueue  = new ArrayBlockingQueue<>(50);

        public CompoundThread(VideoAudioMerger videoAudioMerger,
                              File file) {
            this.file = file;
            helper = new WeakReference<VideoAudioMerger>(videoAudioMerger);
        }

        @Override
        public void run() {
            Looper.prepare();
            compoundHandler = new CompoundHandler(this);
            Looper.loop();
            if (helper.get() != null) {
                if (isSucceed) {
                    try {
                        helper.get().muxer.stop();
                        helper.get().musicMuxer.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                        sendMessageFail("停止合成失败");
                        return;
                    }
                    if (isSucceed)
                        sendMessageSuccess(file);
                }
                try {
                    helper.get().muxer.release();
                    helper.get().musicMuxer.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }


        public void shutdown() {
           getHandler().getLooper().quit();
        }

        /**
         * @return nullable
         */
        public CompoundHandler getHandler() {
            return compoundHandler;
        }

        private void sendMessageFail(String msg) {
            if (helper.get() != null) {
                Message message = Message.obtain();
                message.what = MSG_Result_Error;
                message.obj = msg;
                helper.get().callBackhandler.sendMessage(message);
            }

        }

        private void sendMessageSuccess(File file) {
            if (helper.get() != null) {
                Message message = Message.obtain();
                message.what = MSG_Result_Succeed;
                message.obj = file;
                helper.get().callBackhandler.sendMessage(message);
            }
        }


        private void frameAvailable(int track, MediaFrameData frameData) throws Exception {
            if (helper.get() != null) {
                helper.get().muxer.writeSampleData(track, frameData.getBufferData(), frameData.getInfo());
            }

        }

        private void aacFrameAvailable(int track, MediaFrameData frameData) throws Exception {
            if (helper.get() != null){
                helper.get().musicMuxer.writeSampleData(track, frameData.getBufferData(), frameData.getInfo());
                //Log.e("aacFrameAvailable", "track=" + track + ",buffer=" + frameData.buf + ",info" + frameData.getInfo() + ",size=" + frameData.getInfo().size);
            }
        }

    }

    private static class WrapperData {
        int arg = 0;
        MediaFrameData mediaFrameData;

        public WrapperData(int arg, MediaFrameData mediaFrameData) {
            this.arg = arg;
            this.mediaFrameData = mediaFrameData;
        }
    }

    //合并音频视频线程消息
    private static class CompoundHandler extends Handler {
        private WeakReference<CompoundThread> compoundThreadWeakReference;

        private CompoundHandler(CompoundThread compoundThread) {
            compoundThreadWeakReference = new WeakReference<CompoundThread>(compoundThread);
        }

        @Override
        public void handleMessage(Message msg) {
            //帧编码完毕
            CompoundThread compoundThread = compoundThreadWeakReference.get();
            if (compoundThread != null) {
                if (msg.what == MSG_Work_Frame) {
                    try {
                        compoundThread.mediaFrameDataQueue.add(new WrapperData(msg.arg1, (MediaFrameData) msg.obj));
                        if (compoundThread.helper.get().isCompound) {
                            while (!compoundThread.mediaFrameDataQueue.isEmpty()) {
                                WrapperData wrapperData = compoundThread.mediaFrameDataQueue.poll();
                                compoundThread.frameAvailable(wrapperData.arg, wrapperData.mediaFrameData);
                            }

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        compoundThread.sendMessageFail("合并失败");
                        compoundThread.isSucceed = false;
                        compoundThread.shutdown();

                    }
                }
                //开始
                else if (msg.what == MSG_Work_Start) {
                    try {
                        if (compoundThread.helper.get() != null) {
                            compoundThread.helper.get().muxer.start();
                            compoundThread.helper.get().musicMuxer.start();
                            compoundThread.helper.get().isCompound = true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        compoundThread.sendMessageFail("开始合成失败");
                        compoundThread.isSucceed = false;
                        compoundThread.shutdown();
                    }
                } else if (msg.what == MSG_Work_End) {
                    compoundThread.isSucceed = (boolean) msg.obj;
                    compoundThread.shutdown();
                }
                //AAC转MP3
                else if (msg.what == MSG_AAC_Frame){
                    try {
                        compoundThread.mediaFrameAacQueue.add(new WrapperData(msg.arg1, (MediaFrameData) msg.obj));
                        if (compoundThread.helper.get().isCompound) {
                            while (!compoundThread.mediaFrameAacQueue.isEmpty()) {
                                WrapperData wrapperData = compoundThread.mediaFrameAacQueue.poll();
                                compoundThread.aacFrameAvailable(wrapperData.arg, wrapperData.mediaFrameData);
                            }

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        compoundThread.sendMessageFail("merge to mp3 is fail!!!");
                        compoundThread.isSucceed = false;
                        compoundThread.shutdown();

                    }
                }
            }
            super.handleMessage(msg);
        }
    }

    //回调消息
    private static class CallBackHandler extends Handler {
        private WeakReference<VideoAudioMerger> callBackWeakReference;

        private CallBackHandler(VideoAudioMerger helper) {
            callBackWeakReference = new WeakReference<VideoAudioMerger>(helper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            VideoAudioMerger helper = callBackWeakReference.get();
            if (helper == null)
                return;
            if (msg.what == MSG_Result_Succeed) {
                helper.callBack.compoundSuccess((File) msg.obj);
            } else if (msg.what == MSG_Result_Error) {
                helper.callBack.compoundFail((String) msg.obj);
            }

        }
    }



    public interface CallBack {
        void compoundFail(String msg);

        void compoundSuccess(File file);
    }

    public CallBack getCallBack() {
        return callBack;
    }

    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
        callBackhandler=new CallBackHandler(this);
    }
}

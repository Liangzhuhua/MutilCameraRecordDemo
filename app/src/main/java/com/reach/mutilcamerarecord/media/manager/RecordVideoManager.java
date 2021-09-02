package com.reach.mutilcamerarecord.media.manager;

import android.content.Context;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Message;
import android.view.Surface;

import com.reach.mutilcamerarecord.media.encode.VideoEncoder;
import com.reach.mutilcamerarecord.media.setting.MediaFrameData;

import java.lang.ref.WeakReference;

/**
 * Created by ${UserWrapper} on 2017/5/3 0003.
 * 录制视频控制
 */

public class RecordVideoManager {
    private Context context;
    private CallBackEvent event;
    private VideoEncoder videoEncoder;


    private boolean is_record = false;
    private RecordHandler recordHandler;

    private int recordWidth;
    private int recordHeight;

    private int track;


    public RecordVideoManager(Context context) {
        this.context = context;
        recordHandler = new RecordHandler(this);

    }


    private void updateBufferStatus(long duration) {
        if (event != null)
            event.onDuringUpdate(duration / 1000000.0f);
    }


    //录制
    public boolean startRecord(int width, int height, int fps, int bitRatio, int colorFormat) {
        if (is_record)
            return false;
        try {
            videoEncoder = new VideoEncoder(width, height, bitRatio, fps, recordHandler, colorFormat);
            is_record = true;
            recordWidth = width;
            recordHeight = height;
        } catch (Exception ioe) {
            ioe.printStackTrace();
            if (event != null) {
                event.recordVideoError("启动失败");
                return false;
            }

        }
        if (event != null)
            event.startRecordSucceed();
        return true;
    }

    //获取录制视窗
    public Surface getInputSurface() {
        if (videoEncoder != null)
            return videoEncoder.getInputSurface();
        return null;
    }


    /**
     * 停止录制
     */
    public void stopRecord() {
        if (!is_record)
            return;
        if (videoEncoder != null) {
            videoEncoder.shutdown(true);
        }
        is_record = false;
    }

    public void cancelRecord() {
        if (!is_record)
            return;
        if (videoEncoder != null) {
            videoEncoder.shutdown(false);
        }
        is_record = false;
    }


    //告知数据准备完毕
    public void callRecordFrameAvailable() {
        if (videoEncoder != null && is_record)
            videoEncoder.frameAvailableSoon();
    }


    private static class RecordHandler extends Handler {
        private WeakReference<RecordVideoManager> recordVideoManagerRef;

        public RecordHandler(RecordVideoManager recordVideoManagerRef) {
            this.recordVideoManagerRef = new WeakReference<RecordVideoManager>(recordVideoManagerRef);
        }


        @Override
        public void handleMessage(Message msg) {
            RecordVideoManager recordVideoManager = recordVideoManagerRef.get();
            if (recordVideoManager == null) {
                return;
            }

            switch (msg.what) {
                case VideoEncoder.MSG_BUFFER_STATUS: {
                    long duration = (((long) msg.arg1) << 32) |
                            (((long) msg.arg2) & 0xffffffffL);
                    recordVideoManager.updateBufferStatus(duration);
                    break;
                }
                case VideoEncoder.MSG_Format_Confirm: {
                    recordVideoManager.track = recordVideoManager.event.formatConfirm((MediaFormat) msg.obj);
                    break;
                }
                case VideoEncoder.MSG_Frame_Available: {
                    if (recordVideoManager.is_record)
                        recordVideoManager.event.frameAvailable((MediaFrameData) msg.obj);
                    break;

                }
                case VideoEncoder.MSG_Thread_Finish: {
                    recordVideoManager.event.recordVideoFinish();
                    break;
                }
                case VideoEncoder.MSG_Thread_Error: {
                    recordVideoManager.event.recordVideoError((String) msg.obj);
                    break;
                }
                default:
                    throw new RuntimeException("Unknown message " + msg.what);
            }
        }
    }

    public VideoEncoder getVideoEncoder() {
        return videoEncoder;
    }

    public interface CallBackEvent {
        void startRecordSucceed();

        void onDuringUpdate(float time);

        void recordVideoFinish();

        void recordVideoError(String errorMsg);

        int formatConfirm(MediaFormat mediaFormat);

        void frameAvailable(MediaFrameData frameData);
    }

    public CallBackEvent getEvent() {
        return event;
    }

    public void setEvent(CallBackEvent event) {
        this.event = event;
    }

    public int getRecordWidth() {
        return recordWidth;
    }

    public int getRecordHeight() {
        return recordHeight;
    }

    public boolean is_record() {
        return is_record;
    }

    public int getTrack() {
        return track;
    }
}

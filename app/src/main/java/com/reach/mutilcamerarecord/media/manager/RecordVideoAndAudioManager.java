package com.reach.mutilcamerarecord.media.manager;

import android.app.Activity;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.view.SurfaceView;

import androidx.annotation.Nullable;

import com.reach.mutilcamerarecord.media.base.RecordManageBase;
import com.reach.mutilcamerarecord.media.encode.IAudioListener;
import com.reach.mutilcamerarecord.media.setting.CameraSetting;
import com.reach.mutilcamerarecord.media.setting.MediaFrameData;
import com.reach.mutilcamerarecord.media.setting.RecordSetting;
import com.reach.mutilcamerarecord.media.setting.RenderSetting;

import java.io.File;

/**
 * Created by Lzc on 2018/3/22 0022.
 */

public class RecordVideoAndAudioManager extends RecordManageBase {
    //合并音频和视频
    private VideoAudioMerger videoAudioMerger;

    private int formatConfirmSucceedCount = 0;

    private boolean vatrack = false;

    //private File file;

    public RecordVideoAndAudioManager(Activity context, File file,
                                      @Nullable RecordSetting recordSetting,
                                      @Nullable CameraSetting cameraSetting,
                                      @Nullable RenderSetting renderSetting,
                                      SurfaceView surfaceView) {
        super(context, recordSetting, cameraSetting, renderSetting, surfaceView);

        videoAudioMerger = new VideoAudioMerger();
        initCallBack();
    }

    private void initCallBack() {
        videoAudioMerger.setCallBack(new VideoAudioMerger.CallBack() {

            @Override
            public void compoundFail(String msg) {
                cancelRecord();
                callBackEvent.recordError(msg);

            }

            @Override
            public void compoundSuccess(File file) {
                cancelRecord();

                // TODO: 2019/03/15
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(file.getPath());
                //Bitmap bmp = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                // FileUntil.saveImageAndGetFile(bmp, videoImg, -1, Bitmap.CompressFormat.JPEG);
Log.e("TAG", "compoundSuccess--------" + file.getAbsolutePath());
                callBackEvent.stopRecordFinish(file);
            }
        });
    }

    @Override
    protected void onRecordStop() {
        videoAudioMerger.shutdownCompoundVideo();
        vatrack = false;
    }

    @Override
    public void startRecord() {
        super.startRecord();//启动音视频数据
        onRecordStart();//启动MeadiaMuxer
    }

    public void setFileName(File file, File mp3){
        videoAudioMerger.setFile(file, mp3);
    }

    private void onRecordStart() {
        formatConfirmSucceedCount = 0;
        if (!videoAudioMerger.initCompoundVideo()) {//传入文件名
            callBackEvent.recordError("开始合成器失败!");
            destroy();
        }
    }

    public IAudioListener getGLAudioListener(){
        return super.getGLAudioListener();
    }

    @Override
    protected void onFrameAvailable(DataType type, MediaFrameData frameData) {
        if (type == DataType.Type_Video) {
            videoAudioMerger.frameAvailable(frameData, recordVideoManager.getTrack());
        } else {
            videoAudioMerger.frameAvailable(frameData, recordAudioManager.getTrack());
            videoAudioMerger.frameAacAvailable(frameData, recordAudioManager.getMusicTrack());//mp3
        }
    }

    @Override
    protected int onFormatConfirm(DataType type, MediaFormat mediaFormat) {
        int trace = -1;
        try {
            if (type == DataType.Type_Video) {
                if (needVideo) {
                    trace = videoAudioMerger.addTrack(mediaFormat);
                    Log.e("onFormatConfirm", "add video track=" + trace + ",mediaFormat:" + mediaFormat);
                }
            } else if (type == DataType.Type_Audio) {
                if (needAudio) {
                    trace = videoAudioMerger.addTrack(mediaFormat);
                    Log.e("onFormatConfirm", "add Audio track=" + trace + ",type=" + type);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            cancelRecord();
            callBackEvent.recordError("添加合并轨道失败！");
        }
        formatConfirmSucceedCount++;
        if (formatConfirmSucceedCount == 3/*MaxCount()*/)
            videoAudioMerger.start();
        return trace;
    }

    @Override
    public int onMusicFormatConfirm(DataType type, final MediaFormat mediaFormat){
        int trace = -1;
        try{
            trace = videoAudioMerger.addAudioTrack(mediaFormat);
            Log.e("onFormatConfirm", "add music track=" + trace + ",type=" + type);
        } catch (Exception e){
            e.printStackTrace();
        }
        formatConfirmSucceedCount++;
        if (formatConfirmSucceedCount == 3/*MaxCount()*/)
            videoAudioMerger.start();
        return trace;
    }




    @Override
    public void cancelRecord() {
        super.cancelRecord();
        if (videoAudioMerger != null)
            videoAudioMerger.cancelCompoundVideo();
        vatrack = false;
    }

    @Override
    public void destroy() {
        super.destroy();
        if (videoAudioMerger != null)
            videoAudioMerger.cancelCompoundVideo();
        vatrack = false;
    }

    /*public File getFile() {
        return file;
    }*/

//    public File getVideoImg() {
//        return videoImg;
//    }
}

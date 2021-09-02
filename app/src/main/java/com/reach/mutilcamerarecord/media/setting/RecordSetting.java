package com.reach.mutilcamerarecord.media.setting;

import android.media.MediaCodecInfo;

/**
 * Created by Lzc on 2018/3/22 0022.
 */

public class RecordSetting {
    //音频采样率
    private final static int AudioSampleRateDefault = 16000;
    //编码码率
    private final static int AudioRateDefault = 64000;
    //视频清晰度Level
    private static final float BitRatioDefault = 2.2f;
    //视频默认帧率
    private static final int FPSDefault = 30;
    //默认颜色空间
    public static final int ColorFormatDefault = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;

    public RecordSetting() {
    }

    public RecordSetting(int desiredSpanSec) {
        this.desiredSpanSec = desiredSpanSec;
    }


    public void setVideoSetting(int width, int height, int frameRate, int colorFormat) {
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
        this.videoBitRate = (int) (width * height * BitRatioDefault);
        this.colorFormat = colorFormat;
    }

    public void setAudioSetting(int audioSampleRate, int audioBitRate) {
        this.audioSampleRate = audioSampleRate;
        this.audioBitRate = audioBitRate;
    }

    public void setPushUrl(String pushUrl) {
        this.pushUrl = pushUrl;
    }

    //视频宽
    public int width = 0;
    //视频高
    public int height = 0;
    //视频比特率
    public int videoBitRate;
    //视频帧率
    public int frameRate = FPSDefault;
    //预计时常
    public int desiredSpanSec = -1;
    //推流地址
    public String pushUrl = null;
    //音频采样率
    public int audioSampleRate = AudioSampleRateDefault;
    ;
    // 音频比特率
    public int audioBitRate = AudioRateDefault;
    //视频颜色类型
    public int colorFormat = ColorFormatDefault;

    public void setVideoBitRate(int videoBitRate) {
        this.videoBitRate = videoBitRate;
    }

    public int getVideoBitRate() {
        return videoBitRate;
    }
}

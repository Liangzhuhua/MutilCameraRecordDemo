package com.reach.mutilcamerarecord.mediausb.model;

/**
 * Author：lzh on 2021/6/10 11:11
 */
public class AudioBuff {
    public boolean isReadyToFill;
    public int audioFormat = -1;
    public byte[] buff;

    public AudioBuff(int audioFormat, int size) {
        isReadyToFill = true;
        this.audioFormat = audioFormat;
        buff = new byte[size];
    }
}

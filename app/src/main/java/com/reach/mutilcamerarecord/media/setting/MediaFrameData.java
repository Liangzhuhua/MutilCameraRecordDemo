package com.reach.mutilcamerarecord.media.setting;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/**
 * Created by ${User} on 2017/11/25 0025.
 */

public class MediaFrameData {
    private MediaCodec.BufferInfo info;
    private byte[] mDataBuffer;

    public ByteBuffer buf;

    public MediaFrameData(ByteBuffer buf, MediaCodec.BufferInfo originInfo) {
        int size = originInfo.size;
        this.info = new MediaCodec.BufferInfo();
        this.info.offset = originInfo.offset;
        this.info.size = originInfo.size;
        this.info.presentationTimeUs = originInfo.presentationTimeUs;
        this.info.flags = originInfo.flags;
        mDataBuffer = new byte[size];
        buf.get(mDataBuffer, 0, size);
        this.buf = buf;
    }

    public ByteBuffer getBufferData() {
        ByteBuffer tempBuf = ByteBuffer.allocateDirect(mDataBuffer.length);
        tempBuf.put(mDataBuffer, 0, mDataBuffer.length);
        return tempBuf;
    }

    public byte[] getmDataBuffer() {
        return mDataBuffer;
    }

    public MediaCodec.BufferInfo getInfo() {
        return info;
    }

    public void setInfo(MediaCodec.BufferInfo info) {
        this.info = info;
    }
}

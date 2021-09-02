package com.reach.mutilcamerarecord.mediausb.code.listener;

import java.nio.ByteBuffer;

public interface UvcPcmDataCallBack {

    /**
     * pcm音频数据回调
     * @param buffer
     * @param readBytes
     */
    void onFramePcm(ByteBuffer buffer, int readBytes);
}

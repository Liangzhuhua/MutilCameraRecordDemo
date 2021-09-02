package com.reach.mutilcamerarecord.media.encode;

import java.nio.ByteBuffer;

public interface IAudioListener {
    void onPcm(ByteBuffer buffer, int readBytes);
}

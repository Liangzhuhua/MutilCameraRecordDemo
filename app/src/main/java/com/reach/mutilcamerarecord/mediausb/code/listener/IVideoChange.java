package com.reach.mutilcamerarecord.mediausb.code.listener;

/**
 * Author：lzh on 2021/6/10 10:50
 */
public interface IVideoChange {
    void onVideoSizeChanged(int width, int height);

    class VideoChangeRunable implements Runnable {
        IVideoChange videoChangeListener;
        int w, h;

        public VideoChangeRunable(IVideoChange videoChangeListener, int w, int h) {
            this.videoChangeListener = videoChangeListener;
            this.w = w;
            this.h = h;
        }

        @Override
        public void run() {
            if (videoChangeListener != null) {
                videoChangeListener.onVideoSizeChanged(w, h);
            }
        }
    }
}

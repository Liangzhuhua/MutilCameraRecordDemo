package com.reach.mutilcamerarecord.media.manager;

/**
 * Created by Lzc on 2018/3/16 0016.
 * 帧数计算
 */

public class FrameRateMeter {


    private static final long TIMETRAVEL = 1;
    private static final long TIMETRAVEL_MS = TIMETRAVEL * 1000;
    private static final long TIMETRAVEL_MAX_DIVIDE = 2 * TIMETRAVEL_MS;

    private int mTimes;
    private float mCurrentFps;
    private long mUpdateTime;

    private static FrameRateMeter frameRateMeter;


    public static FrameRateMeter getInstance() {
        if (frameRateMeter == null) {
            synchronized (FrameRateMeter.class) {
                if (frameRateMeter == null)
                    frameRateMeter = new FrameRateMeter();
            }
        }
        return frameRateMeter;
    }

    private FrameRateMeter() {
        mTimes = 0;
        mCurrentFps = 0;
        mUpdateTime = 0;
    }

    /**
     * 计算绘制帧数据
     */
    public void drawFrameCount() {
        long currentTime = System.currentTimeMillis();
        if (mUpdateTime == 0) {
            mUpdateTime = currentTime;
        }
        if ((currentTime - mUpdateTime) > TIMETRAVEL_MS) {
            mCurrentFps = ((float) mTimes / (currentTime - mUpdateTime)) * 1000.0f;
            mUpdateTime = currentTime;
            mTimes = 0;
        }
        mTimes++;
    }

    /**
     * 获取FPS
     *
     * @return FPS
     */
    public float getFPS() {
        if ((System.currentTimeMillis() - mUpdateTime) > TIMETRAVEL_MAX_DIVIDE) {
            return 0;
        } else {
            return mCurrentFps;
        }
    }
}

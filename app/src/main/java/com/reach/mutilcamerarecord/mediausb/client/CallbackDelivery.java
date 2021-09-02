package com.reach.mutilcamerarecord.mediausb.client;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

/**
 * Author：lzh on 2021/6/10 11:05
 */
public class CallbackDelivery {
    static private CallbackDelivery instance;
    private final Executor mCallbackPoster;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public static CallbackDelivery i() {
        return instance == null ? instance = new CallbackDelivery() : instance;
    }

    private CallbackDelivery() {
        mCallbackPoster = new Executor() {
            @Override
            public void execute(Runnable command) {
                handler.post(command);
            }
        };
    }

    public void post(Runnable runnable) {
        mCallbackPoster.execute(runnable);
    }
}

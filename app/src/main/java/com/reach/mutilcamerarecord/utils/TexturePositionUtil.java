package com.reach.mutilcamerarecord.utils;

import java.nio.FloatBuffer;

/**
 * Created by Lzc on 2018/3/10 0010.
 */

public class TexturePositionUtil {
    public static final float VertexPositionFull[] = {
            -1.0f, -1.0f,  // 0 bottom left
            1.0f, -1.0f,  // 1 bottom right
            -1.0f, 1.0f,  // 2 top left
            1.0f, 1.0f,  // 3 top right
    };


//    //Android 坐标系  贴图坐标
//    public static final float NormalFragmentFull[] = {
//            0.0f, 1.0f,  // 0 bottom left
//            1.0f, 1.0f,   // 1 bottom right
//            1.0f, 0.0f,   // 2 top right
//            0.0f, 0.0f,  // 3 top left
//
//    };


    //Android 坐标系  贴图坐标
    private static final float NormalFragmentFull[] = {
            1.0f, 1.0f,     // 3 top right
            1.0f, 0.0f,      // 2 top left
            0.0f, 1.0f,       // 1 bottom right
            0.0f, 0.0f,       // 0 bottom left
    };

    private static final float NormalFragmentMirroring[] = {
            0.0f, 1.0f,      // 0 bottom left
            0.0f, 0.0f,        // 1 bottom right
            1.0f, 1.0f,    // 2 top left
            1.0f, 0.0f,     // 3 top right
    };


//     0.0f, 1.0f,    // 3 top right
//             1.0f, 1.0f,          // 2 top left
//             0.0f, 0.0f,      // 1 bottom right
//             1.0f, 0.0f,         // 0 bottom left

    //Android 坐标系  贴图坐标
    private static final float FBOFragmentFull[] = {
            0.0f, 0.0f,    // 3 top right
            1.0f, 0.0f,         // 2 top left
            0.0f, 1.0f,      // 1 bottom right
            1.0f, 1.0f,        // 0 bottom left
    };


//        1.0f, 1.0f,      // 0 bottom left
//                0.0f, 1.0f,         // 1 bottom right
//                1.0f, 0.0f,    // 2 top left
//                0.0f, 0.0f,      // 3 top right

    private static final float FBOFragmentMirroring[] = {
            1.0f, 0.0f,     // 0 bottom left
            0.0f, 0.0f,          // 1 bottom right
            1.0f, 1.0f,    // 2 top left
            0.0f, 1.0f,      // 3 top right
    };


//    //Android 坐标系  贴图坐标
//    public static final float FragmentPositionRotate90AndMirroring[] = {
//            0.0f, 1.0f,
//            0.0f, 0.0f,
//            1.0f, 0.0f,
//            1.0f, 1.0f,
//    };
//
//
//    //Android 坐标系  贴图坐标 镜像
//    public static final float FragmentPositionFullMirroring[] = {
//            1.0f, 1.0f,
//            1.0f, 0.0f,
//            0.0f, 0.0f,
//            0.0f, 1.0f,
//    };


//    //普通贴图
//    public static final float FragmentOrigin[] = {
//            0.0f, 0.0f,// 3 top right
//            0.0f, 1.0f,    // 0 bottom left
//            1.0f, 1.0f, // 1 bottom right
//            1.0f, 0.0f      // 2 top left
//
//    };

    public static final FloatBuffer DefaultVertexFloatBuffer = GlUtil.createFloatBuffer(TexturePositionUtil.VertexPositionFull);
    public static final FloatBuffer DefaultTextureFloatBuffer = GlUtil.createFloatBuffer(TexturePositionUtil.NormalFragmentFull);
    public static final FloatBuffer DefaultTextureMirror = GlUtil.createFloatBuffer(TexturePositionUtil.NormalFragmentMirroring);

    public static final FloatBuffer FBOFragmentFloatBuffer = GlUtil.createFloatBuffer(TexturePositionUtil.FBOFragmentFull);
    public static final FloatBuffer FBOMirrorFragmentFloatBuffer = GlUtil.createFloatBuffer(TexturePositionUtil.FBOFragmentMirroring);
    //    public static final FloatBuffer CameraTextureFloatBufferMirror = GlUtil.createFloatBuffer(TexturePositionUtil.FragmentPositionFullMirroring);

}

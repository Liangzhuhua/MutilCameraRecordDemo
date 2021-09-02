package com.reach.mutilcamerarecord.media.render;

import android.graphics.Bitmap;
import android.opengl.GLES20;

import com.reach.mutilcamerarecord.utils.GlUtil;
import com.reach.mutilcamerarecord.utils.TexturePositionUtil;

import java.nio.FloatBuffer;

/**
 * Created by Lzc on 2018/3/12 0012.
 */

public class GlRenderImg {
    private int texture = -1;
    private FloatBuffer vertexPosition;
    private FloatBuffer vertexPositionHorizontal;
    private FloatBuffer fragmentPosition;
    private Bitmap bitmap;


    /**
     * @param bitmap 图片
     */
    public GlRenderImg(Bitmap bitmap) {
        texture = GlUtil.create2DTexture(bitmap);
        fragmentPosition = TexturePositionUtil.DefaultTextureFloatBuffer;
        this.bitmap = bitmap;
    }


    private FloatBuffer createVerticalPosition(float width, float height, float positionX, float positionY) {
        return GlUtil.createFloatBuffer(new float[]{
                -1 + positionY * 2 + height * 2, 1 - positionX * 2 - width * 2,
                -1 + positionY * 2, 1 - positionX * 2 - width * 2,
                -1 + positionY * 2 + height * 2, 1 - positionX * 2,
                -1 + positionY * 2, 1 - positionX * 2,

        });
    }

//       -1 + positionY * 2, 1 - positionX * 2 - width * 2,
//            -1 + positionY * 2 + height * 2, 1 - positionX * 2 - width * 2,
//            -1 + positionY * 2, 1 - positionX * 2,
//            -1 + positionY * 2 + height * 2, 1 - positionX * 2,

    /**
     * @param verticalWidth  1-0  1为屏幕大小
     * @param verticalHeight 1-0  1为屏幕大小
     * @param positionX      1-0  贴图左上角X 0为屏幕左上角
     * @param positionY      1-0  贴图左上角Y 1为屏幕左上角
     */
    public void initVerticalPosition(float verticalWidth, float verticalHeight, float positionX, float positionY) {
        vertexPosition = createVerticalPosition(verticalWidth, verticalHeight, positionX, positionY);
    }
//            -1.0f, -1.0f,  // 0 bottom left
//                    1.0f, -1.0f,  // 1 bottom right
//                    -1.0f, 1.0f,  // 2 top left
//                    1.0f, 1.0f,  // 3 top right

    /**
     * @param verticalWidth  1-0  1为屏幕大小
     * @param verticalHeight 1-0  1为屏幕大小
     * @param positionX      1-0  贴图左上角X 0为屏幕左上角
     * @param positionY      1-0  贴图左上角Y 1为屏幕左上角
     */
    public void initHorizontalPosition(float verticalWidth, float verticalHeight, float positionX, float positionY) {
        vertexPositionHorizontal = createVerticalPosition(verticalWidth, verticalHeight, positionX, positionY);
    }


    public void release() {
        if (texture != 0 && texture != -1) {
            GLES20.glDeleteTextures(1, new int[]{texture}, 0);
            texture = -1;
        }
        if (bitmap != null) {
            bitmap.recycle();
        }
        bitmap = null;
    }

    public void setVertexPosition(FloatBuffer vertexPosition) {
        this.vertexPosition = vertexPosition;
    }

    public void setVertexPositionHorizontal(FloatBuffer vertexPositionHorizontal) {
        this.vertexPositionHorizontal = vertexPositionHorizontal;
    }

    public int getTexture() {
        return texture;
    }

    public FloatBuffer getVertexPosition() {
        return vertexPosition;
    }

    public FloatBuffer getVertexPositionHorizontal() {
        return vertexPositionHorizontal;
    }

    public FloatBuffer getFragmentPosition() {
        return fragmentPosition;
    }
}

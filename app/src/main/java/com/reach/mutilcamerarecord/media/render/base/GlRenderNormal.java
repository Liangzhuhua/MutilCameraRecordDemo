package com.reach.mutilcamerarecord.media.render.base;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLES30;
import android.opengl.Matrix;

import com.reach.mutilcamerarecord.utils.GlUtil;
import com.reach.mutilcamerarecord.utils.TexturePositionUtil;

import java.nio.FloatBuffer;
import java.util.LinkedList;

/**
 * Created by Lzc on 2018/3/10 0010.
 */

public abstract class GlRenderNormal implements GlRender {
//    //顶点排序
//    protected static final short drawOrder[] = {0, 1, 3, 1, 2, 3};
//    //绘制顺序缓存
//    protected static final ShortBuffer drawListBuffer = ShortBuffer.wrap(drawOrder);

    protected FloatBuffer vertexBuffer = TexturePositionUtil.DefaultVertexFloatBuffer;
    protected FloatBuffer textureBuffer = TexturePositionUtil.DefaultTextureFloatBuffer;

    //当前 渲染器
    protected int mProgram;

    // 渲染的Image的宽高
    protected int mImageWidth;
    protected int mImageHeight;
    // 显示输出的宽高
    protected int mDisplayWidth;
    protected int mDisplayHeight;

    //渲染器属性
    protected int muMVPMatrixLoc;
    protected int maPositionLoc;
    protected int uTexMatrix;
    protected int maTextureCoordLoc;

    protected int mInputTextureLoc;


    // 变换矩阵
    protected float[] mMVPMatrix = new float[16];
    // 缩放矩阵
    protected float[] mTexMatrix = new float[16];

    public static final int EmptyTextureId = -1;

    private final LinkedList<Runnable> mRunOnDraw;

    protected Context context;


    public GlRenderNormal(Context context) {
        this.context = context;
        mRunOnDraw = new LinkedList<>();
        try {
            mProgram = GlUtil.createProgram(getVertexShaderCode(), getFragmentShaderCode());
        } catch (GlUtil.OpenGlException e) {
            e.printStackTrace();
        }
        initHandle();
        initIdentityMatrix();
    }


    public abstract String getFragmentShaderCode();

    public abstract String getVertexShaderCode();

    protected void initIdentityMatrix() {
        Matrix.setIdentityM(mMVPMatrix, 0);
        Matrix.setIdentityM(mTexMatrix, 0);
    }

    protected void initHandle() {
        maPositionLoc = GLES30.glGetAttribLocation(mProgram, "aPosition");
        maTextureCoordLoc = GLES30.glGetAttribLocation(mProgram, "aTextureCoord");
        uTexMatrix = GLES30.glGetUniformLocation(mProgram, "uTexMatrix");
        muMVPMatrixLoc = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
        mInputTextureLoc = GLES30.glGetUniformLocation(mProgram, "inputTexture");
    }


    @Override
    public void onInputSizeChanged(int width, int height) {
        mImageWidth = width;
        mImageHeight = height;
    }

    @Override
    public void onDisplayChanged(int width, int height) {
        mDisplayWidth = width;
        mDisplayHeight = height;
    }


    @Override
    public int drawFrame(int textureId) {
        return drawFrame(textureId, vertexBuffer, textureBuffer);
    }

    public int drawFrame(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        if (textureId == EmptyTextureId) {
            return -1;
        }
        GLES30.glUseProgram(mProgram);
        runPendingOnDrawTasks();
        // 绑定数据
        bindValue(textureId, vertexBuffer, textureBuffer);
        onDrawArraysBegin();
//        GLES30.glDrawElements(GLES30.GL_TRIANGLES, drawOrder.length, GLES30.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        onDrawArraysAfter();
        unBindValue();
        return textureId;
    }


    /**
     * 调用drawArrays之前，方便添加其他属性
     */
    public  void onDrawArraysBegin()
    {

    };

    /**
     * drawArrays调用之后，方便销毁其他属性
     */
    public  void onDrawArraysAfter()
    {

    }


    /**
     * 绑定数据
     *
     * @param textureId
     * @param vertexBuffer
     * @param textureBuffer
     */
    protected void bindValue(int textureId, FloatBuffer vertexBuffer,
                             FloatBuffer textureBuffer) {
        vertexBuffer.position(0);
        textureBuffer.position(0);
        GLES30.glUniform1i(mInputTextureLoc, 0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(getTextureType(), textureId);

        GLES30.glEnableVertexAttribArray(maPositionLoc);
        GLES30.glVertexAttribPointer(maPositionLoc, 2,
                GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glEnableVertexAttribArray(maTextureCoordLoc);
        GLES30.glVertexAttribPointer(maTextureCoordLoc, 2,
                GLES30.GL_FLOAT, false, 0, textureBuffer);

        GLES30.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMVPMatrix, 0);
        GLES30.glUniformMatrix4fv(uTexMatrix, 1, false, mTexMatrix, 0);
    }

    public abstract int getTextureType();

    /**
     * 解除绑定
     */
    protected void unBindValue() {
        GLES30.glDisableVertexAttribArray(maPositionLoc);
        GLES30.glDisableVertexAttribArray(maTextureCoordLoc);
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }

    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }

    @Override
    public void release() {
        GLES30.glDeleteProgram(mProgram);
        mProgram = EmptyTextureId;
        vertexBuffer = null;
        textureBuffer = null;
    }

    ///------------------ 统一变量(uniform)设置 ------------------------///
    protected void setInteger(final int location, final int intValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform1i(location, intValue);
            }
        });
    }

    protected void setFloat(final int location, final float floatValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform1f(location, floatValue);
            }
        });
    }

    protected void setFloatVec2(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec3(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec4(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatArray(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform1fv(location, arrayValue.length, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setPoint(final int location, final PointF point) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                float[] vec2 = new float[2];
                vec2[0] = point.x;
                vec2[1] = point.y;
                GLES30.glUniform2fv(location, 1, vec2, 0);
            }
        });
    }

    protected void setUniformMatrix3f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES30.glUniformMatrix3fv(location, 1, false, matrix, 0);
            }
        });
    }

    protected void setUniformMatrix4f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES30.glUniformMatrix4fv(location, 1, false, matrix, 0);
            }
        });
    }

}

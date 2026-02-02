/*
 * Copyright (C) 2025 Răzvan Albu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */

package com.stario.launcher.ui.common;

import android.content.Context;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import com.stario.launcher.R;
import com.stario.launcher.themes.ThemedActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

// Based on the JS implementation from
// Stripe and Kevin Hufnagl
//
// Shader loading generated with Gemini 2.5 Pro
//
// https://gist.github.com/jordienr/64bcf75f8b08641f205bd6a1a0d4ce1d
// https://stripe.com
// https://kevinhufnagl.com

public class GradientView extends TextureView implements TextureView.SurfaceTextureListener {
    private static final String TAG = "GradientView";

    private final int[] themeColors = new int[4];

    private RenderThread renderThread;

    public GradientView(@NonNull Context context) {
        super(context);

        init(context);
    }

    public GradientView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public GradientView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    private void init(Context context) {
        if (!(context instanceof ThemedActivity)) {
            throw new RuntimeException("GradientView must be in a ThemedActivity.");
        }

        setSurfaceTextureListener(this);
        setOpaque(false);

        ThemedActivity activity = (ThemedActivity) context;
        themeColors[0] = activity.getAttributeData(com.google.android.material.R.attr.colorSurfaceContainer);
        themeColors[1] = activity.getAttributeData(com.google.android.material.R.attr.colorSecondaryContainer);
        themeColors[2] = activity.getAttributeData(com.google.android.material.R.attr.colorPrimaryContainer);
        themeColors[3] = activity.getAttributeData(com.google.android.material.R.attr.colorSurfaceContainerHigh);
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        renderThread = new RenderThread(surface, getContext(), themeColors);

        renderThread.updateSize(width, height);
        renderThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
        if (renderThread != null) {
            renderThread.updateSize(width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        if (renderThread != null) {
            renderThread.stopRendering();

            try {
                renderThread.join();
            } catch (InterruptedException exception) {
                Log.e(TAG, "Failed to stop render thread", exception);
            }
        }

        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
    }

    private static class RenderThread extends Thread {
        private final SurfaceTexture surfaceTexture;
        private final int[] themeColors;
        private final float[] noiseSeeds;
        private final Context context;

        private volatile boolean running;
        private boolean sizeChanged = false;
        private int width;
        private int height;

        private EGLDisplay eglDisplay;
        private EGLContext eglContext;
        private EGLSurface eglSurface;
        private EGL10 egl;

        private final float[] projectionMatrix;
        private final float[] modelViewMatrix;
        private ShortBuffer indexBuffer;
        private FloatBuffer vertexBuffer;
        private FloatBuffer uvNormBuffer;
        private FloatBuffer uvBuffer;
        private long startTime;
        private int indexCount;
        private int program;

        public RenderThread(SurfaceTexture surface, Context context, int[] colors) {
            this.projectionMatrix = new float[16];
            this.modelViewMatrix = new float[16];
            this.noiseSeeds = new float[3];
            this.surfaceTexture = surface;
            this.themeColors = colors;
            this.context = context;
            this.running = true;

            float baseSeed = new Random().nextFloat() * 200.0f;
            for (int index = 0; index < 3; index++) {
                noiseSeeds[index] = baseSeed + (10.0f * (index + 1));
            }
        }

        public void updateSize(int width, int height) {
            this.sizeChanged = true;

            this.height = height;
            this.width = width;
        }

        public void stopRendering() {
            running = false;
        }

        @Override
        public void run() {
            initEGL();
            initGL();

            while (running) {
                if (sizeChanged) {
                    GLES20.glViewport(0, 0, width, height);
                    Matrix.orthoM(projectionMatrix,
                            0, -1, 1, -1, 1, -10, 10);
                    Matrix.setIdentityM(modelViewMatrix, 0);
                    generatePlaneMesh((int) Math.ceil(width * 0.06), (int) Math.ceil(height * 0.16));

                    sizeChanged = false;
                }

                drawFrame();

                if (!egl.eglSwapBuffers(eglDisplay, eglSurface)) {
                    Log.e(TAG, "Buffer swap failed");
                }
            }

            destroyEGL();
        }

        private void initEGL() {
            egl = (EGL10) EGLContext.getEGL();
            eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            egl.eglInitialize(eglDisplay, null);

            int[] configAttribs = {
                    EGL10.EGL_RENDERABLE_TYPE, 4, // ES2
                    EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_ALPHA_SIZE, 8,
                    EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE
            };

            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            egl.eglChooseConfig(eglDisplay, configAttribs, configs, 1, numConfigs);

            eglContext = egl.eglCreateContext(eglDisplay, configs[0],
                    EGL10.EGL_NO_CONTEXT, new int[]{0x3098, 2, EGL10.EGL_NONE});
            eglSurface = egl.eglCreateWindowSurface(eglDisplay, configs[0], surfaceTexture, null);
            egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
        }

        private void initGL() {
            String vertexCode = readShader(context, R.raw.gradient_vert);
            String fragmentCode = readShader(context, R.raw.gradient_frag);

            program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, loadShader(GLES20.GL_VERTEX_SHADER, vertexCode));
            GLES20.glAttachShader(program, loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode));
            GLES20.glLinkProgram(program);

            startTime = System.currentTimeMillis();

            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glDisable(GLES20.GL_CULL_FACE);
        }

        private void drawFrame() {
            GLES20.glClearColor(0, 0, 0, 0);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            GLES20.glUseProgram(program);

            float timeParam = (float) (System.currentTimeMillis() - startTime);
            GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uTime"), timeParam);
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uProjectionMatrix"), 1, false, projectionMatrix, 0);
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uModelViewMatrix"), 1, false, modelViewMatrix, 0);
            GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "uResolution"), (float) width, (float) height);
            GLES20.glUniform1fv(GLES20.glGetUniformLocation(program, "uNoiseSeeds"), 3, noiseSeeds, 0);

            bindColor(program, "uBaseColor", themeColors[0]);
            bindColor(program, "uColor1", themeColors[1]);
            bindColor(program, "uColor2", themeColors[2]);
            bindColor(program, "uColor3", themeColors[3]);


            bindAttribute(program, "aPosition", vertexBuffer, 3);
            bindAttribute(program, "aUv", uvBuffer, 2);
            bindAttribute(program, "aUvNorm", uvNormBuffer, 2);

            GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        }

        private void generatePlaneMesh(int xSegCount, int ySegCount) {
            int vertexCount = (xSegCount + 1) * (ySegCount + 1);

            float[] vertices = new float[vertexCount * 3];
            float[] uvs = new float[vertexCount * 2];
            float[] uvNorms = new float[vertexCount * 2];
            short[] indices = new short[xSegCount * ySegCount * 6];

            int vIdx = 0;
            int uIdx = 0;
            for (int y = 0; y <= ySegCount; y++) {
                for (int x = 0; x <= xSegCount; x++) {
                    float xN = (float) x / xSegCount;
                    float yN = (float) y / ySegCount;

                    vertices[vIdx++] = xN * 2.0f - 1.0f;
                    vertices[vIdx++] = yN * 2.0f - 1.0f;
                    vertices[vIdx++] = 0.0f;
                    uvs[uIdx] = xN;
                    uvs[uIdx + 1] = 1.0f - yN;
                    uvNorms[uIdx] = xN * 2.0f - 1.0f;
                    uvNorms[uIdx + 1] = 1.0f - yN * 2.0f;
                    uIdx += 2;
                }
            }

            int iIdx = 0;
            for (int y = 0; y < ySegCount; y++) {
                for (int x = 0; x < xSegCount; x++) {
                    short tl = (short) (y * (xSegCount + 1) + x);
                    short tr = (short) (tl + 1);
                    short bl = (short) ((y + 1) * (xSegCount + 1) + x);
                    short br = (short) (bl + 1);

                    indices[iIdx++] = tl;
                    indices[iIdx++] = bl;
                    indices[iIdx++] = tr;
                    indices[iIdx++] = tr;
                    indices[iIdx++] = bl;
                    indices[iIdx++] = br;
                }
            }

            indexCount = indices.length;
            vertexBuffer = createFloatBuffer(vertices);
            uvBuffer = createFloatBuffer(uvs);
            uvNormBuffer = createFloatBuffer(uvNorms);
            indexBuffer = createShortBuffer(indices);
        }

        private void bindColor(int program, String name, int color) {
            int loc = GLES20.glGetUniformLocation(program, name);
            GLES20.glUniform3f(loc, Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f);
        }

        private void bindAttribute(int program, String name, FloatBuffer buffer, int size) {
            int loc = GLES20.glGetAttribLocation(program, name);
            if (loc != -1) {
                GLES20.glEnableVertexAttribArray(loc);
                GLES20.glVertexAttribPointer(loc, size, GLES20.GL_FLOAT, false, 0, buffer);
            }
        }

        private FloatBuffer createFloatBuffer(float[] coordinates) {
            FloatBuffer floatBuffer = ByteBuffer.allocateDirect(coordinates.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            floatBuffer.put(coordinates).position(0);

            return floatBuffer;
        }

        private ShortBuffer createShortBuffer(short[] coordinates) {
            ShortBuffer shortBuffer = ByteBuffer.allocateDirect(coordinates.length * 2)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer();
            shortBuffer.put(coordinates).position(0);

            return shortBuffer;
        }

        private int loadShader(int type, String code) {
            int shader = GLES20.glCreateShader(type);

            GLES20.glShaderSource(shader, code);
            GLES20.glCompileShader(shader);

            return shader;
        }

        private String readShader(Context context, @RawRes int resId) {

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getResources().openRawResource(resId)))) {

                StringBuilder builder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }

                return builder.toString();
            } catch (IOException exception) {
                Log.e(TAG, "readShader: ", exception);

                return "";
            }
        }

        private void destroyEGL() {
            egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            egl.eglDestroySurface(eglDisplay, eglSurface);
            egl.eglDestroyContext(eglDisplay, eglContext);
            egl.eglTerminate(eglDisplay);
        }
    }
}
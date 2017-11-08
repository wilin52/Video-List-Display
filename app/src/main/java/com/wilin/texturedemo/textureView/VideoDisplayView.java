package com.wilin.texturedemo.textureView;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by Lin WenLong on 2017/11/7.
 * <p>
 * video display.
 */
public class VideoDisplayView extends FrameLayout implements TextureView.SurfaceTextureListener {
    private static String TAG = "VideoDisplayView";
    private final WeakReference<VideoDisplayView> mThisWeakRef = new WeakReference<>(this);
    /* for displaying video */
    private TextureView textureView;
    /* control video display thread */
    private final GLThreadManager threadManager = new GLThreadManager();
    /* video display thread */
    private GLThread mGLThread;
    /* render video */
    private VideoSurfaceRenderer renderer;
    /* render video */
    private String videoPath;
    /* mark thread's state*/
    private boolean started;
    private OnVisibleChangeListener visibleChangeListener;

    public VideoDisplayView(Context context) {
        this(context, null);
    }

    public VideoDisplayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoDisplayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            if (textureView != null) {
                removeView(textureView);
            }

            textureView = new TextureView(getContext());
            textureView.setSurfaceTextureListener(this);
            LayoutParams params = new LayoutParams(getWidth(), getHeight());
            textureView.setLayoutParams(params);
            addView(textureView);
        }
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (null != visibleChangeListener) {
            visibleChangeListener.onVisibleChanged(visibility);
        }

        if (visibility != View.VISIBLE) {
            pausePlay();
        }
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        renderer = new VideoSurfaceRenderer(getContext(), surface, width, height);
        mGLThread = new GLThread(mThisWeakRef);
        if (started) {
            mGLThread.start();
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    private void init() {

    }

    public void startPlay() {
        started = true;
        if (null != mGLThread && mGLThread.isExited()) {
            mGLThread.interrupt();
            mGLThread = new GLThread(mThisWeakRef);
            mGLThread.start();
        } else if (null != mGLThread) {
            if (!mGLThread.isPaused()) {
                mGLThread.start();
            }
            mGLThread.onResume();
        }
    }

    public void pausePlay() {
        if (null != mGLThread) {
            mGLThread.onPause();
        }
    }

    public void stopPlay() {
        started = false;
        if (null != mGLThread) {
            mGLThread.onStop();
        }
        mGLThread = null;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    private static class GLThreadManager {

    }

    private static class GLThread extends Thread {
        private boolean paused;
        private boolean exited;
        private boolean start;
        private WeakReference<VideoDisplayView> videoDisplayView;
        private Renderer renderer;
        private MediaPlayer mediaPlayer;
        private final GLThreadManager threadManager;

        GLThread(WeakReference<VideoDisplayView> videoDisplayViewWeakReference) {
            super(TAG + " GLThread");
            videoDisplayView = videoDisplayViewWeakReference;
            if (null != videoDisplayView && null != videoDisplayView.get()) {
                threadManager = videoDisplayView.get().threadManager;
            } else {
                threadManager = null;
            }
        }

        @Override
        public void run() {
            onResume();
            if (null == videoDisplayView || videoDisplayView.get() == null) {
                exited = true;
                onStop();
                return;
            }
            renderer = videoDisplayView.get().renderer;
            if (null == renderer) {
                exited = true;
            } else {
                renderer.initialize(); // initialize OpenGL
                try {

                    while (videoDisplayView.get().getVideoTexture() == null) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    mediaPlayer = new MediaPlayer();
                    Surface surface = new Surface(videoDisplayView.get().getVideoTexture());
                    mediaPlayer.setDataSource(videoDisplayView.get().videoPath);
                    mediaPlayer.setSurface(surface);

                    surface.release();

                    mediaPlayer.prepareAsync();
                    mediaPlayer.setLooping(true);
                } catch (IllegalArgumentException | SecurityException |
                        IllegalStateException | IOException e) {
                    exited = true;
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mediaPlayer.start();
                synchronized (threadManager) {
                    while (!exited) {
                        try {
                            while (!start) {
                                threadManager.wait();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        renderer.render(); // draw video
                    }
                }
                renderer.destroy();// destroy OpenGL
            }
            onStop();
        }

        void onPause() {
            start = false;
            paused = true;
            synchronized (threadManager) {
                threadManager.notifyAll();
            }
        }

        void onResume() {
            paused = false;
            exited = false;
            start = true;
            synchronized (threadManager) {
                threadManager.notifyAll();
            }
        }

        void onStop() {
            start = false;
            paused = true;
            exited = true;
            synchronized (threadManager) {
                threadManager.notifyAll();
            }
        }

        boolean isExited() {
            return exited;
        }

        boolean isPaused() {
            return paused;
        }
    }

    public interface OnVisibleChangeListener {
        void onVisibleChanged(int visibility);

    }

    public void setOnVisibleChangeListener(OnVisibleChangeListener visibleChangeListener) {
        this.visibleChangeListener = visibleChangeListener;
    }

    public SurfaceTexture getVideoTexture() {
        return renderer == null ? null : renderer.getVideoTexture();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopPlay();
        super.onDetachedFromWindow();
    }
}

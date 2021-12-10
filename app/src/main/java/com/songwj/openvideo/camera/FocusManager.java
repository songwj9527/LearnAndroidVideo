package com.songwj.openvideo.camera;

import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

public class FocusManager {
    private FocusView focusView;
    private MainHandler handler;
    private float currentX;
    private float currentY;
    private CoordinateTransformer transformer;
    private Rect previewRect;
    private Rect focusRect;
    private CameraUiEvent cameraUiEvent;

    private static final int HIDE_FOCUS_DELAY = 4000;
    private static final int MSG_HIDE_FOCUS = 0x10;
    private static class MainHandler extends Handler {
        private final WeakReference<FocusManager> weakReference;
        public MainHandler(FocusManager manager, Looper looper) {
            super(looper);
            weakReference = new WeakReference<>(manager);
        }

        @Override
        public void handleMessage(@NonNull @NotNull Message msg) {
            super.handleMessage(msg);
            if (weakReference.get() == null) {
                return;
            }
            switch (msg.what) {
                case MSG_HIDE_FOCUS:
                    FocusManager manager = weakReference.get();
                    if (manager == null) {
                        return;
                    }
                    manager.focusView.resetToDefaultPosition();
                    manager.hideFocusUI();
                    if (manager.cameraUiEvent != null) {
                        manager.cameraUiEvent.resetTouchToFocus();
                    }
                    break;
            }
        }
    }


    public FocusManager(FocusView focusView, Looper looper) {
        this.focusView = focusView;
        this.handler = new MainHandler(this, looper);
        this.focusView.resetToDefaultPosition();
        this.focusView.hideFocusView();
        this.focusRect = new Rect();
    }

    public void setListener(CameraUiEvent listener) {
        cameraUiEvent = listener;
    }

    public void onPreviewChanged(int width, int height, Integer face, Integer sensorOrientation, Rect rect) {
        previewRect = new Rect(0, 0, width, height);
        transformer = new CoordinateTransformer(face, sensorOrientation, rect, rectToRectF(previewRect));
    }

    /* just set focus view position, not start animation*/
    public void startFocus(float x, float y) {
        currentX = x;
        currentY = y;
        handler.removeMessages(MSG_HIDE_FOCUS);
        focusView.moveToPosition(x, y);
        //focusView.startFocus();
        handler.sendEmptyMessageDelayed(MSG_HIDE_FOCUS, HIDE_FOCUS_DELAY);
    }
    /* show focus view by af state */
    public void startFocus() {
        handler.removeMessages(MSG_HIDE_FOCUS);
        focusView.startFocus();
        handler.sendEmptyMessageDelayed(MSG_HIDE_FOCUS, HIDE_FOCUS_DELAY);
    }

    public void autoFocus() {
        handler.removeMessages(MSG_HIDE_FOCUS);
        focusView.resetToDefaultPosition();
        focusView.startFocus();
        handler.sendEmptyMessageDelayed(MSG_HIDE_FOCUS, 1000);
    }

    public void focusSuccess() {
        focusView.focusSuccess();
    }

    public void focusFailed() {
        focusView.focusFailed();
    }

    public void hideFocusUI() {
        //focusView.resetToDefaultPosition();
        focusView.hideFocusView();
    }

    public void removeDelayMessage() {
        handler.removeMessages(MSG_HIDE_FOCUS);
    }

    public MeteringRectangle getFocusArea(float x, float y, boolean isFocusArea) {
        currentX = x;
        currentY = y;
        if (isFocusArea) {
            return calcTapAreaForCamera2(previewRect.width() / 5, 1000);
        } else {
            return calcTapAreaForCamera2(previewRect.width() / 4, 1000);
        }
    }

    private MeteringRectangle calcTapAreaForCamera2(int areaSize, int weight) {
        int left = clamp((int) currentX - areaSize / 2,
                previewRect.left, previewRect.right - areaSize);
        int top = clamp((int) currentY - areaSize / 2,
                previewRect.top, previewRect.bottom - areaSize);
        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        toFocusRect(transformer.toCameraSpace(rectF));
        return new MeteringRectangle(focusRect, weight);
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    private RectF rectToRectF(Rect rect) {
        return new RectF(rect);
    }

    private void toFocusRect(RectF rectF) {
        focusRect.left = Math.round(rectF.left);
        focusRect.top = Math.round(rectF.top);
        focusRect.right = Math.round(rectF.right);
        focusRect.bottom = Math.round(rectF.bottom);
    }

    public interface CameraUiEvent {
        public void resetTouchToFocus();
    }
}

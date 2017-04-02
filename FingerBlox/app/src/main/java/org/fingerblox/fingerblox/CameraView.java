package org.fingerblox.fingerblox;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;

import java.util.ArrayList;
import java.util.Collections;


@SuppressWarnings("deprecation")
public class CameraView extends JavaCameraView implements PictureCallback {
    private static final String TAG = "cameraView";

    private static final int width = 2000;
    private static final int height = 2000;

    private PictureCallback pictureListener;

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void takePicture() {
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }

    public void setPictureListener(PictureCallback listener) {
        pictureListener = listener;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        if (pictureListener != null) {
            pictureListener.onPictureTaken(data, camera);
        }
    }

    @Override
    protected boolean initializeCamera(int width, int height) {
        boolean res = super.initializeCamera(width, height);
        setFixedFocusDistance();
        return res;
    }

    protected void setFixedFocusDistance() {

        float padding = 0.2f;
        Rect focusRect = new Rect(
                Math.round(-(0.5f * width) + (padding * width)),
                Math.round(-(0.5f * height) + (padding * height)),
                Math.round((0.5f * width) - (padding * width)),
                Math.round((0.5f * height) - (padding * height))
        );

        Camera.Area focusArea = new Camera.Area(focusRect, 1000);
        ArrayList<Camera.Area> focusAreaList = new ArrayList<>(Collections.singletonList(focusArea));

        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        parameters.setFocusAreas(focusAreaList);

        Camera.Size max_size = null;
        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            if (max_size == null || (size.height >= max_size.height &&
                    size.width >= max_size.width)) {
                max_size = size;
            }
        }
        assert max_size != null;
        parameters.setPictureSize(max_size.width, max_size.height);
        mCamera.setParameters(parameters);
    }

    protected void fixFocusToggle() {
        Camera.Parameters parameters = mCamera.getParameters();
        // parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        if (!parameters.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_MACRO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        } else if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_FIXED)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
        } else {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        mCamera.setParameters(parameters);
    }

    public Camera.Parameters getCameraParameters() {
        try {
            return mCamera.getParameters();
        } catch (NullPointerException e) {
            Log.i(TAG, "Could not retrieve camera parameters. Camera in CameraView == NULL");
            return null;
        }
    }
}

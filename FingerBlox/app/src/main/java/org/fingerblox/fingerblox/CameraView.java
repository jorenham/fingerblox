package org.fingerblox.fingerblox;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;

import java.util.ArrayList;
import java.util.Collections;


public class CameraView extends JavaCameraView implements PictureCallback {
    private static final String TAG = "cameraView";
    private static final int width = 2000;
    private static final int height = 2000;

    private PictureCallback pictureListener;

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void takePicture() {
        Log.i(TAG, "Taking picture");

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

        float padding = 0.4f;
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
        parameters.setPictureSize(720, 480);
        mCamera.setParameters(parameters);
    }

    protected void fixFocus() {
        Camera.Parameters parameters = mCamera.getParameters();
        if (parameters.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_MACRO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        }
        mCamera.setParameters(parameters);
    }
}

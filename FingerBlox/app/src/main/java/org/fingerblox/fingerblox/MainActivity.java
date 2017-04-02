package org.fingerblox.fingerblox;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.Locale;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity implements CvCameraViewListener {
    public static final String TAG = "MainActivity";
    public static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi" +
            "LquHhsRG2KdlJjawS67302BNXTpkSRe3WGWO8yzacuS/d/2Y7j7u9/u+kflAh5t+3dhc/KoX1ONS/9drFSfU" +
            "1zg/AKiTOpVpuLzNttqdDJfvUX7dKn9uw7CQN9jEzRSKdRaDlX/w9BOmOpfO8XJ9pEXR2t62W6Lt/xfujxja" +
            "+uqUjIQ08RmKMtrTxcvmc26nz3VXqxtkMloCGMzhojDhEFHS9DPuZ3TePp2g21XMKS4kmi414U8N86hW6DIQ" +
            "wlJnWgCWP75lRImqP8G+NvWrvBclQfPcRL8Mj3O22s5UbD5NUVaxdG/xHb9DQXvSpXI3fDrnaxwaf8Mv+Sbw" +
            "ZsD+QIDAQAB";

    private CameraView mOpenCvCameraView;
    private boolean staticTextViewsSet = false;

    static {
        if(!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Failed to load OpenCV");
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private PictureCallback pictureCallback = new PictureCallback() {
        public void onPictureTaken(final byte[] data, Camera camera) {
            camera.startPreview();
            camera.setPreviewCallback(mOpenCvCameraView);

            final ProgressDialog progress = new ProgressDialog(MainActivity.this);
            progress.setTitle("Loading");
            progress.setMessage("Processing image...");
            progress.setCancelable(false);
            progress.show();

            Thread mThread = new Thread() {
                @Override
                public void run() {
                    ImageProcessing p = new ImageProcessing(data);
                    ImageSingleton.image = p.getProcessedImage();
                    progress.dismiss();

                    Intent intent = new Intent(MainActivity.this, ImageDisplayActivity.class);
                    startActivity(intent);
                }
            };
            mThread.start();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            }

        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.main);

        mOpenCvCameraView = (CameraView) findViewById(R.id.camera_preview);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setPictureListener(pictureCallback);

        Button takePictureButton = (Button) findViewById(R.id.btn_takepicture);
        assert takePictureButton != null;
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOpenCvCameraView.takePicture();
            }
        });
        Button fixedFocusButton = (Button) findViewById(R.id.btn_fixfocus);
        assert fixedFocusButton != null;
        fixedFocusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOpenCvCameraView.fixFocusToggle();
            }
        });

        updateStaticTextViews();

    }

    protected boolean updateStaticTextViews() {
        Camera.Parameters params = null;
        try {
            params = mOpenCvCameraView.getCameraParameters();
        } catch (NullPointerException e) {
            return false;
        }

        TextView labelMacroEnabled = (TextView) findViewById(R.id.lbl_macro_available);
        assert labelMacroEnabled != null;
        String macroRes = params.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_MACRO
        ) ? "True" : "False";
        String macroEnabledText = labelMacroEnabled.getText().toString().replace("False", macroRes);
        labelMacroEnabled.setText(macroEnabledText);

        TextView labelResolution = (TextView) findViewById(R.id.lbl_resolution);
        assert labelResolution != null;
        String resolution = params.getPictureSize().width + " x " + params.getPictureSize().height;
        String labelResText = labelResolution.getText().toString();
        labelResolution.setText(labelResText + " " + resolution);
        return true;
    }

    protected void updateDynamicTextViews() {
        Camera.Parameters params = null;
        try {
            params = mOpenCvCameraView.getCameraParameters();
        } catch (NullPointerException e) {
            return;
        }

        TextView labelCurrentFocusMode = (TextView) findViewById(R.id.lbl_current_focus_mode);
        assert labelCurrentFocusMode != null;
        String focusModePreText = labelCurrentFocusMode.getText().toString().split(": ")[0];
        String focusModeText = focusModePreText + ": " + params.getFocusMode();
        labelCurrentFocusMode.setText(focusModeText);

        TextView labelEstFocusDistance = (TextView) findViewById(R.id.lbl_focus_distance);
        assert labelEstFocusDistance != null;
        float[] focusDistanceData = new float[3];
        params.getFocusDistances(focusDistanceData);

        String focusDistanceText = String.format(Locale.ENGLISH, "%.2f",
                focusDistanceData[Camera.Parameters.FOCUS_DISTANCE_OPTIMAL_INDEX] * 100.0f);
        String focusDistanceOldText = labelEstFocusDistance.getText().toString();
        String focusDistanceRes = focusDistanceOldText.replaceAll("([0-9]+.[0-9]*)|Infinity", focusDistanceText);
        labelEstFocusDistance.setText(focusDistanceRes);
    }

    @Override
    public void onResume() {
        super.onResume();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {

    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(Mat inputFrame) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!staticTextViewsSet) {
                    staticTextViewsSet = updateStaticTextViews();
                }
                updateDynamicTextViews();
            }}
        );

        return inputFrame;
    }
}

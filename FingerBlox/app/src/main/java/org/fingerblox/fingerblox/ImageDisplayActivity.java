package org.fingerblox.fingerblox;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ImageDisplayActivity extends AppCompatActivity {
    public static final String TAG = "ImageDisplayActivity";

    private ImageView mImageView;
    private Mat image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_display);
        mImageView = (ImageView) findViewById(R.id.image_view);

        initializeImage();
        displayImage();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

    protected void initializeImage() {
        byte[] imageData = new byte[0];

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            byte[] possibleImage = bundle.getByteArray("CAPTURED_IMAGE");
            if (possibleImage != null) {
                imageData = possibleImage;
                Log.i(TAG, String.format("Picture received of %d bytes", imageData.length));
            }
        }

        Mat BGRImage = Imgcodecs.imdecode(new MatOfByte(imageData), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);

        // Convert BGR to Grayscale
        image = emptyMat(BGRImage.cols(), BGRImage.rows());
        Imgproc.cvtColor(BGRImage, image, Imgproc.COLOR_BGR2GRAY, 4);

        image = rotateImage(image);
    }

    @NonNull
    private Mat emptyMat(int width, int height) {
        return new Mat(width, height, CvType.CV_8U, new Scalar(2));
    }

    protected void displayImage() {
        // To rgba
        Mat rgbaMat = new Mat(image.width(), image.height(), CvType.CV_8U, new Scalar(4));
        Imgproc.cvtColor(image, rgbaMat, Imgproc.COLOR_GRAY2RGBA, 4);

        // to bitmap
        Bitmap bmp = Bitmap.createBitmap(rgbaMat.cols(), rgbaMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bmp);

        mImageView.setImageBitmap(bmp);
    }

    /**
     * OpenCV only supports landscape pictures, so we gotta rotate 90 degrees.
     */
    protected Mat rotateImage(Mat imageMat) {
        Mat result = emptyMat(imageMat.rows(), imageMat.cols());

        Core.transpose(imageMat, result);
        Core.flip(result, result, 1);

        return result;
    }
}

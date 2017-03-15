package org.fingerblox.fingerblox;


import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class ImageProcessing {
    private byte[] data;

    public ImageProcessing(byte[] data) {
        this.data = data;
    }

    public Bitmap getProcessedImage(int screenWidth, int screenHeight) {
        Mat BGRImage = Imgcodecs.imdecode(new MatOfByte(data), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
        Mat image = emptyMat(BGRImage.cols(), BGRImage.rows());
        Imgproc.cvtColor(BGRImage, image, Imgproc.COLOR_BGR2GRAY, 4);

        image = rotateImage(image);

        Mat rgbaMat = new Mat(image.width(), image.height(), CvType.CV_8U, new Scalar(4));
        Imgproc.cvtColor(image, rgbaMat, Imgproc.COLOR_GRAY2RGBA, 4);

        // to bitmap
        Bitmap bmp = Bitmap.createBitmap(rgbaMat.cols(), rgbaMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bmp);

        return getResizedBitmap(bmp, screenWidth, screenHeight);
    }

    private Bitmap getResizedBitmap(Bitmap original, int screenWidth, int screenHeight) {
        int imageWidth = original.getWidth();
        int imageHeight = original.getHeight();
        float xRatio = (float)screenWidth / (float)imageWidth;
        float yRatio = (float)screenHeight / (float)imageHeight;
        float ratio = (xRatio < yRatio) ? xRatio : yRatio;

        // scale down the image in order to transfer it to ImageDisplayActivity using Intent
        ratio = ratio / 4;

        if (ratio >= 1) return original;

        int newWidth = (int)(ratio * imageWidth);
        int newHeight = (int)(ratio * imageHeight);

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }

    @NonNull
    private Mat emptyMat(int width, int height) {
        return new Mat(width, height, CvType.CV_8U, new Scalar(2));
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

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
        Mat image = BGRToGray(data);
        image = rotate(image);
        Mat rgbaMat = grayToRGBA(image);

        return toBitmap(image, rgbaMat);
    }

    @NonNull
    private Mat BGRToGray(byte[] image) {
        Mat BGRImage = Imgcodecs.imdecode(new MatOfByte(image), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
        Mat res = emptyMat(BGRImage.cols(), BGRImage.rows());
        Imgproc.cvtColor(BGRImage, res, Imgproc.COLOR_BGR2GRAY, 4);
        return res;
    }

    @NonNull
    private Bitmap toBitmap(Mat image, Mat rgbaMat) {
        Bitmap bmp = Bitmap.createBitmap(rgbaMat.cols(), rgbaMat.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(image, bmp);
        return bmp;
    }

    @NonNull
    private Mat grayToRGBA(Mat image) {
        Mat rgbaMat = new Mat(image.width(), image.height(), CvType.CV_8U, new Scalar(4));
        Imgproc.cvtColor(image, rgbaMat, Imgproc.COLOR_GRAY2RGBA, 4);
        return rgbaMat;
    }

    @NonNull
    private Mat emptyMat(int width, int height) {
        return emptyMat(width, height, 1);
    }

    @NonNull
    private Mat emptyMat(int width, int height, int dimension) {
        return new Mat(width, height, CvType.CV_8U, new Scalar(dimension));
    }

    /**
     * OpenCV only supports landscape pictures, so we gotta rotate 90 degrees.
     */
    private Mat rotate(Mat image) {
        Mat result = emptyMat(image.rows(), image.cols());

        Core.transpose(image, result);
        Core.flip(result, result, 1);

        return result;
    }
}

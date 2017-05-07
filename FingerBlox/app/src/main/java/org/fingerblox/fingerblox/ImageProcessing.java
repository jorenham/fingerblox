package org.fingerblox.fingerblox;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.baoyachi.stepview.VerticalStepView;

import org.opencv.BuildConfig;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.opencv.core.Core.NORM_HAMMING;

class ImageProcessing {
    public static final String TAG = "ImageProcessing";

    private static MatOfKeyPoint keypointsField;
    private static Mat descriptorsField;

    private byte[] data;

    private int paddingSize;

    private AppCompatActivity mainActivity;
    private VerticalStepView stepView;

    private int progressStep;

    ImageProcessing(byte[] data, AppCompatActivity mainActivity, VerticalStepView stepView) {
        this.data = data;
        this.mainActivity = mainActivity;
        this.stepView = stepView;
    }

    /*
     * get fingerprint skeleton image. Large part of this code is copied from
     * https://github.com/noureldien/FingerprintRecognition/blob/master/Java/src/com/fingerprintrecognition/ProcessActivity.java
     */
    Bitmap getProcessedImage() {
        this.progressStep = 0;

        Mat imageColor = bytesToMat(data);

        imageColor = rotateImage(imageColor);
        imageColor = cropFingerprint(imageColor);
        imageColor = skinDetection(imageColor);

        incrementProgress();

        Mat image = new Mat(imageColor.rows(), imageColor.cols(), CvType.CV_8UC1);
        Imgproc.cvtColor(imageColor, image, Imgproc.COLOR_BGR2GRAY);

        final int rows = image.rows();
        final int cols = image.cols();

        // apply histogram equalization
        Mat equalized = new Mat(rows, cols, CvType.CV_32FC1);
        Imgproc.equalizeHist(image, equalized);

        incrementProgress();

        // convert to float, very important
        Mat floated = new Mat(rows, cols, CvType.CV_32FC1);
        equalized.convertTo(floated, CvType.CV_32FC1);

        Mat skeleton = getSkeletonImage(floated, rows, cols);
        incrementProgress();

        skeleton = thinning(skeleton);
        incrementProgress();

        Mat skeleton_with_keypoints = detectMinutiae(skeleton, 1);
        incrementProgress();

        return mat2Bitmap(skeleton_with_keypoints, Imgproc.COLOR_RGB2RGBA);
    }

    private void incrementProgress() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressStep += 1;
                stepView.setStepsViewIndicatorComplectingPosition(progressStep);
            }
        });
    }

    private int neighbourCount(Mat skeleton, int row, int col) {
        int cn = 0;
        if (skeleton.get(row-1, col-1)[0] != 0) cn++;
        if (skeleton.get(row-1, col  )[0] != 0) cn++;
        if (skeleton.get(row-1, col+1)[0] != 0) cn++;
        if (skeleton.get(row  , col-1)[0] != 0) cn++;
        if (skeleton.get(row  , col+1)[0] != 0) cn++;
        if (skeleton.get(row+1, col-1)[0] != 0) cn++;
        if (skeleton.get(row+1, col  )[0] != 0) cn++;
        if (skeleton.get(row+1, col+1)[0] != 0) cn++;
        return cn;
    }

    private Mat detectMinutiae(Mat skeleton, int border) {
        HashSet<Minutiae> minutiaeSet = new HashSet<>();
        System.out.println("Detecting minutiae");
        for(int c = border; c<skeleton.cols()-border; c++){
            for(int r = border; r<skeleton.rows()-border; r++) {
                double point = skeleton.get(r, c)[0];
                if (point != 0) {  // Not black
                    int cn = neighbourCount(skeleton, r, c);

                    if(cn == 1)
                        minutiaeSet.add(new Minutiae(c, r, Minutiae.Type.RIDGEENDING));
                    else if(cn == 3)
                        minutiaeSet.add(new Minutiae(c, r, Minutiae.Type.BIFURCATION));
                }
            }
        }

        System.out.println("filtering minutiae");
        HashSet<Minutiae> filteredMinutiae = filterMinutiae(minutiaeSet, skeleton);
        System.out.println("number of minutiae: " + filteredMinutiae.size());
        Mat result = new Mat();
        System.out.println("Drawing minutiae");
        Imgproc.cvtColor(skeleton, result, Imgproc.COLOR_GRAY2RGB);
        double[] red = {255, 0, 0};
        double[] green = {0, 255, 0};
        for (Minutiae m : filteredMinutiae) {
            double [] color;
            if (m.type == Minutiae.Type.BIFURCATION) color = green;
            else color = red;
            result.put(m.y, m.x  , color);
            result.put(m.y, m.x-1, color);
            result.put(m.y, m.x+1, color);
            result.put(m.y-1, m.x  , color);
            result.put(m.y+1, m.x  , color);
        }
        MatOfKeyPoint keypoints = new MatOfKeyPoint();
        keypoints.fromArray(minutiaeToKeyPoints(skeleton, filteredMinutiae));
        keypointsField = keypoints;
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        Mat descriptors = new Mat();
        extractor.compute(skeleton, keypoints, descriptors);
        descriptorsField = descriptors;
        return result;
    }

    private KeyPoint[] minutiaeToKeyPoints(Mat skeleton, HashSet<Minutiae> minutiae) {
        KeyPoint[] result = new KeyPoint[minutiae.size()];
        int index = 0;
        float size = 1;
        for (Minutiae m : minutiae) {
            KeyPoint k;
            float angle = -1;
            float response = 1;
            int octave = 1;
            int class_id;
            if (m.type == Minutiae.Type.RIDGEENDING) {
                angle = getMinutiaeAngle(skeleton, m);
                class_id = Minutiae.RIDGE_ENDING_LABEL;
            } else {
                class_id = Minutiae.BIFURCATION_LABEL;
            }
            result[index] = new KeyPoint(m.x, m.y, size, angle, response, octave, class_id);
            index++;
        }
        return result;
    }

    private float getMinutiaeAngle(Mat skeleton, Minutiae m) {
        Minutiae direction;
        try {
            direction = followRidge(skeleton, 5, m.x, m.y, m.x, m.y);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        double length = Math.sqrt(Math.pow(direction.x-m.x, 2) + Math.pow(direction.y-m.y, 2));
        double cosine = (direction.y-m.y) / length;
        float angle = (float)Math.acos(cosine);
        if (direction.x - m.x < 0)
            angle = -angle + 2 * (float)Math.PI;
        return angle;
    }

    private Minutiae followRidge(Mat skeleton, int length, int currentX, int currentY, int previousX, int previousY) throws Exception {
        if (length == 0)
            return new Minutiae(currentX, currentY, Minutiae.Type.RIDGEENDING);
        if (currentY >= skeleton.rows() - 1 || currentY == 0 || currentX >= skeleton.cols() - 1 || currentX == 0)
            throw new Exception("out of bounds");
        int _x, _y;
        _x = currentX - 1;
        _y = currentY - 1;
        if (followRidgeCheck(skeleton, _x, _y, previousX, previousY))
            return followRidge(skeleton, length-1, _x, _y, currentX, currentY);
        _x = currentX - 1;
        _y = currentY;
        if (followRidgeCheck(skeleton, _x, _y, previousX, previousY))
            return followRidge(skeleton, length-1, _x, _y, currentX, currentY);
        _x = currentX - 1;
        _y = currentY + 1;
        if (followRidgeCheck(skeleton, _x, _y, previousX, previousY))
            return followRidge(skeleton, length-1, _x, _y, currentX, currentY);
        _x = currentX;
        _y = currentY - 1;
        if (followRidgeCheck(skeleton, _x, _y, previousX, previousY))
            return followRidge(skeleton, length-1, _x, _y, currentX, currentY);
        _x = currentX;
        _y = currentY + 1;
        if (followRidgeCheck(skeleton, _x, _y, previousX, previousY))
            return followRidge(skeleton, length-1, _x, _y, currentX, currentY);
        _x = currentX + 1;
        _y = currentY - 1;
        if (followRidgeCheck(skeleton, _x, _y, previousX, previousY))
            return followRidge(skeleton, length-1, _x, _y, currentX, currentY);
        _x = currentX + 1;
        _y = currentY;
        if (followRidgeCheck(skeleton, _x, _y, previousX, previousY))
            return followRidge(skeleton, length-1, _x, _y, currentX, currentY);
        _x = currentX + 1;
        _y = currentY + 1;
        return followRidge(skeleton, length-1, _x, _y, currentX, currentY);
    }

    private boolean followRidgeCheck(Mat skeleton, int x, int y, int previousX, int previousY) {
        if (x == previousX && y == previousY)
            return false;
        return skeleton.get(y, x)[0] != 0;
    }

    private void removeMinutiae(int minDistance, HashSet<Minutiae> source, HashSet<Minutiae> target) {
        HashSet<Minutiae> toBeRemoved = new HashSet<>();
        HashSet<Minutiae> check = new HashSet<>(source);
        for (Minutiae m : source) {
            if (toBeRemoved.contains(m)) continue;
            boolean ok = true;
            check.remove(m);
            for (Minutiae m2 : check) {
                if (m.euclideanDistance(m2) < minDistance) {
                    ok = false;
                    toBeRemoved.add(m2);
                }
            }
            if (ok) target.add(m);
            else toBeRemoved.add(m);
        }
    }

    private HashSet<Minutiae> filterMinutiae(HashSet<Minutiae> src, Mat skeleton) {
        Mat mask = snapShotMask(skeleton.rows(), skeleton.cols(), paddingSize + 5);

        HashSet<Minutiae> ridgeEnding = new HashSet<>();
        HashSet<Minutiae> bifurcation = new HashSet<>();
        HashSet<Minutiae> filtered = new HashSet<>();
        for (Minutiae m : src) {
            if (mask.get(m.y, m.x)[0] > 0) {  // filter out borders
                if (m.type == Minutiae.Type.BIFURCATION) ridgeEnding.add(m);
                else bifurcation.add(m);
            }
        }

        int minDistance = 5;
        removeMinutiae(minDistance, ridgeEnding, filtered);
        removeMinutiae(minDistance, bifurcation, filtered);
        return filtered;
    }

    @NonNull
    private Mat detectFeatures(Mat skeleton, Mat edges) {
        FeatureDetector star = FeatureDetector.create(FeatureDetector.ORB);
        DescriptorExtractor brief = DescriptorExtractor.create(DescriptorExtractor.ORB);

        MatOfKeyPoint keypoints = new MatOfKeyPoint();
        star.detect(skeleton, keypoints);
        keypointsField = keypoints;

        KeyPoint[] keypointArray = keypoints.toArray();
        ArrayList<KeyPoint> filteredKeypointArray = new ArrayList<>(keypointArray.length);

        int filterCount = 0;
        for (KeyPoint k : keypointArray) {
            if (edges.get((int)k.pt.y, (int)k.pt.x)[0] <= 0.0) {
                k.size /= 8;
                filteredKeypointArray.add(k);
            } else {
                filterCount++;
            }
        }
        Log.d(TAG, String.format("Filtered %s Keypoints", filterCount));

        keypoints.fromList(filteredKeypointArray);

        Mat descriptors = new Mat();
        brief.compute(skeleton, keypoints, descriptors);
        descriptorsField = descriptors;

        Mat results = new Mat();
        Scalar color = new Scalar(255, 0, 0); // RGB
        Features2d.drawKeypoints(skeleton, keypoints, results, color, Features2d.DRAW_RICH_KEYPOINTS);
        return results;
    }

    /**
     * Match using the ratio test and RANSAC.
     * Returns the ratio matches and the total keypoints
     */
    static double matchFeatures(MatOfKeyPoint keyPoints, Mat descriptors) {
        BFMatcher matcher = BFMatcher.create(NORM_HAMMING, true);
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(descriptorsField, descriptors, matches);
        int maxDistance = 100;
        int matchCount = 0;
        for (DMatch m : matches.toArray()) {
            if (m.distance <= maxDistance)
                matchCount++;
        }
        return (float)matchCount / Math.max(keyPoints.rows(), keypointsField.rows());
    }

    public Mat skinDetection(Mat src) {
        // define the upper and lower boundaries of the HSV pixel
        // intensities to be considered 'skin'
        Scalar lower = new Scalar(0, 48, 80);
        Scalar upper = new Scalar(20, 255, 255);

        // Convert to HSV
        Mat hsvFrame = new Mat(src.rows(), src.cols(), CvType.CV_8U, new Scalar(3));
        Imgproc.cvtColor(src, hsvFrame, Imgproc.COLOR_RGB2HSV, 3);

        // Mask the image for skin colors
        Mat skinMask = new Mat(hsvFrame.rows(), hsvFrame.cols(), CvType.CV_8U, new Scalar(3));
        Core.inRange(hsvFrame, lower, upper, skinMask);
//        currentSkinMask = new Mat(hsvFrame.rows(), hsvFrame.cols(), CvType.CV_8U, new Scalar(3));
//        skinMask.copyTo(currentSkinMask);

        // apply a series of erosions and dilations to the mask
        // using an elliptical kernel
        final Size kernelSize = new Size(11, 11);
        final Point anchor = new Point(-1, -1);
        final int iterations = 2;

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, kernelSize);
        Imgproc.erode(skinMask, skinMask, kernel, anchor, iterations);
        Imgproc.dilate(skinMask, skinMask, kernel, anchor, iterations);

        // blur the mask to help remove noise, then apply the
        // mask to the frame
        final Size ksize = new Size(3, 3);

        Mat skin = new Mat(skinMask.rows(), skinMask.cols(), CvType.CV_8U, new Scalar(3));
        Imgproc.GaussianBlur(skinMask, skinMask, ksize, 0);
        Core.bitwise_and(src, src, skin, skinMask);

        return skin;
    }

    private Mat getSkeletonImage(Mat src, int rows, int cols) {
        // step 1: get ridge segment by padding then do block process
        int blockSize = 24;
        double threshold = 0.05;
        Mat padded = imagePadding(src, blockSize);

        int imgRows = padded.rows();
        int imgCols = padded.cols();

        Mat matRidgeSegment = new Mat(imgRows, imgCols, CvType.CV_32FC1);
        Mat segmentMask = new Mat(imgRows, imgCols, CvType.CV_8UC1);
        ridgeSegment(padded, matRidgeSegment, segmentMask, blockSize, threshold);

        // step 2: get ridge orientation
        int gradientSigma = 1;
        int blockSigma = 13;
        int orientSmoothSigma = 15;
        Mat matRidgeOrientation = new Mat(imgRows, imgCols, CvType.CV_32FC1);
        ridgeOrientation(matRidgeSegment, matRidgeOrientation, gradientSigma, blockSigma, orientSmoothSigma);

        // step 3: get ridge frequency
        int fBlockSize = 36;
        int fWindowSize = 5;
        int fMinWaveLength = 5;
        int fMaxWaveLength = 25;
        Mat matFrequency = new Mat(imgRows, imgCols, CvType.CV_32FC1);
        double medianFreq = ridgeFrequency(matRidgeSegment, segmentMask, matRidgeOrientation, matFrequency, fBlockSize, fWindowSize, fMinWaveLength, fMaxWaveLength);

        // step 4: get ridge filter
        Mat matRidgeFilter = new Mat(imgRows, imgCols, CvType.CV_32FC1);
        double filterSize = 1.9;
        int padding = ridgeFilter(matRidgeSegment, matRidgeOrientation, matFrequency, matRidgeFilter, filterSize, filterSize, medianFreq);
        paddingSize = padding;

        // step 5: enhance image after ridge filter
        Mat matEnhanced = new Mat(imgRows, imgCols, CvType.CV_8UC1);
        enhancement(matRidgeFilter, matEnhanced, blockSize, rows, cols, padding);

        return matEnhanced;
    }

    private Bitmap mat2Bitmap(Mat src, int code) {
        Mat rgbaMat = new Mat(src.width(), src.height(), CvType.CV_8UC4);
        Imgproc.cvtColor(src, rgbaMat, code, 4);
        Bitmap bmp = Bitmap.createBitmap(rgbaMat.cols(), rgbaMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgbaMat, bmp);
        return bmp;
    }

    private Mat cropFingerprint(Mat src) {
        int rowStart = (int) (CameraOverlayView.PADDING * src.rows());
        int rowEnd = (int) ((1 - CameraOverlayView.PADDING) * src.rows());
        int colStart = (int) (CameraOverlayView.PADDING * src.cols());
        int colEnd = (int) ((1 - CameraOverlayView.PADDING) * src.cols());
        Range rowRange = new Range(rowStart, rowEnd);
        Range colRange = new Range(colStart, colEnd);
        return src.submat(rowRange, colRange);
    }

    private Mat bytesToMat(byte[] data) {
        // Scale down the image for performance
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        int targetWidth = 1200;
        if (bmp.getWidth() > targetWidth) {
            float scaleDownFactor = (float)targetWidth / bmp.getWidth();
            bmp = Bitmap.createScaledBitmap(bmp,
                    (int)(bmp.getWidth()*scaleDownFactor),
                    (int)(bmp.getHeight()*scaleDownFactor),
                    true);
        }
        Mat BGRImage = new Mat (bmp.getWidth(), bmp.getHeight(), CvType.CV_8UC3);
        Utils.bitmapToMat(bmp, BGRImage);

        return BGRImage;
    }

    @NonNull
    private Mat emptyMat(int width, int height, int dimension) {
        return new Mat(width, height, CvType.CV_8U, new Scalar(dimension));
    }

    /**
     * OpenCV only supports landscape pictures, so we gotta rotate 90 degrees.
     */
    private Mat rotateImage(Mat image) {
        Mat result = emptyMat(image.rows(), image.cols(), 3);

        Core.transpose(image, result);
        Core.flip(result, result, 1);

        return result;
    }

    // region FingerprintRecognition

    /**
     * Apply padding to the image.
     */
    private Mat imagePadding(Mat source, int blockSize) {

        int width = source.width();
        int height = source.height();

        int bottomPadding = 0;
        int rightPadding = 0;

        if (width % blockSize != 0) {
            bottomPadding = blockSize - (width % blockSize);
        }
        if (height % blockSize != 0) {
            rightPadding = blockSize - (height % blockSize);
        }
        Core.copyMakeBorder(source, source, 0, bottomPadding, 0, rightPadding, Core.BORDER_CONSTANT, Scalar.all(0));
        return source;
    }

    /**
     * calculate ridge segment by doing block process for the given image using the given block size.
     */
    private void ridgeSegment(Mat source, Mat result, Mat mask, int blockSize, double threshold) {

        // for each block, get standard deviation
        // and replace the block with it
        int widthSteps = source.width() / blockSize;
        int heightSteps = source.height() / blockSize;

        MatOfDouble mean = new MatOfDouble(0);
        MatOfDouble std = new MatOfDouble(0);
        Mat window;
        Scalar scalarBlack = Scalar.all(0);
        Scalar scalarWhile = Scalar.all(255);

        Mat windowMask = new Mat(source.rows(), source.cols(), CvType.CV_8UC1);

        Rect roi;
        double stdVal;

        for (int y = 1; y <= heightSteps; y++) {
            for (int x = 1; x <= widthSteps; x++) {

                roi = new Rect((blockSize) * (x - 1), (blockSize) * (y - 1), blockSize, blockSize);
                windowMask.setTo(scalarBlack);
                Imgproc.rectangle(windowMask, new Point(roi.x, roi.y), new Point(roi.x + roi.width, roi.y + roi.height), scalarWhile, -1, 8, 0);

                window = source.submat(roi);
                Core.meanStdDev(window, mean, std);
                stdVal = std.toArray()[0];
                result.setTo(Scalar.all(stdVal), windowMask);

                // mask used to calc mean and standard deviation later
                mask.setTo(Scalar.all(stdVal >= threshold ? 1 : 0), windowMask);
            }
        }

        // get mean and standard deviation
        Core.meanStdDev(source, mean, std, mask);
        Core.subtract(source, Scalar.all(mean.toArray()[0]), result);
        Core.meanStdDev(result, mean, std, mask);
        Core.divide(result, Scalar.all(std.toArray()[0]), result);
    }

    /**
     * Calculate ridge orientation.
     */
    private void ridgeOrientation(Mat ridgeSegment, Mat result, int gradientSigma, int blockSigma, int orientSmoothSigma) {

        int rows = ridgeSegment.rows();
        int cols = ridgeSegment.cols();

        // calculate image gradients
        int kSize = Math.round(6 * gradientSigma);
        if (kSize % 2 == 0) {
            kSize++;
        }
        Mat kernel = gaussianKernel(kSize, gradientSigma);

        Mat fXKernel = new Mat(1, 3, CvType.CV_32FC1);
        Mat fYKernel = new Mat(3, 1, CvType.CV_32FC1);
        fXKernel.put(0, 0, -1);
        fXKernel.put(0, 1, 0);
        fXKernel.put(0, 2, 1);
        fYKernel.put(0, 0, -1);
        fYKernel.put(1, 0, 0);
        fYKernel.put(2, 0, 1);

        Mat fX = new Mat(kSize, kSize, CvType.CV_32FC1);
        Mat fY = new Mat(kSize, kSize, CvType.CV_32FC1);
        Imgproc.filter2D(kernel, fX, CvType.CV_32FC1, fXKernel);
        Imgproc.filter2D(kernel, fY, CvType.CV_32FC1, fYKernel);

        Mat gX = new Mat(rows, cols, CvType.CV_32FC1);
        Mat gY = new Mat(rows, cols, CvType.CV_32FC1);
        Imgproc.filter2D(ridgeSegment, gX, CvType.CV_32FC1, fX);
        Imgproc.filter2D(ridgeSegment, gY, CvType.CV_32FC1, fY);

        // covariance data for the image gradients
        Mat gXX = new Mat(rows, cols, CvType.CV_32FC1);
        Mat gXY = new Mat(rows, cols, CvType.CV_32FC1);
        Mat gYY = new Mat(rows, cols, CvType.CV_32FC1);
        Core.multiply(gX, gX, gXX);
        Core.multiply(gX, gY, gXY);
        Core.multiply(gY, gY, gYY);

        // smooth the covariance data to perform a weighted summation of the data.
        kSize = Math.round(6 * blockSigma);
        if (kSize % 2 == 0) {
            kSize++;
        }
        kernel = gaussianKernel(kSize, blockSigma);
        Imgproc.filter2D(gXX, gXX, CvType.CV_32FC1, kernel);
        Imgproc.filter2D(gYY, gYY, CvType.CV_32FC1, kernel);
        Imgproc.filter2D(gXY, gXY, CvType.CV_32FC1, kernel);
        Core.multiply(gXY, Scalar.all(2), gXY);

        // analytic solution of principal direction
        Mat denom = new Mat(rows, cols, CvType.CV_32FC1);
        Mat gXXMiusgYY = new Mat(rows, cols, CvType.CV_32FC1);
        Mat gXXMiusgYYSquared = new Mat(rows, cols, CvType.CV_32FC1);
        Mat gXYSquared = new Mat(rows, cols, CvType.CV_32FC1);
        Core.subtract(gXX, gYY, gXXMiusgYY);
        Core.multiply(gXXMiusgYY, gXXMiusgYY, gXXMiusgYYSquared);
        Core.multiply(gXY, gXY, gXYSquared);
        Core.add(gXXMiusgYYSquared, gXYSquared, denom);
        Core.sqrt(denom, denom);

        // sine and cosine of doubled angles
        Mat sin2Theta = new Mat(rows, cols, CvType.CV_32FC1);
        Mat cos2Theta = new Mat(rows, cols, CvType.CV_32FC1);
        Core.divide(gXY, denom, sin2Theta);
        Core.divide(gXXMiusgYY, denom, cos2Theta);

        // smooth orientations (sine and cosine)
        // smoothed sine and cosine of doubled angles
        kSize = Math.round(6 * orientSmoothSigma);
        if (kSize % 2 == 0) {
            kSize++;
        }
        kernel = gaussianKernel(kSize, orientSmoothSigma);
        Imgproc.filter2D(sin2Theta, sin2Theta, CvType.CV_32FC1, kernel);
        Imgproc.filter2D(cos2Theta, cos2Theta, CvType.CV_32FC1, kernel);

        // calculate the result as the following, so the values of the matrix range [0, PI]
        //orientim = atan2(sin2theta,cos2theta)/360;
        atan2(sin2Theta, cos2Theta, result);
        Core.multiply(result, Scalar.all(Math.PI / 360.0), result);
    }

    /**
     * Create Gaussian kernel.
     */
    private Mat gaussianKernel(int kSize, int sigma) {

        Mat kernelX = Imgproc.getGaussianKernel(kSize, sigma, CvType.CV_32FC1);
        Mat kernelY = Imgproc.getGaussianKernel(kSize, sigma, CvType.CV_32FC1);

        Mat kernel = new Mat(kSize, kSize, CvType.CV_32FC1);
        Core.gemm(kernelX, kernelY.t(), 1, Mat.zeros(kSize, kSize, CvType.CV_32FC1), 0, kernel, 0);
        return kernel;
    }

    /**
     * Calculate bitwise atan2 for the given 2 images.
     */
    private void atan2(Mat src1, Mat src2, Mat dst) {

        int height = src1.height();
        int width = src2.width();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                dst.put(y, x, Core.fastAtan2((float) src1.get(y, x)[0], (float) src2.get(y, x)[0]));
            }
        }
    }

    /**
     * Calculate ridge frequency.
     */
    private double ridgeFrequency(Mat ridgeSegment, Mat segmentMask, Mat ridgeOrientation, Mat frequencies, int blockSize, int windowSize, int minWaveLength, int maxWaveLength) {

        int rows = ridgeSegment.rows();
        int cols = ridgeSegment.cols();

        Mat blockSegment;
        Mat blockOrientation;
        Mat frequency;

        for (int y = 0; y < rows - blockSize; y += blockSize) {
            for (int x = 0; x < cols - blockSize; x += blockSize) {
                blockSegment = ridgeSegment.submat(y, y + blockSize, x, x + blockSize);
                blockOrientation = ridgeOrientation.submat(y, y + blockSize, x, x + blockSize);
                frequency = calculateFrequency(blockSegment, blockOrientation, windowSize, minWaveLength, maxWaveLength);
                frequency.copyTo(frequencies.rowRange(y, y + blockSize).colRange(x, x + blockSize));
            }
        }

        // mask out frequencies calculated for non ridge regions
        Core.multiply(frequencies, segmentMask, frequencies, 1.0, CvType.CV_32FC1);

        // find median frequency over all the valid regions of the image.
        double medianFrequency = medianFrequency(frequencies);

        // the median frequency value used across the whole fingerprint gives a more satisfactory result
        Core.multiply(segmentMask, Scalar.all(medianFrequency), frequencies, 1.0, CvType.CV_32FC1);

        return medianFrequency;
    }

    /**
     * Estimate fingerprint ridge frequency within image block.
     */
    private Mat calculateFrequency(Mat block, Mat blockOrientation, int windowSize, int minWaveLength, int maxWaveLength) {

        int rows = block.rows();
        int cols = block.cols();

        Mat orientation = blockOrientation.clone();
        Core.multiply(orientation, Scalar.all(2.0), orientation);

        int orientLength = (int) (orientation.total());
        float[] orientations = new float[orientLength];
        orientation.get(0, 0, orientations);

        double[] sinOrient = new double[orientLength];
        double[] cosOrient = new double[orientLength];
        for (int i = 1; i < orientLength; i++) {
            sinOrient[i] = Math.sin((double) orientations[i]);
            cosOrient[i] = Math.cos((double) orientations[i]);
        }
        float orient = Core.fastAtan2((float) calculateMean(sinOrient), (float) calculateMean(cosOrient)) / (float) 2.0;

        // rotate the image block so that the ridges are vertical
        Mat rotated = new Mat(rows, cols, CvType.CV_32FC1);
        Point center = new Point(cols / 2, rows / 2);
        double rotateAngle = ((orient / Math.PI) * (180.0)) + 90.0;
        double rotateScale = 1.0;
        Size rotatedSize = new Size(cols, rows);
        Mat rotateMatrix = Imgproc.getRotationMatrix2D(center, rotateAngle, rotateScale);
        Imgproc.warpAffine(block, rotated, rotateMatrix, rotatedSize, Imgproc.INTER_NEAREST);

        // crop the image so that the rotated image does not contain any invalid regions
        // this prevents the projection down the columns from being mucked up
        int cropSize = (int) Math.round(rows / Math.sqrt(2));
        int offset = (int) Math.round((rows - cropSize) / 2.0) - 1;
        Mat cropped = rotated.submat(offset, offset + cropSize, offset, offset + cropSize);

        // get sums of columns
        float sum;
        Mat proj = new Mat(1, cropped.cols(), CvType.CV_32FC1);
        for (int c = 1; c < cropped.cols(); c++) {
            sum = 0;
            for (int r = 1; r < cropped.cols(); r++) {
                sum += cropped.get(r, c)[0];
            }
            proj.put(0, c, sum);
        }

        // find peaks in projected grey values by performing a grayScale
        // dilation and then finding where the dilation equals the original values.
        Mat dilateKernel = new Mat(windowSize, windowSize, CvType.CV_32FC1, Scalar.all(1.0));
        Mat dilate = new Mat(1, cropped.cols(), CvType.CV_32FC1);
        Imgproc.dilate(proj, dilate, dilateKernel, new Point(-1, -1), 1);
        //Imgproc.dilate(proj, dilate, dilateKernel, new Point(-1, -1), 1, Imgproc.BORDER_CONSTANT, Scalar.all(0.0));

        double projMean = Core.mean(proj).val[0];
        double projValue;
        double dilateValue;
        final double ROUND_POINTS = 1000;
        ArrayList<Integer> maxind = new ArrayList<>();
        for (int i = 0; i < cropped.cols(); i++) {

            projValue = proj.get(0, i)[0];
            dilateValue = dilate.get(0, i)[0];

            // round to maximize the likelihood of equality
            projValue = (double) Math.round(projValue * ROUND_POINTS) / ROUND_POINTS;
            dilateValue = (double) Math.round(dilateValue * ROUND_POINTS) / ROUND_POINTS;

            if (dilateValue == projValue && projValue > projMean) {
                maxind.add(i);
            }
        }

        // determine the spatial frequency of the ridges by dividing the distance between
        // the 1st and last peaks by the (No of peaks-1). If no peaks are detected
        // or the wavelength is outside the allowed bounds, the frequency image is set to 0
        Mat result = new Mat(rows, cols, CvType.CV_32FC1, Scalar.all(0.0));
        int peaks = maxind.size();
        if (peaks >= 2) {
            double waveLength = (maxind.get(peaks - 1) - maxind.get(0)) / (peaks - 1);
            if (waveLength >= minWaveLength && waveLength <= maxWaveLength) {
                result = new Mat(rows, cols, CvType.CV_32FC1, Scalar.all((1.0 / waveLength)));
            }
        }

        return result;
    }

    /**
     * Enhance fingerprint image using oriented filters.
     */
    private int ridgeFilter(Mat ridgeSegment, Mat orientation, Mat frequency, Mat result, double kx, double ky, double medianFreq) {

        int angleInc = 3;
        int rows = ridgeSegment.rows();
        int cols = ridgeSegment.cols();

        int filterCount = 180 / angleInc;
        Mat[] filters = new Mat[filterCount];

        double sigmaX = kx / medianFreq;
        double sigmaY = ky / medianFreq;

        //mat refFilter = exp(-(x. ^ 2 / sigmaX ^ 2 + y. ^ 2 / sigmaY ^ 2) / 2). * cos(2 * pi * medianFreq * x);
        int size = (int) Math.round(3 * Math.max(sigmaX, sigmaY));
        size = (size % 2 == 0) ? size : size + 1;
        int length = (size * 2) + 1;
        Mat x = meshGrid(size);
        Mat y = x.t();

        Mat xSquared = new Mat(length, length, CvType.CV_32FC1);
        Mat ySquared = new Mat(length, length, CvType.CV_32FC1);
        Core.multiply(x, x, xSquared);
        Core.multiply(y, y, ySquared);
        Core.divide(xSquared, Scalar.all(sigmaX * sigmaX), xSquared);
        Core.divide(ySquared, Scalar.all(sigmaY * sigmaY), ySquared);

        Mat refFilterPart1 = new Mat(length, length, CvType.CV_32FC1);
        Core.add(xSquared, ySquared, refFilterPart1);
        Core.divide(refFilterPart1, Scalar.all(-2.0), refFilterPart1);
        Core.exp(refFilterPart1, refFilterPart1);

        Mat refFilterPart2 = new Mat(length, length, CvType.CV_32FC1);
        Core.multiply(x, Scalar.all(2 * Math.PI * medianFreq), refFilterPart2);
        refFilterPart2 = matCos(refFilterPart2);

        Mat refFilter = new Mat(length, length, CvType.CV_32FC1);
        Core.multiply(refFilterPart1, refFilterPart2, refFilter);

        // Generate rotated versions of the filter.  Note orientation
        // image provides orientation *along* the ridges, hence +90
        // degrees, and the function requires angles +ve anticlockwise, hence the minus sign.
        Mat rotated;
        Mat rotateMatrix;
        double rotateAngle;
        Point center = new Point(length / 2, length / 2);
        Size rotatedSize = new Size(length, length);
        double rotateScale = 1.0;
        for (int i = 0; i < filterCount; i++) {
            rotateAngle = -(i * angleInc);
            rotated = new Mat(length, length, CvType.CV_32FC1);
            rotateMatrix = Imgproc.getRotationMatrix2D(center, rotateAngle, rotateScale);
            Imgproc.warpAffine(refFilter, rotated, rotateMatrix, rotatedSize, Imgproc.INTER_LINEAR);
            filters[i] = rotated;
        }

        // convert orientation matrix values from radians to an index value
        // that corresponds to round(degrees/angleInc)
        Mat orientIndexes = new Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1);
        Core.multiply(orientation, Scalar.all((double) filterCount / Math.PI), orientIndexes, 1.0, CvType.CV_8UC1);

        Mat orientMask;
        Mat orientThreshold;

        orientMask = new Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1, Scalar.all(0));
        orientThreshold = new Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1, Scalar.all(0.0));
        Core.compare(orientIndexes, orientThreshold, orientMask, Core.CMP_LT);
        Core.add(orientIndexes, Scalar.all(filterCount), orientIndexes, orientMask);

        orientMask = new Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1, Scalar.all(0));
        orientThreshold = new Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1, Scalar.all(filterCount));
        Core.compare(orientIndexes, orientThreshold, orientMask, Core.CMP_GE);
        Core.subtract(orientIndexes, Scalar.all(filterCount), orientIndexes, orientMask);

        // finally, find where there is valid frequency data then do the filtering
        Mat value = new Mat(length, length, CvType.CV_32FC1);
        Mat subSegment;
        int orientIndex;
        double sum;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (frequency.get(r, c)[0] > 0
                        && r > (size + 1)
                        && r < (rows - size - 1)
                        && c > (size + 1)
                        && c < (cols - size - 1)) {
                    orientIndex = (int) orientIndexes.get(r, c)[0];
                    subSegment = ridgeSegment.submat(r - size - 1, r + size, c - size - 1, c + size);
                    Core.multiply(subSegment, filters[orientIndex], value);
                    sum = Core.sumElems(value).val[0];
                    result.put(r, c, sum);
                }
            }
        }

        return size;
    }

    /**
     * Enhance the image after ridge filter.
     * Apply mask, binary threshold, thinning, ..., etc.
     */
    private void enhancement(Mat source, Mat result, int blockSize, int rows, int cols, int padding) {
        Mat MatSnapShotMask = snapShotMask(rows, cols, padding);
        Mat paddedMask = imagePadding(MatSnapShotMask, blockSize);

        if (BuildConfig.DEBUG && !paddedMask.size().equals(source.size())) {
            throw new RuntimeException("Incompatible sizes of image and mask");
        }

        // apply the original mask to get rid of extras
        Core.multiply(source, paddedMask, result, 1.0, CvType.CV_8UC1);

        // apply binary threshold
        Imgproc.threshold(result, result, 0, 255, Imgproc.THRESH_BINARY);
    }

    /**
     * Create mesh grid.
     */
    private Mat meshGrid(int size) {

        int l = (size * 2) + 1;
        int value = -size;

        Mat result = new Mat(l, l, CvType.CV_32FC1);
        for (int c = 0; c < l; c++) {
            for (int r = 0; r < l; r++) {
                result.put(r, c, value);
            }
            value++;
        }
        return result;
    }

    /**
     * Apply cos to each element of the matrix.
     */
    private Mat matCos(Mat source) {

        int rows = source.rows();
        int cols = source.cols();

        Mat result = new Mat(cols, rows, CvType.CV_32FC1);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                result.put(r, c, Math.cos(source.get(r, c)[0]));
            }
        }

        return result;
    }

    /**
     * Calculate the median of all values greater than zero.
     */
    private double medianFrequency(Mat image) {

        ArrayList<Double> values = new ArrayList<>();
        double value;

        for (int r = 0; r < image.rows(); r++) {
            for (int c = 0; c < image.cols(); c++) {
                value = image.get(r, c)[0];
                if (value > 0) {
                    values.add(value);
                }
            }
        }

        Collections.sort(values);
        int size = values.size();
        double median = 0;

        if (size > 0) {
            int halfSize = size / 2;
            if ((size % 2) == 0) {
                median = (values.get(halfSize - 1) + values.get(halfSize)) / 2.0;
            } else {
                median = values.get(halfSize);
            }
        }
        return median;
    }

    /**
     * Calculate mean of given array.
     */
    private double calculateMean(double[] m) {
        double sum = 0;
        for (double aM : m) {
            sum += aM;
        }
        return sum / m.length;
    }

    /**
     * Mask used in the snapshot.
     */
    private Mat snapShotMask(int rows, int cols, int padding) {
        /*
        Some magic numbers. We have no idea where these come from?!
        int maskWidth = 260;
        int maskHeight = 160;
        */

        Point center = new Point(cols / 2, rows / 2);
        Size axes = new Size(cols/2 - padding, rows/2 - padding);
        Scalar scalarWhite = new Scalar(255, 255, 255);
        Scalar scalarBlack = new Scalar(0, 0, 0);
        int thickness = -1;
        int lineType = 8;

        Mat mask = new Mat(rows, cols, CvType.CV_8UC1, scalarBlack);
        Imgproc.ellipse(mask, center, axes, 0, 0, 360, scalarWhite, thickness, lineType, 0);
        return mask;
    }

    private Mat thinning(Mat img) {
        Mat thinned = new Mat(img.size(), CvType.CV_8UC1);

        Imgproc.threshold(img, thinned, 0, 255, Imgproc.THRESH_OTSU);

        Thinning t = new Thinning();
        thinned = t.doJaniThinning(thinned);

        return thinned;
    }

    public static MatOfKeyPoint getKeypoints(){
        return keypointsField;
    }

    public static Mat getDescriptors(){
        return descriptorsField;
    }
  
    static Bitmap preprocess(Mat frame, int width, int height) {
        // convert to grayscale
        Mat frameGrey = new Mat(height, width, CvType.CV_8UC1);
        Imgproc.cvtColor(frame, frameGrey, Imgproc.COLOR_BGR2GRAY, 1);

        // rotate
        Mat rotatedFrame = new Mat(width, height, frameGrey.type());
        Core.transpose(frameGrey, rotatedFrame);
        Core.flip(rotatedFrame, rotatedFrame, Core.ROTATE_180);

        // resize to match the surface view
        Mat resizedFrame = new Mat(width, height, rotatedFrame.type());
        Imgproc.resize(rotatedFrame, resizedFrame, new Size(width, height));

        // crop
        Mat ellipseMask = getEllipseMask(width, height);
        Mat frameCropped = new Mat(resizedFrame.rows(), resizedFrame.cols(), resizedFrame.type(), new Scalar(0));
        resizedFrame.copyTo(frameCropped, ellipseMask);

        // histogram equalisation
        Mat frameHistEq = new Mat(frame.rows(), frameCropped.cols(), frameCropped.type());
        Imgproc.equalizeHist(frameCropped, frameHistEq);

        // convert back to rgba
        Mat frameRgba = new Mat(frameHistEq.rows(), frameHistEq.cols(), CvType.CV_8UC4);
        Imgproc.cvtColor(frameHistEq, frameRgba, Imgproc.COLOR_GRAY2RGBA);

        // crop again to correct alpha
        Mat frameAlpha = new Mat(frameRgba.rows(), frameRgba.cols(), CvType.CV_8UC4, new Scalar(0, 0, 0, 0));
        frameRgba.copyTo(frameAlpha, ellipseMask);

        // convert to bitmap
        Bitmap bmp = Bitmap.createBitmap(frameAlpha.cols(), frameAlpha.rows(), Bitmap.Config.ARGB_4444);
        Utils.matToBitmap(frameAlpha, bmp);

        return bmp;
    }

    private static Mat ellipseMask;

    @NonNull
    private static Mat getEllipseMask(int width, int height) {
        if (ellipseMask == null || ellipseMask.cols() != width || ellipseMask.rows() != height) {
            int paddingX = (int) (CameraOverlayView.PADDING * (float) width);
            int paddingY = (int) (CameraOverlayView.PADDING * (float) height);
            RotatedRect box = new RotatedRect(
                    new Point(width / 2, height / 2),
                    new Size(width - (2 * paddingX), height - (2 * paddingY)),
                    0
            );
            ellipseMask = new Mat(height, width, CvType.CV_8UC1, new Scalar(0));
            Imgproc.ellipse(ellipseMask, box, new Scalar(255), -1);
        }
        return ellipseMask;
    }
}

class Thinning {
    private boolean B[][];

    Mat doJaniThinning(Mat Image) {
        B = new boolean[Image.rows()][Image.cols()];
        // Inverse of B
        boolean [][] B_ = new boolean[Image.rows()][Image.cols()];
        for(int i=0; i<Image.rows(); i++)
            for(int j=0; j<Image.cols(); j++)
                B[i][j] = (Image.get(i, j)[0] > 10); //not a mistake, in matlab first invert and then morph

        boolean[][] prevB = new boolean[Image.rows()][Image.cols()];
        final int maxIter = 1000;
        for(int iter = 0; iter < maxIter; iter++) {
            // Assign B to prevB
            for (int i=0; i<Image.rows(); i++)
                System.arraycopy(B[i], 0, prevB[i], 0, Image.cols());

            //Iteration #1
            for(int i=0; i<Image.rows(); i++)
                for (int j = 0; j < Image.cols(); j++)
                    B_[i][j] = !(B[i][j] && G1(i, j) && G2(i, j) && G3(i, j)) && B[i][j];

            // Assign result of iteration #1 to B, so that iteration #2 will see the results
            for(int i=0; i<Image.rows(); i++)
                System.arraycopy(B_[i], 0, B[i], 0, Image.cols());


            //Iteration #2
            for(int i=0; i<Image.rows(); i++)
                for (int j = 0; j < Image.cols(); j++)
                    B_[i][j] = !(B[i][j] && G1(i, j) && G2(i, j) && G3_(i, j)) && B[i][j];

            // Assign result of Iteration #2 to B
            for(int i=0; i<Image.rows(); i++)
                System.arraycopy(B_[i], 0, B[i], 0, Image.cols());

            // stop when it doesn't change anymore
            boolean convergence = true;
            for (int i = 0; i < Image.rows(); i++)
                convergence &= Arrays.equals(B[i], prevB[i]);
            if (convergence){
                break;
            }
        }

        removeFalseRidgeEndings(Image);

        Mat r = Mat.zeros(Image.size(), CvType.CV_8UC1);

        for(int i=0; i<Image.rows(); i++)
            for(int j=0; j<Image.cols(); j++)
                if (B[i][j])
                    r.put(i, j, 255);
        return r;
    }

    // remove ridge endings shorter than minimumRidgeLength
    private void removeFalseRidgeEndings(Mat Image) {
        int minimumRidgeLength = 5;
        for(int i=0; i<Image.rows(); i++)
            for (int j = 0; j < Image.cols(); j++)
                if (B[i][j] && neighbourCount(i, j) == 1)
                    removeEnding(i, j, minimumRidgeLength);
    }

    // follow ridge recursively and remove if shorter than minimumlength
    private boolean removeEnding(int i, int j, int minimumLength) {
        if (minimumLength < 0)
            return true;
        if (neighbourCount(i, j) > 1)
            return false;
        B[i][j] = false;
        if (neighbourCount(i, j) == 0)
            return false;
        int index = 0;
        for (int a=1; a<=8; a++) {
            if (x(a, i, j)) {
                index = a;
                break;
            }
        }
        int _i = i, _j = j;
        switch (index) {
            case 1:
                _i = i+1;
                break;
            case 2:
                _i = i+1;
                _j = j+1;
                break;
            case 3:
                _j = j+1;
                break;
            case 4:
                _i = i-1;
                _j = j+1;
                break;
            case 5:
                _i = i-1;
                break;
            case 6:
                _i = i-1;
                _j = j-1;
                break;
            case 7:
                _j = j-1;
                break;
            case 8:
                _i = i+1;
                _j = j-1;
                break;
        }
        boolean ok = removeEnding(_i, _j, minimumLength-1);
        if (ok)
            B[i][j] = true;
        return ok;
    }

    private int neighbourCount(int i, int j) {
        int cn = 0;
        for (int a=1; a<=8; a++)
            if (x(a, i, j))
                cn++;
        return cn;
    }

    private boolean x(int a, int i, int j) {
        try {
            switch(a) {
                case 1:
                    return B[i+1][j];
                case 2:
                    return B[i+1][j+1];
                case 3:
                    return B[i][j+1];
                case 4:
                    return B[i-1][j+1];
                case 5:
                    return B[i-1][j];
                case 6:
                    return B[i-1][j-1];
                case 7:
                    return B[i][j-1];
                case 8:
                    return B[i+1][j-1];
            }
        } catch(IndexOutOfBoundsException e) {
            return false;
        }
        return false;
    }

    private boolean G1(int i, int j) {
        int X = 0;
        for(int q=1; q<=4; q++) {
            if(!x(2*q-1, i, j) && (x(2*q, i, j) || x(2*q+1, i, j))) X++;
        }
        return X==1;
    }

    private boolean G2(int i, int j) {
        int m = Math.min(n1(i, j), n2(i, j));
        return (m==2 || m==3);
    }

    private int n1(int i, int j) {
        int r = 0;
        for(int q=1; q<=4; q++)
            if(x(2*q-1, i, j) || x(2*q, i, j)) r++;
        return r;
    }

    private int n2(int i, int j) {
        int r = 0;
        for(int q=1; q<=4; q++)
            if(x(2*q, i, j) || x(2*q+1, i, j)) r++;
        return r;
    }

    private boolean G3(int i, int j) {
        return (x(2, i, j) || x(3, i, j) || !x(8, i, j)) && x(1, i, j);
    }

    private boolean G3_(int i, int j) {
        return (x(6, i, j) || x(7, i, j) || !x(4, i, j)) && x(5, i, j);
    }

}


class Minutiae {
    static final int BIFURCATION_LABEL = 1;
    static final int RIDGE_ENDING_LABEL = 0;
    enum Type {BIFURCATION, RIDGEENDING};
    int x;
    int y;
    Type type;

    public Minutiae(int x, int y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public double euclideanDistance(Minutiae m) {
        return Math.sqrt(Math.pow(this.x - m.x, 2) + Math.pow(this.y - m.y, 2));
    }
}
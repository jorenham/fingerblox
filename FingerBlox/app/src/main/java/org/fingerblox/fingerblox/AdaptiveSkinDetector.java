package org.fingerblox.fingerblox;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

import static org.opencv.core.Core.NORM_MINMAX;
import static org.opencv.core.Core.inRange;
import static org.opencv.core.Core.normalize;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2HSV;
import static org.opencv.imgproc.Imgproc.COLOR_HSV2BGR;
import static org.opencv.imgproc.Imgproc.dilate;

public class AdaptiveSkinDetector {
    Mat image;

    //adaptive hue thresholds for skin color detection
    int _hueLower;
    int _hueUpper;

    //global lower and upper thresholds for skin color detection
    Scalar lower;
    Scalar higher;

    Mat hist;

    //histogram merge factor for weighted average
    float _mergeFactor;

    //histogram paramters
    int[] histSize;
    float[] ranges;
    int[] channels;

    //object for histogram computation
    Histogram h;

    //image required for image motion histogram
    Mat p1;

    public AdaptiveSkinDetector() {
        _hueLower=3;
        _hueUpper=33;

        lower=new Scalar(3,50,50);
        higher=new Scalar(33,255,255);

        //the global histogram is given 0.95% weightage
        _mergeFactor=0.95f;

        //setting the historgram computation parameters
        channels = new int[1];
        channels[0]=0;

        histSize = new int[1];
        histSize[0]=30;

        ranges = new float[2];
        ranges[0]=0;
        ranges[1]=30;
        h = new Histogram(histSize, ranges, channels);
    }

    public void run(Mat image, Mat mask) {
        Imgproc.cvtColor(image,image,COLOR_BGR2HSV);
        inRange(image,lower,higher,mask);
        ArrayList<Mat> ch = new ArrayList<>();
        Mat hue = new Mat();
        Core.split(image,ch);
        ch.get(0).copyTo(hue);

        //setting the mask for histogram
        h.setMask(mask);

        //build histogram based on global skin color threshols
        Mat hist=h.BuildHistogram(hue,false);

        //normalize the histogram
        normalize(hist,hist,0,1,NORM_MINMAX);
        //update the histograms
        h.setHist(hist);

        //get the histogram thresholds
        int[] range1=h.getThreshHist(hist,0.05f,0.05f);

        _hueLower=range1[0];
        _hueUpper=range1[1];

        //obseve the pixels encountering motion
        Mat mmask = new Mat();
        if(!p1.empty()) {
            Mat motion = new Mat();
            Core.absdiff(p1,ch.get(2),motion);
            inRange(motion,new Scalar(8,0,0),new Scalar(255,0,0),mmask);
            Imgproc.erode(mmask,mmask,new Mat());
            dilate(mmask,mmask,new Mat());
        }


        //compute a combined mask,representing motion of skin colored pixels
        if(!mmask.empty())
            Core.bitwise_and(mask,mmask,mmask);

        //set the new histogram mask
        h.setMask(mmask);

        //compute the histogram based on updated mask
        Mat shist=h.BuildHistogram(hue,false);
        //normalize the histogram
        normalize(shist,shist,0,1,NORM_MINMAX);

        //merge both the histograms
        h.mergeHistogram(shist, 0.02f);

        //get the final histogram
        hist=h.getHist();

        //get the histogram thresholds
        h.getThreshHist(hist, 0.05f, 0.05f);

        //update the histogram thresholds
        _hueLower=range1[0];
        _hueUpper=range1[1];

        //comptute the new mask
        int[] zero = {0, 0, 0, 0};
        for (int i=0; i<mask.rows(); i++) {
            for (int j=0; j<mask.cols(); j++) {
                if (!((hue.get(i, j)[0] >= _hueLower) && (hue.get(i, j)[0] <= _hueUpper))) {
                    mask.put(i, j, zero);
                }
            }
        }

        //store the current intensity image
        ch.get(2).copyTo(p1);
        Imgproc.cvtColor(image,image,COLOR_HSV2BGR);
    }
}

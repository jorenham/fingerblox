package org.fingerblox.fingerblox;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

import static org.opencv.core.Core.mean;

public class Histogram {
    Mat _mask;
    Mat _histMat = new Mat();
    int[] _histSize = {30};  // Size of the histogram
    float[] _histRange = {0, 30};  //Range for all the matrices
    int[]_channels = {0};
    Mat _tmpHist;

    public Histogram() {}

    void setMask(Mat mask) {
        _mask = new Mat(mask.rows(), mask.cols(), mask.type());
        mask.copyTo(_mask);
    }

    Mat BuildHistogram(Mat srcImage, boolean accumulate) {
        Mat histMat = new Mat();
        MatOfInt c = new MatOfInt(0);

        // MatOfInt h = new MatOfInt(_channels.length, 1);
        MatOfInt h = new MatOfInt(30);

        // MatOfFloat ranges = new MatOfFloat(_channels.length, 2);
        MatOfFloat ranges = new MatOfFloat(0, 30);
        if(accumulate==true)
            _histMat.copyTo(histMat);

        ArrayList<Mat> imgList = new ArrayList<>();
        imgList.add(srcImage);

        Imgproc.calcHist(
                imgList,
                c,
                _mask,
                histMat,
                h,
                ranges,
                accumulate
        );
        if(accumulate==true)
            histMat.copyTo(_histMat);
        return histMat;
    }

    int[] getThreshHist(Mat histMat, float s1, float s2){
        int[] imgThresh = new int[2];

        Scalar s=mean(histMat);
        float N = (float)(s.val[0]) * histMat.cols() * histMat.rows();
        float maxth = (1-s2)*N;
        float minth = s1*N;
        int mini=0,maxi=0;
        float cmini=0,cmaxi=N;
        boolean lower=true,higher=true;

        for (int i = 0; i < histMat.rows(); i++){

            //if(i>=(int)((float)_histRange[0]/histMat.rows))
            cmini += histMat.get(i, 0)[0];
            //if(i<=(int)((float)_histRange[1]/histMat.rows))
            cmaxi-= histMat.get(histMat.rows()-1-i, 0)[0];

            if(cmini >= minth && lower==true){
                mini = i;
                lower=false;
            }
            if(cmaxi <= maxth && higher==true){
                maxi = histMat.rows()-i;
                higher=false;
            }
            if(lower==false && higher ==false )
                break;
        }
        imgThresh[0]=mini;
        imgThresh[1]=maxi;
        return imgThresh;
    }

    void mergeHistogram(Mat hist,float factor)
    {
        if(_histMat.empty()) {
            hist.copyTo(_histMat);
        }
        else {
            Core.multiply(_histMat, new Scalar(factor), _histMat);
            Core.scaleAdd(hist, 1-factor, _histMat, _histMat);
        }
    }

    Mat getHist() {
        return _histMat;
    }

    void setHist(Mat hist) {
        hist.copyTo(_histMat);
    }
}

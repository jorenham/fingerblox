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
    Mat _histMat;
    int[] _histSize;  // Size of the histogram
    float[] _histRange;  //Range for all the matrices
    int[]_channels;
    Mat _tmpHist;

    public Histogram(int[] histSize, float[] ranges, int[] channels) {
        _histSize = histSize;
        _histRange = ranges;
        _channels = channels;
    }

    void setMask(Mat mask) {
        mask.copyTo(_mask);
    }

    Mat BuildHistogram(Mat srcImage, boolean accumulate) {
        Mat histMat = new Mat();
        MatOfInt c = new MatOfInt(_channels.length, 1);
        // int[] c = new int[_channels.length];
        for(int i=0;i<_channels.length;i++) {
            c.put(i, 0, _channels[i]);
        }

        MatOfInt h = new MatOfInt(_channels.length, 1);
        // int[] h = new int[_channels.length];
        for(int i=0;i<_channels.length;i++) {
            h.put(i, 0, _histSize[_channels[i]]);
        }

        MatOfFloat ranges = new MatOfFloat(_channels.length, 2);
        // float[][] ranges = new float[_channels.length][2];
        int size=_channels.length;
        for(int i=0;i<size;i++) {
            /*
            float[] x = new float[2];
            int index=2*_channels[i];
            x[0]=_histRange[index];
            x[1]=_histRange[index+1];
            ranges[i]=x;
            */
            int index=2*_channels[i];
            ranges.put(i, 0, _histRange[index]);
            ranges.put(i, 1, _histRange[index+1]);
        }
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
            cmaxi-= histMat.get(histMat.rows()-i, 0)[0];

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

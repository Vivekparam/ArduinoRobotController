package com.param.vivek.arduinorobotcontroller;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;

public class HandDetectionActivity extends Activity implements CvCameraViewListener2 {
    private static final String  TAG              = "HandDetection::Activity";

    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Mat                  prevImage;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private HandDetector    mDetector;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;


    // TODO: MOVE THIS TO SEPARATE CLASS
    private static Mat skinCrCbHist;

    private int blockSize = 2;
    private int apertureSize = 3;

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public HandDetectionActivity() {
        Log.i(TAG, "Instantiated new HandDetectionActivity.");
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.hand_detection_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.hand_detection_activity_surface_view);
//        mOpenCvCameraView.setMaxFrameSize(500, 500);
        if( Camera.getNumberOfCameras() > 1) {
            mOpenCvCameraView.setCameraIndex(1);
        }
        mOpenCvCameraView.setCvCameraViewListener(this);

//        skinCrCbHist = new Mat();
//        skinCrCbHist.se
////        for(int j = 0; j < skinCrCbHist.rows(); j++) {
////            for (int i = 0; i < skinCrCbHist.cols(); i++) {
////                skinCrCbHist.setTo(new Scalar(0, 0, 0)); // set all zeroes
////            }
////        }
//        Imgproc.ellipse(skinCrCbHist, new Point(113, 155.6), new Size(23.4, 15.2), 43.0, 0.0, 360, new Scalar(255, 255, 255), -1);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new HandDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        if (skinCrCbHist == null) {
            skinCrCbHist =  Mat.zeros(256, 256, CvType.CV_8UC1);
//          for(int j = 0; j < skinCrCbHist.rows(); j++) {
//              for (int i = 0; i < skinCrCbHist.cols(); i++) {
//                  skinCrCbHist.setTo(new Scalar(0, 0, 0)); // set all zeroes
//              }
//          }
            Imgproc.ellipse(skinCrCbHist, new Point(113, 155.6), new Size(23.4, 15.2), 43.0, 0.0, 360, new Scalar(255, 255, 255), -1);
        }
//        if(prevImage == null) {
//            return inputFrame.rgba();
//        }
//        return skinDetectionYCRCB(inputFrame);
        return skinDetection(inputFrame);
//        return cornerDetection(inputFrame);
    }

    private Mat skinDetectionYCRCB(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        Mat output = new Mat();

        Scalar minYCrCb = new Scalar(0,133,77);
        Scalar maxYCrCb = new Scalar(255,173,127);

        Imgproc.cvtColor(mRgba, output, Imgproc.COLOR_RGB2YCrCb);

        Mat inRangeMatrix = new Mat();
        Core.inRange(output, minYCrCb, maxYCrCb, inRangeMatrix);

        List<MatOfPoint> contours = new LinkedList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(inRangeMatrix, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        for(int i = 0; i < contours.size(); i++) {
            MatOfPoint c = contours.get(i);
            if(Imgproc.contourArea(c) > 1000) {
                Imgproc.drawContours(mRgba, contours, i , new Scalar(0, 255, 0), 3);
            }
        }
        return mRgba;
    }
    private Mat skinDetection(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        // local params
        int downFactor = 2;
        int shiftFactor = 10;



        Mat down = new Mat();
        Imgproc.pyrDown(mRgba, down, new Size(mRgba.cols() / downFactor, mRgba.rows() / downFactor));
        // init to zeroes
        Mat handPossibilities = Mat.zeros(new Size(mRgba.cols() / downFactor, mRgba.rows() / downFactor), mRgba.type());

        // set of points (row, col)
        List<Point> handPossiblePoints = new LinkedList<Point>();

//        Imgproc
        long startTime = System.currentTimeMillis();
        for(int j = 0; j < down.rows(); j += shiftFactor) {
            for(int i = 0; i < down.cols(); i += shiftFactor) {
                double[] rgb = down.get(j, i);
                Scalar color = new Scalar(rgb[0], rgb[1], rgb[2]);
                if(isSkinRGB((int)rgb[0], (int) rgb[1],(int) rgb[2])) {
//                if(isSkin(color)) {
                    rgb[0] = 255; // rgb[0] (HIGH) signifies this is a hand point
                    rgb[1] = 255; // rgb[1] (HIGH) signifies that this point has not been processed
                    rgb[2] = 255; // this is irrelevant right now
                    down.put(j, i, rgb);
                    handPossibilities.put(j,i,rgb);
                    Imgproc.circle(handPossibilities, new Point(i, j), 5, new Scalar(128, 128, 128), 2, 8, 0);
                    handPossiblePoints.add(new Point(i, j)); // col, row
                }
            }
        }
        Log.i("TAG", "[time] SKIN_DETECTION took: " + (System.currentTimeMillis() - startTime) + " ms");
        Log.i("TAG", "[time] SKIN_DETECTION result points: " + handPossiblePoints.size());

        // Construct downsized result matrix
        Mat resultDown = new Mat();
        Imgproc.pyrDown(mRgba, resultDown, new Size(mRgba.cols() / downFactor, mRgba.rows() / downFactor));

//        add a point at every possible location
//        for(int j = 0; j < down.rows(); j += shiftFactor) {
//            for (int i = 0; i < down.cols(); i += shiftFactor) {
//                Imgproc.circle(resultDown, new Point(i, j), 3, new Scalar(128, 128, 128), 2, 8, 0);
//            }
//        }

        // Copy all possible points into result so we can see the guesses
        for(Point clusterPoint : handPossiblePoints) {
            double[] color = {255, 255, 255, 255};
            Imgproc.circle(resultDown, new Point(clusterPoint.x, clusterPoint.y), 3, new Scalar(128, 128, 128), 2, 8, 0);
        }

        // Stores all possible clusters, each of which is a Set<Point>
//        Set<Set<Point>> clusters = new HashSet<Set<Point>>();

        startTime = System.currentTimeMillis();
        // generate all clusters
//        while(handPossiblePoints.size() > 0) {
//            Point p = handPossiblePoints.remove(0);
//            clusters.add(clusterHelper(p, handPossibilities, clusters, 0, shiftFactor));
////            Log.i("TAG", "[time] handPossiblePoints.size(): " + handPossiblePoints.size());
//
//
//        }

        Set<Set<Point>> clusters = euclideanClustering(handPossiblePoints, handPossibilities, shiftFactor);
        Log.i("TAG", "[time] GENERATE_CLUSTERS took: " + (System.currentTimeMillis() - startTime) + " ms -- numClusters: " + clusters.size());

        // find largest cluster
        int max = 0;
        Set<Point> largestCluster = new HashSet<>();
        for(Set<Point> cluster : clusters) {
            if(cluster.size() >= max) {
                max = cluster.size();
                largestCluster = cluster;
            }
        }
        Log.i(TAG, "[cluster] Largest Cluster size is " + largestCluster.size());


        Scalar clusterColor = new Scalar(0, 128, 255);

        // downsized version of result
//        for(Set<Point> cluster: clusters) {
            for (Point clusterPoint : largestCluster) {
//              Log.d(TAG, "[cluster] adding point from largest cluster");
//              resultDown.put((int) clusterPoint.x, (int) clusterPoint.y, color);
                Imgproc.circle(resultDown, new Point(clusterPoint.x, clusterPoint.y), 5, clusterColor, 2, 8, 0);
            }

//            // change colors
//            double[] colors = clusterColor.val;
//            colors[0] += 100;
//            colors[1] += 150;
//            colors[2] += 200;
//            clusterColor.set(colors);
//        }
        Mat result = new Mat();
        Imgproc.pyrUp(resultDown, result, new Size(mRgba.cols(), mRgba.rows()));
        return result;
    }

    private Set<Set<Point>> euclideanClustering(List<Point> handPossiblePoints, Mat handPossibilities, int shiftFactor) {
        Set<Set<Point>> clusters = new HashSet<>();

        // how far is someone considered a neighbor?
        final int radius = 1 * shiftFactor;

        for(Point p : handPossiblePoints) {

            // skip if already in a cluster
            if(handPossibilities.get((int) p.y, (int) p.x)[1] != 255) continue;

            Queue<Point> q = new LinkedList<>();
            Set<Point> cluster = new HashSet();
            q.add(p);
            while(!q.isEmpty()) {
                // process the next element by:
                // 1. Add this point to the cluster
                // 2. search its neighbors for points and add them to the queue
                Point cur = q.remove();

                int row = (int) cur.y;
                int col = (int) cur.x;

                // if this point has already been processed, no point being here

                cluster.add(cur); // add this point to the cluster
                Log.i(TAG, "Unprocessed point");
                // Get neighbors which are hand points
                for(int j = row - radius; j <= row + radius; j+= shiftFactor) {
                    for (int i = col - radius; i <= col + radius; i += shiftFactor) {
                        if( j > 0 && j < handPossibilities.rows() &&
                            i > 0 && i < handPossibilities.cols()) {

                            // get the neighbor
                            double[] other = handPossibilities.get(j, i);

                            // if this point has not been visited
                            if (other[0] == 255) {
                                // if this is a handpoint
                                if(other[1] == 255) {

                                    // only add to queue if not visited
                                    q.add(new Point(i, j)); // add it to toProcess queue
                                    other[1] = 0; // disable this point
                                    handPossibilities.put(j, i, other); //disable this point
                                }
                            }
                        }
                    }
                }
            }
            clusters.add(cluster);
            Log.e(TAG, "ADDING CLUSTER OF SIZE " + cluster.size());
        }
        return clusters;
    }

    /** Removes first element from handPossiblePoints. Then, iterates through list to find any direct
     * neighbors which are also possible hand points. If at least three such neighbors are found,
     * Each of those points is removed from handPossibilePoints and
     * added to a local cluster. Then, for each of those points, recursively
     * call this function until we get to a point where there are < 3 neighbors
     * Returns the cluster for that first point that was removed.
     */
    private Set<Point> clusterHelper(Point p, Mat handPossibilities, Set<Set<Point>> clusters, int minNeighbors, final int shiftFactor) {
        // get first element
        // check all points around it to see if it has neighboring hand points
        int row = (int) p.x;
        int col = (int) p.y;
        final double[] visitedPointMarker = {255, 0, 0, 255};
//        handPossibilities.put(row, col, visitedPointMarker); // mark this point as visited

        // how far is someone considered a neighbor?
        final int neighborDistThresh = 1 * shiftFactor;

        Queue<Point> localCluster = new LinkedList<Point>();

//        // Get number of neighbors which are hand points
//        for(int i = 0; i < handPossiblePoints.size(); i++) {
//            Point other = handPossiblePoints.get(i);
//            if ((other.y - row <= neighborDistThresh ) && (other.x - col <= neighborDistThresh)) {
//                // This is a handpoint
//                localCluster.add(other); // add it to localCluster
//                handPossiblePoints.remove(i); // this point belongs to this cluster one way or another
//                i--; // we want to stay on this i
//            }
//        }

        int numNeighbors = 0; // This is separate from localCluster.size() because
                                // it includes points which have already been counted
        // Get number of neighbors which are hand points
        for(int j = row - neighborDistThresh; j <= row + neighborDistThresh; j+= shiftFactor) {
            for(int i = col - neighborDistThresh; i <= col + neighborDistThresh; i+= shiftFactor ) {
                // if it is within bounds
                if( j > 0 && j < handPossibilities.rows() &&
                    i > 0 && i < handPossibilities.cols()) {
                    double[] other = handPossibilities.get(j, i);

                    // if this point is a handPoint and has not been visited
                    if (other[0] == 255) {
                        // This is a handpoint
                        if(other[1] == 255) {
                            // only add to cluster if not visited
                            localCluster.add(new Point(i, j)); // add it to localCluster
                            other[1] = 0; // disable this point
                            handPossibilities.put(j, i, other); // disable this point
                        }

                        // add this point to numNeighbors
                        // whether or not it was previously visited
                        numNeighbors++;

                    }
                }
            }
        }

//        if(numNeighbors < minNeighbors) {
//            // Since there werent enough neighbors, return empty hashset -- dont want outliers
//            return new HashSet<Point>();
//        }

        // This will store any results passed up to us from recursive calls
        Set<Point> resultCluster = new HashSet<Point>();

        // If we get here, we need to remove all the points in localCluster from
        // handPossiblePoints. We will do this, then recurse, for each point

        // For each point in localCluster, remove it from handPossiblePoints,
        // add it to the final Result cluster and
        // recurse, adding returned Points to resultCluster
        for(Point toRecurse : localCluster) {
            resultCluster.add(toRecurse);
            resultCluster.addAll(clusterHelper(toRecurse, handPossibilities, clusters, minNeighbors, shiftFactor));
        }
        return resultCluster;
    }

    public static boolean isSkin(Scalar color) {
        Mat input = new Mat(new Size(1, 1), CvType.CV_8UC3, color);
        Mat output = new Mat();

        Imgproc.cvtColor(input, output, Imgproc.COLOR_RGB2YCrCb);

        double ycrcb[] = output.get(0, 0);
        return (skinCrCbHist.get((int) ycrcb[1], (int) ycrcb[2])[0] > 0);
    }



    public boolean isSkinRGB(int r, int g, int b) {
        // first easiest comparisons
        if ( (r<95) | (g<40) | (b<20) | (r<g) | (r<b) ) {
            return false; // no match, stop function here
        }
        int d = r-g;
        if ( -15<d && d<15) {
            return false; // no match, stop function here
        }
        // we have left most time consuming operation last
        // hopefully most of the time we are not reaching this point
        int max = Math.max(r, Math.max(g,b));
        int min = Math.min(r, Math.min(g,b));
        if ((max-min)<15) {
            // this is the worst case
            return false; // no match, stop function
        }
        // all comparisons passed
        return true;
    }

    private Mat cornerDetection(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.gray();
        Mat down = new Mat();
        Imgproc.pyrDown(mRgba, down, new Size(mRgba.cols() / 2, mRgba.rows() / 2));
//        Imgproc.pyrdown
//        Mat down2 = new Mat();
//        Imgproc.pyrDown(down, down2);
//        Mat down3 = new Mat();
//        Imgproc.pyrDown(down2, down3);

        Mat dst = new Mat();
        Mat dst_norm = new Mat();
        Mat dst_norm_scaled = new Mat();


        Imgproc.cornerHarris(down, dst, blockSize, apertureSize, Core.BORDER_DEFAULT );
        Core.normalize(dst, dst_norm, 0, 255, Core.NORM_MINMAX, CvType.CV_32FC1, new Mat());
        Core.convertScaleAbs(dst_norm, dst_norm_scaled);

//        Mat upTemp = new Mat();
//        Imgproc.pyrUp(dst_norm_scaled, upTemp);
        Mat result = new Mat();
        Imgproc.pyrUp(dst_norm_scaled, result, new Size(mRgba.cols(), mRgba.rows()));
        for(int j = 0; j < dst_norm.rows(); j++) {
            for(int i = 0; i < dst_norm.cols(); i++) {
                if(dst_norm.get(j, i)[0] <100 ) {
//                    Log.i(TAG, "Drawing circle at (" + i + ", " + j + ")");
                    Imgproc.circle(result, new Point(i * 2, j * 2), 5, new Scalar(0), 2, 8, 0);
                }
            }
        }
        return result;
    }


    // ==== BUTTON CALLBACKS ====

    public void onIncrBlockSize(View v) {
        blockSize++;
        Log.e(TAG, "[camparams] Blocksize incremented to " + blockSize);
    }

    public void onDecrBlockSize(View v) {
        if (blockSize > 1) {
            blockSize--;
        }
        Log.e(TAG, "[camparams] Blocksize decremented to " + blockSize);
    }

    public void onIncrApertureSize(View v) {
        apertureSize += 2; // must remain odd
        Log.e(TAG, "[camparams] apertureSize incremented to " + apertureSize);
    }

    public void onDecrApertureSize(View v) {
        if (apertureSize >= 3) {
            apertureSize -= 2;
        }
        Log.e(TAG, "[camparams] apertureSize decremented to " + apertureSize);
    }
}

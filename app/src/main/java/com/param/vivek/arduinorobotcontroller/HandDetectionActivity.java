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
            Imgproc.ellipse(skinCrCbHist, new Point(113, 155.6), new Size(23.4, 15.2), 43.0, 0.0, 360, new Scalar(255, 255, 255), -1);
        }
        return handDetection(inputFrame);
    }

    private Mat handDetection(CvCameraViewFrame inputFrame) {
        // Get start time for optimization purposes
        long skinDetectionStartTime = System.currentTimeMillis();

        // convert image to rgba
        mRgba = inputFrame.rgba();
        Mat flipped = new Mat();
        Core.flip(mRgba, flipped, 1); // flip around y axis

        // local params
        int downFactor = 2;
        int shiftFactor = 15;

        // downsample image for faster processing
        Mat down = new Mat();
        Imgproc.pyrDown(flipped, down, new Size(mRgba.cols() / downFactor, mRgba.rows() / downFactor));

        // find center of image
        int imgCenterRow = down.rows() / 2;
        int imgCenterCol = down.cols() / 2;
        Point imgCenterPoint = new Point(imgCenterCol, imgCenterRow);

        // init to zeroes
        Mat handPossibilities = Mat.zeros(new Size(mRgba.cols() / downFactor, mRgba.rows() / downFactor), mRgba.type());

        // set of points (row, col)
        List<Point> handPossiblePoints = new LinkedList<Point>();

        // For each shiftFactor'th point, calculate
        // whether or not it is a skin-colored pixel.
        // If it is skin-colored, mark it as so and
        // add the point to handPossiblePoints.
        long startTime = System.currentTimeMillis();
        for(int j = 0; j < down.rows(); j += shiftFactor) {
            for(int i = 0; i < down.cols(); i += shiftFactor) {
                double[] rgb = down.get(j, i);
                Scalar color = new Scalar(rgb[0], rgb[1], rgb[2]);
                if(isSkinRGB((int)rgb[0], (int) rgb[1],(int) rgb[2])) {
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
        Log.i(TAG, "[time] SKIN_DETECTION took: " + (System.currentTimeMillis() - startTime) + " ms");

        // Construct downsized result matrix
        Mat resultDown = new Mat();
        Imgproc.pyrDown(flipped, resultDown, new Size(mRgba.cols() / downFactor, mRgba.rows() / downFactor));

        // Copy all possible points into result so we can see the guesses
        for(Point clusterPoint : handPossiblePoints) {
            double[] color = {255, 255, 255, 255};
            Imgproc.circle(resultDown, new Point(clusterPoint.x, clusterPoint.y), 3, new Scalar(128, 128, 128), 2, 8, 0);
        }


        // Perform euclidean cluster extraction
        startTime = System.currentTimeMillis();
        Set<Set<Point>> clusters = euclideanClustering(handPossiblePoints, handPossibilities, shiftFactor);
        Log.i(TAG, "[time] GENERATE_CLUSTERS took: " + (System.currentTimeMillis() - startTime) + " ms -- numClusters: " + clusters.size());

        // find largest cluster
        int max = 0;
        Set<Point> largestCluster = new HashSet<>();
        for(Set<Point> cluster : clusters) {
            if(cluster.size() >= max) {
                max = cluster.size();
                largestCluster = cluster;
            }
        }
        // Log.i(TAG, "[cluster] Largest Cluster size is " + largestCluster.size());

        // Draw the largest cluster in resultDown
        for (Point clusterPoint : largestCluster) {
            Scalar clusterColor = new Scalar(0, 128, 255);
            Imgproc.circle(resultDown, new Point(clusterPoint.x, clusterPoint.y), 5, clusterColor, 2, 8, 0);
        }

        // get the centroid of the largest cluster
        Point centroid = centroid(largestCluster);

        // draw the centroid in ResultDown
        Imgproc.circle(resultDown, centroid, 7, new Scalar(0, 255, 0));
        Imgproc.circle(resultDown, centroid, 5, new Scalar(255, 0, 0));
        Imgproc.circle(resultDown, centroid, 9, new Scalar(0, 0, 255));

        // draw the centerPoint of image:
        Imgproc.circle(resultDown, centroid, 9, new Scalar(255, 0, 255), 9);
        Imgproc.circle(resultDown, centroid, 7, new Scalar(0, 255, 255));

        // draw direction rectangles, find which one centroid is in
        Scalar white = new Scalar(255, 255, 255);
        Scalar selected = new Scalar(0, 255, 0);
        for(int n = 0; n < 5; n++) {
            Scalar color = white;
            if(centroid.x > n * down.cols() / 5 && centroid.x < (1+n) * down.cols() / 5) {
                color = selected;
            }
            Imgproc.rectangle(resultDown, new Point(n * down.cols() / 5, 0), new Point((1+n) * down.cols() / 5, down.rows()), color, 7);
        }


        Mat result = new Mat();
        Imgproc.pyrUp(resultDown, result, new Size(mRgba.cols(), mRgba.rows()));

        // calculate direction to send Robot

        if(distance(centroid, imgCenterPoint) > 100) {
            if(centroid.x > imgCenterCol + 200) {

            }
        }

        Log.e(TAG, "[time] SKIN_DETECT_AND_CLUSTER took: " + (System.currentTimeMillis() - skinDetectionStartTime) + " ms");
        return result;
    }

    private double distance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    /**
     * Euclidean Clustering Algorithm. Takes in a list of all possible hand points, and a matrix
     * representing state of each point in the image. Shift-factor is the shiftFactor used
     * when analyzing skin tone (the distance between analyzed points).
     *
     * Returns a Set of Set<Point>. That is, a set of all clusters in the image.
     * @param handPossiblePoints list of all possible hand points
     * @param handPossibilities matrix at which for each point, value at index 0 is a VISITED flag,
     *                          and index 1 is a IS_HAND flag.
     * @param shiftFactor the shiftFactor (distance between measured points on image)
     * @return
     */
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

                cluster.add(cur); // add this point to the cluster
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
        }
        return clusters;
    }

    /**
     *     Returns a point which is the centroid of the passed-in cluster.
     */
    public Point centroid(Set<Point> cluster)  {
        double centroidX = 0, centroidY = 0;

        for(Point knot : cluster) {
            centroidX += knot.x;
            centroidY += knot.y;
        }
        return new Point(centroidX / cluster.size(), centroidY / cluster.size());
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

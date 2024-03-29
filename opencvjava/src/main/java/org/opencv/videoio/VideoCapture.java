
//
// This file is auto-generated. Please don't modify it!
//
package org.opencv.videoio;

import org.opencv.core.Mat;

// C++: class VideoCapture
//javadoc: VideoCapture
public class VideoCapture {

    protected final long nativeObj;
    protected VideoCapture(long addr) { nativeObj = addr; }


    //
    // C++:   VideoCapture(int device)
    //

    //javadoc: VideoCapture::VideoCapture(device)
    public   VideoCapture(int device)
    {
        
        nativeObj = VideoCapture_0(device);
        
        return;
    }


    //
    // C++:   VideoCapture()
    //

    //javadoc: VideoCapture::VideoCapture()
    public   VideoCapture()
    {
        
        nativeObj = VideoCapture_1();
        
        return;
    }


    //
    // C++:   VideoCapture(String filename)
    //

    //javadoc: VideoCapture::VideoCapture(filename)
    public   VideoCapture(String filename)
    {
        
        nativeObj = VideoCapture_2(filename);
        
        return;
    }


    //
    // C++:  double get(int propId)
    //

    //javadoc: VideoCapture::get(propId)
    public  double get(int propId)
    {
        
        double retVal = get_0(nativeObj, propId);
        
        return retVal;
    }


    //
    // C++:  bool set(int propId, double value)
    //

    //javadoc: VideoCapture::set(propId, value)
    public  boolean set(int propId, double value)
    {
        
        boolean retVal = set_0(nativeObj, propId, value);
        
        return retVal;
    }


    //
    // C++:  void release()
    //

    //javadoc: VideoCapture::release()
    public  void release()
    {
        
        release_0(nativeObj);
        
        return;
    }


    //
    // C++:  bool open(int device)
    //

    //javadoc: VideoCapture::open(device)
    public  boolean open(int device)
    {
        
        boolean retVal = open_0(nativeObj, device);
        
        return retVal;
    }


    //
    // C++:  bool open(String filename)
    //

    //javadoc: VideoCapture::open(filename)
    public  boolean open(String filename)
    {
        
        boolean retVal = open_1(nativeObj, filename);
        
        return retVal;
    }


    //
    // C++:  bool grab()
    //

    //javadoc: VideoCapture::grab()
    public  boolean grab()
    {
        
        boolean retVal = grab_0(nativeObj);
        
        return retVal;
    }


    //
    // C++:  bool isOpened()
    //

    //javadoc: VideoCapture::isOpened()
    public  boolean isOpened()
    {
        
        boolean retVal = isOpened_0(nativeObj);
        
        return retVal;
    }


    //
    // C++:  bool read(Mat& image)
    //

    //javadoc: VideoCapture::read(image)
    public  boolean read(Mat image)
    {
        
        boolean retVal = read_0(nativeObj, image.nativeObj);
        
        return retVal;
    }


    //
    // C++:  bool retrieve(Mat& image, int flag = 0)
    //

    //javadoc: VideoCapture::retrieve(image, flag)
    public  boolean retrieve(Mat image, int flag)
    {
        
        boolean retVal = retrieve_0(nativeObj, image.nativeObj, flag);
        
        return retVal;
    }

    //javadoc: VideoCapture::retrieve(image)
    public  boolean retrieve(Mat image)
    {
        
        boolean retVal = retrieve_1(nativeObj, image.nativeObj);
        
        return retVal;
    }


    public java.util.List<org.opencv.core.Size> getSupportedPreviewSizes()
    {
        String[] sizes_str = getSupportedPreviewSizes_0(nativeObj).split(",");
        java.util.List<org.opencv.core.Size> sizes = new java.util.ArrayList<org.opencv.core.Size>(sizes_str.length);

        for (String str : sizes_str) {
            String[] wh = str.split("x");
            sizes.add(new org.opencv.core.Size(Double.parseDouble(wh[0]), Double.parseDouble(wh[1])));
        }

        return sizes;
    }


    @Override
    protected void finalize() throws Throwable {
        delete(nativeObj);
    }



    // C++:   VideoCapture(int device)
    private static native long VideoCapture_0(int device);

    // C++:   VideoCapture()
    private static native long VideoCapture_1();

    // C++:   VideoCapture(String filename)
    private static native long VideoCapture_2(String filename);

    // C++:  double get(int propId)
    private static native double get_0(long nativeObj, int propId);

    // C++:  bool set(int propId, double value)
    private static native boolean set_0(long nativeObj, int propId, double value);

    // C++:  void release()
    private static native void release_0(long nativeObj);

    // C++:  bool open(int device)
    private static native boolean open_0(long nativeObj, int device);

    // C++:  bool open(String filename)
    private static native boolean open_1(long nativeObj, String filename);

    // C++:  bool grab()
    private static native boolean grab_0(long nativeObj);

    // C++:  bool isOpened()
    private static native boolean isOpened_0(long nativeObj);

    // C++:  bool read(Mat& image)
    private static native boolean read_0(long nativeObj, long image_nativeObj);

    // C++:  bool retrieve(Mat& image, int flag = 0)
    private static native boolean retrieve_0(long nativeObj, long image_nativeObj, int flag);
    private static native boolean retrieve_1(long nativeObj, long image_nativeObj);

    private static native String getSupportedPreviewSizes_0(long nativeObj);

    // native support for java finalize()
    private static native void delete(long nativeObj);

}

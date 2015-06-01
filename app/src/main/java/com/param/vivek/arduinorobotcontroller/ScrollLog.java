package com.param.vivek.arduinorobotcontroller;

import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Created by Vivek on 4/22/2015.
 */
public class ScrollLog {
    private TextView mLogTextView;
    private ScrollView mScrollView;

    public ScrollLog(TextView mLogTextView, ScrollView mScrollView) {
        this.mLogTextView = mLogTextView;
        this.mScrollView = mScrollView;
    }

    public void logItem(String message) {
        mLogTextView.append(message + "\n");
        mScrollView.smoothScrollTo(0, mLogTextView.getBottom());
        Log.i("ScrollLog", message);
    }
}

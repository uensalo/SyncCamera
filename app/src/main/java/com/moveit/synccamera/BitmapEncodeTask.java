package com.moveit.synccamera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Process;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BitmapEncodeTask implements Runnable, ProgressObservable {
    private int mJobID;
    private final String mDirectoryPath;
    private volatile int mBitmapsEncoded;
    private Bitmap.CompressFormat mCompressFormat;
    private int mCompressionFactor;
    private List<Bitmap> mBitmaps;
    private int mStartIdx;
    private int mEndIdx;
    private Activity mActivity;

    private List<ProgressObserver> mObservers;
    private Thread mThread;

    public BitmapEncodeTask(int jobID, final List<Bitmap> bitmaps, String directoryPath, Bitmap.CompressFormat compressFormat, int compressionFactor, int startIdx, int endIdx, Activity activity) {
        mJobID = jobID;
        mDirectoryPath = directoryPath;
        mBitmapsEncoded = 0;
        mCompressFormat = compressFormat;
        mCompressionFactor = compressionFactor;
        mBitmaps = bitmaps;
        mStartIdx = startIdx;
        mEndIdx = endIdx;
        mObservers = new ArrayList<>();
        mActivity = activity;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mThread = Thread.currentThread();
        for (int i = mStartIdx; i <= mEndIdx; i++) {
            String filePath = mDirectoryPath + "/" + (i) + "." + mCompressFormat.toString().toLowerCase();
            try (FileOutputStream out = new FileOutputStream(filePath)) {
                if (mBitmaps.get(i) != null && !mBitmaps.get(i).isRecycled()) {
                    mBitmaps.get(i).compress(mCompressFormat, mCompressionFactor, out);
                    mBitmaps.get(i).recycle();
                } else {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.w("WORKER", "DEAD BITMAP!");
                        }
                    });
                }
                mBitmapsEncoded++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyObservers();
                Log.w("WORKER", "DONE");
            }
        });
    }

    @Override
    public void addObserver(ProgressObserver o) {
        mObservers.add(o);
    }

    @Override
    public void notifyObservers() {
        for (ProgressObserver o : mObservers) {
            o.onComplete(mJobID);
        }
    }

    public int jobID() {
        return mJobID;
    }

    public int bitmapsEncoded() {
        return mBitmapsEncoded;
    }

    public int size() {
        return mEndIdx - mStartIdx + 1;
    }

    public float progress() {
        return (float) (mBitmapsEncoded - mStartIdx) / (mEndIdx - mStartIdx + 1);
    }

}

package com.moveit.synccamera;

import android.graphics.Bitmap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RGBDCaptureContext {
    private int mVideoIndex;
    private boolean mCaptureState;
    private int mCurrentCaptureIndex;
    private int mInitialCaptureIndex;

    private String mVideosPath;
    private File mVideoDirectory;
    private File mContextRGBDirectory;
    private File mContextDepthDirectory;

    private List<Bitmap> mColorBitmaps;
    private List<Bitmap> mDepthBitmaps;

    private Bitmap.CompressFormat mColorCompressionFormat;
    private Bitmap.CompressFormat mDepthCompressionFormat;
    private int mCompressionRatio;

    public RGBDCaptureContext(String videosPath, int videoIndex, Bitmap.CompressFormat colorCompressionFormat, Bitmap.CompressFormat depthCompressionFormat, int compressionRatio) {
        mVideoIndex = videoIndex;
        mCaptureState = false;
        mCurrentCaptureIndex = 0;
        mInitialCaptureIndex = 0;

        mVideosPath = videosPath;
        mVideoDirectory = null;
        mContextRGBDirectory = null;
        mContextDepthDirectory = null;

        mColorBitmaps = new ArrayList<>();
        mDepthBitmaps = new ArrayList<>();

        mColorCompressionFormat = colorCompressionFormat;
        mDepthCompressionFormat = depthCompressionFormat;
        mCompressionRatio = compressionRatio;

    }

    public boolean captureState() {
        return mCaptureState;
    }

    public void startCapture() {
        mCaptureState = true;
        mVideoDirectory = new File(mVideosPath + "/" + mVideoIndex);
        mContextRGBDirectory = new File(mVideosPath + "/" + mVideoIndex + "/rgb");
        mContextDepthDirectory = new File(mVideosPath + "/" + mVideoIndex + "/depth");

        mVideoDirectory.mkdir();
        mContextRGBDirectory.mkdir();
        mContextDepthDirectory.mkdir();
    }

    public void insertFrame(Bitmap colorBitmap, Bitmap depthBitmap) {
        if (mCaptureState) {
            mColorBitmaps.add(colorBitmap.copy(null, false));
            mDepthBitmaps.add(depthBitmap.copy(null, false));
            mCurrentCaptureIndex++;
        }
    }

    //TODO: Write relevant metadata to the image files, not needed at the moment
    public void stopCapture() {
        mCaptureState = false;
        BitmapEncoder encoder = BitmapEncoder.instance();
        encoder.setCompressionFactor(mCompressionRatio);

        encoder.setCompressFormat(mColorCompressionFormat);
        for (int i = mInitialCaptureIndex; i < mCurrentCaptureIndex; i++) {
            encoder.encode(mColorBitmaps.get(i), mContextRGBDirectory.getPath() + "/" + i + "." + mColorCompressionFormat.toString().toLowerCase());
        }

        encoder.setCompressFormat(mDepthCompressionFormat);
        for (int i = mInitialCaptureIndex; i < mCurrentCaptureIndex; i++) {
            encoder.encode(mDepthBitmaps.get(i), mContextDepthDirectory.getPath() + "/" + i + "." + mDepthCompressionFormat.toString().toLowerCase());
        }
        //flush to allow the garbage collector to return memory
        mColorBitmaps = new ArrayList<>();
        mDepthBitmaps = new ArrayList<>();
        mInitialCaptureIndex = mCurrentCaptureIndex++;
    }
}

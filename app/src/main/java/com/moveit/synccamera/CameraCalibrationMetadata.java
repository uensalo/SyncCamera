package com.moveit.synccamera;

import android.util.Size;
import android.util.SizeF;

public class CameraCalibrationMetadata {
    private String mCameraID;
    private float[] mIntrinsicCalibration;
    private float[] mExtrinsicRotation;
    private float[] mExtrinsicTranslation;
    private float[] mLensDistortion;
    private int mPoseReference;
    private Size mPixelArraySize;
    private SizeF mPhysicalSize;

    public CameraCalibrationMetadata(String cameraID,
                                     float[] intrinsicCalibration,
                                     float[] extrinsicRotation,
                                     float[] extrinsicTranslation,
                                     float[] lensDistortion,
                                     int poseReference,
                                     Size pixelArraySize,
                                     SizeF physicalSize) {
        mCameraID = cameraID;
        mIntrinsicCalibration = intrinsicCalibration;
        mExtrinsicRotation = extrinsicRotation;
        mExtrinsicTranslation = extrinsicTranslation;
        mLensDistortion = lensDistortion;
        mPoseReference = poseReference;
        mPixelArraySize = pixelArraySize;
        mPhysicalSize = physicalSize;
    }

    public String getCameraID() {
        return mCameraID;
    }

    public float[] getIntrinsicCalibration() {
        return mIntrinsicCalibration;
    }

    public float[] getExtrinsicRotation() {
        return mExtrinsicRotation;
    }

    public float[] getExtrinsicTranslation() {
        return mExtrinsicTranslation;
    }

    public float[] getLensDistortion() {
        return mLensDistortion;
    }

    public int getPoseReference() {
        return mPoseReference;
    }

    public Size getPixelArraySize() {
        return mPixelArraySize;
    }

    public SizeF getPhysicalSize() {
        return mPhysicalSize;
    }

    public float[][] getIntrinsicCalibrationMatrix() {
        return null;
    }

    public float[][] getExtrinsicCalibrationMatrix() {
        return null;
    }

    public float[][] getRotationMatrix() {
        return null;
    }


}

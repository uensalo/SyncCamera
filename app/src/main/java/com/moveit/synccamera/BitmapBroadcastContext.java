package com.moveit.synccamera;

public class BitmapBroadcastContext {
    private String mCameraID;
    private BitmapDataType mDataType;

    public BitmapBroadcastContext(String cameraID, BitmapDataType dataType) {
        mCameraID = cameraID;
        mDataType = dataType;
    }

    public String getCameraID() {
        return mCameraID;
    }

    public BitmapDataType getDataType() {
        return mDataType;
    }
}

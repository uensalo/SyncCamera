package com.moveit.synccamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SizeF;

import java.nio.ByteBuffer;

import static java.lang.Math.acos;
import static java.lang.Math.sin;

public class ColorCamera extends AbstractCamera {
    protected String TAG = "ColorCamera";

    final public static int RES_4032x3024 = 0;
    final public static int RES_4032x2268 = 1;
    final public static int RES_4032x1908 = 2;
    final public static int RES_3024x3024 = 3;
    final public static int RES_3984x2988 = 4;
    final public static int RES_3840x2160 = 5;
    final public static int RES_3264x2448 = 6;
    final public static int RES_3264x1836 = 7;
    final public static int RES_2976x2976 = 8;
    final public static int RES_2880x2160 = 9;
    final public static int RES_2560x1440 = 10;
    final public static int RES_2160x2160 = 11;
    final public static int RES_2048x1152 = 12;
    final public static int RES_1920x1080 = 13;
    final public static int RES_1440x1080 = 14;
    final public static int RES_1088x1088 = 15;
    final public static int RES_1280x720 = 16;
    final public static int RES_960x720 = 17;
    final public static int RES_800x450 = 18;
    final public static int RES_720x720 = 19;
    final public static int RES_720x480 = 20;
    final public static int RES_640x480 = 21;
    final public static int RES_352x288 = 22;
    final public static int RES_320x240 = 23;

    final public static int RES_WIDE_4608x3456 = 0;
    final public static int RES_WIDE_4608x2592 = 1;
    final public static int RES_WIDE_4608x2184 = 2;
    final public static int RES_WIDE_3456x3456 = 3;
    final public static int RES_WIDE_3984x2988 = 4;
    final public static int RES_WIDE_3840x2160 = 5;
    final public static int RES_WIDE_3264x2448 = 6;
    final public static int RES_WIDE_3264x1836 = 7;
    final public static int RES_WIDE_2976x2976 = 8;
    final public static int RES_WIDE_2880x2160 = 9;
    final public static int RES_WIDE_2560x1440 = 10;
    final public static int RES_WIDE_2160x2160 = 11;
    final public static int RES_WIDE_2048x1152 = 12;
    final public static int RES_WIDE_1920x1080 = 13;
    final public static int RES_WIDE_1440x1080 = 14;
    final public static int RES_WIDE_1088x1088 = 15;
    final public static int RES_WIDE_1280x720 = 16;
    final public static int RES_WIDE_1056x704 = 17;
    final public static int RES_WIDE_1024x768 = 18;
    final public static int RES_WIDE_960x720 = 19;
    final public static int RES_WIDE_800x450 = 20;
    final public static int RES_WIDE_720x720 = 21;
    final public static int RES_WIDE_720x480 = 22;
    final public static int RES_WIDE_640x480 = 23;
    final public static int RES_WIDE_352x288 = 24;
    final public static int RES_WIDE_320x240 = 25;

    @Override
    public void broadcastBitmap() {
        BitmapBroadcastContext context = new BitmapBroadcastContext(mCameraID, BitmapDataType.COLOR);
        for (BitmapObserver o : mBitmapObserverList) {
            o.onBitmapAvailable(mBitmap, context);
        }
    }


    @Override
    public Bitmap decodeSensorData(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        for (int i = 0; i < buffer.remaining(); i++) {
            bytes[i] = buffer.get(i);
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @Override
    public void setCaptureRequestParameters() {
        mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<Integer>(30, 30));
        mCaptureRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY);
        mCaptureRequestBuilder.set(CaptureRequest.LENS_FOCAL_LENGTH, mFocalLengths[0]);

        int w = mCameraActiveArraySize.right - mCameraActiveArraySize.left;
        int h = mCameraActiveArraySize.bottom - mCameraActiveArraySize.top;

        float zoomx = 0.97f;
        float zoomy = 0.97f;

        int w2 = (int) (w * zoomx);
        int h2 = (int) (h * zoomy);
        int l = (w - w2) / 2;
        int r = (w + w2) / 2;
        int t = (h - h2) / 2;
        int b = (h + h2) / 2;

        mCaptureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, new Rect(l, t, r, b));
        mCaptureRequestBuilder.set(CaptureRequest.DISTORTION_CORRECTION_MODE, CaptureRequest.DISTORTION_CORRECTION_MODE_HIGH_QUALITY);
    }

    public ColorCamera(Context context, int sizeIndex, int maxImages) {
        super(context, AbstractCamera.DEFAULT_FPS, sizeIndex, maxImages);
        startBackgroundThread();
        try {
            for (String cameraID : mCameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraID);
                int[] capabilities = cameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                assert capabilities != null;
                boolean backFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK;

                float[] focus = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                boolean depthCamera = false;
                boolean logicalMultiCamera = false;
                for(int capability: capabilities) {
                    depthCamera = depthCamera || capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT;
                    logicalMultiCamera = logicalMultiCamera || capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA;
                }
                if(!depthCamera && backFacing) {
                    if (mCameraID == null) {
                        mCameraID = cameraID;
                        mCameraRes = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
                        assert mCameraRes != null;
                        float[] intrinsicCalibration = cameraCharacteristics.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION);
                        float[] poseRotationQuaternion = cameraCharacteristics.get(CameraCharacteristics.LENS_POSE_ROTATION);
                        float[] poseTranslation = cameraCharacteristics.get(CameraCharacteristics.LENS_POSE_TRANSLATION);
                        float[] lensDistortion = cameraCharacteristics.get(CameraCharacteristics.LENS_DISTORTION);
                        int poseReference = cameraCharacteristics.get(CameraCharacteristics.LENS_POSE_REFERENCE);
                        Size pixelArraySize = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
                        SizeF physicalSize = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                        mCameraCalibration = new CameraCalibrationMetadata(cameraID, intrinsicCalibration, poseRotationQuaternion, poseTranslation, lensDistortion, poseReference, pixelArraySize, physicalSize);
                        mFocalLengths = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                        mApertureSizes = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
                        mCameraActiveArraySize = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                        mImageReader = ImageReader.newInstance(mCameraRes[mSizeIndex].getWidth(), mCameraRes[mSizeIndex].getHeight(), ImageFormat.JPEG, mMaxImages);
                    }
                }
            }
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
    }
}

package com.moveit.synccamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.util.Range;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class DepthCamera extends AbstractCamera {
    private String TAG = "DepthCamera";

    private static float sRangeMin = 0.0f;
    private static float sRangeMax = 1800.0f;

    final public static int DEPTH_640_480 = 0;
    final public static int DEPTH_320_240 = 1;

    @Override
    public void broadcastBitmap() {
        BitmapBroadcastContext context = new BitmapBroadcastContext(mCameraID, BitmapDataType.DEPTH);
        for (BitmapObserver o : mBitmapObserverList) {
            o.onBitmapAvailable(mBitmap, context);
        }
    }

    @Override
    public Bitmap decodeSensorData(ByteBuffer buffer) {
        ShortBuffer shortBuffer = buffer.asShortBuffer();
        int[] depthBitmapPixels = new int[mCameraRes[mSizeIndex].getWidth() * mCameraRes[mSizeIndex].getHeight()];
        for (int i = 0; i < mCameraRes[mSizeIndex].getWidth() * mCameraRes[mSizeIndex].getHeight(); i++) {
            short sample = shortBuffer.get(i);
            short depthRange = (short) (sample & 0x1FFF);
            short depthConf = (short) ((sample >> 13) & 0x7);
            float depthPer = depthConf == 0 ? 1.f : (depthConf - 1) / 7.f;
            float actualRange = (depthRange - sRangeMin) / (sRangeMax - sRangeMin);
            //actual order is -> ABGR, because of bit level color bitmap speed hack
            if (depthConf > 0.1) {
                depthBitmapPixels[i] = Color.argb(1.0f, 0, 0, actualRange);
            } else {
                depthBitmapPixels[i] = Color.argb(1.0f, actualRange, 0, 0);
            }
        }
        IntBuffer finalBuffer = IntBuffer.wrap(depthBitmapPixels);
        Bitmap bmp = Bitmap.createBitmap(mCameraRes[mSizeIndex].getWidth(), mCameraRes[mSizeIndex].getHeight(), Bitmap.Config.ARGB_8888);
        bmp.copyPixelsFromBuffer(finalBuffer);
        return bmp;
    }

    @Override
    public void setCaptureRequestParameters() {
        mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0);
        //mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<Integer>(15,60));
        mCaptureRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY);
        mCaptureRequestBuilder.set(CaptureRequest.LENS_FOCAL_LENGTH, new Float(4.3));
        //mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
        //mCaptureRequestBuilder.set(CaptureRequest.LENS_FOCAL_LENGTH, new Float(35));
        //mCaptureRequestBuilder.set(CaptureRequest.LENS_APERTURE, new Float(3));
        mCaptureRequestBuilder.set(CaptureRequest.DISTORTION_CORRECTION_MODE, CaptureRequest.DISTORTION_CORRECTION_MODE_HIGH_QUALITY);
    }

    public DepthCamera(Context context, int sizeIndex, int maxImages) {
        super(context, AbstractCamera.DEFAULT_FPS, sizeIndex, maxImages);
        try {
            for (String cameraID : mCameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraID);
                int[] capabilities = cameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                assert capabilities != null;
                boolean backFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK;
                boolean depthCamera = false;
                boolean logicalMultiCamera = false;
                for(int capability: capabilities) {
                    depthCamera = depthCamera || capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT;
                    logicalMultiCamera = logicalMultiCamera || capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA;
                }
                if(depthCamera && backFacing) {
                    mCameraID = cameraID;
                    mCameraRes = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.DEPTH16);
                    assert mCameraRes != null;
                    mFocalLengths = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                    mApertureSizes = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
                    mIntrinsicCameraParameters = cameraCharacteristics.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION);
                    mCameraActiveArraySize = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                    mImageReader = ImageReader.newInstance(mCameraRes[mSizeIndex].getWidth(), mCameraRes[mSizeIndex].getHeight(), ImageFormat.DEPTH16, mMaxImages);
                }
            }
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
    }
}

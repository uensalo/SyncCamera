package com.moveit.synccamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.util.Half;
import android.util.Log;
import android.util.Range;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class KinectCamera extends AbstractCamera {
    private String TAG = "KinectCamera";

    private static short sDeviceID = 0x0007;

    private static float sRangeMin = 0.0f;
    private static float sRangeMax = 1800.0f;

    final public static int DEPTH_640_480 = 0;
    final public static int DEPTH_320_240 = 1;

    @Override
    public void broadcastBitmap() {
        BitmapBroadcastContext context = new BitmapBroadcastContext(mCameraID, BitmapDataType.DEPTH_KINECT);
        for (BitmapObserver o : mBitmapObserverList) {
            o.onBitmapAvailable(mBitmap, context);
        }
    }

    @Override
    public Bitmap decodeSensorData(ByteBuffer buffer) {
        /*
            Android does not provide any utilities to encode a bitmap with a single channel with 16 bits.
         */
        ShortBuffer shortBuffer = buffer.asShortBuffer();
        int[] depthBitmapPixels = new int[mCameraRes[mSizeIndex].getWidth() * mCameraRes[mSizeIndex].getHeight()];
        for (int i = 0; i < mCameraRes[mSizeIndex].getWidth() * mCameraRes[mSizeIndex].getHeight(); i++) {
            short sample = shortBuffer.get();
            short depthRange = (short) (((sample & 0x1FFF) << 3) | sDeviceID);
            int lower = (int) (depthRange & 0x00FF);
            int upper = (int) (depthRange & 0xFF00) >> 8;
            int color = Color.argb(255, upper, lower, 0);
            depthBitmapPixels[i] = color;
        }
        IntBuffer finalBuffer = IntBuffer.wrap(depthBitmapPixels);
        Bitmap bmp = Bitmap.createBitmap(mCameraRes[mSizeIndex].getWidth(), mCameraRes[mSizeIndex].getHeight(), Bitmap.Config.ARGB_8888);
        bmp.copyPixelsFromBuffer(finalBuffer);
        return bmp;
    }

    @Override
    public void setCaptureRequestParameters() {
        mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<Integer>(30, 30));
        mCaptureRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY);
        mCaptureRequestBuilder.set(CaptureRequest.LENS_FOCAL_LENGTH, mFocalLengths[0]);
        mCaptureRequestBuilder.set(CaptureRequest.DISTORTION_CORRECTION_MODE, CaptureRequest.DISTORTION_CORRECTION_MODE_HIGH_QUALITY);
    }

    public KinectCamera(Context context, int sizeIndex, int maxImages) {
        super(context, AbstractCamera.DEFAULT_FPS, sizeIndex, maxImages);
        try {
            for (String cameraID : mCameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraID);
                int[] capabilities = cameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                assert capabilities != null;
                boolean backFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK;
                boolean depthCamera = false;
                boolean logicalMultiCamera = false;
                for (int capability : capabilities) {
                    depthCamera = depthCamera || capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT;
                    logicalMultiCamera = logicalMultiCamera || capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA;
                }
                if (depthCamera && backFacing) {
                    mCameraID = cameraID;
                    mCameraRes = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.DEPTH16);
                    assert mCameraRes != null;
                    mFocalLengths = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                    mApertureSizes = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
                    mIntrinsicCameraParameters = cameraCharacteristics.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION);
                    mCameraActiveArraySize = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                    mImageReader = ImageReader.newInstance(mCameraRes[mSizeIndex].getWidth(), mCameraRes[mSizeIndex].getHeight(), ImageFormat.DEPTH16, mMaxImages);
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}

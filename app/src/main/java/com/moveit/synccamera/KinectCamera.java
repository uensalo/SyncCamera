package com.moveit.synccamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.media.ImageReader;
import android.util.Log;

import java.nio.ByteBuffer;
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
        ShortBuffer shortBuffer = buffer.asShortBuffer();
        short[] depthBitmapPixels = new short[mCameraRes[mSizeIndex].getWidth() * mCameraRes[mSizeIndex].getHeight()];
        for (int i = 0; i < mCameraRes[mSizeIndex].getWidth() * mCameraRes[mSizeIndex].getHeight(); i++) {
            short sample = shortBuffer.get(i);
            short depthRange = (short) (((sample & 0x1FFF) << 3) | sDeviceID);
            depthBitmapPixels[i] = depthRange;
        }
        ShortBuffer finalBuffer = ShortBuffer.wrap(depthBitmapPixels);
        Bitmap bmp = Bitmap.createBitmap(mCameraRes[mSizeIndex].getWidth(), mCameraRes[mSizeIndex].getHeight(), Bitmap.Config.RGB_565);
        bmp.copyPixelsFromBuffer(finalBuffer);
        return bmp;
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
                    mImageReader = ImageReader.newInstance(mCameraRes[mSizeIndex].getWidth(), mCameraRes[mSizeIndex].getHeight(), ImageFormat.DEPTH16, mMaxImages);
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}

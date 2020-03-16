package com.moveit.synccamera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.Context.CAMERA_SERVICE;

public class DepthCamera implements DepthBitmapProducer {

    private String TAG = "DepthCamera";
    private Context mContext;

    private String mDepthCameraID;
    private CameraDevice mDepthCamera;
    private Size[] mDepthCameraRes;

    private CameraManager mCameraManager;

    private CameraDevice.StateCallback mDepthStateCallback;
    private CaptureRequest.Builder mDepthRequestBuilder;

    private ImageReader mDepthImageReader;
    private ImageReader.OnImageAvailableListener mDepthImageAvailableListener;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private Bitmap mDepthBitmap;
    private int[] mDepthBitmapPixels;

    private static float sRangeMin = 0.0f;
    private static float sRangeMax = 1800.0f;

    private List<DepthBitmapObserver> mDepthBitmapObserverList;

    final public static int DEPTH_640_480 = 0;
    final public static int DEPTH_320_240 = 1;

    @Override
    public void broadcastDepthBitmap() {
        for(DepthBitmapObserver o : mDepthBitmapObserverList) {
            o.onDepthBitmapAvailable(mDepthBitmap);
        }
    }

    @Override
    public void addObserver(DepthBitmapObserver o) {
        mDepthBitmapObserverList.add(o);
    }

    public DepthCamera(Context context) {
        startBackgroundThread();
        mDepthBitmapObserverList = new ArrayList<>();

        mContext = context;
        mCameraManager = (CameraManager)mContext.getSystemService(CAMERA_SERVICE);
        try {
            for (String cameraID : mCameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraID);
                int[] capabilities = cameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                boolean backFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK;
                boolean depthCamera = false;
                boolean logicalMultiCamera = false;
                for(int capability: capabilities) {
                    depthCamera = depthCamera || capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT;
                    logicalMultiCamera = logicalMultiCamera || capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA;
                }
                if(depthCamera && backFacing) {
                    //Log.w(TAG, "Camera " + cameraID + " is a back facing depth camera");
                    //Log.w(TAG, "Setting mDepthCamera to " + cameraID);
                    mDepthCameraID = cameraID;
                    mDepthCameraRes = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.DEPTH16);
                    //for(Size res : mDepthCameraRes) {
                    //    Log.w(TAG, "Depth camera supports a resolution of " + res);
                    //}
                }
            }
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Background Thread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void killBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void setDepthCameraCallback(Range<Integer> fpsRange, final int sizeIndex, int maxImages) {
        if(mDepthImageReader == null) {
            mDepthImageReader = ImageReader.newInstance(mDepthCameraRes[sizeIndex].getWidth(), mDepthCameraRes[sizeIndex].getHeight(), ImageFormat.DEPTH16, maxImages);
        }
        mDepthBitmap = Bitmap.createBitmap(mDepthCameraRes[sizeIndex].getWidth(), mDepthCameraRes[sizeIndex].getHeight(), Bitmap.Config.ARGB_8888);
        mDepthBitmapPixels = new int[mDepthCameraRes[sizeIndex].getWidth() * mDepthCameraRes[sizeIndex].getHeight()];
        mDepthImageAvailableListener = new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(final ImageReader reader) {
                Image image = reader.acquireNextImage();
                final ShortBuffer buffer = image.getPlanes()[0].getBuffer().asShortBuffer();
                long start = System.currentTimeMillis();
                for(int i = 0; i < mDepthCameraRes[sizeIndex].getWidth() * mDepthCameraRes[sizeIndex].getHeight(); i++) {
                    short sample = buffer.get(i);
                    short depthRange = (short) (sample & 0x1FFF);
                    short depthConf  = (short) ((sample >> 13) & 0x7);
                    float depthPer = depthConf == 0 ? 1.f : (depthConf - 1) / 7.f;
                    float actualRange = (depthRange - sRangeMin) / (sRangeMax - sRangeMin);
                    //actual order is -> ABGR, because of bit level color bitmap speed hack
                    if(depthConf > 0.1) {
                        mDepthBitmapPixels[i] = Color.argb(1.0f ,0, 0, actualRange);
                    } else {
                        mDepthBitmapPixels[i] = Color.argb(1.0f, actualRange, 0, 0);
                    }
                }
                IntBuffer finalBuffer = IntBuffer.wrap(mDepthBitmapPixels);
                mDepthBitmap.copyPixelsFromBuffer(finalBuffer);
                long end = System.currentTimeMillis();
                //Log.w(TAG, "time: " + (end - start));
                image.close();
                broadcastDepthBitmap();
            }
        };
        mDepthImageReader.setOnImageAvailableListener(mDepthImageAvailableListener, null);
        final Range<Integer> fps = fpsRange;
        mDepthStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                try {
                    mDepthRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    mDepthRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0);
                    mDepthRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fps);
                    mDepthRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY);
                    mDepthRequestBuilder.addTarget(mDepthImageReader.getSurface());
                    camera.createCaptureSession(Arrays.asList(mDepthImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mDepthRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                            try {
                                session.setRepeatingRequest(mDepthRequestBuilder.build(), null, null);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                        }
                    }, null);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                /*
                if(mDepthCamera != null)
                    mDepthCamera.close();
                mDepthCamera = null;
                mDepthRequestBuilder = null;
                mDepthCameraID = null;
                mDepthCameraRes = null;
                mDepthStateCallback = null;
                 */
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {

            }
        };
    }

    public void openDepthCamera() {
        try {
            if(PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)) {
                //Log.w(TAG, "Attempting to open depth camera");
                mCameraManager.openCamera(mDepthCameraID, mDepthStateCallback, null);
            } else {
                Log.e(TAG, "Camera permission not granted.");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}

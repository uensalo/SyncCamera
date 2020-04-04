package com.moveit.synccamera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraCaptureSession;
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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.Context.CAMERA_SERVICE;

public abstract class AbstractCamera implements BitmapProducer {
    protected Context mContext;
    protected static String TAG = "AbstractCamera";
    protected static Range<Integer> DEFAULT_FPS = new Range<>(15, 60);

    protected String mCameraID;
    protected Size[] mCameraRes;
    protected CameraManager mCameraManager;

    protected Handler mHandler;
    protected HandlerThread mHandlerThread;

    protected Bitmap mBitmap;

    protected ImageReader mImageReader;
    protected ImageReader.OnImageAvailableListener mImageAvailableListener;

    protected CameraDevice.StateCallback mStateCallback;
    protected CaptureRequest.Builder mCaptureRequestBuilder;
    protected CameraDevice mCameraDevice;

    protected List<BitmapObserver> mBitmapObserverList;

    protected int mSizeIndex;
    protected int mMaxImages;

    protected Range<Integer> mFps;

    protected int mSensorFormat;

    public AbstractCamera(Context context, Range<Integer> fps, int sizeIndex, int maxImages) {
        mContext = context;
        mCameraManager = (CameraManager) mContext.getSystemService(CAMERA_SERVICE);
        mBitmapObserverList = new ArrayList<>();
        mSizeIndex = sizeIndex;
        mMaxImages = maxImages;
        mFps = fps;
        startBackgroundThread();
    }

    public String getCameraID() {
        return mCameraID;
    }

    public Size[] getCameraResolutions() {
        return Arrays.copyOf(mCameraRes, mCameraRes.length);
    }

    public CameraManager getCameraManager() {
        return mCameraManager;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public HandlerThread getHandlerThread() {
        return mHandlerThread;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public ImageReader getImageReader() {
        return mImageReader;
    }

    @Override
    public void attachObserver(BitmapObserver o) {
        mBitmapObserverList.add(o);
    }

    public void detachObserver(BitmapObserver o) {
        mBitmapObserverList.remove(o);
    }

    protected void startBackgroundThread() {
        mHandlerThread = new HandlerThread("Background Thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    protected void killBackgroundThread() {
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void openCamera() {
        try {
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)) {
                setCameraCallback();
                mCameraManager.openCamera(mCameraID, mStateCallback, null);
            } else {
                Log.e(TAG, "Camera permission not granted.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    abstract public Bitmap decodeSensorData(ByteBuffer buffer);

    private void setCameraCallback() {
        mImageAvailableListener = new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(final ImageReader reader) {
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                if (mBitmap != null) mBitmap.recycle();
                mBitmap = decodeSensorData(buffer);
                image.close();
                broadcastBitmap();
            }
        };
        mImageReader.setOnImageAvailableListener(mImageAvailableListener, null);
        final Range<Integer> fps = mFps;
        mStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                try {
                    mCaptureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0);
                    mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fps);
                    mCaptureRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY);
                    mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
                    camera.createCaptureSession(Arrays.asList(mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                            try {
                                session.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                        }
                    }, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                /*
                if(mColorCamera != null)
                    mColorCamera.close();
                mColorCamera = null;
                mColorRequestBuilder = null;
                mColorCameraID = null;
                mColorCameraRes = null;
                mColorStateCallback = null;
                 */
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {

            }
        };
    }
}

package com.moveit.synccamera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.Context.CAMERA_SERVICE;

public class ColorCamera implements ColorBitmapProducer {

    private String TAG = "ColorCamera";

    private Context mContext;
    private String mColorCameraID;

    private CameraDevice mColorCamera;
    private Size[] mColorCameraRes;
    private CameraManager mCameraManager;

    private CameraDevice.StateCallback mColorStateCallback;
    private CaptureRequest.Builder mColorRequestBuilder;

    private ImageReader mColorImageReader;
    private ImageReader.OnImageAvailableListener mColorImageAvailableListener;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private Bitmap mColorBitmap;
    //private int[] mColorBitmapPixels;

    private List<ColorBitmapObserver> mBitmapObserverList;

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
    public void broadcastColorBitmap() {
        for(ColorBitmapObserver o : mBitmapObserverList) {
            o.onColorBitmapAvailable(mColorBitmap);
        }
    }

    @Override
    public void addObserver(ColorBitmapObserver o) {
        mBitmapObserverList.add(o);
    }

    public ColorCamera(Context context) {
        startBackgroundThread();
        mBitmapObserverList = new ArrayList<>();

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
                if(!depthCamera && backFacing) {
                    //Log.w(TAG, "Camera " + cameraID + " is a back facing camera");
                    if(mColorCameraID == null) {
                        mColorCameraID = cameraID;
                        //Log.w(TAG, "Setting mColorCamera to " + cameraID);
                        mColorCameraRes = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
                        //for (Size res : mColorCameraRes) {
                        //  Log.w(TAG, "Color camera supports a resolution of " + res);
                        //}
                    }
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


    public void setColorCameraCallback(Range<Integer> fpsRange, final int sizeIndex, int maxImages) {
        if(mColorImageReader == null) {
            mColorImageReader = ImageReader.newInstance(mColorCameraRes[sizeIndex].getWidth(), mColorCameraRes[sizeIndex].getHeight(), ImageFormat.JPEG, maxImages);
        }
        mColorBitmap = Bitmap.createBitmap(mColorCameraRes[sizeIndex].getWidth() / 16, mColorCameraRes[sizeIndex].getHeight() / 16, Bitmap.Config.ARGB_8888);
        //mColorBitmapPixels = new int[mColorCameraRes[sizeIndex].getWidth() * mColorCameraRes[sizeIndex].getHeight()];
        mColorImageAvailableListener = new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(final ImageReader reader) {
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                for(int i = 0; i < buffer.remaining(); i++) {
                    bytes[i] = buffer.get(i);
                }
                mColorBitmap.recycle();
                mColorBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                image.close();
                broadcastColorBitmap();
            }
        };
        mColorImageReader.setOnImageAvailableListener(mColorImageAvailableListener, null);
        final Range<Integer> fps = fpsRange;
        mColorStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                try {
                    mColorRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    mColorRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0);
                    mColorRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fps);
                    mColorRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY);
                    mColorRequestBuilder.addTarget(mColorImageReader.getSurface());
                    camera.createCaptureSession(Arrays.asList(mColorImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mColorRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                            try {
                                session.setRepeatingRequest(mColorRequestBuilder.build(), null, null);
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

    public void openColorCamera() {
        try {
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)) {
                //Log.w(TAG, "Attempting to open color camera");
                mCameraManager.openCamera(mColorCameraID, mColorStateCallback, null);
            } else {
                Log.e(TAG, "Camera permission not granted.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package com.moveit.synccamera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;

public class MainActivity extends AppCompatActivity implements BitmapObserver, ProgressObserver {
    private ImageView mDepthImageView;
    private ImageView mColorImageView;

    private Button mCaptureButton;

    private String mAppPath;
    private String mVideoPath;
    private File mAppDir;
    private File mVideoDir;
    private int mCurrentVideoIndex;

    private RGBDCaptureContext mCaptureContext;

    private ColorCamera mColorCamera;
    private KinectCamera mKinectCamera;

    private SensorDataManager mSensorDataManager;

    private boolean saving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        saving = false;
        mAppPath = getExternalFilesDir(null).getAbsolutePath();
        mVideoPath = mAppPath + "/videos";
        File[] filesAppDir = (new File(mAppPath)).listFiles();
        boolean videosExists = false;
        if (filesAppDir != null) {
            for (File file : filesAppDir) {
                Log.w("APP", file.getPath());
                if (file.isDirectory() && file.getName().equals("videos")) {
                    videosExists = true;
                }
            }
        } else {
            return;
        }

        mAppDir = new File(mAppPath);
        mVideoDir = new File(mVideoPath);
        if (!videosExists) {
            Log.w("APP", "Videos directory not found, creating");
            Log.w("APP", "Directory Created: " + mVideoDir.mkdir());
        }
        File[] videoFiles = mVideoDir.listFiles();
        if (videoFiles == null) {
            mCurrentVideoIndex = 0;
        } else {
            int max = -1;
            for (File file : videoFiles) {
                int videoNo = Integer.parseInt(file.getName());
                if (videoNo > max) {
                    max = videoNo;
                }
            }
            mCurrentVideoIndex = max + 1;
        }

        mDepthImageView = findViewById(R.id.depthImageView);
        mColorImageView = findViewById(R.id.colorImageView);

        mCaptureButton = findViewById(R.id.captureButton);
        mCaptureButton.setText("Tap to start capturing");
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean captureState = mCaptureContext.captureState();
                if (captureState) {
                    mCaptureButton.setEnabled(false);
                    mCaptureContext.stopCapture();
                    saving = true;
                    Toast.makeText(getApplicationContext(), "Capture ended", Toast.LENGTH_SHORT);
                } else {
                    mCaptureContext.startCapture(mCurrentVideoIndex, mColorCamera.getCameraCalibration(), mKinectCamera.getCameraCalibration());
                    mCaptureButton.setText("Capturing, tap to save.");
                    Toast.makeText(getApplicationContext(), "Capture Started", Toast.LENGTH_SHORT);
                }
            }
        });

        mKinectCamera = new KinectCamera(this, DepthCamera.DEPTH_640_480, 2);
        mColorCamera = new ColorCamera(this, ColorCamera.RES_640x480, 2);

        mColorCamera.attachObserver(this);
        mColorCamera.openCamera();

        mKinectCamera.attachObserver(this);
        mKinectCamera.openCamera();

        mCaptureContext = new RGBDCaptureContext(mVideoPath, Bitmap.CompressFormat.PNG, Bitmap.CompressFormat.PNG, 50, this);
        mCaptureContext.addObserver(this);

        mSensorDataManager = new SensorDataManager(this);
    }

    @Override
    public void onBitmapAvailable(Bitmap bitmap, BitmapBroadcastContext context) {
        if (context.getDataType() == BitmapDataType.DEPTH || context.getDataType() == BitmapDataType.DEPTH_KINECT) {
            if (saving) {
                String colorProgress = (int) (mCaptureContext.colorEncodingProgress() * 100) + "%";
                String depthProgress = (int) (mCaptureContext.depthEncodingProgress() * 100) + "%";
                mCaptureButton.setText("Depth: " + depthProgress + ", Color:" + colorProgress);
            }
            mDepthImageView.setImageBitmap(bitmap);
            mCaptureContext.insertFrame(((BitmapDrawable) mColorImageView.getDrawable()).getBitmap(), ((BitmapDrawable) mDepthImageView.getDrawable()).getBitmap());
        } else if (context.getDataType() == BitmapDataType.COLOR) {
            mColorImageView.setImageBitmap(bitmap);
            //Log.w("CAMPARAM", "" + listToString((context.getPoseReference())));
        }
    }

    @Override
    public void onComplete(int jobID) {
        saving = false;
        mCurrentVideoIndex++;
        mCaptureButton.setEnabled(true);
        mCaptureButton.setText("Tap to start capturing");
    }

    public String listToString(float[] fList) {
        String str = "";
        for (float f : fList) {
            str = str + f + " ";
        }
        return str;
    }

}

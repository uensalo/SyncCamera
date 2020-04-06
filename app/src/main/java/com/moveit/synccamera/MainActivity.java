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

public class MainActivity extends AppCompatActivity implements BitmapObserver {
    /*
        This is the actual app which queries cameras for sensor information and then renders the bitmaps
        on to the screen of the mobile device. Usage is simple: The app implements BitmapObserver, then
        it attaches itself to the logical camera models. onBitmapAvailable() is executed whenever a
        sensor notifies its observers, in this case the app itself. The app then instructs the view to
        update itself according to the broadcast bitmap.

        TODO: It is now possible to save an image to the storage using the following utilities:
            - upon receiving a bitmap, request an instance of the BitmapEncoder
            - encode the bitmap into an image file (.jpeg recommended)
            - create a BitmapMetadata Object, and set the relevant fields (camera parameters, etc.)
              (path of the file on the device should be returned from the encode method of the
               BitmapEncoder, required by the constructor of the BitmapMetadata object)
            - call saveInstance() on the BitmapMetadata to attach the metadata to the image file.
     */

    private ImageView mDepthImageView;
    private ImageView mColorImageView;

    private Button mCaptureButton;

    private String mAppPath;
    private String mVideoPath;
    private File mAppDir;
    private File mVideoDir;
    private int mCurrentVideoIndex;

    private RGBDCaptureContext mCaptureContext;

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
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean captureState = mCaptureContext.captureState();
                if (captureState) {
                    mCaptureContext.stopCapture();
                    Toast.makeText(getApplicationContext(), "Capture Ended", Toast.LENGTH_SHORT);
                } else {
                    mCaptureContext.startCapture();
                    Toast.makeText(getApplicationContext(), "Capture Started", Toast.LENGTH_SHORT);
                }
            }
        });

        KinectCamera kinectCamera = new KinectCamera(this, DepthCamera.DEPTH_640_480, 10);
        ColorCamera colorCamera = new ColorCamera(this, ColorCamera.RES_640x480, 10);

        colorCamera.attachObserver(this);
        colorCamera.openCamera();

        kinectCamera.attachObserver(this);
        kinectCamera.openCamera();

        mCaptureContext = new RGBDCaptureContext(mVideoPath, mCurrentVideoIndex, Bitmap.CompressFormat.PNG, Bitmap.CompressFormat.PNG, 50);
    }

    @Override
    public void onBitmapAvailable(Bitmap bitmap, BitmapBroadcastContext context) {
        if (context.getDataType() == BitmapDataType.DEPTH || context.getDataType() == BitmapDataType.DEPTH_KINECT) {
            mDepthImageView.setImageBitmap(bitmap);
            mCaptureContext.insertFrame(((BitmapDrawable) mColorImageView.getDrawable()).getBitmap(), ((BitmapDrawable) mDepthImageView.getDrawable()).getBitmap());
        } else if (context.getDataType() == BitmapDataType.COLOR) {
            mColorImageView.setImageBitmap(bitmap);
        }
    }
}

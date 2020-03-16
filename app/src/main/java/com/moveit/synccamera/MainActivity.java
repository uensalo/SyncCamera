package com.moveit.synccamera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Range;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity implements DepthBitmapObserver, ColorBitmapObserver {
    private ImageView mDepthImageView;
    private ImageView mColorImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDepthImageView = findViewById(R.id.depthImageView);
        mColorImageView = findViewById(R.id.colorImageView);

        DepthCamera depthCamera = new DepthCamera(this);
        ColorCamera colorCamera = new ColorCamera(this);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
        }
        colorCamera.addObserver(this);
        colorCamera.setColorCameraCallback(new Range<Integer>(15,60), ColorCamera.RES_WIDE_640x480, 10);
        colorCamera.openColorCamera();

        depthCamera.addObserver(this);
        depthCamera.setDepthCameraCallback(new Range<Integer>(15,60), DepthCamera.DEPTH_640_480, 10);
        depthCamera.openDepthCamera();
    }

    @Override
    public void onDepthBitmapAvailable(Bitmap bitmap) {
        mDepthImageView.setImageBitmap(bitmap);
    }

    @Override
    public void onColorBitmapAvailable(Bitmap bitmap) {
        mColorImageView.setImageBitmap(bitmap);
    }
}

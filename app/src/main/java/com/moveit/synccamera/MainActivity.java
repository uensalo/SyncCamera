package com.moveit.synccamera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity implements BitmapObserver {
    private ImageView mDepthImageView;
    private ImageView mColorImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
        }

        mDepthImageView = findViewById(R.id.depthImageView);
        mColorImageView = findViewById(R.id.colorImageView);

        DepthCamera depthCamera = new DepthCamera(this, DepthCamera.DEPTH_640_480, 10);
        ColorCamera colorCamera = new ColorCamera(this, ColorCamera.RES_640x480, 10);

        colorCamera.attachObserver(this);
        colorCamera.openCamera();

        depthCamera.attachObserver(this);
        depthCamera.openCamera();
    }

    @Override
    public void onBitmapAvailable(Bitmap bitmap, BitmapBroadcastContext context) {
        if (context.getDataType() == BitmapDataType.DEPTH) {
            mDepthImageView.setImageBitmap(bitmap);
        } else if (context.getDataType() == BitmapDataType.COLOR) {
            mColorImageView.setImageBitmap(bitmap);
        }
    }
}

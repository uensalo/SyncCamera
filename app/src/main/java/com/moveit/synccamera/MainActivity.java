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
        if (context.getDataType() == BitmapDataType.DEPTH || context.getDataType() == BitmapDataType.DEPTH_KINECT) {
            mDepthImageView.setImageBitmap(bitmap);
        } else if (context.getDataType() == BitmapDataType.COLOR) {
            mColorImageView.setImageBitmap(bitmap);
        }
    }
}

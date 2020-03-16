package com.moveit.synccamera;

import android.graphics.Bitmap;

public interface DepthBitmapObserver {
    void onDepthBitmapAvailable(Bitmap bitmap);
}

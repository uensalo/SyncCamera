package com.moveit.synccamera;

import android.graphics.Bitmap;

public interface BitmapObserver {
    void onBitmapAvailable(Bitmap bitmap, BitmapBroadcastContext context);
}

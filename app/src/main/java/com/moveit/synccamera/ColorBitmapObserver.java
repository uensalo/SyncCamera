package com.moveit.synccamera;

import android.graphics.Bitmap;

public interface ColorBitmapObserver {
    void onColorBitmapAvailable(Bitmap bitmap);
}

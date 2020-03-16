package com.moveit.synccamera;

public interface ColorBitmapProducer {
    void broadcastColorBitmap();
    void addObserver(ColorBitmapObserver o);
}

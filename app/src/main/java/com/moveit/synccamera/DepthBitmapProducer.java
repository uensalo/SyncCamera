package com.moveit.synccamera;

public interface DepthBitmapProducer {
    void broadcastDepthBitmap();
    void addObserver(DepthBitmapObserver o);
}

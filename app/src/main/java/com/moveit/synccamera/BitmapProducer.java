package com.moveit.synccamera;

public interface BitmapProducer {
    void broadcastBitmap();

    void attachObserver(BitmapObserver o);

    void detachObserver(BitmapObserver o);
}

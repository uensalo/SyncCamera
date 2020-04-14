package com.moveit.synccamera;

import android.app.Activity;
import android.graphics.Bitmap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BitmapEncoder {
    private static BitmapEncoder sInstance;

    private static Bitmap.CompressFormat sCompressFormat;
    private static int sCompressionFactor;

    private static int sLastID;

    private static Map<Integer, BitmapEncodeTask> sWorkers;
    private static BlockingQueue<Runnable> sWorkQueue;

    private static Activity sActivity;

    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final int KEEP_ALIVE_TIME = 2;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    private static ThreadPoolExecutor sThreadPool;

    private BitmapEncoder() {
    }

    public static synchronized BitmapEncoder instance() {
        if (sInstance == null) {
            sInstance = new BitmapEncoder();
            sWorkers = new HashMap<>();
            sWorkQueue = new LinkedBlockingQueue<>();
            sThreadPool = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, sWorkQueue);
            sLastID = 0;
        }
        return sInstance;
    }

    public synchronized BitmapEncoder setCompressFormat(Bitmap.CompressFormat format) {
        sCompressFormat = format;
        return sInstance;
    }

    public synchronized BitmapEncoder setCompressionFactor(int factor) {
        assert factor <= 100;
        assert factor >= 0;
        sCompressionFactor = factor;
        return sInstance;
    }

    public synchronized BitmapEncoder setActivity(Activity activity) {
        assert activity != null;
        sActivity = activity;
        return sInstance;
    }

    public synchronized int encode(List<Bitmap> bitmaps, String filePath, int startIdx, int endIdx, ProgressObserver observer) {
        BitmapEncodeTask worker = new BitmapEncodeTask(sLastID, bitmaps, filePath, sCompressFormat, sCompressionFactor, startIdx, endIdx, sActivity);
        sWorkers.put(sLastID, worker);
        worker.addObserver(observer);
        sThreadPool.execute(worker);
        sLastID++;
        return sLastID - 1;
    }

    public synchronized float progress(int id) {
        return sWorkers.get(id) == null ? -1 : sWorkers.get(id).progress();
    }

    public synchronized int size(int id) {
        return sWorkers.get(id) == null ? -1 : sWorkers.get(id).size();
    }

    public synchronized int bitmapsEncoded(int id) {
        return sWorkers.get(id) == null ? -1 : sWorkers.get(id).bitmapsEncoded();
    }

    public synchronized void addObserverToWorker(int jobID, ProgressObserver o) {
        sWorkers.get(jobID).addObserver(o);
    }

}

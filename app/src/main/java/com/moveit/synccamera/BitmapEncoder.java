package com.moveit.synccamera;

import android.graphics.Bitmap;

import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapEncoder {
    private BitmapEncoder mInstance;

    private static Bitmap.CompressFormat mCompressFormat;
    private static int mCompressionFactor;

    private BitmapEncoder() {
    }

    public BitmapEncoder instance() {
        if (mInstance == null) {
            mInstance = new BitmapEncoder();
        }
        return mInstance;
    }

    public synchronized BitmapEncoder setCompressFormat(Bitmap.CompressFormat format) {
        mCompressFormat = format;
        return mInstance;
    }

    public synchronized BitmapEncoder setCompressionFactor(int factor) {
        assert factor <= 100;
        assert factor >= 0;
        mCompressionFactor = factor;
        return mInstance;
    }

    public static synchronized String encode(Bitmap bmp, String fileName) {
        try (FileOutputStream out = new FileOutputStream(fileName)) {
            bmp.compress(mCompressFormat, mCompressionFactor, out);
            return fileName;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

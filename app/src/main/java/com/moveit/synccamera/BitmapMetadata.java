package com.moveit.synccamera;

import android.media.ExifInterface;

import java.io.IOException;

public class BitmapMetadata {
    private ExifInterface mExif;
    private String mAbsolutePath;

    public static String ORIENTATION = ExifInterface.TAG_ORIENTATION;

    public static String DATETIME = ExifInterface.TAG_DATETIME;
    public static String DATETIME_ORIGINAL = ExifInterface.TAG_DATETIME_ORIGINAL;
    public static String DATETIME_DIGITIZED = ExifInterface.TAG_DATETIME_DIGITIZED;

    public static String GPS_TIMESTAMP = ExifInterface.TAG_GPS_TIMESTAMP;
    public static String BITS_PER_SAMPLE = ExifInterface.TAG_BITS_PER_SAMPLE;

    public static String FOCAL_LENGTH = ExifInterface.TAG_FOCAL_LENGTH;
    public static String IMAGE_WIDTH = ExifInterface.TAG_IMAGE_WIDTH;
    public static String IMAGE_HEIGHT = ExifInterface.TAG_IMAGE_LENGTH; // called ImageLength by the EXIF specification
    public static String FOCAL_PLANE_X_RESOLUTION = ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION;
    public static String FOCAL_PLANE_Y_RESOLUTION = ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION;
    public static String FOCAL_PLANE_RESOLUTION_UNIT = ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT;
    public static String FOCAL_LENGTH_IN_35MM_FILM = ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM;

    public static String IMAGE_UNIQUE_ID = ExifInterface.TAG_IMAGE_UNIQUE_ID;

    public BitmapMetadata(String imagePath) {
        mAbsolutePath = imagePath;
    }

    public void setAttribute(String attribute, String value) {
        try {
            if (mExif == null) {
                mExif = new ExifInterface(mAbsolutePath);
            }
            mExif.setAttribute(attribute, value);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAttribute() {
        try {
            if (mExif != null) {
                mExif.saveAttributes();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

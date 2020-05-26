package com.moveit.synccamera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RGBDCaptureContext implements ProgressObserver, ProgressObservable {
    private int mVideoIndex;
    private boolean mCaptureState;

    private String mVideosPath;
    private File mVideoDirectory;
    private File mContextRGBDirectory;
    private File mContextDepthDirectory;
    private File mContextMetadataDirectory;

    private List<Bitmap> mColorBitmaps;
    private List<Bitmap> mDepthBitmaps;

    private Bitmap.CompressFormat mColorCompressionFormat;
    private Bitmap.CompressFormat mDepthCompressionFormat;
    private int mCompressionRatio;

    private int[] mColorJobsID;
    private int[] mDepthJobsID;

    private volatile boolean[] mColorJobStatus;
    private volatile boolean[] mDepthJobStatus;

    private List<ProgressObserver> mObservers;

    private Activity mActivity;

    private CameraCalibrationMetadata mColorCameraMetadata;
    private CameraCalibrationMetadata mDepthCameraMetadata;

    public RGBDCaptureContext(String videosPath, Bitmap.CompressFormat colorCompressionFormat, Bitmap.CompressFormat depthCompressionFormat, int compressionRatio, Activity activity) {
        mCaptureState = false;
        mVideosPath = videosPath;
        mVideoDirectory = null;
        mContextRGBDirectory = null;
        mContextDepthDirectory = null;

        mColorBitmaps = new ArrayList<>();
        mDepthBitmaps = new ArrayList<>();

        mColorCompressionFormat = colorCompressionFormat;
        mDepthCompressionFormat = depthCompressionFormat;
        mCompressionRatio = compressionRatio;

        mActivity = activity;

        mObservers = new ArrayList<>();
    }

    @Override
    public void addObserver(ProgressObserver o) {
        mObservers.add(o);
    }

    public boolean captureState() {
        return mCaptureState;
    }

    public void startCapture(int videoIndex, CameraCalibrationMetadata colorCameraMetadata, CameraCalibrationMetadata depthCameraMetadata) {
        mVideoIndex = videoIndex;
        mCaptureState = true;
        mVideoDirectory = new File(mVideosPath + "/" + mVideoIndex);
        mContextRGBDirectory = new File(mVideosPath + "/" + mVideoIndex + "/rgb");
        mContextDepthDirectory = new File(mVideosPath + "/" + mVideoIndex + "/depth");
        mContextMetadataDirectory = new File(mVideosPath + "/" + mVideoIndex + "/metadata");
        mColorCameraMetadata = colorCameraMetadata;
        mDepthCameraMetadata = depthCameraMetadata;


        mVideoDirectory.mkdir();
        mContextRGBDirectory.mkdir();
        mContextDepthDirectory.mkdir();
        mContextMetadataDirectory.mkdir();
    }

    public void insertFrame(Bitmap colorBitmap, Bitmap depthBitmap) {
        if (mCaptureState) {
            mColorBitmaps.add(colorBitmap.copy(null, false));
            mDepthBitmaps.add(depthBitmap.copy(null, false));
        }
    }

    //TODO: Write relevant metadata to the image files, not needed at the moment
    public void stopCapture() {
        mCaptureState = false;
        BitmapEncoder encoder = BitmapEncoder.instance();

        int noColorJobs = Math.max((int) Math.log10(mColorBitmaps.size()), 1);
        int noDepthJobs = Math.max((int) Math.log10(mDepthBitmaps.size()), 1);

        mColorJobStatus = new boolean[noColorJobs];
        mDepthJobStatus = new boolean[noDepthJobs];

        mColorJobsID = new int[noColorJobs];
        mDepthJobsID = new int[noDepthJobs];

        int idx = 0;
        int idxIncrement = mColorBitmaps.size() / noColorJobs;

        for (int i = 0; i < noColorJobs; i++) {
            int endIdx = Math.min(idx + idxIncrement, mColorBitmaps.size() - 1);
            encoder.setCompressionFactor(mCompressionRatio);
            encoder.setCompressFormat(mColorCompressionFormat);
            encoder.setActivity(mActivity);
            mColorJobsID[i] = encoder.encode(mColorBitmaps, mContextRGBDirectory.getPath(), idx, endIdx, this);

            encoder.setCompressionFactor(mCompressionRatio);
            encoder.setCompressFormat(mDepthCompressionFormat);
            encoder.setActivity(mActivity);
            mDepthJobsID[i] = encoder.encode(mDepthBitmaps, mContextDepthDirectory.getPath(), idx, endIdx, this);

            idx = idx + idxIncrement + 1;
        }
        Arrays.sort(mColorJobsID);
        Arrays.sort(mDepthJobsID);

    }


    @Override
    public void onComplete(int jobID) {
        int colorIdx = Arrays.binarySearch(mColorJobsID, jobID);
        int depthIdx = Arrays.binarySearch(mDepthJobsID, jobID);

        if (colorIdx >= 0) {
            mColorJobStatus[colorIdx] = true;
        }

        if (depthIdx >= 0) {
            mDepthJobStatus[depthIdx] = true;
        }

        boolean colorComplete = true;
        boolean depthComplete = true;

        for (int i = 0; i < mColorJobStatus.length; i++) {
            colorComplete = colorComplete && mColorJobStatus[i];
            depthComplete = depthComplete && mDepthJobStatus[i];
        }

        Log.w("CONTEXT", "COLOR COMPLETE: " + colorComplete);
        Log.w("CONTEXT", "DEPTH COMPLETE: " + colorComplete);

        if (colorComplete && depthComplete) {

            for (int i : mDepthJobsID) {
                Log.w("DPROGRESS", "" + BitmapEncoder.instance().progress(i));
            }

            for (int i : mDepthJobsID) {
                Log.w("CPROGRESS", "" + BitmapEncoder.instance().progress(i));
            }

            notifyObservers();
            mColorJobStatus = null;
            mDepthJobStatus = null;
            mColorJobsID = null;
            mDepthJobsID = null;
            for (Bitmap bmp : mColorBitmaps) {
                if (!bmp.isRecycled())
                    bmp.recycle();
            }
            mColorBitmaps = new ArrayList<>();

            for (Bitmap bmp : mDepthBitmaps) {
                if (!bmp.isRecycled())
                    bmp.recycle();
            }
            mDepthBitmaps = new ArrayList<>();

            File colorMetadata = new File(mContextMetadataDirectory.getPath() + "/", "color.txt");
            File depthMetadata = new File(mContextMetadataDirectory.getPath() + "/", "depth.txt");

            try (FileOutputStream stream = new FileOutputStream(colorMetadata)) {
                stream.write("Field Name|Schema|Units: Intrinsics|[fx, fy, cx, cy, s]|Pixels; Rotation|[x y z w]|Quaternion; Translation|[x y z]|mm; Distortion|[k1 k2 k3]|-; Reference|[Ref]|-; Pixel Array|[wxh]|Pixels; Sensor Size|[wxh]|mm;".getBytes());
                stream.write(System.getProperty("line.separator").getBytes());
                stream.write(Arrays.toString(mColorCameraMetadata.getIntrinsicCalibration()).getBytes());
                stream.write(System.getProperty("line.separator").getBytes());
                stream.write(Arrays.toString(mColorCameraMetadata.getExtrinsicRotation()).getBytes());
                stream.write(System.getProperty("line.separator").getBytes());
                stream.write(Arrays.toString(mColorCameraMetadata.getExtrinsicTranslation()).getBytes());
                stream.write(System.getProperty("line.separator").getBytes());
                stream.write(Arrays.toString(mColorCameraMetadata.getLensDistortion()).getBytes());
                stream.write(System.getProperty("line.separator").getBytes());
                stream.write((mColorCameraMetadata.getPoseReference() + "").getBytes());
                stream.write(System.getProperty("line.separator").getBytes());
                stream.write(mColorCameraMetadata.getPixelArraySize().toString().getBytes());
                stream.write(System.getProperty("line.separator").getBytes());
                stream.write(mColorCameraMetadata.getPhysicalSize().toString().getBytes());
                stream.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try (FileOutputStream stream = new FileOutputStream(depthMetadata)) {
                stream.write("Field Name|Schema|Units: Intrinsics|[fx, fy, cx, cy, s]|Pixels; Rotation|[x y z w]|Quaternion; Translation|[x y z]|mm; Distortion|[k1 k2 k3]|-; Reference|[Ref]|-; Pixel Array|[wxh]|Pixels; Sensor Size|[wxh]|mm;".getBytes());
                stream.write(System.getProperty("line.separator").getBytes());
                stream.write(Arrays.toString(mDepthCameraMetadata.getIntrinsicCalibration()).getBytes());
                stream.write(System.getProperty("line.separator").getBytes());
                stream.write(Arrays.toString(mDepthCameraMetadata.getExtrinsicRotation()).getBytes());
                stream.write(System.getProperty("line.separator").getBytes());
                stream.write(Arrays.toString(mDepthCameraMetadata.getExtrinsicTranslation()).getBytes());
                stream.write(System.getProperty("line.separator").getBytes());
                stream.write(Arrays.toString(mDepthCameraMetadata.getLensDistortion()).getBytes());
                stream.write(System.getProperty("line.separator").getBytes());
                stream.write((mDepthCameraMetadata.getPoseReference() + "").getBytes());
                stream.write(System.getProperty("line.separator").getBytes());
                stream.write(mDepthCameraMetadata.getPixelArraySize().toString().getBytes());
                stream.write(System.getProperty("line.separator").getBytes());
                stream.write(mDepthCameraMetadata.getPhysicalSize().toString().getBytes());
                stream.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void notifyObservers() {
        for (ProgressObserver o : mObservers) {
            o.onComplete(-1);
        }
    }

    public float depthEncodingProgress() {
        if (mDepthJobStatus == null) {
            return -1.0f;
        }
        BitmapEncoder encoder = BitmapEncoder.instance();
        float totalProgress = 0;
        for (int i = 0; i < mDepthJobsID.length; i++) {
            totalProgress += encoder.bitmapsEncoded(mDepthJobsID[i]);
        }
        return totalProgress / mDepthBitmaps.size();
    }

    public float colorEncodingProgress() {
        if (mColorJobStatus == null) {
            return -1.0f;
        }
        BitmapEncoder encoder = BitmapEncoder.instance();
        float totalProgress = 0;
        for (int i = 0; i < mColorJobsID.length; i++) {
            totalProgress += encoder.bitmapsEncoded(mColorJobsID[i]);
        }
        return totalProgress / mColorBitmaps.size();

    }
}

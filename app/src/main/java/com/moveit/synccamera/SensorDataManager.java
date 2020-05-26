package com.moveit.synccamera;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class SensorDataManager implements SensorEventListener {
    private String TAG = "SensorDataManager";

    private SensorManager mSensorManager;
    private Sensor mRotationSensor;
    private Sensor mAccelerationSensor;
    private Context mContext;
    private float[] mEulerRotation;
    private float[] mPosition;
    private float[] mVelocity;
    private float[] mAcceleration;
    private float[] mPrevVelocity;
    private long mAccelerationTimeStamp;

    public SensorDataManager(Context context) {
        mAccelerationTimeStamp = -1;
        mContext = context;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mAcceleration = new float[3];
        mPosition = new float[3];
        mVelocity = new float[3];
        mPrevVelocity = new float[3];
        assert mSensorManager != null;
        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mAccelerationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorManager.registerListener(this, mRotationSensor, mSensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mAccelerationSensor, mSensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.equals(mRotationSensor)) {
            mEulerRotation = event.values;
        } else if (event.sensor.equals(mAccelerationSensor)) {
            if (mAccelerationTimeStamp < 0.0f) {
                mAccelerationTimeStamp = event.timestamp;
            } else {
                double dt = (event.timestamp - mAccelerationTimeStamp) / 1000000000.0;
                mAccelerationTimeStamp = event.timestamp;

                mAcceleration[0] = event.values[0];
                mAcceleration[1] = event.values[1];
                mAcceleration[2] = event.values[2];

                mVelocity[0] += mAcceleration[0] * dt;
                mVelocity[1] += mAcceleration[1] * dt;
                mVelocity[2] += mAcceleration[2] * dt;
                mPosition[0] += mPrevVelocity[0] + mVelocity[0] * dt;
                mPosition[1] += mPrevVelocity[1] + mVelocity[1] * dt;
                mPosition[2] += mPrevVelocity[2] + mVelocity[2] * dt;

                mPrevVelocity[0] = mVelocity[0];
                mPrevVelocity[1] = mVelocity[1];
                mPrevVelocity[2] = mVelocity[2];

                Log.w(TAG, "x: " + mPosition[0] + " y: " + mPosition[1] + " z: " + mPosition[2]);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

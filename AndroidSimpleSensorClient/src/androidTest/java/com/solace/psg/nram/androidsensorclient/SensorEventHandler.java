package com.solace.psg.nram.androidsensorclient;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import java.sql.Timestamp;

/**
 * Created by nram on 4/3/17.
 */

//------- UNUSED ----

public class SensorEventHandler implements SensorEventListener {
        float mValp = -1 ;
    private SensorManager mSensorManager;
    private Sensor mPressure;
    private float mLightMax;

    /*
    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);

        // Get an instance of the sensor service, and use that to get an instance of
        // a particular sensor.
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
    }

    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();
        mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    */
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float val = event.values[0];
            //Log.d(TAG, "Got value " + val + " from sensor " + event.sensor.getType() + " valp " + mValp);
            if(event.sensor.getType() == Sensor.TYPE_LIGHT){
                int valp = (int)(100*val / mLightMax) ;
                //Log.d(TAG, "Max val: " + mLightMax + " valperc : " + valp);
                if (Math.abs(mValp - valp) < 1) {
                    mValp = valp ;
                    return;
                }
                //Log.i(TAG, "New lux perc val: " + valp);
                mValp = valp ;
                Timestamp ts = new Timestamp(System.currentTimeMillis());
                //String s = ts + ":" + handle + ":" + id + ":" + valp;
                //if (mqttConnection != null) {
                 //   mqttConnection.SendSensorData("LIGHT", s);
                //}
            }

        }

    };

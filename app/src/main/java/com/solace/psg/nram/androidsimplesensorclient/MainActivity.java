package com.solace.psg.nram.androidsimplesensorclient;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.sql.Timestamp;
import java.util.List;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.Thing;

import org.eclipse.paho.client.mqttv3.MqttException;

public class MainActivity extends AppCompatActivity {

    static String TAG = "MainActivity";
    
    // Android Session vars
    SessionInfo si = null;
    Context ctx;

    // Connection vars
    MqttConnection mqttConnection = null;

    // sensor vars
    private SensorManager mSensorManager;
    private Sensor mLight;
    private float mLightMax ;
    private Sensor mPressure;
    private float mPressureMax ;
    private Sensor mProximity;
    private float mProximityMax ;


    // location (GPS) vars
    private LocationManager locationManager;
    private LocationListener locationListener;

    // App state vars
    String name;
    String id;
    

    private ResponseReceiver receiver; // intent response receiver


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "AndriodSimpleSensorClient Starting!");

        // Initialize all session info
        si = new SessionInfo();
        si.ctx = this.getApplicationContext();
        si.textName = (TextView) findViewById(R.id.textName);
        si.textID = (TextView) findViewById(R.id.textID);
        si.textUrl = (TextView) findViewById(R.id.textURL);
        si.textClientname = (TextView) findViewById(R.id.textClientname);
        si.textClientPassword = (TextView) findViewById(R.id.textClientPassword);
        si.textStatus = (TextView) findViewById(R.id.textStatus);
        si.textInfo = (TextView) findViewById(R.id.textInfo);
        si.textInfo.setMovementMethod(new ScrollingMovementMethod());


        // ----------------------------------------------------------------------------------------
        // Setup button initial states
        //
        si.btnConnect = (Button) (findViewById(R.id.btnConnect));
        si.btnConnect.setEnabled(true);

        si.btnQuit = (Button) findViewById(R.id.btnQuit);
        si.btnQuit.setEnabled(false);

        // ----------------------------------------------------------------------------------------
        // Register receiver for intentService response

        IntentFilter filter = new IntentFilter(ResponseReceiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new ResponseReceiver(si);
        registerReceiver(receiver, filter);

        // ----------------------------------------------------------------------------------------
        // Register (connect) with MQTT broker
        //
        si.btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                name = ((EditText) findViewById(R.id.inID)).getText().toString().replaceAll("\\s", "");
                id = ((EditText) findViewById(R.id.inID)).getText().toString().replaceAll("\\s", "");
                String passwd = "UNUSED";

                Log.d(TAG, "Name : " + name + " ID: " + id);
                if ((name.isEmpty()) ||
                        (id.isEmpty()) ||
                        (passwd.isEmpty())) {
                    si.textStatus.setText("Invalid Name, Email or Password" + "\n");
                    return;
                }

                try {
                    mqttConnection = new MqttConnection(si, name, id, passwd);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Timestamp ts = new Timestamp(System.currentTimeMillis());
                String s = ts + ":" + name + ":" + id;
                Log.i(TAG, "\n" + s + " registration");

                //mqttConnection.SendOut("Its show time");
                si.btnConnect.setEnabled(false);
                si.btnQuit.setEnabled(true);

                if (mqttConnection != null) {
                    mqttConnection.SendRegister(s);
                    sensorInfo();
                    configureButton();
                }
            }
        });

        // ----------------------------------------------------------------------------------------
        // Get GPS Coordinates
        //
        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);


        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Timestamp ts = new Timestamp(System.currentTimeMillis());
                String s = ts + ":" + name + ":" + id + ":" +
                        location.getLatitude() + ":" + location.getLongitude();
                Log.i(TAG, s + "  location changed");
                //si.textInfo.setVisibility(View.VISIBLE);
                //si.textInfo.append("\n" + s);
                if (mqttConnection != null) {
                    mqttConnection.SendLocation(s);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.i(TAG, "GPS STATUS CHANGED :");

            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.i(TAG, "GPS PROVIDER ENABLED :");

            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.i(TAG, "GPS PROVIDER DISABLED :");
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);

            }
        };


        // ----------------------------------------------------------------------------------------
        // Quit Button
        //
        si.btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Timestamp ts = new Timestamp(System.currentTimeMillis());
                String s = ts + ":" + name + ":" + id;
                Log.i(TAG, s + " Client exit by user");

                // unregister for inter service responses
                Log.i(TAG,"Unregister responses");
                unregisterReceiver(receiver);

                // disconnect mqtt connection
                if (mqttConnection != null) {
                    Log.i(TAG, "Disconnet from MQTT broker");
                    mqttConnection.SendUnregister(s);
                    try {
                        mqttConnection.Close();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
                //AppIndex.AppIndexApi.end(client, getIndexApiAction());
                //client.disconnect();
                Log.i(TAG, "goodbye!");
                finish();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.INTERNET
                }, 10);
                return;
            }
        } else {
            configureButton();
        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        //client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    // ----------------------------------------------------------------------------------------
    // Sensor Info -- Get list of sensors when "Sensor Info" button clicked -- tsting
    //

    public void sensorInfo() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> mList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        Log.i(TAG, "SENSOR INFO: ");
        //si.textInfo.setText("SENSOR INFO" + "\n");
        for (int i = 1; i < mList.size(); i++) {
            //si.textInfo.setVisibility(View.VISIBLE);
            String s = mList.get(i).getName() + " - " + mList.get(i).getVendor() + " : " + mList.get(i).getVersion();
            Log.i(TAG, "   " + s);
            //si.textInfo.append("\n" + s);
            if (mqttConnection != null) {
                mqttConnection.SendSensorInfo(s);
            }
        }

        //mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        String s = ts + ":" + name + ":" + id;
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) == null){
            Log.i(TAG, "Light sensor Not found");
            mqttConnection.SendSensorData("LIGHT/STATUS", s + ":NA");
        }
        else {
            Log.i(TAG, "Registering light sensor");
            mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            mLightMax = mLight.getMaximumRange();
            ts = new Timestamp(System.currentTimeMillis());
            s = ts + ":" + name + ":" + id;

            if (mLight != null) {
                mSensorManager.registerListener(SimpleSensorListener, mLight, SensorManager.SENSOR_DELAY_NORMAL);
                Log.i(TAG, "Light sensor registered");


                if (mSensorManager != null) {
                    mqttConnection.SendSensorData("LIGHT/STATUS", s + ":OK");
                }
            } else {
                Log.e(TAG, "Light sensor FAILED to register");
                if (mSensorManager != null) {
                    mqttConnection.SendSensorData("LIGHT/STATUS", s + ":ERR");
                }
            }
        }


        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) == null){
            Log.i(TAG, "Pressure sensor Not found");
            mqttConnection.SendSensorData("PRESSURE/STATUS", s+":NA");
        }
        else {
            Log.i(TAG, "Registering Pressure sensor");
            mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
            mPressureMax = mPressure.getMaximumRange();
            if (mPressure != null) {
                mSensorManager.registerListener(SimpleSensorListener, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
                Log.i(TAG, "Pressure sensor registered");


                if (mSensorManager != null) {
                    mqttConnection.SendSensorData("PRESSURE/STATUS", s + ":OK");
                }
            } else {
                Log.e(TAG, "Pressure sensor FAILED to register");
                if (mSensorManager != null) {
                    mqttConnection.SendSensorData("PRESSURE/STATUS", s + ":ERR");
                }
            }
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) == null){
            Log.i(TAG, "Proximity sensor Not found");
            mqttConnection.SendSensorData("PROXIMITY/STATUS", s + ":NA");
        }
        else {
            Log.i(TAG, "Registering Proximity sensor");
            mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            mProximityMax = mProximity.getMaximumRange();
            if (mProximity != null) {
                mSensorManager.registerListener(SimpleSensorListener, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
                Log.i(TAG, "Proximity sensor registered");


                if (mSensorManager != null) {
                    mqttConnection.SendSensorData("PROXIMITY/STATUS", s + ":OK");
                }
            } else {
                Log.e(TAG, "Proximity sensor FAILED to register");
                if (mSensorManager != null) {
                    mqttConnection.SendSensorData("PROXIMITY/STATUS", s + ":ERR");
                }
            }
        }
    }

    private final SensorEventListener SimpleSensorListener = new SensorEventListener() {
        float mLightPrev = -1 ;
        float mProximityPrev = -100;
        float mPressurePrev = -1;

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float val = event.values[0];
            //Log.d(TAG, "Got value " + val + " from sensor " + event.sensor.getType() + " valp " + mValp);
            if(event.sensor.getType() == Sensor.TYPE_LIGHT){
                //int valp = (int)(100*val / mLightMax) ;
                //Log.d(TAG, "Max val: " + mLightMax + " valperc : " + valp);
                if ( Math.abs(mLightPrev - val) < 10) {
                   return;
                }
                Log.i(TAG, "New lux  val: " + val);
                mLightPrev = val ;
                Timestamp ts = new Timestamp(System.currentTimeMillis());
                String s = ts + ":" + name + ":" + id + ":" + val + ":" + mLightMax;
                if (mqttConnection != null) {
                    mqttConnection.SendSensorData("LIGHT", s);
                }
            }

            if(event.sensor.getType() == Sensor.TYPE_PRESSURE){
                if (Math.abs(mPressurePrev - val)<1) {
                    return;
                }
                Log.i(TAG, "New Pressure  val: " + val);
                mPressurePrev = val ;
                Timestamp ts = new Timestamp(System.currentTimeMillis());
                String s = ts + ":" + name + ":" + id + ":" + val + ":" + mPressureMax;
                if (mqttConnection != null) {
                    mqttConnection.SendSensorData("PRESSURE", s);
                }
            }


        if(event.sensor.getType() == Sensor.TYPE_PROXIMITY ){
            if ( Math.abs(mProximityPrev - val) < 1) {
                return;
            }
            Log.i(TAG, "New Proximity  val: " + val);
            mProximityPrev = val ;
            Timestamp ts = new Timestamp(System.currentTimeMillis());
            String s = ts + ":" + name + ":" + id + ":" + val + ":" + mProximityMax;
            if (mqttConnection != null) {
                mqttConnection.SendSensorData("PROXIMITY", s);
            }
        }
    }

    };



    // --------------------------------------------------------------------------------------------
    // GPS methods
    //

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 10:
                Log.d(TAG, "GPS call configureButton :");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    configureButton();
                return;
        }
    }

    private void configureButton() {

        Log.i(TAG, "GPS Requesting Location update. interval: " + R.string.location_update_interval);
        //si.textInfo.setVisibility(View.VISIBLE);
        //si.textInfo.setText("GPS LOCATION:" + "\n");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to name the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates("gps", R.string.location_update_interval, 0, locationListener);
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        //client.connect();
        //AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        //AppIndex.AppIndexApi.end(client, getIndexApiAction());
        //client.disconnect();
    }
}
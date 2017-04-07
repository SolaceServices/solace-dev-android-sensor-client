package com.solace.psg.nram.androidsimplesensorclient;

/**
 * This class implements All MQTT server interaction
 * Created by nram on 1/10/17.
 */

//TODO: Provide a Setting option to change server settings, etc.

//TODO: Google Map interface for received coordinates as web client


import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.solace.psg.nram.androidsimplesensorclient.R;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;



public class MqttConnection  {

    // MQTT settings
    //MqttClient mqttClient = null;
    MqttAsyncClient mqttAsyncClient ;
    String mqttBrokerUrl ;
    String mqttClientUser ;
    String mqttClientPassword ;
    String mqttClientId ;
    int    mqttQos = 0;
    //Context ctx ;
    String handle ;
    String id ;
    String passwd ;
    SessionInfo si;


    MqttConnection(SessionInfo _si, String _handle, String _id, String _passwd) throws MqttException, InterruptedException {
        final String TAG = "Mqtt::Ctor";


        si = _si;
        Log.i(TAG, "MqttConnection initializing");


        mqttBrokerUrl = si.ctx.getString(R.string.mqtt_broker_url);
        mqttClientUser = si.ctx.getString(R.string.mqtt_client_username);
        mqttClientPassword = si.ctx.getString(R.string.mqtt_client_password);
        mqttQos = Integer.parseInt(si.ctx.getString(R.string.mqtt_qos));
        mqttClientId = MqttClient.generateClientId();

        Log.i(TAG, "MQTT Broker URL : " + mqttBrokerUrl) ;
        Log.i(TAG, "MQTT Client User: " + mqttClientUser) ;
        Log.i(TAG, "MQTT ClientID   : " + mqttClientId) ;
        Log.i(TAG, "MQTT QoS        : " + mqttQos) ;


        handle = _handle ;
        id = _id ;
        passwd = _passwd ;


        try {
            Connect();
            Subsribe();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    void Reconnect() {
        final String TAG = "MqttCB::Reconnect";
        Log.i (TAG, "*** Reconnecting to MQTT server ***");
        try {
            Connect();
            Subsribe();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // Callback - Anonymous inner-class for receiving messages
    MqttCallback mqttCallback;
    {
        mqttCallback = new MqttCallback() {

            @SuppressLint("LongLogTag")
            // When a message arrrives from Solace, sent it to an IntentService
            // which does the job in another thread
            // its eventually sent back to this thread (MainActivity - OnReceive)
            // for UI updates.
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final String TAG = "MqttCallback::messageArrived";
                // Called when a message arrives from the server that
                // matches any subscription made by the client
                String payload = new String(message.getPayload());
                String s = "Received a Message on Topic:   " + topic + " Qos: " + message.getQos();
                Logi(TAG, s);

                /* Starting Message processing Service */
                Log.d(TAG, "creating intend to process status");
                Intent intent = new Intent(si.ctx,ResponseStatusService.class);
                intent.setData(Uri.parse(payload)) ;
                intent.putExtra(ResponseStatusService.PARAM_IN_TOPIC, topic);
                intent.putExtra(ResponseStatusService.PARAM_IN_MESSAGE, payload);
                si.ctx.startService(intent) ;

                //latch.countDown(); // unblock main thread
            }

            public void connectionLost(Throwable cause) {
                final String TAG = "MqttCB::ConLost";
                String s = "Connection to Solace broker lost: " + cause.getMessage();
                Log.i(TAG, s);
                Reconnect();
                //latch.countDown();
            }

            public void deliveryComplete(IMqttDeliveryToken token) {
                final String TAG = "MqttCB::Complete";
                String s = "Delivery Complete";
                Logi(TAG, s);
            }

        };
    }

    // TODO: Should connect be an IntentService?
    void Connect() throws InterruptedException {
        final String TAG = "Mqtt::Connect";

        Log.i(TAG, "Connecting to MQTT server: " + mqttBrokerUrl + " as user: " + mqttClientUser + " clientID: " + mqttClientId);

        MemoryPersistence persistence = new MemoryPersistence();
        try {
            //mqttClient = new MqttClient(mqttBrokerUrl, mqttClientId, persistence);
            mqttAsyncClient = new MqttAsyncClient(mqttBrokerUrl, mqttClientId, persistence);
            mqttAsyncClient.setCallback(mqttCallback);

        } catch (MqttException e) {
            e.printStackTrace();
        }


        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        //don't turn this off -- will lead to zombie client connections on appliance
        //options.setCleanSession(false);
        //solace supports MQTT 3.1.1 No need to use 3.1
        //options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
        options.setUserName(mqttClientUser);
        options.setPassword(mqttClientPassword.toCharArray());
        options.setConnectionTimeout(3);

        try {
            //mqttClient.connect(options);
            IMqttToken t = mqttAsyncClient.connect(options);
            t.waitForCompletion(100000);

        } catch (MqttException e) {
            Loge(TAG, "Connect Exception");
            e.printStackTrace();
        }

    }

    void Subsribe() throws MqttException {

        final String TAG = "Mqtt::Subsribe";

        // Topic filter the client will subscribe to
        // Eg: T/Status/myid/#
        final String subTopic = si.ctx.getString(R.string.mqtt_sub_topic_prefix)+id+"/#" ;
        final String subTopicAll = si.ctx.getString(R.string.mqtt_sub_topic_prefix)+"/ALL/#" ;

        try {
            // Subscribe client to the topic filter and a QoS level of 0
            Logi(TAG, "Subscribing client to topic: " + subTopic);
            mqttAsyncClient.subscribe(subTopic, 0);

            //trying to subscribe to multiple topics -- this gives syntax error
            //mqttAsyncClient.subscribe([(subTopic, 0), (subTopicAll, 0)]);

            // no errors, but topics published to T/Status/ALL/ONLINE weren't received
            // client status on SolAdmin lists both topics though
            Logi(TAG, "Subscribing client to topic: " + subTopicAll);
            mqttAsyncClient.subscribe(subTopicAll, 0);

        } catch (MqttException e) {
            e.printStackTrace();
        }

        //Logd(TAG, "End of subsribe");
    }

    void Send(String _topic, String _payload) {
        // prepare message
        final String TAG = "MqttConnection::Send" ;
        //TODO -- This shouldn't be required. Server keeps disconnecting and client needs
        // to reconnect forcefully -- FIXIT
        if (! mqttAsyncClient.isConnected()) {
            this.Reconnect();
        }

        Log.i(TAG, "Publishing  msg message [" + _payload + "] to topic: <" + _topic +
                "> with QOS: " + mqttQos);
        Log.d(TAG, _payload);

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ResponseReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        String s = ts + " :<" + _topic + ">: " + _payload;
        broadcastIntent.putExtra(ResponseStatusService.PARAM_OUT_MSG, s);
        broadcastIntent.putExtra(ResponseStatusService.PARAM_OUT_VIEW, ResponseStatusService.TEXTVIEW_INFO);
        si.ctx.sendBroadcast(broadcastIntent);

        try {
            byte[] encodedPayload = new byte[0];
            encodedPayload = _payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            message.setQos(mqttQos);
            // Send message
            mqttAsyncClient.publish(_topic, message);
        } catch (MqttException e) {
            Log.e(TAG, "Publish Exception");
            Log.i(TAG, "Message: " + e.getMessage());
            Log.i(TAG, "Cause: " + e.getCause());
            Log.i(TAG, "Loc: " + e.getLocalizedMessage());
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


    void SendRegister(String _payload) {
        final String TAG = "Mqtt::Register" ;
        Send(   si.ctx.getString(R.string.mqtt_pub_topic_register)+"/"+id,
                _payload);
    }

    void SendUnregister (String _payload) {
        final String TAG = "Mqtt::Unregister" ;
        Send(   si.ctx.getString(R.string.mqtt_pub_topic_unregister)+"/"+id,
                _payload);
    }


    void SendSensorInfo(String _payload) {
        final String TAG = "Mqtt::Sensor" ;
        Send(   si.ctx.getString(R.string.mqtt_pub_topic_sensor_info)+"/"+id,
                _payload);
    }

    void SendSensorData(String _sensorname, String _payload) {
        final String TAG = "Mqtt::SensorData" ;
        Send(   si.ctx.getString(R.string.mqtt_pub_topic_sensor_info)+ "/" + _sensorname + "/"+id,
                _payload);
    }

    void SendLocation (String _payload) {
        final String TAG = "Mqtt::Sensor" ;
        Send(   si.ctx.getString(R.string.mqtt_pub_topic_location)+"/"+handle+"-"+id,
                _payload);
    }


    void Close() throws MqttException {
        String TAG = "Mqtt::Close" ;
        Logi(TAG, "Closing MQTT connection");
        try {
            mqttAsyncClient.close();
            System.out.println("Exiting");
            System.exit(0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void Logd (String tag, String log)
    {
        Log.d(tag, log) ;
    }

    private void Logi (String tag, String log)
    {
        Log.i(tag, log) ;
    }

    private void Loge (String tag, String log)
    {
        Log.e(tag, log) ;
    }
}

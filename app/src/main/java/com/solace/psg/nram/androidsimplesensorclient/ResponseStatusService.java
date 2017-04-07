package com.solace.psg.nram.androidsimplesensorclient;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.sql.Timestamp;

/**
 * This class queues response messages from Solace before they are processed
 * Without this, even simple textview updates are often missed.
 * Created by nram on 1/19/17.
 */

public class ResponseStatusService extends IntentService {

    // Fields in Brodacast message
    public static final String PARAM_IN_TOPIC   = "TOPIC/ResponseStatusService/IN";
    public static final String PARAM_IN_MESSAGE = "MESSAGE/ResponseStatusService/IN";
    public static final String PARAM_OUT_MSG    = "MESSAGE/ResponseStatusService/OUT";
    public static final String PARAM_OUT_VIEW   = "VIEW/ResponseStatusService/OUT";

    public static final String TEXTVIEW_INFO        = "1";

    private static final String TAG = "ResponseStatusService";

    public ResponseStatusService() {
        super(ResponseStatusService.class.getName());
        //Log.d(TAG, "constructor");

    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.i(TAG, "Service Started!");
        String dataString = intent.getDataString();
        String topic = intent.getStringExtra(PARAM_IN_TOPIC);
        String payload = intent.getStringExtra(PARAM_IN_MESSAGE);

        String time = new Timestamp(System.currentTimeMillis()).toString();
        String s = " Time:    " + time +
                " Topic: " + topic +
                " Message: " + payload ;
        Log.d(TAG, s);

        // Response broadcast Intent
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ResponseReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        //broadcastIntent.putExtra(PARAM_OUT_MSG, payload);

        if (topic.indexOf("Status") > -1) {
            Log.d(TAG, "Got Info from backend: " + payload);
            Timestamp ts = new Timestamp(System.currentTimeMillis());

            // update on status views
            sendBroadcast(broadcastIntent);

            // update output view
            s = ts + " :<" + topic + ">: " + payload;
            Log.d(TAG, "broadcast responses :" + s);
            broadcastIntent.putExtra(PARAM_OUT_MSG, s);
            broadcastIntent.putExtra(PARAM_OUT_VIEW, TEXTVIEW_INFO);
            sendBroadcast(broadcastIntent);
        }

        //final ResultReceiver receiver = intent.getParcelableExtra("receiver");

    }
}

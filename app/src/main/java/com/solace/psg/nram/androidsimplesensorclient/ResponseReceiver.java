package com.solace.psg.nram.androidsimplesensorclient;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;
import android.view.View;

/**
 * Created by nram on 4/7/17.
 */

public class ResponseReceiver extends BroadcastReceiver {
    final String TAG = "ResponseReceiver::Ctor";

    public static final String ACTION_RESP =
            "com.solace.psg.nram.androidsimplesensorclient.MESSAGE_PROCESSED";
     SessionInfo si;

    public  ResponseReceiver (SessionInfo _si) {
        Log.d(TAG, "init");
        si = _si;
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onReceive(Context context, Intent intent) {
        final String TAG = "ResponseReceiver::onReceive";
        String out = intent.getStringExtra(ResponseStatusService.PARAM_OUT_MSG);
        if (out == null) {
            return;
        }
        Log.d(TAG, "Data from service : " + out);
        String payload = intent.getStringExtra(ResponseStatusService.PARAM_OUT_MSG);

        Log.d(TAG, "Adding to Info view : " + payload);
        String s = "INFO :" + payload;
        if (payload.indexOf("Device") > -1) {
            s = "SENT :" + payload;
        }
        else if (payload.indexOf("Status") > -1) {
            s = "RECV :" + payload;
        }
        si.textInfo.setVisibility(View.VISIBLE);
        si.textInfo.append ("\n" + s);
    }
}
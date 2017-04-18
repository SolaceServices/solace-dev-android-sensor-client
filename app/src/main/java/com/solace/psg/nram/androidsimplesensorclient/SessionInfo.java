package com.solace.psg.nram.androidsimplesensorclient;

import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * This class stores all session properties
 * Android has no simple means to pass Activity to other classes
 * Handy to share with sub classes like MqttConnection
 * Created by nram on 1/11/17.
 */

public class SessionInfo {

    public Context ctx ;
/*
    public TextView textName;
    public TextView textID ;
    public TextView textUrl ;
    public TextView textClientname ;
    public TextView textClientPassword ;
    */
    public TextView textStatus;
    public TextView textInfo;

    public EditText Url;
    public EditText Name ;
    public EditText Id;
    public EditText Clientname ;
    public EditText ClientPassword ;


    public Button btnConnect;
    public Button btnReset;
    public Button btnQuit;

    SessionInfo() {}

}

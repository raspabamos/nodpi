package org.nodpi.hello.receiver;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.content.SharedPreferences;


import androidx.core.content.ContextCompat;

import org.nodpi.hello.service.DpiService;
import org.nodpi.hello.service.DpiVpnService;

public class BReceiver extends BroadcastReceiver {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("BReceiver", "onReceive called");
        ContextCompat.startForegroundService(context, new Intent(context, DpiService.class));



    }
}

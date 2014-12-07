package com.group15.toq_o.present;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Mari on 9/14/14.
 */
public class ToqBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Launch receiver for powerpoint when completing install of deck of cards applet
        Intent launchIntent= new Intent(context, GoogleDriveActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(launchIntent);
    }
}

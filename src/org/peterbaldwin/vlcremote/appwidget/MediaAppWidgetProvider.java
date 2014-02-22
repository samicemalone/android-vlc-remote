/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Modifications:
 * -Connect to VLC server instead of media service
 * -Listen for VLC status events
 * -Schedule status updates for time at which current track is expected to end
 */

package org.peterbaldwin.vlcremote.appwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.intent.Intents;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.model.Status;
import org.peterbaldwin.vlcremote.net.MediaServer;
import org.peterbaldwin.vlcremote.widget.RemoteViewsFactory;

/**
 * Simple widget to show currently playing album art along with play/pause and
 * next track buttons.
 */
public class MediaAppWidgetProvider extends AppWidgetProvider {
    
    public static final String LOG_TAG = "VlcRemoteAppWidgetProvider";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intents.ACTION_MANUAL_APPWIDGET_UPDATE.equals(action)
         || ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            update(context);
        } else {
            super.onReceive(context, intent);
        }
    }
    
    public static int[] getWidgetIds(Context context) {
        AppWidgetManager app = AppWidgetManager.getInstance(context);
        ComponentName cn = new ComponentName(context, org.peterbaldwin.client.android.vlcremote.MediaAppWidgetProvider.class);
        return app.getAppWidgetIds(cn);
    }
    
    private static PendingIntent createManualAppWidgetUpdateIntent(Context context) {
        Intent intent = new Intent(Intents.ACTION_MANUAL_APPWIDGET_UPDATE);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    /**
     * Schedule an update shortly after the current track is expected to end
     * @param context
     * @param status
     */
    public static void scheduleUpdate(Context context, Status status) {
        long time = status.getTime();
        long length = status.getLength();
        if (status.isPlaying() && time >= 0L && length > 0L && time <= length) {
            long delay = (length - time + 1) * 1000;
            scheduleUpdate(context, delay);
        } else if(status.isPaused()) {
            scheduleUpdate(context, 1000 * 60 * 15); // check again in 15 mins
        }
    }
    
    private static void scheduleUpdate(Context context, long delay) {
        Object service = context.getSystemService(Context.ALARM_SERVICE);
        AlarmManager alarmManager = (AlarmManager) service;
        int type = AlarmManager.ELAPSED_REALTIME_WAKEUP;
        long triggerAtTime = SystemClock.elapsedRealtime() + delay;
        PendingIntent operation = createManualAppWidgetUpdateIntent(context);
        alarmManager.set(type, triggerAtTime, operation);
    }

    public static void cancelPendingUpdate(Context context) {
        Object service = context.getSystemService(Context.ALARM_SERVICE);
        AlarmManager alarmManager = (AlarmManager) service;
        PendingIntent operation = createManualAppWidgetUpdateIntent(context);
        alarmManager.cancel(operation);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        context.startService(Intents.service(context, Intents.ACTION_PROGRAMMATIC_APPWIDGET_UPDATE));
    }

    private void update(Context context) {
        String authority = Preferences.get(context).getAuthority();
        if (authority != null) {
            new MediaServer(context, authority).status().get();
        } else {
            update(context, context.getText(R.string.noserver).toString());
        }
    }
    
    public static void update(Context context, Status status, Bitmap bitmap) {
        update(context, new RemoteViewsFactory(context).getWidget(status, bitmap));
    }
    
    public static void update(Context context, Throwable tr) {
        update(context, new RemoteViewsFactory(context).getWidget(tr));
    }
    
    public static void update(Context context, String title) {
        update(context, new RemoteViewsFactory(context).getWidget(title));
    }
    
    private static void update(Context context, RemoteViews remote) {
        AppWidgetManager app = AppWidgetManager.getInstance(context);
        ComponentName cn = new ComponentName(context, org.peterbaldwin.client.android.vlcremote.MediaAppWidgetProvider.class);
        app.updateAppWidget(cn, remote);
    }

}

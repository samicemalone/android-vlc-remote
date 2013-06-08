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
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.intent.Intents;
import org.peterbaldwin.vlcremote.model.File;
import org.peterbaldwin.vlcremote.model.Media;
import org.peterbaldwin.vlcremote.model.PlaylistItem;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.model.Status;
import org.peterbaldwin.vlcremote.net.MediaServer;
import org.peterbaldwin.vlcremote.parser.EpisodeParser;

/**
 * Simple widget to show currently playing album art along with play/pause and
 * next track buttons.
 */
public class MediaAppWidgetProvider extends AppWidgetProvider {
    
    private static class TextHolder {
        public TextHolder(String headingText, String infoText) {
            this.headingText = headingText;
            this.infoText = infoText;
        }
        private String headingText;
        private String infoText;
    }
    
    public static final String LOG_TAG = "VlcRemoteAppWidgetProvider";
    
    private EpisodeParser mMediaParser;
    
    public MediaAppWidgetProvider() {
        mMediaParser = new EpisodeParser();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intents.ACTION_STATUS.equals(action)) {
            Status status = (Status) intent.getSerializableExtra(Intents.EXTRA_STATUS);
            TextHolder text = updateText(context, status);
            if(text == null) {
                return;
            }
            int[] appWidgetIds = null;
            performUpdate(context, text.headingText, text.infoText, status.isPlaying(), appWidgetIds);

            long time = status.getTime();
            long length = status.getLength();
            if (status.isPlaying() && time >= 0L && length > 0L && time <= length) {
                // Schedule an update shortly after the current track is
                // expected to end.
                long delay = length - time + 1000;
                scheduleUpdate(context, delay);
            }
        } else if (Intents.ACTION_ERROR.equals(action)) {
            CharSequence text1 = context.getText(R.string.connection_error);
            Throwable t = (Throwable) intent.getSerializableExtra(Intents.EXTRA_THROWABLE);
            String text2 = t.getMessage();
            if (text2 == null) {
                text2 = t.getClass().getName();
            }
            Boolean playing = null;
            int[] appWidgetIds = null;
            performUpdate(context, text1, text2, playing, appWidgetIds);
            cancelPendingUpdate(context);
        } else if (Intents.ACTION_MANUAL_APPWIDGET_UPDATE.equals(action)
                || ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            int[] appWidgetIds = null;
            update(context, appWidgetIds);
        } else {
            super.onReceive(context, intent);
        }
    }
    
    /**
     * Gets the new heading and text to be updated for the given status.
     * @param ctx Context
     * @param status Status
     * @return TextHolder with new text to display. null if no change needed
     */
    private TextHolder updateText(Context ctx, Status status) {
        if (status.isStopped()) {
            return new TextHolder(ctx.getString(R.string.no_media), "");
        }
        String fileName;
        if(status.getTrack().getName() == null) {
            if(status.getTrack().getTitle() == null) {
                return new TextHolder("", "");
            }
            fileName = status.getTrack().getTitle();
        } else {
            fileName = status.getTrack().getName();
        }
        if(!status.getTrack().containsStream() || status.getTrack().hasVideoStream()) {
            Media media = mMediaParser.parse(fileName);
            if(media != null) {
                return setPlaylistText(media, fileName);
            }
        }
        return setPlaylistText(status.getTrack(), fileName);
    }
    
    private TextHolder setPlaylistText(PlaylistItem item, String fileName) {
        if(TextUtils.isEmpty(item.getPlaylistText())) {
            return new TextHolder(item.getPlaylistHeading(), File.baseName(fileName));
        }
        return new TextHolder(item.getPlaylistHeading(), item.getPlaylistText());
    }

    private static PendingIntent createManualAppWidgetUpdateIntent(Context context) {
        int requestCode = 0;
        Intent intent = new Intent(Intents.ACTION_MANUAL_APPWIDGET_UPDATE);
        int flags = 0;
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    private void scheduleUpdate(Context context, long delay) {
        Object service = context.getSystemService(Context.ALARM_SERVICE);
        AlarmManager alarmManager = (AlarmManager) service;
        int type = AlarmManager.ELAPSED_REALTIME_WAKEUP;
        long triggerAtTime = SystemClock.elapsedRealtime() + delay;
        PendingIntent operation = createManualAppWidgetUpdateIntent(context);
        alarmManager.set(type, triggerAtTime, operation);
    }

    private void cancelPendingUpdate(Context context) {
        Object service = context.getSystemService(Context.ALARM_SERVICE);
        AlarmManager alarmManager = (AlarmManager) service;
        PendingIntent operation = createManualAppWidgetUpdateIntent(context);
        alarmManager.cancel(operation);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        update(context, appWidgetIds);
    }

    private void update(Context context, int[] appWidgetIds) {
        Preferences preferences = Preferences.get(context);
        String authority = preferences.getAuthority();
        if (authority != null) {
            MediaServer server = new MediaServer(context, authority);
            server.status().get();
        } else {
            CharSequence text1 = context.getText(R.string.noserver);
            CharSequence text2 = "";
            Boolean playing = null;
            performUpdate(context, text1, text2, playing, appWidgetIds);
        }
    }

    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        // Update specific list of appWidgetIds if given,
        // otherwise default to all
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            manager.updateAppWidget(appWidgetIds, views);
        } else {
            Class<? extends AppWidgetProvider> cls = getClass();
            ComponentName provider = new ComponentName(context, cls);
            manager.updateAppWidget(provider, views);
        }
    }

    /**
     * Update all active widget instances by pushing changes
     */
    void performUpdate(Context context, CharSequence title, CharSequence artist, Boolean playing,
            int[] appWidgetIds) {
        String packageName = context.getPackageName();
        RemoteViews views = new RemoteViews(packageName, R.layout.album_appwidget);

        views.setViewVisibility(R.id.title, View.VISIBLE);
        views.setTextViewText(R.id.title, title);
        views.setTextViewText(R.id.artist, artist);

        if (playing != null) {
            views.setImageViewResource(R.id.control_play,
                    playing ? R.drawable.ic_appwidget_music_pause
                            : R.drawable.ic_appwidget_music_play);
        } else {
            views.setImageViewResource(R.id.control_play, R.drawable.ic_popup_sync_2);
        }

        views.setViewVisibility(R.id.control_next, playing != null ? View.VISIBLE : View.GONE);

        // Link actions buttons to intents
        linkButtons(context, views, playing);

        pushUpdate(context, appWidgetIds, views);
    }

    /**
     * Link up various button actions using {@link PendingIntent}.
     */
    private void linkButtons(Context context, RemoteViews views, Boolean playing) {
        {
            int requestCode = 0;
            Intent intent = getLaunchIntent(context);
            int flags = 0;
            PendingIntent pendingIntent = PendingIntent.getActivity(context, requestCode, intent,
                    flags);
            views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent);
        }
        
        Preferences preferences = Preferences.get(context);
        String authority = preferences.getAuthority();
        if (authority == null) {
            return;
        }
        MediaServer server = new MediaServer(context, authority);

        if (playing != null) {
            PendingIntent intent = server.status().command.playback.pendingPause();
            views.setOnClickPendingIntent(R.id.control_play, intent);
        } else {
            PendingIntent intent = server.status().pendingGet();
            views.setOnClickPendingIntent(R.id.control_play, intent);
        }

        {
            PendingIntent intent = server.status().command.playback.pendingNext();
            views.setOnClickPendingIntent(R.id.control_next, intent);
        }
    }

    /**
     * Returns the {@link Intent} to launch VLC Remote.
     */
    private static Intent getLaunchIntent(Context context) {
        return context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
    }
}

/*
 * Copyright (C) 2014 Sam Malone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.peterbaldwin.vlcremote.widget;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.widget.RemoteViews;
import org.peterbaldwin.client.android.vlcremote.PlaybackActivity;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.intent.Intents;
import org.peterbaldwin.vlcremote.model.File;
import org.peterbaldwin.vlcremote.model.Media;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.model.Status;
import org.peterbaldwin.vlcremote.model.Track;
import org.peterbaldwin.vlcremote.net.MediaServer;
import org.peterbaldwin.vlcremote.parser.MediaParser;

/**
 *
 * @author Sam Malone
 */
public class NotificationControls {
    
    public static void cancel(Context context) {
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(ID);
    }
    
    private static final int ID = 1;
    
    private final Context context;
    private final Preferences preferences;
    private final MediaServer server;
    private final MediaParser parser;

    public NotificationControls(Context context) {
        this.context = context;
        this.preferences = Preferences.get(context);
        this.server = new MediaServer(context, preferences.getAuthority());
        this.parser = new MediaParser();
    }
    
    public void showLoading() {
        show(null);
    }
    
    public void show(Status status) {
        show(status, BitmapFactory.decodeResource(context.getResources(), R.drawable.albumart_mp_unknown));
    }
    
    public void show(Status status, Bitmap art) {       
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, PlaybackActivity.class);

        // The stack builder will contain an artificial back stack for the started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(PlaybackActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification n = builder.setContent(getNormalView(status, art))
                                .setWhen(0)
                                .setOngoing(true)
                                .setSmallIcon(R.drawable.ic_vlc_server)
                                .build();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            n.bigContentView = getExpandedView(status, art);
        }
        notificationManager.notify(ID, n);
    }
    
    private RemoteViews getNormalView(Status status, Bitmap art) {
        RemoteViews remote = new RemoteViews(context.getPackageName(), R.layout.notification_normal);
        setupCommonViews(remote, status, art);
        if(status == null || status.isStopped()) {
            resetText(remote, R.id.text);
            return remote;
        }
        String fileName = status.getTrack().getName() != null ? status.getTrack().getName() : status.getTrack().getTitle();
        Media track = parseMedia(status, fileName);
        String text = TextUtils.isEmpty(track.getPlaylistText()) ? File.baseName(fileName) : track.getPlaylistText();
        remote.setTextViewText(R.id.title, track.getPlaylistHeading());
        remote.setTextViewText(R.id.text, text);
        return remote;
    }
    
    private RemoteViews getExpandedView(Status status, Bitmap art) {
        RemoteViews remote = new RemoteViews(context.getPackageName(), R.layout.notification_expanded);
        setupCommonViews(remote, status, art);
        remote.setOnClickPendingIntent(R.id.control_prev, server.status().command.playback.pendingPrevious());
        if(status == null || status.isStopped()) {
            resetText(remote, R.id.text1, R.id.text2);
            return remote;
        }
        String fileName = status.getTrack().getName() != null ? status.getTrack().getName() : status.getTrack().getTitle();
        Media track = parseMedia(status, fileName);
        String text2 = TextUtils.isEmpty(track.getMediaSecondText()) ? File.baseName(fileName) : track.getMediaSecondText();
        remote.setTextViewText(R.id.title, track.getMediaHeading());
        remote.setTextViewText(R.id.text1, track.getMediaFirstText());
        remote.setTextViewText(R.id.text2, text2);
        return remote;
    }
    
    private void setupCommonViews(RemoteViews remote, Status status, Bitmap art) {
        remote.setOnClickPendingIntent(R.id.control_play, server.status().command.playback.pendingPause());
        remote.setOnClickPendingIntent(R.id.control_next, server.status().command.playback.pendingNext());
        remote.setOnClickPendingIntent(R.id.control_close, PendingIntent.getService(context, 0, Intents.service(context, Intents.ACTION_NOTIFICATION_CANCEL), 0));
        remote.setImageViewBitmap(R.id.art, art);
        if(status == null) {
            remote.setTextViewText(R.id.title, context.getString(R.string.loading));
        } else {
            if(status.isStopped()) {
                remote.setTextViewText(R.id.title, context.getString(R.string.no_media));
                remote.setImageViewResource(R.id.control_play, R.drawable.ic_media_play);
            } else {
                remote.setImageViewResource(R.id.control_play, status.isPaused() ? R.drawable.ic_media_play : R.drawable.ic_media_pause);
            }
        }
    }
    
    private Media parseMedia(Status status, String fileName) {
        Track track = status.getTrack();
        if(fileName != null && (!track.containsStream() || track.hasVideoStream())) {
            Media media = parser.parse(fileName);
            if(media != null) {
                return media;
            }
        }
        return track;
    }
    
    private void resetText(RemoteViews remote, int... textViewIds) {
        for(int id : textViewIds) {
            remote.setTextViewText(id, "");
        }
    }
    
}

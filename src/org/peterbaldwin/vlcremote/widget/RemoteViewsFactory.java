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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;
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
public class RemoteViewsFactory {
    
    private final Context context;
    private final Preferences preferences;
    private final MediaServer server;
    private final MediaParser parser;

    public RemoteViewsFactory(Context context) {
        this.context = context;
        this.preferences = Preferences.get(context);
        this.server = new MediaServer(context, preferences.getAuthority());
        this.parser = new MediaParser();
    }
    
    public RemoteViews getNotifiation(Status status, Bitmap bitmap) {
        RemoteViews remote = new RemoteViews(context.getPackageName(), R.layout.notification_normal);
        return getNormalView(remote, status, bitmap);
    }
    
    public RemoteViews getNotifiationExpanded(Status status, Bitmap bitmap) {
        RemoteViews remote = new RemoteViews(context.getPackageName(), R.layout.notification_expanded);
        return getExpandedView(remote, status, bitmap);
    }

    public RemoteViews getWidget(Status status, Bitmap bitmap) {
        return getNormalView(getEmptyWidget(), status, bitmap);
    }
    
    public RemoteViews getNotification(Throwable tr) {
        return getErrorView(getEmptyNotification(), tr);
    }
    
    public RemoteViews getNotificationExpanded(Throwable tr) {
        return getErrorView(getEmptyNotificationExpanded(), tr);
    }
    
    public RemoteViews getWidget(Throwable tr) {
        return getErrorView(getEmptyWidget(), tr);
    }
    
    public RemoteViews getWidget(String errorText) {
        return setError(getEmptyWidget(), errorText);
    }
    
    private RemoteViews getEmptyNotification() {
        return new RemoteViews(context.getPackageName(), R.layout.notification_normal);
    }
    
    private RemoteViews getEmptyNotificationExpanded() {
        return new RemoteViews(context.getPackageName(), R.layout.notification_expanded);
    }
    
    private RemoteViews getEmptyWidget() {
        RemoteViews remote = new RemoteViews(context.getPackageName(), R.layout.album_appwidget);
        return setLaunchIntent(remote, context);
    }
    
    private RemoteViews getErrorView(RemoteViews remote, Throwable tr) {
        return setError(remote, tr.getMessage() == null ? tr.getClass().getName() : tr.getMessage());
    }
    
    private RemoteViews getNormalView(RemoteViews remote, Status status, Bitmap art) {
        setupCommonViews(remote, status, art);
        if(status == null || status.isStopped() || server.getAuthority() == null) {
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
    
    private RemoteViews getExpandedView(RemoteViews remote, Status status, Bitmap art) {
        setupCommonViews(remote, status, art);
        remote.setViewVisibility(R.id.control_prev, View.VISIBLE);
        remote.setOnClickPendingIntent(R.id.control_prev, server.status().command.playback.pendingPrevious());
        if(status == null || status.isStopped() || server.getAuthority() == null) {
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
        remote.setImageViewBitmap(R.id.art, art);
        if(server.getAuthority() == null) {
            setError(remote, context.getString(R.string.noserver));
            return;
        }
        remote.setViewVisibility(R.id.control_next, View.VISIBLE);
        remote.setOnClickPendingIntent(R.id.control_play, server.status().command.playback.pendingPause());
        remote.setOnClickPendingIntent(R.id.control_next, server.status().command.playback.pendingNext());
        remote.setOnClickPendingIntent(R.id.control_close, PendingIntent.getService(context, 0, Intents.service(context, Intents.ACTION_NOTIFICATION_CANCEL), 0));
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
    
    private RemoteViews setError(RemoteViews remote, String title) {
        remote.setTextViewText(R.id.title, title);
        remote.setOnClickPendingIntent(R.id.control_play, server.status().pendingGet());
        remote.setImageViewResource(R.id.control_play, R.drawable.ic_menu_refresh);
        remote.setImageViewResource(R.id.art, R.drawable.albumart_mp_unknown);
        remote.setViewVisibility(R.id.control_prev, View.GONE);
        remote.setViewVisibility(R.id.control_next, View.GONE);
        resetText(remote, R.id.text, R.id.text1, R.id.text2);
        return remote;
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
    
    private static RemoteViews setLaunchIntent(RemoteViews remote, Context context) {
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        PendingIntent pi = PendingIntent.getActivity(context, 0, launch, 0);
        remote.setOnClickPendingIntent(R.id.text_container, pi);
        return remote;
    }
    
}

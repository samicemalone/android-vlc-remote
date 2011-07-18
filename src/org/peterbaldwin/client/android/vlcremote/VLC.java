/*-
 *  Copyright (C) 2009 Peter Baldwin   
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.peterbaldwin.client.android.vlcremote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;

public final class VLC {
	
	// TODO: Add permission for broadcast
	private static final String PACKAGE = "org.peterbaldwin.client.android.vlcremote";
	
	/**
	 * Plays a media URI with VLC (usually streaming audio/video).
	 * <p>
	 * For example, {@code http://www.example.com/video.mp4}
	 */
	public static final String ACTION_VIEW = "org.peterbaldwin.vlcremote.intent.action.VIEW";

	/**
	 * Plays a browse URI (local file).
	 * <p>
	 * For example, {@code
	 * http://mediaserver/requests/browse.xml?dir=...&file=...}
	 */
	public static final String ACTION_PLAY = "org.peterbaldwin.vlcremote.intent.action.PLAY";

    /**
     * Streams a media file.
     */
    public static final String ACTION_STREAM = "org.peterbaldwin.vlcremote.intent.action.STREAM";

	/**
	 * Enqueues a browse URI (local file).
	 * <p>
	 * For example, {@code
	 * http://mediaserver/requests/browse.xml?dir=...&file=...}
	 */
	public static final String ACTION_ENQUEUE = "org.peterbaldwin.vlcremote.intent.action.ENQUEUE";

	public static final String ACTION_STATUS = "org.peterbaldwin.vlcremote.intent.action.STATUS";
	
	public static final String ACTION_PLAYLIST = "org.peterbaldwin.vlcremote.intent.action.PLAYLIST";
	
	public static final String ACTION_ALBUM_ART = "org.peterbaldwin.vlcremote.intent.action.ALBUM_ART";

	public static final String ACTION_EXCEPTION = "org.peterbaldwin.vlcremote.intent.action.EXCEPTION";

	public static final String ACTION_MANUAL_APPWIDGET_UPDATE = "org.peterbaldwin.vlcremote.intent.action.MANUAL_APPWIDGET_UPDATE";

	public static final String EXTRA_PLAYING = "org.peterbaldwin.vlcremote.intent.extra.PLAYING";
	public static final String EXTRA_PAUSED = "org.peterbaldwin.vlcremote.intent.extra.PAUSED";
	public static final String EXTRA_STOPPED = "org.peterbaldwin.vlcremote.intent.extra.STOPPED";
	public static final String EXTRA_RANDOM = "org.peterbaldwin.vlcremote.intent.extra.RANDOM";
	public static final String EXTRA_LOOP = "org.peterbaldwin.vlcremote.intent.extra.LOOP";
	public static final String EXTRA_REPEAT = "org.peterbaldwin.vlcremote.intent.extra.REPEAT";
	public static final String EXTRA_TIME = "org.peterbaldwin.vlcremote.intent.extra.TIME";
	public static final String EXTRA_LENGTH = "org.peterbaldwin.vlcremote.intent.extra.LENGTH";
	public static final String EXTRA_CURRENT_TRACK_TITLE = "org.peterbaldwin.vlcremote.intent.extra.CURRENT_TRACK_TITLE";
	public static final String EXTRA_CURRENT_TRACK_ARTIST = "org.peterbaldwin.vlcremote.intent.extra.CURRENT_TRACK_ARTIST";
	public static final String EXTRA_CURRENT_TRACK_NAME = "org.peterbaldwin.vlcremote.intent.extra.CURRENT_TRACK_NAME";
	public static final String EXTRA_EXCEPTION_CLASS = "org.peterbaldwin.vlcremote.intent.extra.EXCEPTION_CLASS";
	public static final String EXTRA_EXCEPTION_MESSAGE = "org.peterbaldwin.vlcremote.intent.extra.EXCEPTION_MESSAGE";
		
	public static final String EXTRA_STATUS = "org.peterbaldwin.vlcremote.intent.extra.STATUS";
	public static final String EXTRA_PLAYLIST = "org.peterbaldwin.vlcremote.intent.extra.PLAYLIST";
	public static final String EXTRA_ALBUM_ART = "org.peterbaldwin.vlcremote.intent.extra.ALBUM_ART";
	public static final String EXTRA_EXCEPTION = "org.peterbaldwin.vlcremote.intent.extra.EXCEPTION";
	
	public static final String EXTRA_STREAM_DATA = "org.peterbaldwin.vlcremote.intent.extra.STREAM_DATA";
	public static final String EXTRA_STREAM_TYPE = "org.peterbaldwin.vlcremote.intent.extra.STREAM_TYPE";

	static final String PREFERENCES = "preferences";
	static final String PREFERENCE_SERVER = "server";
	static final String PREFERENCE_REMEMBERED_SERVERS = "remembered_servers";
	static final String PREFERENCE_BROWSE_DIRECTORY = "browse_directory";
	static final String PREFERENCE_HOME_DIRECTORY = "home_directory";
	static final String PREFERENCE_RESUME_ON_IDLE = "resume_on_idle";
			
	public static String fileUri(String path) {
		java.io.File file = new java.io.File(path);
		Uri uri = Uri.fromFile(file);
		return uri.toString();
	}
	
	private final Context mContext;
	private final String mServer;
	private final SharedPreferences mPreferences;

	public VLC(Context context, String server) {
		mContext = context;
		mPreferences = mContext.getSharedPreferences(PREFERENCES,
				Context.MODE_PRIVATE);
		mServer = server;
	}

	public VLC(Context context) {
		mContext = context;
		mPreferences = mContext.getSharedPreferences(PREFERENCES,
				Context.MODE_PRIVATE);
		mServer = mPreferences.getString(PREFERENCE_SERVER, null);
	}

	public static Intent createStatusBroadcastIntent(Status status) {
		Track track = status.getTrack();
		Intent intent = new Intent(ACTION_STATUS);
		intent.setPackage(PACKAGE);
		intent.putExtra(EXTRA_STATUS, status);
		intent.putExtra(EXTRA_PLAYING, status.isPlaying());
		intent.putExtra(EXTRA_PAUSED, status.isPaused());
		intent.putExtra(EXTRA_RANDOM, status.isRandom());
		intent.putExtra(EXTRA_LOOP, status.isLoop());
		intent.putExtra(EXTRA_REPEAT, status.isRepeat());
		intent.putExtra(EXTRA_STOPPED, status.isStopped());
		intent.putExtra(EXTRA_TIME, status.getTime() * 1000L);
		intent.putExtra(EXTRA_LENGTH, status.getLength() * 1000L);
		intent.putExtra(EXTRA_CURRENT_TRACK_TITLE, track.getTitle());
		intent.putExtra(EXTRA_CURRENT_TRACK_ARTIST, track.getArtist());
		intent.putExtra(EXTRA_CURRENT_TRACK_NAME, track.getName());
		return intent;
	}

	public static Intent createPlaylistBroadcastIntent(Playlist playlist) {
		Intent intent = new Intent(ACTION_PLAYLIST);
		intent.setPackage(PACKAGE);
		intent.putExtra(EXTRA_PLAYING, playlist);
		return intent;
	}

	public static Intent createAlbumArtBroadcastIntent(Bitmap bitmap) {
		Intent intent = new Intent(ACTION_ALBUM_ART);
		intent.setPackage(PACKAGE);
		intent.putExtra(EXTRA_ALBUM_ART, bitmap);
		return intent;
	}
	
	private static boolean isSerializable(Object obj) {
		try {
			new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(obj);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public static Intent createExceptionBroadcastIntent(Throwable tr) {
		if (!isSerializable(tr)) {
			String message = tr.getMessage();
			Throwable cause = tr.getCause();
			tr = new Exception(message);
			if (isSerializable(cause)) {
				tr.initCause(cause);
			}
		}
		Intent intent = new Intent(ACTION_EXCEPTION);
		intent.setPackage(PACKAGE);
		intent.putExtra(EXTRA_EXCEPTION, tr);
		intent.putExtra(EXTRA_EXCEPTION_CLASS, tr.getClass().getSimpleName());
		String message = tr.getMessage();
		if (message != null) {
			intent.putExtra(EXTRA_EXCEPTION_MESSAGE, message);
		}
		return intent;
	}
	
	public Intent command(String query) {
		Intent intent = new Intent(ACTION_STATUS);
		intent.setClass(mContext, StatusService.class);
		if (!TextUtils.isEmpty(mServer)) {
			Uri uri = Uri.parse("http://" + mServer + "/requests/status.xml?"
					+ query);
			intent.setData(uri);
		}
		return intent;
	}
	
	public Intent status() {
		return command("");	
	}

	public PendingIntent pendingCommand(String query) {
		int requestCode = 0;
		Intent intent = new Intent(ACTION_STATUS);
		intent.setClass(mContext, StatusService.class);
		if (!TextUtils.isEmpty(mServer)) {
			String uriString = ("http://" + mServer + "/requests/status.xml?" + query);
			Uri uri = Uri.parse(uriString);
			intent.setData(uri);
		}
		int flags = 0;
		return PendingIntent.getService(mContext, requestCode, intent, flags);
	}

	public PendingIntent pendingStatus() {
		return pendingCommand("");
	}
	
	public boolean setResumeOnIdle() {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putLong(PREFERENCE_RESUME_ON_IDLE, System.currentTimeMillis());
		return editor.commit();
	}

	/**
	 * Returns {@code true} if {@link #setResumeOnIdle()} was called in the last hour.
	 */
	public boolean isResumeOnIdleSet() {
		long start = mPreferences.getLong(PREFERENCE_RESUME_ON_IDLE, 0L);
		long end = System.currentTimeMillis();
		return start < end && (end - start) < DateUtils.HOUR_IN_MILLIS;
	}
	
	public boolean clearResumeOnIdle() {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.remove(PREFERENCE_RESUME_ON_IDLE);
		return editor.commit();
	}
}

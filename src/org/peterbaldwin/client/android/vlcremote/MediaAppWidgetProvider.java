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

package org.peterbaldwin.client.android.vlcremote;

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

/**
 * Simple widget to show currently playing album art along with play/pause and
 * next track buttons.
 */
public class MediaAppWidgetProvider extends AppWidgetProvider {
	static final String LOG_TAG = "VlcRemoteAppWidgetProvider";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (VLC.ACTION_STATUS.equals(action)) {
			boolean playing = intent.getBooleanExtra(VLC.EXTRA_PLAYING, false);
			boolean stopped = intent.getBooleanExtra(VLC.EXTRA_STOPPED, false);
			String extraTitle = VLC.EXTRA_CURRENT_TRACK_TITLE;
			String extraArtist = VLC.EXTRA_CURRENT_TRACK_ARTIST;
			String extraName = VLC.EXTRA_CURRENT_TRACK_NAME;
			String title = intent.getStringExtra(extraTitle);
			String artist = intent.getStringExtra(extraArtist);
			String noMedia = context.getString(R.string.no_media);

			String text1;
			String text2;
			if (stopped) {
				text1 = noMedia;
				text2 = "";
			} else {
				text1 = title;
				text2 = artist;
				if (TextUtils.isEmpty(text1) && TextUtils.isEmpty(text2)) {
					text1 = intent.getStringExtra(extraName);
				}
			}
			int[] appWidgetIds = null;
			performUpdate(context, text1, text2, playing, appWidgetIds);

			long time = intent.getLongExtra(VLC.EXTRA_TIME, -1L);
			long length = intent.getLongExtra(VLC.EXTRA_LENGTH, -1L);
			if (playing && time >= 0L && length > 0L && time <= length) {
				// Schedule an update shortly after the current track is
				// expected to end.
				long delay = length - time + 1000;
				scheduleUpdate(context, delay);
			}
		} else if (VLC.ACTION_EXCEPTION.equals(action)) {
			CharSequence text1 = context.getText(R.string.connection_error);
			String text2 = intent.getStringExtra(VLC.EXTRA_EXCEPTION_MESSAGE);
			if (text2 == null) {
				text2 = intent.getStringExtra(VLC.EXTRA_EXCEPTION_CLASS);
			}
			Boolean playing = null;
			int[] appWidgetIds = null;
			performUpdate(context, text1, text2, playing, appWidgetIds);
			cancelPendingUpdate(context);
		} else if (VLC.ACTION_MANUAL_APPWIDGET_UPDATE.equals(action)
				|| ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
			update(context);
		} else {
			super.onReceive(context, intent);
		}
	}

	private static PendingIntent createManualAppWidgetUpdateIntent(
			Context context) {
		int requestCode = 0;
		Intent intent = new Intent(VLC.ACTION_MANUAL_APPWIDGET_UPDATE);
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
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		update(context);
	}

	private void update(Context context) {
		VLC vlc = new VLC(context);
		context.startService(vlc.status());
	}

	private void pushUpdate(Context context, int[] appWidgetIds,
			RemoteViews views) {
		// Update specific list of appWidgetIds if given,
		// otherwise default to all
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		if (appWidgetIds != null) {
			manager.updateAppWidget(appWidgetIds, views);
		} else {
			ComponentName provider = new ComponentName(context,
					MediaAppWidgetProvider.class);
			manager.updateAppWidget(provider, views);
		}
	}

	/**
	 * Update all active widget instances by pushing changes
	 */
	void performUpdate(Context context, CharSequence title,
			CharSequence artist, Boolean playing, int[] appWidgetIds) {
		String packageName = context.getPackageName();
		RemoteViews views = new RemoteViews(packageName,
				R.layout.album_appwidget);

		views.setViewVisibility(R.id.title, View.VISIBLE);
		views.setTextViewText(R.id.title, title);
		views.setTextViewText(R.id.artist, artist);

		if (playing != null) {
			views.setImageViewResource(R.id.control_play,
					playing ? R.drawable.ic_appwidget_music_pause
							: R.drawable.ic_appwidget_music_play);
		} else {
			views.setImageViewResource(R.id.control_play,
					R.drawable.ic_popup_sync_2);
		}

		views.setViewVisibility(R.id.control_next,
				playing != null ? View.VISIBLE : View.GONE);

		// Link actions buttons to intents
		linkButtons(context, views, playing);

		pushUpdate(context, appWidgetIds, views);
	}

	/**
	 * Link up various button actions using {@link PendingIntent}.
	 */
	private void linkButtons(Context context, RemoteViews views, Boolean playing) {
		VLC vlc = new VLC(context);
		{
			int requestCode = 0;
			Intent intent = new Intent(context, PlaybackActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			int flags = 0;
			PendingIntent pendingIntent = PendingIntent.getActivity(context,
					requestCode, intent, flags);
			views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent);
		}

		if (playing != null) {
			PendingIntent intent = vlc.pendingCommand("command=pl_pause");
			views.setOnClickPendingIntent(R.id.control_play, intent);
		} else {
			PendingIntent intent = vlc.pendingStatus();
			views.setOnClickPendingIntent(R.id.control_play, intent);
		}

		{
			PendingIntent intent = vlc.pendingCommand("command=pl_next");
			views.setOnClickPendingIntent(R.id.control_next, intent);
		}
	}
}

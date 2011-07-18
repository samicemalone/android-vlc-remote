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

import org.json.JSONArray;
import org.json.JSONException;
import org.peterbaldwin.client.android.portsweep.PickServerActivity;

import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.MessageFormat;
import java.util.ArrayList;

// TODO: clickable mute button
public class PlaybackActivity extends Activity implements View.OnClickListener,
		SeekBar.OnSeekBarChangeListener, Handler.Callback {

	private static final Uri URI_TROUBLESHOOTING = Uri
			.parse("http://code.google.com/p/android-vlc-remote/wiki/Troubleshooting");
	
	private static final String ACTION_REMOTE_VIEW = "org.openintents.remote.intent.action.VIEW";
	private static final String EXTRA_REMOTE_HOST = "org.openintents.remote.intent.extra.HOST";
	private static final String EXTRA_REMOTE_PORT = "org.openintents.remote.intent.extra.PORT";
	
	/**
	 * Maximum volume (200%).
	 */
	private static final int MAX_VOLUME = 1024;

	private static final int STATUS = 0;
	private static final int ALBUMART = 1;
	private static final int ERROR = 2;
	private static final int TIMER = 3;

	private static final int MENU_PREFERENCES = 1;
	private static final int MENU_HELP = 2;

	private static final int REQUEST_PICK_SERVER = 1;
	private static final int REQUEST_BROWSE = 2;
	private static final int REQUEST_PLAYLIST = 3;

	private static final String KEY_INPUT = "input";

	private static ArrayList<String> toArrayList(String json) {
		try {
			JSONArray array = new JSONArray(json);
			int n = array.length();
			ArrayList<String> list = new ArrayList<String>(n);
			for (int i = 0; i < n; i++) {
				String element = array.getString(i);
				list.add(element);
			}
			return list;
		} catch (JSONException e) {
			return new ArrayList<String>(0);
		}
	}

	private static String toJSONArray(ArrayList<String> list) {
		JSONArray array = new JSONArray(list);
		return array.toString();
	}

	/**
	 * Recursively removes focusability from a view hierarchy.
	 */
	private static void removeFocusability(View v) {
		v.setFocusable(false);
		if (v instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) v;
			for (int i = 0; i < group.getChildCount(); i++) {
				View child = group.getChildAt(i);
				removeFocusability(child);
			}
		}
	}

	private String mArtUrl;
	private int mVolume;
	private int mLastNonZeroVolume;

	// Emulator host IP is: 10.0.2.2
	// Default port is: 8080
	private String mServer;
	private VLC mVlc;
	private ArrayList<String> mRememberedServers;

	private String mInput;
	private String mBrowseDir;

	private BroadcastReceiver mStatusReceiver;
	private Handler mHandler;

	private ImageButton mButtonInput;
	private ImageButton mButtonPlaylistPause;
	private ImageButton mButtonPlaylistStop;
	private ImageButton mButtonPlaylistSkipForward;
	private ImageButton mButtonPlaylistSkipBackward;
	private ImageButton mButtonPlaylistSeekForward;
	private ImageButton mButtonPlaylistSeekBackward;
	private ImageButton mButtonPlaylistShuffle;
	private ImageButton mButtonPlaylistRepeat;
	private ImageButton mButtonPlaylist;
	private ImageButton mButtonFullscreen;
	private ImageButton mButtonAspectRatio;
	private ImageButton mButtonAudioTrack;
	private ImageButton mButtonSubtitleTrack;
	private ImageView mImageAlbumArt;
	private ImageView mImageVolume;
	private SeekBar mSeekPosition;
	private SeekBar mSeekVolume;
	private TextView mTextTime;
	private TextView mTextLength;
	private TextView mTextMedia;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.audio_player);
		
		// Remove focusability from all views so that view traversal
		// will not interfere with directional navigation.
		removeFocusability(getWindow().getDecorView());
		
		mButtonInput = setupImageButton(R.id.button_input);
		mButtonPlaylistPause = setupImageButton(R.id.button_pause);
		mButtonPlaylistStop = setupImageButton(R.id.button_stop);
		mButtonPlaylistSkipForward = setupImageButton(R.id.button_skip_forward);
		mButtonPlaylistSkipBackward = setupImageButton(R.id.button_skip_backward);
		mButtonPlaylistSeekForward = setupImageButton(R.id.button_seek_forward);
		mButtonPlaylistSeekBackward = setupImageButton(R.id.button_seek_backward);
		// mButtonPlaylistShuffle = setupImageButton(R.id.button_shuffle);
		// mButtonPlaylistRepeat = setupImageButton(R.id.button_repeat);
		mButtonPlaylist = setupImageButton(R.id.button_playlist);
		mButtonFullscreen = setupImageButton(R.id.button_fullscreen);
		mButtonAspectRatio = setupImageButton(R.id.button_ratio);
		mButtonAudioTrack = setupImageButton(R.id.button_audio_track);
		mButtonSubtitleTrack = setupImageButton(R.id.button_subtitle_track);

		mImageAlbumArt = (ImageView) findViewById(R.id.image_album);
		mImageVolume = (ImageView) findViewById(R.id.image_volume);

		mSeekPosition = (SeekBar) findViewById(R.id.seek_progress);
		mSeekPosition.setMax(100);
		mSeekPosition.setOnSeekBarChangeListener(this);

		mSeekVolume = (SeekBar) findViewById(R.id.seek_volume);
		mSeekVolume.setMax(MAX_VOLUME);
		mSeekVolume.setOnSeekBarChangeListener(this);

		// Set the control stream to STREAM_MUSIC to avoid annoying beeps even
		// when activity handles volume keys.
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		mTextTime = (TextView) findViewById(R.id.text_time);
		mTextLength = (TextView) findViewById(R.id.text_length);
		mTextMedia = (TextView) findViewById(R.id.text_media);

		SharedPreferences preferences = getSharedPreferences(VLC.PREFERENCES,
				MODE_PRIVATE);
		changeServer(preferences.getString(VLC.PREFERENCE_SERVER, ""));
		mBrowseDir = preferences.getString(VLC.PREFERENCE_BROWSE_DIRECTORY,
				null);
		mRememberedServers = toArrayList(preferences.getString(
				VLC.PREFERENCE_REMEMBERED_SERVERS, "[]"));
		updateTitle();
		
		mHandler = new Handler(this);

		if (savedInstanceState == null) {
			Intent intent = getIntent();
			processIntent(intent);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_INPUT, mInput);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mInput = savedInstanceState.getString(KEY_INPUT);
	}

	private void processIntent(Intent intent) {
		if (intent != null) {
			String host = intent.getStringExtra(EXTRA_REMOTE_HOST);
			if (host != null) {
				int port = intent.getIntExtra(EXTRA_REMOTE_PORT, 8080);
				String authority = host + ":" + port;
				changeServer(authority);
			}
			
			String action = intent.getAction();
			if (Intent.ACTION_VIEW.equals(action)
					|| ACTION_REMOTE_VIEW.equals(action)
					|| VLC.ACTION_VIEW.equals(action)) {
				Uri data = intent.getData();
				if (data != null) {
					changeInput(data.toString());
				}
			} else if (Intent.ACTION_SEARCH.equals(action)) {
				String input = intent.getStringExtra(SearchManager.QUERY);
				changeInput(input);
			}
		}
	}

	@Override
	public boolean onSearchRequested() {
		String initialQuery = mInput;
		boolean selectInitialQuery = true;
		Bundle appSearchData = null;
		boolean globalSearch = false;
		startSearch(initialQuery, selectInitialQuery, appSearchData,
				globalSearch);
		return true;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		processIntent(intent);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private static void doubleDigit(StringBuilder builder, long value) {
		builder.insert(0, value);
		if (value < 10) {
			builder.insert(0, '0');
		}
	}

	/**
	 * Formats a time.
	 * 
	 * @param time
	 *            the time (in seconds)
	 * @return the formatted time.
	 */
	private String formatTime(int time) {
		long seconds = time % 60;
		time /= 60;
		long minutes = time % 60;
		time /= 60;
		long hours = time;
		StringBuilder builder = new StringBuilder(8);
		doubleDigit(builder, seconds);
		builder.insert(0, ':');
		if (hours == 0) {
			builder.insert(0, minutes);
		} else {
			doubleDigit(builder, minutes);
			builder.insert(0, ':');
			builder.insert(0, hours);
		}
		return builder.toString();
	}

	private void onTimerEvent() {
		if (!isFinishing()) {
			if (mServer != null) {
				startService(mVlc.status());
			}
			mHandler.sendEmptyMessageDelayed(TIMER, 1000);
		}
	}

	private void setStatus(Status status) {
		int resId = status.isPlaying() ? R.drawable.ic_media_playback_pause
				: R.drawable.ic_media_playback_start;
		mButtonPlaylistPause.setImageResource(resId);

		mVolume = status.getVolume();
		mSeekVolume.setProgress(mVolume);
		if (mVolume != 0) {
			mLastNonZeroVolume = mVolume;
		}
		
		int time = status.getTime();
		int length = status.getLength();
		mSeekPosition.setMax(length);
		mSeekPosition.setProgress(time);
		
		// Call setKeyProgressIncrement after calling setMax because the
		// implementation of setMax will automatically adjust the increment.
		mSeekPosition.setKeyProgressIncrement(3);

		String formattedTime = formatTime(time);
		mTextTime.setText(formattedTime);

		String formattedLength = formatTime(length);
		mTextLength.setText(formattedLength);

		Track track = status.getTrack();
		mTextMedia.setText(track.toString());

		String artUrl = track.getArtUrl();
		if (mArtUrl == null || !mArtUrl.equals(artUrl)) {
			mArtUrl = artUrl;
			setAlbumArt(null);
			if (mArtUrl != null && mArtUrl.startsWith("http:")) {
				Intent service = new Intent(VLC.ACTION_ALBUM_ART);
				service.setData(Uri.parse(mArtUrl));
				startService(service);
			}
		}
	}

	private int getVolumeImage() {
		int volume = mSeekVolume.getProgress();
		if (volume == 0) {
			return R.drawable.ic_media_volume_muted;
		} else if (volume < (MAX_VOLUME / 3)) {
			return R.drawable.ic_media_volume_low;
		} else if (volume < (2 * MAX_VOLUME / 3)) {
			return R.drawable.ic_media_volume_medium;
		} else {
			return R.drawable.ic_media_volume_high;
		}
	}

	private void showError(CharSequence message) {
		Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
		toast.show();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mStatusReceiver = new StatusReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(VLC.ACTION_STATUS);
		filter.addAction(VLC.ACTION_ALBUM_ART);
		filter.addAction(VLC.ACTION_EXCEPTION);
		registerReceiver(mStatusReceiver, filter);
		startTimer();
	}

	@Override
	protected void onPause() {
		stopTimer();
		unregisterReceiver(mStatusReceiver);
		mStatusReceiver = null;
		super.onPause();
	}

	private void startTimer() {
		mHandler.sendEmptyMessage(TIMER);
	}

	private void stopTimer() {
		mHandler.removeMessages(TIMER);
	}

	private ImageButton setupImageButton(int viewId) {
		ImageButton button = (ImageButton) findViewById(viewId);
		if (button != null) {
			button.setOnClickListener(this);
		}
		return button;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		int c = event.getUnicodeChar();
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			volume_up();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			volume_down();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
			if (event.isAltPressed()) {
				seek(Uri.encode("-10"));
				return true;
			} else if (event.isShiftPressed()) {
				seek(Uri.encode("-3"));
				return true;
			} else {
				key("nav-left");
				return true;
			}
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			if (event.isAltPressed()) {
				seek(Uri.encode("+10"));
				return true;
			} else if (event.isShiftPressed()) {
				seek(Uri.encode("+3"));
				return true;
			} else {
				key("nav-right");
				return true;
			}
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			key("nav-up");
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			key("nav-down");
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			key("nav-activate");
			return true;
		} else if (c == ' ') {
			pl_pause();
			return true;
		} else if (c == 's') {
			pl_stop();
			return true;
		} else if (c == 'p') {
			pl_previous();
			return true;
		} else if (c == 'n') {
			pl_next();
			return true;
		} else if (c == '+') {
			// TODO: Play faster
			return super.onKeyDown(keyCode, event);
		} else if (c == '-') {
			// TODO: Play slower
			return super.onKeyDown(keyCode, event);
		} else if (c == 'f') {
			fullscreen();
			return true;
		} else if (c == 'm') {
			mute();
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	/** {@inheritDoc} */
	public void onClick(View v) {
		if (v == mButtonPlaylistPause) {
			pl_pause();
		} else if (v == mButtonPlaylistStop) {
			pl_stop();
		} else if (v == mButtonPlaylistSkipBackward) {
			pl_previous();
		} else if (v == mButtonPlaylistSkipForward) {
			pl_next();
		} else if (v == mButtonPlaylistSeekBackward) {
			seek(Uri.encode("-10"));
		} else if (v == mButtonPlaylistSeekForward) {
			seek(Uri.encode("+10"));
		} else if (v == mButtonInput) {
			showBrowser();
		} else if (v == mButtonPlaylistShuffle) {
			pl_shuffle();
		} else if (v == mButtonPlaylistRepeat) {
			pl_repeat();
		} else if (v == mButtonPlaylist) {
			showPlaylist();
		} else if (v == mButtonFullscreen) {
			fullscreen();
		} else if (v == mButtonAspectRatio) {
			command("command=key&val=aspect-ratio");
		} else if (v == mButtonAudioTrack) {
			command("command=key&val=audio-track");
		} else if (v == mButtonSubtitleTrack) {
			command("command=key&val=subtitle-track");
		}
	}

	private void showBrowser() {
		if (isServerSet()) {
			Uri uri = Uri.parse("http://" + mServer + "/requests/browse.xml");
			Uri.Builder builder = uri.buildUpon();
			String dir = mBrowseDir != null ? mBrowseDir : "~";
			builder.appendQueryParameter("dir", dir);
			Uri data = builder.build();

			Intent intent = new Intent(this, BrowseActivity.class);
			intent.setData(data);
			startActivityForResult(intent, REQUEST_BROWSE);
		} else {
			pickServer();
		}
	}
	
	private void showPlaylist() {
		if (isServerSet()) {
			Uri uri = Uri.parse("http://" + mServer + "/requests/playlist.xml");
			Intent intent = new Intent(this, PlaylistActivity.class);
			intent.setData(uri);
			startActivityForResult(intent, REQUEST_PLAYLIST);
		} else {
			pickServer();
		}
	}

	private void pickServer() {
		Intent intent = new Intent(this, PickServerActivity.class);
		intent.putExtra(PickServerActivity.EXTRA_PORT, 8080);
		intent.putExtra(PickServerActivity.EXTRA_FILE, "/requests/status.xml");
		intent.putStringArrayListExtra(PickServerActivity.EXTRA_REMEMBERED,
				mRememberedServers);
		startActivityForResult(intent, REQUEST_PICK_SERVER);
	}

	private void saveBrowseDirectory() {
		SharedPreferences preferences = getSharedPreferences(VLC.PREFERENCES,
				MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		String key = VLC.PREFERENCE_BROWSE_DIRECTORY;
		if (mBrowseDir != null) {
			editor.putString(key, mBrowseDir);
		} else {
			editor.remove(key);
		}
		editor.commit();
	}
	
	private void clearBrowseDirectory() {
		mBrowseDir = null;
		saveBrowseDirectory();
	}
	
	private void clearHomeDirectory() {
		SharedPreferences preferences = getSharedPreferences(VLC.PREFERENCES,
				MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.remove(VLC.PREFERENCE_HOME_DIRECTORY);
		editor.commit();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_BROWSE) {
			if (resultCode == RESULT_OK) {
				String action = data.getAction();
				Uri uri = data.getData();
				mBrowseDir = uri.getQueryParameter("dir");
				String mrl = uri.getQueryParameter("mrl");
				if (VLC.ACTION_ENQUEUE.equals(action)) {
					in_enqueue(mrl);
				} else {
					in_play(mrl);
				}
				Uri stream = data.getParcelableExtra(VLC.EXTRA_STREAM_DATA);
				String type = data.getStringExtra(VLC.EXTRA_STREAM_TYPE);
				if (stream != null) {
				    Intent intent = new Intent(Intent.ACTION_VIEW);
				    intent.setDataAndType(stream, type);
				    startActivity(intent);
				}
				saveBrowseDirectory();
			} else if (resultCode == BrowseActivity.RESULT_DOES_NOT_EXIST) {
				mBrowseDir = null;
				showError(getText(R.string.error_does_not_exist));
			}
		} else if (requestCode == REQUEST_PLAYLIST) {
			if (resultCode == RESULT_OK) {
				Uri uri = data.getData();
				String idString = uri.getQueryParameter("id");
				if (idString != null) {
					int trackId = Integer.parseInt(idString);
					pl_play(trackId);
				}
			}
		} else if (requestCode == REQUEST_PICK_SERVER) {
			boolean serverChanged = false;
			boolean rememberedServersChanged = false;

			if (resultCode == RESULT_OK) {
				String authority = data.getData().getAuthority();
				changeServer(authority);
				serverChanged = true;
			}

			if (data != null) {
				// Update remembered servers even if
				// (resultCode == RESULT_CANCELED)
				ArrayList<String> remembered = data
						.getStringArrayListExtra(PickServerActivity.EXTRA_REMEMBERED);
				if (remembered != null) {
					mRememberedServers = remembered;
					rememberedServersChanged = true;
				}
			}

			if (serverChanged || rememberedServersChanged) {
				SharedPreferences preferences = getSharedPreferences(
						VLC.PREFERENCES, MODE_PRIVATE);
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(VLC.PREFERENCE_SERVER, mServer);
				editor.putString(VLC.PREFERENCE_REMEMBERED_SERVERS,
						toJSONArray(mRememberedServers));
				editor.commit();
			}
		}
	}

	/** {@inheritDoc} */
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if (fromUser) {
			if (seekBar == mSeekPosition) {
				seekPosition();
			} else if (seekBar == mSeekVolume) {
				seekVolume();
			} else {
				throw new IllegalArgumentException("Unexpected SeekBar");
			}
		}
		if (seekBar == mSeekVolume) {
			mImageVolume.setImageResource(getVolumeImage());
		}
	}

	/** {@inheritDoc} */
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	/** {@inheritDoc} */
	public void onStopTrackingTouch(SeekBar seekBar) {
		if (seekBar == mSeekPosition) {
			seekPosition();
		} else if (seekBar == mSeekVolume) {
			seekVolume();
		}
	}

	private void seekPosition() {
		int position = mSeekPosition.getProgress();
		seek(String.valueOf(position));
	}

	private void seekVolume() {
		int value = mSeekVolume.getProgress();
		volume(value);
	}

	private void mute() {
		// The web interface doesn't have a documented mute command.
		if (mVolume != 0) {
			// Set the volume to zero
			volume(0);
		} else {
			// Restore the volume to the last known value
			volume(mLastNonZeroVolume);
		}
	}

	private boolean isServerSet() {
		return mServer != null && mServer.length() != 0;
	}
	
	private void changeServer(String authority) {
		// Do some basic checks to ensure that the caller is not injecting
		// a malicious URI instead of an authority.
		if (authority.indexOf('/') == -1 && authority.indexOf('?') == -1
				&& authority.indexOf('&') == -1 && authority.indexOf('#') == -1) {
			if (isServerSet() && !mServer.equals(authority)) {
				// Clear the browse and home directories when switching servers
				// because the saved directories might not exist on the new
				// server.
				clearHomeDirectory();
				clearBrowseDirectory();
			}
			
			mServer = authority;
			mVlc = new VLC(this, mServer);
			updateTitle();
		}
		if (mVlc == null) {
		  mVlc = new VLC(this, null);
		}
	}
	
	private void command(String query) {
		if (mServer != null) {
			Intent intent = mVlc.command(query);
			intent.putExtra(StatusService.EXTRA_PROGRAMMATIC, false);
			startService(intent);
		} else {
			pickServer();
		}
	}

	private void changeInput(String input) {
		mInput = input;
		if (mInput != null) {
			// Make this a background request to avoid opening the
			// server picker twice when a server has not been set.
			startService(mVlc.command("command=pl_empty"));
			in_play(input);
		}
	}

	private void in_play(String input) {
		command("command=in_play&input=" + Uri.encode(input));
	}

	private void in_enqueue(String input) {
		command("command=in_enqueue&input=" + Uri.encode(input));
	}

	private void pl_play(int id) {
		command("command=pl_play&id=" + id);
	}

	private void pl_pause() {
		command("command=pl_pause");
	}

	private void pl_stop() {
		command("command=pl_stop");
	}

	private void pl_next() {
		command("command=pl_next");
	}

	private void pl_previous() {
		command("command=pl_previous");
	}

	@SuppressWarnings("unused")
	private void pl_delete(int id) {
		command("command=pl_delete&id=" + id);
	}

	@SuppressWarnings("unused")
	private void pl_empty() {
		command("command=pl_empty");
	}

	@SuppressWarnings("unused")
	private void pl_sort(int sort, int order) {
		command("command=pl_sort&id=" + order + "&val="
				+ sort);
	}

	private void pl_shuffle() {
		command("command=pl_random");
	}

	@SuppressWarnings("unused")
	private void pl_loop() {
		command("command=pl_loop");
	}

	private void pl_repeat() {
		command("command=pl_repeat");
	}

	@SuppressWarnings("unused")
	private void pl_sd(String value) {
		command("command=pl_sd&val=" + value);
	}

	private void volume(int value) {
		command("command=volume&val=" + value);
	}

	private void volume_down() {
		command("command=volume&val=-20");
	}

	private void volume_up() {
		command("command=volume&val=%2B20");
	}

	private void seek(String pos) {
		command("command=seek&val=" + pos);
	}

	private void key(String keycode) {
		command("command=key&val=" + keycode);
	}

	private void fullscreen() {
		command("command=fullscreen");
	}

	@SuppressWarnings("unused")
	private void snapshot() {
		command("command=snapshot");
	}

	@SuppressWarnings("unused")
	private void hotkey(String str) {
		/*
		 * Use hotkey name (without the "key-" part) as the argument to simulate
		 * a hotkey press
		 */
		command("command=key&val=" + str);
	}

	private void setAlbumArt(Bitmap bitmap) {
		if (mImageAlbumArt != null) {
			if (bitmap != null) {
				mImageAlbumArt.setImageBitmap(bitmap);
			} else {
				mImageAlbumArt.setImageResource(R.drawable.albumart_mp_unknown);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem settings = menu.add(Menu.NONE, MENU_PREFERENCES, Menu.NONE,
				R.string.settings);
		settings.setIcon(android.R.drawable.ic_menu_preferences);
		MenuItem help = menu.add(Menu.NONE, MENU_HELP, Menu.NONE,
				R.string.troubleshooting);
		help.setIcon(android.R.drawable.ic_menu_help);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_PREFERENCES:
			pickServer();
			return true;
		case MENU_HELP:
			Intent intent = new Intent(Intent.ACTION_VIEW, URI_TROUBLESHOOTING);
			intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void updateTitle() {
		String template = getString(R.string.title);
		Object[] objects = { mServer };
		String title = MessageFormat.format(template, objects);
		setTitle(title);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case STATUS:
			Status status = (Status) msg.obj;
			setStatus(status);
			return true;
		case ALBUMART:
			Bitmap bitmap = (Bitmap) msg.obj;
			setAlbumArt(bitmap);
			return true;
		case ERROR:
			Throwable t = (Throwable) msg.obj;
			boolean programmatic = (msg.arg1 != 0);
			if (!programmatic) {
				CharSequence message = t.getMessage();
				if (message == null) {
					message = String.valueOf(t);
				}
				showError(message);
			}
			return true;
		case TIMER:
			onTimerEvent();
			return true;
		default:
			return false;
		}
	}
	
	private class StatusReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (VLC.ACTION_STATUS.equals(action)) {
				Status status = (Status) intent
						.getSerializableExtra(VLC.EXTRA_STATUS);
				mHandler.obtainMessage(STATUS, status).sendToTarget();
			} else if (VLC.ACTION_ALBUM_ART.equals(action)) {
				Bitmap bitmap = intent.getParcelableExtra(VLC.EXTRA_ALBUM_ART);
				mHandler.obtainMessage(ALBUMART, bitmap).sendToTarget();
			} else if (VLC.ACTION_EXCEPTION.equals(action)) {
				Throwable t = (Throwable) intent
						.getSerializableExtra(VLC.EXTRA_EXCEPTION);
				if (t == null) {
					String message = intent
							.getStringExtra(VLC.EXTRA_EXCEPTION_MESSAGE);
					t = new Exception(message);
				}
				boolean programmatic = intent.getBooleanExtra(
						StatusService.EXTRA_PROGRAMMATIC, true);
				mHandler.obtainMessage(ERROR, programmatic ? 1 : 0, 0, t)
						.sendToTarget();
			}
		}
	}
}
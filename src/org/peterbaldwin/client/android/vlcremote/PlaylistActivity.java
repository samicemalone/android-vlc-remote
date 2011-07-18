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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import android.app.AlarmManager;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class PlaylistActivity extends ListActivity implements
		View.OnClickListener {
	
	private ListView mListView;
	
	private TextView mEmptyView;
	
	private ImageButton mButtonShuffle;
	
	private ImageButton mButtonRepeat;
	
	private TrackArrayAdapter mAdapter;
	
	private VLC mVlc;

	private BroadcastReceiver mStatusReceiver;
	
	private boolean mRandom;
	
	private boolean mRepeat;
	
	private boolean mLoop;

	@Override
	@SuppressWarnings("unchecked")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Window window = getWindow();
		window.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.playlist);

		mAdapter = new TrackArrayAdapter();
		setListAdapter(mAdapter);
		
		mListView = getListView();
		registerForContextMenu(mListView);
		
		mEmptyView = (TextView) findViewById(android.R.id.empty);
		mButtonShuffle = (ImageButton) findViewById(R.id.playlist_button_shuffle);
		mButtonRepeat = (ImageButton) findViewById(R.id.playlist_button_repeat);
		
		mButtonShuffle.setOnClickListener(this);
		mButtonRepeat.setOnClickListener(this);
		
		String server = getIntent().getData().getAuthority();
		mVlc = new VLC(this, server);
		
		List<Track> items = (List<Track>) getLastNonConfigurationInstance();
		if (items != null) {
			mAdapter.setItems(items);
		} else {
			updateData();
		}
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		if (mAdapter.getCount() != 0) {
			return mAdapter.getItems();
		} else {
			return null;
		}
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
		command("");
	}
	
	@Override
	protected void onPause() {
		unregisterReceiver(mStatusReceiver);
		mStatusReceiver = null;
		super.onPause();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.playlist_options, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_clear_playlist:
			removeAllTracks();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (menuInfo instanceof AdapterContextMenuInfo) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
			Track track = mAdapter.getItem(info.position);

			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.playlist_context, menu);

			MenuItem searchItem = menu.findItem(R.id.context_search);
			boolean hasTitle = !TextUtils.isEmpty(track.getTitle());
			boolean hasArtist = !TextUtils.isEmpty(track.getArtist());
			searchItem.setVisible(hasTitle && hasArtist);
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ContextMenuInfo menuInfo = item.getMenuInfo();
		if (menuInfo instanceof AdapterContextMenuInfo) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
			Track track = mAdapter.getItem(info.position);
			switch (item.getItemId()) {
			case R.id.context_play:
				selectTrack(track);
				return true;
			case R.id.context_dequeue:
				removeTrack(track);
				return true;
			case R.id.context_search:
				searchForTrack(track);
				return true;
			default:
				return super.onContextItemSelected(item);
			}
		} else {
			return super.onContextItemSelected(item);
		}
	}
	
	private void removeTrack(Track track) {
		int id = track.getId();
		PlaylistTask task = new PlaylistTask(true);
		Intent intent = getIntent();
		Uri playlistUri = intent.getData();
		Uri.Builder builder = playlistUri.buildUpon();
		builder.encodedPath("/requests/status.xml");
		builder.appendQueryParameter("command", "pl_delete");
		builder.appendQueryParameter("id", String.valueOf(id));
		Uri commandUri = builder.build();
		task.execute(commandUri, playlistUri);
	}
	
	private void removeAllTracks() {
		PlaylistTask task = new PlaylistTask(false);
		Intent intent = getIntent();
		Uri playlistUri = intent.getData();
		Uri.Builder builder = playlistUri.buildUpon();
		builder.encodedPath("/requests/status.xml");
		builder.appendQueryParameter("command", "pl_empty");
		Uri commandUri = builder.build();
		task.execute(commandUri, playlistUri);
	}
	
	private void searchForTrack(Track track) {
		String title = track.getTitle();
		String artist = track.getArtist();
		String query = artist + " " + title;

		Intent intent = new Intent(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
		intent.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist);
		intent.putExtra(MediaStore.EXTRA_MEDIA_TITLE, title);
		intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "audio/*");
		intent.putExtra(SearchManager.QUERY, query);

		String chooserTitle = getString(R.string.mediasearch, title);
		startActivity(Intent.createChooser(intent, chooserTitle));
	}

	private void updateData() {
		PlaylistTask task = new PlaylistTask(false);
		Intent intent = getIntent();
		Uri uri = intent.getData();
		task.execute(uri);
	}

	private void showError(CharSequence message) {
		Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
		toast.show();
	}
	
	private void selectTrack(Track track) {
		String trackId = String.valueOf(track.getId());
		Uri uri = getIntent().getData();
		uri = uri.buildUpon().appendQueryParameter("id", trackId).build();
		Intent data = new Intent();
		data.setData(uri);
		setResult(RESULT_OK, data);
		finish();
	}
	
	private void command(String query) {
		startService(mVlc.command(query));
	}
	
	private void delayedCommand(String query) {
		AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
		long triggerAtTime = SystemClock.elapsedRealtime() + 500;
		PendingIntent operation = mVlc.pendingCommand(query);
		manager.set(AlarmManager.ELAPSED_REALTIME, triggerAtTime, operation);
	}
	
	/** {@inheritDoc} */
	public void onClick(View v) {
		if (v == mButtonShuffle) {
			command("command=pl_random");
			mRandom = !mRandom;
		} else {
			// Order: Normal -> Repeat -> Loop
			if (mRepeat) {
				// Switch to loop
				if (mLoop) {
					// Turn-off repeat
					command("command=pl_repeat");
					mRepeat = false;
				} else {
					// Manual transition:
					// This needs to be a two step process
					// because the commands will conflict
					// if they are issued too close together.
					// The transition is optimized when
					// switching from normal mode to repeat
					// to avoid the two step process when possible.
					// The UI is not updated until the
					// server responds to hide the
					// intermediate state.
					
					// Turn-on loop
					command("command=pl_loop");
					
					// Turn-off repeat shortly after.
					delayedCommand("command=pl_repeat");
				}
			} else if (mLoop) {
				// Switch to normal
					
				// Turn-off loop
				command("command=pl_loop");
				mLoop = false;
			} else {
				// Turn-on repeat
				command("command=pl_repeat");
				mRepeat = true;

				// Turn-on loop to make the transition 
				// from repeat to loop one step
				// instead of two steps.
				// Loop has no effect when repeat is on.
				delayedCommand("command=pl_loop");
				mLoop = true;
			}
		}
		updateButtons();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Track track = (Track) l.getItemAtPosition(position);
		selectTrack(track);
	}
	
	int getShuffleResId() {
		if (mRandom) {
			return R.drawable.ic_mp_shuffle_on_btn;
		} else {
			return R.drawable.ic_mp_shuffle_off_btn;
		}
	}
	
	int getRepeatResId() {
		if (mRepeat) {
			return R.drawable.ic_mp_repeat_once_btn;
		} else if (mLoop) {
			return R.drawable.ic_mp_repeat_all_btn;
		} else {
			return R.drawable.ic_mp_repeat_off_btn;
		}
	}
	
	void updateButtons() {
		mButtonShuffle.setImageResource(getShuffleResId());
		mButtonRepeat.setImageResource(getRepeatResId());
	}

	void selectCurrentTrack() {
		final int count = mAdapter.getCount();
		for (int position = 0; position < count; position++) {
			Track track = mAdapter.getItem(position);
			if (track.isCurrent()) {
				// Scroll to current track
				ListView listView = getListView();
				listView.setSelection(position);
				break;
			}
		}
	}
	
	private class StatusReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			mRandom = intent.getBooleanExtra(VLC.EXTRA_RANDOM, false);
			mLoop = intent.getBooleanExtra(VLC.EXTRA_LOOP, false);
			mRepeat = intent.getBooleanExtra(VLC.EXTRA_REPEAT, false);
			updateButtons();
		}
	}

	private static class TrackArrayAdapter extends BaseAdapter {
		
		private List<Track> mItems;
		
		/** {@inheritDoc} */
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				Context context = parent.getContext();
				LayoutInflater inflater = LayoutInflater.from(context);
				view = inflater.inflate(R.layout.playlist_list_item, parent, false);
			}
			TextView text1 = (TextView) view.findViewById(android.R.id.text1);
			TextView text2 = (TextView) view.findViewById(android.R.id.text2);
			View icon = view.findViewById(android.R.id.icon);
			Track track = getItem(position);
			if (!TextUtils.isEmpty(track.getTitle())) {
				text1.setText(track.getTitle());
				text2.setText(track.getArtist());
			} else {
				text1.setText(track.getName());
				text2.setText("");
			}
			icon.setVisibility(track.isCurrent() ? View.VISIBLE : View.GONE);
			return view;
		}

		/** {@inheritDoc} */
		public int getCount() {
			return mItems != null ? mItems.size() : 0;
		}

		/** {@inheritDoc} */
		public Track getItem(int position) {
			return mItems.get(position);
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}
		
		/** {@inheritDoc} */
		public long getItemId(int position) {
			if (position < getCount()) {
				Track track = getItem(position);
				return track.getId();
			} else {
				return AdapterView.INVALID_ROW_ID;
			}
		}
		
		public void setItems(List<Track> items) {
			mItems = items;
			if (mItems != null) {
				notifyDataSetChanged();
			} else {
				notifyDataSetInvalidated();
			}
		}

		public List<Track> getItems() {
			int count = getCount();
			List<Track> items = new ArrayList<Track>(count);
			for (int position = 0; position < count; position++) {
				Track item = getItem(position);
				items.add(item);
			}
			return items;
		}
	}

	private class PlaylistTask extends AsyncTask<Uri, Integer, Playlist> {
		
		private static final String TAG = "PlaylistTask";
		
		private final boolean mRefresh;

		private Throwable mError;

		public PlaylistTask(boolean refresh) {
			mRefresh = refresh;
		}
		
		private boolean isRefresh() {
			return mRefresh;
		}

		@Override
		protected void onPreExecute() {
			Window window = getWindow();
			window.setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
					Window.PROGRESS_VISIBILITY_ON);
			mEmptyView.setText(R.string.loading);
		}

		@Override
		protected Playlist doInBackground(Uri... params) {
			try {
				for (int i = 0; i < params.length; i++) {
					Uri uri = params[i];
					String uriString = uri.toString();
					URL url = new URL(uriString);
					InputStream in = url.openStream();
					try {
						boolean last = (i == (params.length - 1)); 
						if (last) {
							PlaylistResponseHandler handler = new PlaylistResponseHandler();
							ContentHandler contentHandler = handler
									.getContentHandler();
							Xml.parse(in, Xml.Encoding.UTF_8, contentHandler);
							return (Playlist) handler.getResponse();
						} else {
							while (in.read() != -1) {
								// Consume
							}
						}
					} finally {
						in.close();
					}
				}
			} catch (IOException e) {
				mError = e;
			} catch (SAXException e) {
				// Ignore XML parsing exceptions because VLC server often
				// returns a malformed XML response when the playlist is empty.
				Log.w(TAG, "Parser error", e);
				mError = null;
			} catch (RuntimeException e) {
				mError = e;
			} catch (Error e) {
				mError = e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Playlist result) {
			Window window = getWindow();
			window.setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
					Window.PROGRESS_VISIBILITY_OFF);
			mEmptyView.setText(R.string.emptyplaylist);
			if (mError == null) {
				if (result != null) {
					mAdapter.setItems(result);
					if (isRefresh()) {
						// Don't change scroll position if refreshing
					} else {
						// Scroll to the currently playing track
						selectCurrentTrack();
					}
				}
			} else {
				CharSequence message = String.valueOf(mError);
				showError(message);
			}
		}
	}
}

/*-
 *  Copyright (C) 2011 Peter Baldwin   
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

package org.peterbaldwin.vlcremote.fragment;

import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.intent.Intents;
import org.peterbaldwin.vlcremote.loader.PlaylistLoader;
import org.peterbaldwin.vlcremote.model.Playlist;
import org.peterbaldwin.vlcremote.model.PlaylistItem;
import org.peterbaldwin.vlcremote.model.Remote;
import org.peterbaldwin.vlcremote.model.Status;
import org.peterbaldwin.vlcremote.model.Track;
import org.peterbaldwin.vlcremote.net.MediaServer;
import org.peterbaldwin.vlcremote.widget.PlaylistAdapter;

import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PlaylistFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Remote<Playlist>> {

    private static final int LOADER_PLAYLIST = 1;

    private Context mContext;

    private MediaServer mMediaServer;

    private TextView mEmptyView;

    private PlaylistAdapter mAdapter;

    private BroadcastReceiver mStatusReceiver;

    private String mCurrent;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.playlist, container, false);

        mAdapter = new PlaylistAdapter();
        setListAdapter(mAdapter);

        mEmptyView = (TextView) view.findViewById(android.R.id.empty);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        registerForContextMenu(getListView());

        if (mMediaServer != null) {
            getLoaderManager().initLoader(LOADER_PLAYLIST, null, this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mStatusReceiver = new StatusReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.ACTION_STATUS);
        getActivity().registerReceiver(mStatusReceiver, filter);
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(mStatusReceiver);
        mStatusReceiver = null;
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.playlist_options, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                reload();
                return true;
            case R.id.menu_clear_playlist:
                mMediaServer.status().command.playback.empty();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (menuInfo instanceof AdapterContextMenuInfo) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            PlaylistItem item = mAdapter.getItem(info.position);

            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.playlist_context, menu);

            MenuItem searchItem = menu.findItem(R.id.context_search);
            searchItem.setVisible(isSearchable(item));
        }
    }

    private boolean isSearchable(PlaylistItem item) {
        if (item instanceof Track) {
            Track track = (Track) item;
            boolean hasTitle = !TextUtils.isEmpty(track.getTitle());
            boolean hasArtist = !TextUtils.isEmpty(track.getArtist());
            return hasTitle && hasArtist;
        } else {
            return false;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        ContextMenuInfo menuInfo = menuItem.getMenuInfo();
        if (menuInfo instanceof AdapterContextMenuInfo) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            if (info.position < mAdapter.getCount()) {
                PlaylistItem item = mAdapter.getItem(info.position);
                switch (menuItem.getItemId()) {
                    case R.id.context_play:
                        selectItem(item);
                        return true;
                    case R.id.context_dequeue:
                        removeItem(item);
                        return true;
                    case R.id.context_search:
                        searchForItem(item);
                        return true;
                }
            }
        }
        return super.onContextItemSelected(menuItem);
    }

    public void setMediaServer(MediaServer mediaServer) {
        mMediaServer = mediaServer;
        reload();
    }

    private void removeItem(PlaylistItem item) {
        int id = item.getId();
        // TODO: Register observer and notify observers when playlist item is
        // deleted
        mMediaServer.status().command.playback.delete(id);
    }

    private void searchForItem(PlaylistItem item) {
        if (item instanceof Track) {
            Track track = (Track) item;
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
    }

    private void showError(CharSequence message) {
        Context context = getActivity();
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.show();
    }

    private void selectItem(PlaylistItem item) {
        mMediaServer.status().command.playback.play(item.getId());
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        PlaylistItem item = (PlaylistItem) l.getItemAtPosition(position);
        selectItem(item);
    }

    public void selectCurrentTrack() {
        final int count = mAdapter.getCount();
        for (int position = 0; position < count; position++) {
            PlaylistItem item = mAdapter.getItem(position);
            if (item instanceof Track) {
                Track track = (Track) item;
                if (track.isCurrent()) {
                    // Scroll to current track
                    ListView listView = getListView();
                    listView.setSelection(position);
                    break;
                }
            }
        }
    }

    @Override
    public void setEmptyText(CharSequence text) {
        mEmptyView.setText(text);
    }

    /** {@inheritDoc} */
    public Loader<Remote<Playlist>> onCreateLoader(int id, Bundle args) {
        setEmptyText(getText(R.string.loading));
        String search = "";
        return new PlaylistLoader(mContext, mMediaServer, search);
    }

    /** {@inheritDoc} */
    public void onLoadFinished(Loader<Remote<Playlist>> loader, Remote<Playlist> remote) {
        boolean wasEmpty = mAdapter.isEmpty();
        boolean hasError = (remote.error != null);

        mAdapter.setItems(remote.data);

        if (hasError) {
            setEmptyText(getText(R.string.connection_error));
            showError(String.valueOf(remote.error));
        } else {
            setEmptyText(getText(R.string.emptyplaylist));
        }

        if (wasEmpty) {
            selectCurrentTrack();
        }
    }

    /** {@inheritDoc} */
    public void onLoaderReset(Loader<Remote<Playlist>> loader) {
        mAdapter.setItems(null);
    }

    void onStatusChanged(Status status) {
        String title = status.getTrack().getTitle();
        if (!TextUtils.equals(title, mCurrent)) {
            // Reload the playlist and scroll to the new current track
            mCurrent = title;
            reload();
        }
    }

    public void reload() {
        if (mMediaServer != null) {
            getLoaderManager().restartLoader(LOADER_PLAYLIST, null, this);
        }
    }

    private class StatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Status status = (Status) intent.getSerializableExtra(Intents.EXTRA_STATUS);
            onStatusChanged(status);
        }
    }
}

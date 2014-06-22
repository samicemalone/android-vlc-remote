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

import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.LinkedList;
import java.util.Queue;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.app.PlaybackActivity;
import org.peterbaldwin.vlcremote.intent.Intents;
import org.peterbaldwin.vlcremote.listener.ProgressListener;
import org.peterbaldwin.vlcremote.loader.PlaylistLoader;
import org.peterbaldwin.vlcremote.model.Playlist;
import org.peterbaldwin.vlcremote.model.PlaylistItem;
import org.peterbaldwin.vlcremote.model.Reloadable;
import org.peterbaldwin.vlcremote.model.Reloader;
import org.peterbaldwin.vlcremote.model.Remote;
import org.peterbaldwin.vlcremote.model.Status;
import org.peterbaldwin.vlcremote.model.Tags;
import org.peterbaldwin.vlcremote.model.Track;
import org.peterbaldwin.vlcremote.net.MediaServer;
import org.peterbaldwin.vlcremote.widget.PlaylistAdapter;

public class PlaylistFragment extends MediaListFragment implements SearchView.OnQueryTextListener,
        LoaderManager.LoaderCallbacks<Remote<Playlist>>, Reloadable, ProgressListener {
    
    private static final int LOADER_PLAYLIST = 1;

    private TextView mEmptyView;

    private PlaylistAdapter mAdapter;

    private BroadcastReceiver mStatusReceiver;

    private String mCurrent;
    
    private String mSearchQuery = "";
    
    private Queue<PlaylistLoader> mActiveLoaders;
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Reloader) activity).addReloadable(Tags.FRAGMENT_PLAYLIST, this);
        ((PlaybackActivity) activity).setSearchViewOnQueryTextListener(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActiveLoaders = new LinkedList<PlaylistLoader>();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.playlist, container, false);

        mAdapter = new PlaylistAdapter();
        if(savedInstanceState != null && savedInstanceState.containsKey("adapter")) {
            if(savedInstanceState.containsKey("adapter")) {
                mAdapter = (PlaylistAdapter) savedInstanceState.getSerializable("adapter");
            }
            mCurrent = savedInstanceState.getString("current");
        } 
        setListAdapter(mAdapter);

        mEmptyView = (TextView) view.findViewById(android.R.id.empty);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        registerForContextMenu(getListView());

        if (getMediaServer() != null && savedInstanceState == null) {
            getLoaderManager().initLoader(LOADER_PLAYLIST, null, this);
        } else {
            onProgress(10000);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                reload();
                return true;
            case R.id.menu_clear_playlist:
                getMediaServer().status().command.playback.empty();
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

            MenuItem searchItem = menu.findItem(R.id.playlist_context_search);
            searchItem.setVisible(isSearchable(item));
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mSearchQuery = query;
        reload();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if(!newText.equals(mSearchQuery)) {
            mSearchQuery = newText;
            reload();
        }
        return true;
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
                    case R.id.playlist_context_play:
                        selectItem(item);
                        return true;
                    case R.id.playlist_context_dequeue:
                        removeItem(item, info.position);
                        return true;
                    case R.id.playlist_context_search:
                        searchForItem(item);
                        return true;
                }
            }
        }
        return super.onContextItemSelected(menuItem);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("adapter", mAdapter);
        outState.putString("current", mCurrent);
    }

    @Override
    public void onNewMediaServer(MediaServer server) {
        // onActivityCreated will not have been called yet on first use
        boolean isFirstUse = getMediaServer() == null;
        super.onNewMediaServer(server);
        if(!isFirstUse) {
            reload();
        }
    }

    private void removeItem(PlaylistItem item, int position) {
        int id = item.getId();
        // TODO: Register observer and notify observers when playlist item is
        // deleted
        mAdapter.remove(position);
        getMediaServer().status().command.playback.delete(id);
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
        getMediaServer().status().command.playback.play(item.getId());
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        PlaylistItem item = (PlaylistItem) l.getItemAtPosition(position);
        selectItem(item);
        mAdapter.setCurrentItem(position);
    }

    public void selectCurrentTrack() {
        if(mAdapter.getCurrentItems() != null && !mAdapter.getCurrentItems().isEmpty()) {
            int pos = mAdapter.getCurrentItems().iterator().next();
            getListView().setSelection(pos);
        }
    }

    @Override
    public void setEmptyText(CharSequence text) {
        if(mEmptyView != null) {
            mEmptyView.setText(text);
        }
    }

    @Override
    public void onProgress(final int progress) {
        if(getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(getActivity() != null) {
                        getActivity().getWindow().setFeatureInt(Window.FEATURE_PROGRESS, progress);
                    }
                }
            });
        }
    }

    @Override
    public Loader<Remote<Playlist>> onCreateLoader(int id, Bundle args) {
        setEmptyText(getText(R.string.loading));
        String search = mSearchQuery == null ? "" : mSearchQuery;
        mActiveLoaders.offer(new PlaylistLoader(getActivity(), getMediaServer(), search, this));
        return mActiveLoaders.peek();
    }

    @Override
    public void onLoadFinished(Loader<Remote<Playlist>> loader, Remote<Playlist> remote) {
        if(remote == null) {
            return;
        }

        mAdapter.setItems(remote.data);
        selectCurrentTrack();

        if (remote.error != null) {
            setEmptyText(getText(R.string.connection_error));
            showError(String.valueOf(remote.error));
        } else {
            setEmptyText(getText(R.string.emptyplaylist));
        }
    }

    @Override
    public void onLoaderReset(Loader<Remote<Playlist>> loader) {
        mAdapter.setItems(null);
    }

    void onStatusChanged(Status status) {
        String filePath = status.getTrack().getName() == null ? status.getTrack().getTitle() : status.getTrack().getName();
        if (!TextUtils.equals(filePath, mCurrent)) {
            // Reload the playlist and scroll to the new current track
            mCurrent = filePath;
            reload();
        }
    }
    
    private void reload() {
        reload(null);
    }

    public void reload(Bundle args) {
        if (getActivity() != null && getMediaServer() != null) {
            PlaylistLoader loader;
            while((loader = mActiveLoaders.poll()) != null) {
                loader.cancelBackgroundLoad();
            }
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

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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.app.EnqueueObservable;
import org.peterbaldwin.vlcremote.app.EnqueueObserver;
import org.peterbaldwin.vlcremote.loader.DirectoryLoader;
import org.peterbaldwin.vlcremote.model.Directory;
import org.peterbaldwin.vlcremote.model.File;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.model.Remote;
import org.peterbaldwin.vlcremote.net.MediaServer;
import org.peterbaldwin.vlcremote.widget.DirectoryAdapter;

public class BrowseFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Remote<Directory>>, EnqueueObservable {
    
    private interface Data {
        int DIRECTORY = 1;
    }

    private interface State {
        String DIRECTORY = "vlc:directory";
    }
    
    private List<EnqueueObserver> observers;

    private DirectoryAdapter mAdapter;

    private MediaServer mMediaServer;

    private String mDirectory = "~";

    private Preferences mPreferences;

    private TextView mTitle;

    private TextView mEmpty;

    public void registerObserver(EnqueueObserver o) {
        if(observers == null) {
            observers = new LinkedList<EnqueueObserver>();
        }
        observers.add(o);
    }

    public void notifyEnqueue() {
        for (Iterator<EnqueueObserver> it = observers.iterator(); it.hasNext();) {
            it.next().onEnqueue();
        }
    }
    
    public void notifyPlaylistVisible() {
        for (Iterator<EnqueueObserver> it = observers.iterator(); it.hasNext();) {
            it.next().onPlaylistVisible();
        }
    }

    public void unregisterObserver(EnqueueObserver o) {
        observers.remove(o);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Context context = getActivity();
        mPreferences = Preferences.get(context);
        if (savedInstanceState == null) {
            mDirectory = mPreferences.getBrowseDirectory();
        } else {
            mDirectory = savedInstanceState.getString(State.DIRECTORY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.browse, root, false);
        mTitle = (TextView) view.findViewById(android.R.id.title);
        mEmpty = (TextView) view.findViewById(android.R.id.empty);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(State.DIRECTORY, mDirectory);
    }

    public void setMediaServer(MediaServer mediaServer) {
        mMediaServer = mediaServer;
    }

    @Override
    public void setEmptyText(CharSequence text) {
        mEmpty.setText(text);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Context context = getActivity();
        mAdapter = new DirectoryAdapter(context);
        setListAdapter(mAdapter);
        registerForContextMenu(getListView());
        if (mMediaServer != null) {
            getLoaderManager().initLoader(Data.DIRECTORY, Bundle.EMPTY, this);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        File file = mAdapter.getItem(position);
        if (file.isDirectory()) {
            openDirectory(file);
        } else {
            mMediaServer.status().command.input.play(file.getMrl(), file.getOptions());
        }
    }

    public void reload() {
        openDirectory(mDirectory);
    }
    
    private void openDirectory(File file) {
        openDirectory(file.getNormalizedPath());
    }

    public void openDirectory(String path) {
        mDirectory = path;
        mAdapter.clear();
        getLoaderManager().restartLoader(Data.DIRECTORY, null, this);
    }

    private boolean isDirectory(ContextMenuInfo menuInfo) {
        if (menuInfo instanceof AdapterContextMenuInfo) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            if (info.position < mAdapter.getCount()) {
                File file = mAdapter.getItem(info.position);
                return file.isDirectory();
            }
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.browse_options, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                getLoaderManager().restartLoader(Data.DIRECTORY, Bundle.EMPTY, this);
                return true;
            case R.id.menu_parent:
                openParentDirectory();
                return true;
            case R.id.menu_home:
                mDirectory = mPreferences.getHomeDirectory();
                getLoaderManager().restartLoader(Data.DIRECTORY, Bundle.EMPTY, this);
                return true;
            case R.id.menu_set_home:
                mPreferences.setHomeDirectory(mDirectory);
                showSetHomeToast();
                return true;
            case R.id.menu_size_large:
                mPreferences.setTextSize(Preferences.TEXT_LARGE);
                mAdapter.notifyDataSetChanged();
                return true;
            case R.id.menu_size_medium:
                mPreferences.setTextSize(Preferences.TEXT_MEDIUM);
                mAdapter.notifyDataSetChanged();
                return true;
            case R.id.menu_size_small:
                mPreferences.setTextSize(Preferences.TEXT_SMALL);
                mAdapter.notifyDataSetChanged();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openParentDirectory() {
        for (int position = 0, n = mAdapter.getCount(); position < n; position++) {
            File file = mAdapter.getItem(position);
            if (file.isParent()) {
                openDirectory(file);
                return;
            }
        }
        if(!mAdapter.isEmpty()) { // Open the root directory if no parent.
            openDirectory(Directory.ROOT_DIRECTORY);
        }
    }

    private void showSetHomeToast() {
        Context context = getActivity();
        CharSequence message = getString(R.string.sethome, getTitle());
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.browse_context, menu);
        menu.findItem(R.id.browse_context_open).setVisible(isDirectory(menuInfo));
        menu.findItem(R.id.browse_context_stream).setVisible(!isDirectory(menuInfo));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ContextMenuInfo menuInfo = item.getMenuInfo();
        if (menuInfo instanceof AdapterContextMenuInfo) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            if (info.position < mAdapter.getCount()) {
                File file = mAdapter.getItem(info.position);
                switch (item.getItemId()) {
                    case R.id.browse_context_open:
                        openDirectory(file);
                        return true;
                    case R.id.browse_context_play:
                        mMediaServer.status().command.input.play(file.getMrl(), file.getOptions());
                        return true;
                    case R.id.browse_context_stream:
                        mMediaServer.status().command.input.play(file.getMrl(),
                                file.getStreamingOptions());
                        Intent intent = file.getIntentForStreaming(mMediaServer.getAuthority());
                        startActivity(intent);
                        return true;
                    case R.id.browse_context_enqueue:
                        mMediaServer.status().command.input.enqueue(file.getMrl());
                        notifyEnqueue();
                        return true;
                }
            }
        }
        return super.onContextItemSelected(item);
    }

    /** {@inheritDoc} */
    public Loader<Remote<Directory>> onCreateLoader(int id, Bundle args) {
        Context context = getActivity();
        mPreferences.setBrowseDirectory(mDirectory);
        setEmptyText(getText(R.string.loading));
        return new DirectoryLoader(context, mMediaServer, mDirectory);
    }

    /** {@inheritDoc} */
    public void onLoadFinished(Loader<Remote<Directory>> loader, Remote<Directory> result) {
        mAdapter.setDirectory(result.data);
        setEmptyText(getText(R.string.connection_error));
        setTitle(result.data != null ? result.data.getPath() : null);
        boolean isXMLError = result.error != null && "Invalid XML".equals(result.error.getMessage());
        if (isEmptyDirectory(result.data) || isXMLError) {
            handleEmptyDirectory();
        }
    }

    private void setTitle(CharSequence title) {
        mTitle.setText(title);
    }

    public CharSequence getTitle() {
        return mTitle.getText();
    }

    private boolean isEmptyDirectory(Directory directory) {
        return directory != null ? directory.isEmpty() : false;
    }

    private void handleEmptyDirectory() {
        showEmptyDirectoryError();
        openDirectory(File.getNormalizedPath(mDirectory.concat("/..")));
    }

    private void showEmptyDirectoryError() {
        Toast.makeText(getActivity(), R.string.browse_empty, Toast.LENGTH_LONG).show();
    }

    /** {@inheritDoc} */
    public void onLoaderReset(Loader<Remote<Directory>> loader) {
        mAdapter.setDirectory(null);
    }

    // TODO: Automatically reload directory when connection is restored
}

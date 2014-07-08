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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.loader.DirectoryLoader;
import org.peterbaldwin.vlcremote.loader.LibraryDirectoryLoader;
import org.peterbaldwin.vlcremote.loader.LibraryNameLoader;
import org.peterbaldwin.vlcremote.model.Directory;
import org.peterbaldwin.vlcremote.model.File;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.model.Reloadable;
import org.peterbaldwin.vlcremote.model.Reloader;
import org.peterbaldwin.vlcremote.model.Remote;
import org.peterbaldwin.vlcremote.model.Tags;
import org.peterbaldwin.vlcremote.net.xml.XmlContentHandler;
import org.peterbaldwin.vlcremote.widget.DirectoryAdapter;

public class BrowseFragment extends MediaListFragment implements
        LoaderManager.LoaderCallbacks<Remote<Directory>>, Reloadable {
    
    private interface Data {
        int DIRECTORY = 2;
        int LIBRARIES = 3;
        int LIBRARY_DIRECTORY = 4;
    }

    public interface State {
        final String DIRECTORY = "vlc:directory";
    }

    private DirectoryAdapter mAdapter;

    private String mDirectory = "~";

    private Preferences mPreferences;

    private TextView mTitle;

    private TextView mEmpty;
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Reloader) activity).addReloadable(Tags.FRAGMENT_BROWSE, this);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mPreferences = Preferences.get(getActivity());
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
    
    @Override
    public void setEmptyText(CharSequence text) {
        mEmpty.setText(text);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new DirectoryAdapter(getActivity());
        setListAdapter(mAdapter);
        registerForContextMenu(getListView());
        if (getMediaServer() != null) {
            getLoaderManager().initLoader(getDirectoryType(mDirectory), Bundle.EMPTY, this);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        File file = mAdapter.getItem(position);
        if (file.isDirectory()) {
            openDirectory(file.getNormalizedPath());
        } else if(file.isLibrary()) {
            openDirectory(file.getNormalizedPath(), Data.LIBRARIES);
        } else if(file.isLibraryName() || file.isLibraryDir()) {
            openDirectory(file.getNormalizedPath(), Data.LIBRARY_DIRECTORY);
        } else {
            getMediaServer().status().command.input.play(file.getMrl(), file.getOptions());
        }
    }

    @Override
    public void reload(Bundle args) {
        if(getActivity() != null) {
            String dir = args != null && args.containsKey(State.DIRECTORY) ? args.getString(State.DIRECTORY) : mDirectory;
            openDirectory(dir, getDirectoryType(dir));
        }
    }
    
    private int getDirectoryType(String path) {
        if("library://".equals(path)) {
            return Data.LIBRARIES;
        } else if(path.startsWith("library://")) {
            return Data.LIBRARY_DIRECTORY;
        }
        return Data.DIRECTORY;
    }
    
    private void openDirectory(File file) {
        String path = file.getNormalizedPath();
        openDirectory(path, getDirectoryType(path));
    }
    
    private void openDirectory(String path, int directoryType) {
        mDirectory = path;
        mAdapter.clear();
        getLoaderManager().restartLoader(directoryType, Bundle.EMPTY, this);
    }

    public void openDirectory(String path) {
        openDirectory(path, Data.DIRECTORY);
    }
    
    private File getFile(ContextMenuInfo menuInfo) {
        if (menuInfo instanceof AdapterContextMenuInfo) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            if (info.position < mAdapter.getCount()) {
                return mAdapter.getItem(info.position);
            }
        }
        return null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh_directory:
                openDirectory(mDirectory, getDirectoryType(mDirectory));
                return true;
            case R.id.menu_parent:
                openParentDirectory();
                return true;
            case R.id.menu_libraries:
                openDirectory(File.LIBRARIES.getPath(), Data.LIBRARIES);
                return true;
            case R.id.menu_home:
                mDirectory = mPreferences.getHomeDirectory();
                openDirectory(mDirectory, getDirectoryType(mDirectory));
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
                switch(getDirectoryType(file.getPath())) {
                    case Data.LIBRARIES:
                        openDirectory(File.LIBRARIES.getPath(), Data.LIBRARIES);
                        return;
                    case Data.LIBRARY_DIRECTORY:
                        openDirectory(file.getNormalizedPath(), Data.LIBRARY_DIRECTORY);
                        return;
                    default:
                        openDirectory(file.getNormalizedPath());
                        return;
                }
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
        File sel = getFile(menuInfo);
        boolean isBrowsable = sel != null && sel.isBrowsable();
        boolean isPlayable = sel != null && !sel.isParent();
        boolean isAddable = sel != null && isBrowsable && !(sel.isParent() || sel.isLibrary() || sel.isLibraryName() || sel.isLibraryDir());
        boolean isLibraryContext = sel != null && sel.isLibraryName() && !sel.isParent();
        menu.findItem(R.id.browse_context_open).setVisible(isBrowsable);
        menu.findItem(R.id.browse_context_play).setVisible(isPlayable);
        menu.findItem(R.id.browse_context_enqueue).setVisible(isPlayable);
        menu.findItem(R.id.browse_context_add_library).setVisible(isBrowsable && isAddable);
        menu.findItem(R.id.browse_context_remove_library).setVisible(isLibraryContext);
        menu.findItem(R.id.browse_context_remove_from_library).setVisible(isLibraryContext);
        menu.findItem(R.id.browse_context_stream).setVisible(false);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        File file = getFile(item.getMenuInfo());
        if(file != null) {
            switch (item.getItemId()) {
                case R.id.browse_context_open:
                    openDirectory(file);
                    return true;
                case R.id.browse_context_play:
                    List<String> dirs = new ArrayList<String>(mAdapter.getRealPaths(file));
                    if(dirs.isEmpty()) {
                        Toast.makeText(getActivity(), "Error: unable to find real path", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    getMediaServer().status().command.input.play(File.getMrl(dirs.get(0), file.getExtension()), file.getOptions());
                    for(int i = 1; i < dirs.size(); i++) {
                        getMediaServer().status().command.input.enqueue(File.getMrl(dirs.get(i), file.getExtension()));
                    }
                    return true;
//                    case R.id.browse_context_stream:
//                        getMediaServer().status().command.input.play(file.getMrl(),
//                                file.getStreamingOptions());
//                        Intent intent = file.getIntentForStreaming(getMediaServer().getAuthority());
//                        startActivity(intent);
//                        return true;
                case R.id.browse_context_enqueue:
                    for(String dir : mAdapter.getRealPaths(file)) {
                        getMediaServer().status().command.input.enqueue(File.getMrl(dir, file.getExtension()));
                    }
                    // delay reloading playlist to give vlc time to queue and read metadata
                    ((Reloader) getActivity()).reloadDelayed(Tags.FRAGMENT_PLAYLIST, null, 100);
                    return true;
                case R.id.browse_context_add_library:
                    displayAddToLibraryDialog(file);
                    return true;
                case R.id.browse_context_remove_library:
                    mPreferences.removeLibrary(file.getName());
                    openDirectory(mDirectory, Data.LIBRARIES);
                    return true;
                case R.id.browse_context_remove_from_library:
                    displayRemoveFromLibraryDialog(file);
                    return true;
            }
        }
        return super.onContextItemSelected(item);
    }
    
    private void addDirectoryToLibrary(File file, String libraryName) {
        Set<String> libraryDirs = mPreferences.getLibraryDirectories(libraryName);
        String path = file.getNormalizedPath();
        libraryDirs.add(path);
        mPreferences.setLibrary(libraryName, libraryDirs);
        String m = getString(R.string.toast_added_to_library, path, libraryName);
        Toast.makeText(getActivity(), m, Toast.LENGTH_SHORT).show();
    }
    
    private void displayAddToLibraryDialog(final File file) {
        final List<String> libraries = new ArrayList<String>(mPreferences.getLibraries());
        libraries.add(0, getString(R.string.context_add_library_new));
        new AlertDialog.Builder(getActivity())
            .setItems(libraries.toArray(new String[0]), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which == 0) {
                        displayAddToNewLibraryDialog(file);
                    } else {
                        addDirectoryToLibrary(file, libraries.get(which));
                    }
                    dialog.dismiss();
                }
            })
            .setTitle(R.string.context_add_library)
            .show();
    }
    
    private void displayAddToNewLibraryDialog(final File file) {
        final Set<String> libraries = mPreferences.getLibraries();
        final EditText e = new EditText(getActivity());
        e.setHint(R.string.hint_library_add);
        final AlertDialog d = new AlertDialog.Builder(getActivity())
            .setView(e)
            .setPositiveButton(R.string.ok, null) // set later
            .setNegativeButton(R.string.cancel, null)
            .setTitle(R.string.title_dialog_add_library)
            .create();
        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(e.getText().toString().isEmpty()) {
                            Toast.makeText(getActivity(), "Library name cannot be empty", Toast.LENGTH_SHORT).show();
                        } else if(libraries.contains(e.getText().toString())) {
                            Toast.makeText(getActivity(), "Library name already exists", Toast.LENGTH_SHORT).show();
                        } else {
                            addDirectoryToLibrary(file, e.getText().toString());
                            dialog.dismiss();
                        }
                    }
                });
            }
        });
        d.show();
    }
    
    private void displayRemoveFromLibraryDialog(final File file) {
        final List<String> libraryDirs = new ArrayList<String>(mPreferences.getLibraryDirectories(file.getName()));
        new AlertDialog.Builder(getActivity())
            .setItems(libraryDirs.toArray(new String[0]), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    libraryDirs.remove(which);
                    if(libraryDirs.isEmpty()) {
                        mPreferences.removeLibrary(file.getName());
                        openDirectory(mDirectory, Data.LIBRARIES);
                    } else {
                        mPreferences.setLibrary(file.getName(), new HashSet<String>(libraryDirs));
                    }
                    dialog.dismiss();
                }
            })
            .setTitle(R.string.title_dialog_remove_from_library)
            .show();
    }
    
    @Override
    public Loader<Remote<Directory>> onCreateLoader(int id, Bundle args) {
        mPreferences.setBrowseDirectory(mDirectory);
        setEmptyText(getText(R.string.loading));
        switch(id) {
            case Data.LIBRARIES:
                return new LibraryNameLoader(getActivity(), getMediaServer());
            case Data.LIBRARY_DIRECTORY:
                return new LibraryDirectoryLoader(getActivity(), getMediaServer(), mDirectory);
            default:
                return new DirectoryLoader(getActivity(), getMediaServer(), mDirectory);
        }
    }

    @Override
    public void onLoadFinished(Loader<Remote<Directory>> loader, Remote<Directory> result) {
        mAdapter.setDirectory(result.data);
        setEmptyText(getText(R.string.connection_error));
        setTitle(result.data != null ? File.getNormalizedPath(mDirectory) : null);

        boolean isXMLError = result.error != null && XmlContentHandler.ERROR_INVALID_XML.equals(result.error.getMessage());
        if (isEmptyDirectory(result.data) || isXMLError) {
            handleEmptyDirectory();
        }
    }

    private void setTitle(CharSequence title) {
        mTitle.setText(TextUtils.isEmpty(title) ? "Root Directory" : title);
    }

    public CharSequence getTitle() {
        return mTitle.getText();
    }

    private boolean isEmptyDirectory(Directory directory) {
        return directory != null ? directory.isEmpty() : false;
    }

    private void handleEmptyDirectory() {
        showEmptyDirectoryError();
        String path = File.getNormalizedPath(mDirectory.concat("/.."));
        openDirectory(path, getDirectoryType(path));
    }

    private void showEmptyDirectoryError() {
        Toast.makeText(getActivity(), R.string.browse_empty, Toast.LENGTH_LONG).show();
    }

    /** {@inheritDoc} */
    @Override
    public void onLoaderReset(Loader<Remote<Directory>> loader) {
        mAdapter.setDirectory(null);
    }

    // TODO: Automatically reload directory when connection is restored
}

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

package org.peterbaldwin.vlcremote.app;

import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.sweep.PickServerActivity;
import org.peterbaldwin.vlcremote.fragment.ArtFragment;
import org.peterbaldwin.vlcremote.fragment.BrowseFragment;
import org.peterbaldwin.vlcremote.fragment.ButtonsFragment;
import org.peterbaldwin.vlcremote.fragment.InfoFragment;
import org.peterbaldwin.vlcremote.fragment.NavigationFragment;
import org.peterbaldwin.vlcremote.fragment.PlaybackFragment;
import org.peterbaldwin.vlcremote.fragment.PlaylistFragment;
import org.peterbaldwin.vlcremote.fragment.ServicesDiscoveryFragment;
import org.peterbaldwin.vlcremote.fragment.StatusFragment;
import org.peterbaldwin.vlcremote.fragment.VolumeFragment;
import org.peterbaldwin.vlcremote.intent.Intents;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.model.Status;
import org.peterbaldwin.vlcremote.net.MediaServer;
import org.peterbaldwin.vlcremote.widget.VolumePanel;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Browser;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SlidingDrawer;
import android.widget.TabHost;
import android.widget.ViewFlipper;

import java.util.ArrayList;

public class PlaybackActivity extends FragmentActivity implements TabHost.OnTabChangeListener,
        View.OnClickListener {

    private static final int REQUEST_PICK_SERVER = 1;

    private static final Uri URI_TROUBLESHOOTING = Uri
            .parse("http://code.google.com/p/android-vlc-remote/wiki/Troubleshooting");

    private static final String FRAGMENT_STATUS = "vlc:status";

    private static final int VOLUME_LEVEL_UNKNOWN = -1;

    private static final String STATE_INPUT = "vlc:input";
    private static final String STATE_TAB = "vlc:tab";

    private static final String TAB_PLAYBACK = "playback";
    private static final String TAB_INFO = "info";
    private static final String TAB_MEDIA = "media";
    private static final String TAB_PLAYLIST = "playlist";
    private static final String TAB_BROWSE = "browse";
    private static final String TAB_NAVIGATION = "navigation";

    private static final int MAX_VOLUME = 1024;

    private MediaServer mMediaServer;

    private TabHost mTabHost;

    private PlaybackFragment mPlayback;

    private ArtFragment mArt;

    private ButtonsFragment mButtons;

    private VolumeFragment mVolume;

    @SuppressWarnings("unused")
    private InfoFragment mInfo;

    private PlaylistFragment mPlaylist;

    private BrowseFragment mBrowse;

    private ServicesDiscoveryFragment mServicesDiscovery;

    private NavigationFragment mNavigation;

    private StatusFragment mStatus;

    private VolumePanel mVolumePanel;

    private BroadcastReceiver mStatusReceiver;

    private int mVolumeLevel = VOLUME_LEVEL_UNKNOWN;

    private int mLastNonZeroVolume = VOLUME_LEVEL_UNKNOWN;

    private String mInput;

    private SlidingDrawer mDrawer;

    private ViewFlipper mFlipper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Set the control stream to STREAM_MUSIC to suppress system beeps
        // that sound even when the activity handles volume key events.
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mStatus = findOrAddFragment(FRAGMENT_STATUS, StatusFragment.class);
        mPlayback = findFragmentById(R.id.fragment_playback);
        mArt = findFragmentById(R.id.fragment_art);
        mButtons = findFragmentById(R.id.fragment_buttons);
        mVolume = findFragmentById(R.id.fragment_volume);
        mInfo = findFragmentById(R.id.fragment_info);
        mPlaylist = findFragmentById(R.id.fragment_playlist);
        mBrowse = findFragmentById(R.id.fragment_browse);
        mServicesDiscovery = findFragmentById(R.id.fragment_services_discovery);
        mNavigation = findFragmentById(R.id.fragment_navigation);

        Context context = this;
        mVolumePanel = new VolumePanel(context);

        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        if (mTabHost != null) {
            mTabHost.setup();
            // TODO: Internationalize labels
            addTab(TAB_PLAYBACK, R.id.tab_playback, R.string.goto_playback, R.drawable.ic_tab_songs);
            addTab(TAB_INFO, R.id.tab_info, R.string.nowplaying_title, R.drawable.ic_tab_artists);
            addTab(TAB_MEDIA, R.id.tab_media, R.string.nowplaying_title, R.drawable.ic_tab_artists);
            addTab(TAB_PLAYLIST, R.id.tab_playlist, R.string.tab_playlist,
                    R.drawable.ic_tab_playlists);
            addTab(TAB_BROWSE, R.id.tab_browse, R.string.goto_start, R.drawable.ic_tab_playback);
            addTab(TAB_NAVIGATION, R.id.tab_navigation, R.string.tab_dvd, R.drawable.ic_tab_albums);
            mTabHost.setOnTabChangedListener(this);
            onTabChanged(mTabHost.getCurrentTabTag());
        } else {
            onTabChanged(null);
        }

        mDrawer = (SlidingDrawer) findViewById(R.id.drawer);
        if (mDrawer != null) {
            assert Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
            BrowseDrawerListener listener = new BrowseDrawerListener(this, mDrawer, mBrowse);
            mDrawer.setOnDrawerOpenListener(listener);
            mDrawer.setOnDrawerCloseListener(listener);
        }

        mFlipper = (ViewFlipper) findViewById(R.id.flipper);

        Preferences preferences = Preferences.get(context);
        String authority = preferences.getAuthority();
        if (authority != null) {
            changeServer(authority);
        }

        if (savedInstanceState == null) {
            onNewIntent(getIntent());
        }
    }

    private void addTab(String tag, int content, int label, int icon) {
        if (mTabHost.findViewById(content) != null) {
            TabHost.TabSpec spec = mTabHost.newTabSpec(tag);
            spec.setContent(content);
            spec.setIndicator(getText(label), getResources().getDrawable(icon));
            mTabHost.addTab(spec);
        } else {
            // Tab does not exist in this layout
        }
    }

    /** {@inheritDoc} */
    public void onTabChanged(String tabId) {
        if (TAB_PLAYLIST.equals(tabId)) {
            mPlaylist.selectCurrentTrack();
        }
        mPlaylist.setHasOptionsMenu(TAB_PLAYLIST.equals(tabId) || mTabHost == null);
        mBrowse.setHasOptionsMenu(TAB_BROWSE.equals(tabId) && mDrawer == null);
    }

    /** {@inheritDoc} */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_navigation:
                mFlipper.showNext();
                updateNavigationButton(v);
                break;
        }
    }

    private void updateNavigationButton(View v) {
        ImageButton button = (ImageButton) v;
        boolean on = (mFlipper.getDisplayedChild() != 0);
        int icon = on ? R.drawable.ic_navigation_on : R.drawable.ic_navigation_off;
        button.setImageResource(icon);
    }

    @Override
    public boolean onSearchRequested() {
        String initialQuery = mInput;
        boolean selectInitialQuery = true;
        Bundle appSearchData = null;
        boolean globalSearch = false;
        startSearch(initialQuery, selectInitialQuery, appSearchData, globalSearch);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.playback_options, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        String tabId = mTabHost != null ? mTabHost.getCurrentTabTag() : null;
        boolean visible = tabId == null || TAB_PLAYBACK.equals(tabId) | TAB_MEDIA.equals(tabId);
        menu.findItem(R.id.menu_preferences).setVisible(visible);
        menu.findItem(R.id.menu_help).setVisible(visible);
        return menu.hasVisibleItems();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_preferences:
                pickServer();
                return true;
            case R.id.menu_help:
                Intent intent = new Intent(Intent.ACTION_VIEW, URI_TROUBLESHOOTING);
                intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void pickServer() {
        Preferences preferences = Preferences.get(this);
        ArrayList<String> remembered = preferences.getRememberedServers();
        Intent intent = new Intent(this, PickServerActivity.class);
        intent.putExtra(PickServerActivity.EXTRA_PORT, 8080);
        intent.putExtra(PickServerActivity.EXTRA_FILE, "/requests/status.xml");
        intent.putStringArrayListExtra(PickServerActivity.EXTRA_REMEMBERED, remembered);
        startActivityForResult(intent, REQUEST_PICK_SERVER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_PICK_SERVER:
                Preferences preferences = Preferences.get(this);

                if (resultCode == RESULT_OK) {
                    String authority = data.getData().getAuthority();
                    changeServer(authority);
                    preferences.setAuthority(authority);
                    mBrowse.openDirectory("~");
                }

                if (data != null) {
                    // Update remembered servers even if
                    // (resultCode == RESULT_CANCELED)
                    String key = PickServerActivity.EXTRA_REMEMBERED;
                    ArrayList<String> remembered = data.getStringArrayListExtra(key);
                    if (remembered != null) {
                        preferences.setRemeberedServers(remembered);
                    }
                }

                if (mMediaServer == null) {
                    finish();
                }

                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void changeServer(String authority) {
        Context context = this;
        mMediaServer = new MediaServer(context, authority);
        mPlayback.setMediaServer(mMediaServer);
        mButtons.setMediaServer(mMediaServer);
        mVolume.setMediaServer(mMediaServer);
        if (mArt != null) {
            mArt.setMediaServer(mMediaServer);
        }
        mPlaylist.setMediaServer(mMediaServer);
        mBrowse.setMediaServer(mMediaServer);
        mStatus.setMediaServer(mMediaServer);
        if (mServicesDiscovery != null) {
            mServicesDiscovery.setMediaServer(mMediaServer);
        }
        if (mNavigation != null) {
            mNavigation.setMediaServer(mMediaServer);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String host = intent.getStringExtra(Intents.EXTRA_REMOTE_HOST);
        if (host != null) {
            int port = intent.getIntExtra(Intents.EXTRA_REMOTE_PORT, 8080);
            String authority = host + ":" + port;
            changeServer(authority);
        }

        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action) || Intents.ACTION_REMOTE_VIEW.equals(action)
                || Intents.ACTION_VIEW.equals(action)) {
            Uri data = intent.getData();
            if (data != null) {
                changeInput(data.toString());
            }
        } else if (Intent.ACTION_SEARCH.equals(action)) {
            String input = intent.getStringExtra(SearchManager.QUERY);
            changeInput(input);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_INPUT, mInput);
        if (mTabHost != null) {
            outState.putString(STATE_TAB, mTabHost.getCurrentTabTag());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mInput = savedInstanceState.getString(STATE_INPUT);
        if (mTabHost != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString(STATE_TAB));
        }
    }

    private void changeInput(String input) {
        mInput = input;
        if (mInput != null) {
            mMediaServer.status().command.input.play(mInput);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mStatusReceiver = new StatusReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.ACTION_STATUS);
        registerReceiver(mStatusReceiver, filter);
        if (mMediaServer == null) {
            pickServer();
        }
    }

    @Override
    public void onPause() {
        unregisterReceiver(mStatusReceiver);
        mStatusReceiver = null;
        super.onPause();
    }

    void onVolumeChanged(int volume) {
        if (!hasVolumeFragment() && mVolumeLevel != VOLUME_LEVEL_UNKNOWN && mVolumeLevel != volume) {
            mVolumePanel.onVolumeChanged(volume);
        }
        mVolumeLevel = volume;
        if (0 != volume) {
            mLastNonZeroVolume = volume;
        }
    }

    private boolean hasVolumeFragment() {
        return mVolume != null && mVolume.isInLayout();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int c = event.getUnicodeChar();
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (mVolumeLevel != VOLUME_LEVEL_UNKNOWN) {
                setVolume(mVolumeLevel + 20);
            } else {
                mMediaServer.status().command.volumeUp();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (mVolumeLevel != VOLUME_LEVEL_UNKNOWN) {
                setVolume(mVolumeLevel - 20);
            } else {
                mMediaServer.status().command.volumeDown();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (event.isAltPressed()) {
                mMediaServer.status().command.seek(Uri.encode("-10"));
                return true;
            } else if (event.isShiftPressed()) {
                mMediaServer.status().command.seek(Uri.encode("-3"));
                return true;
            } else {
                mMediaServer.status().command.key("nav-left");
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (event.isAltPressed()) {
                mMediaServer.status().command.seek(Uri.encode("+10"));
                return true;
            } else if (event.isShiftPressed()) {
                mMediaServer.status().command.seek(Uri.encode("+3"));
                return true;
            } else {
                mMediaServer.status().command.key("nav-right");
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            mMediaServer.status().command.key("nav-up");
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            mMediaServer.status().command.key("nav-down");
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            mMediaServer.status().command.key("nav-activate");
            return true;
        } else if (c == ' ') {
            mMediaServer.status().command.playback.pause();
            return true;
        } else if (c == 's') {
            mMediaServer.status().command.playback.stop();
            return true;
        } else if (c == 'p') {
            mMediaServer.status().command.playback.previous();
            return true;
        } else if (c == 'n') {
            mMediaServer.status().command.playback.next();
            return true;
        } else if (c == '+') {
            // TODO: Play faster
            return super.onKeyDown(keyCode, event);
        } else if (c == '-') {
            // TODO: Play slower
            return super.onKeyDown(keyCode, event);
        } else if (c == 'f') {
            mMediaServer.status().command.fullscreen();
            return true;
        } else if (c == 'm') {
            mute();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void setVolume(int volume) {
        volume = Math.max(volume, 0);
        volume = Math.min(volume, MAX_VOLUME);
        mMediaServer.status().command.volume(volume);
        onVolumeChanged(volume);
    }

    private void mute() {
        // The web interface doesn't have a documented mute command.
        if (mVolumeLevel != 0) {
            // Set the volume to zero
            mMediaServer.status().command.volume(0);
        } else if (mLastNonZeroVolume != VOLUME_LEVEL_UNKNOWN) {
            // Restore the volume to the last known value
            mMediaServer.status().command.volume(mLastNonZeroVolume);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Fragment> T findFragmentById(int id) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        return (T) fragmentManager.findFragmentById(id);
    }

    @SuppressWarnings("unchecked")
    private <T extends Fragment> T findOrAddFragment(String tag, Class<T> fragmentClass) {
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            T fragment = (T) fragmentManager.findFragmentByTag(tag);
            if (fragment == null) {
                fragment = fragmentClass.newInstance();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.add(fragment, FRAGMENT_STATUS);
                fragmentTransaction.commit();
                fragmentManager.executePendingTransactions();
            }
            return fragment;
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private class StatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intents.ACTION_STATUS.equals(action)) {
                Status status = (Status) intent.getSerializableExtra(Intents.EXTRA_STATUS);
                onVolumeChanged(status.getVolume());
            }
        }
    }
}

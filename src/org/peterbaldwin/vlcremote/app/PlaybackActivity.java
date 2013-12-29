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
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.SlidingDrawer;
import android.widget.TabHost;
import android.widget.ViewFlipper;
import java.util.ArrayList;
import java.util.List;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.fragment.ArtFragment;
import org.peterbaldwin.vlcremote.fragment.BrowseFragment;
import org.peterbaldwin.vlcremote.fragment.ButtonsFragment;
import org.peterbaldwin.vlcremote.fragment.InfoFragment;
import org.peterbaldwin.vlcremote.fragment.NavigationFragment;
import org.peterbaldwin.vlcremote.fragment.PlaybackFragment;
import org.peterbaldwin.vlcremote.fragment.PlayingFragment;
import org.peterbaldwin.vlcremote.fragment.PlaylistFragment;
import org.peterbaldwin.vlcremote.fragment.StatusFragment;
import org.peterbaldwin.vlcremote.fragment.VolumeFragment;
import org.peterbaldwin.vlcremote.intent.Intents;
import org.peterbaldwin.vlcremote.listener.ButtonVisibilityListener;
import org.peterbaldwin.vlcremote.listener.UIVisibilityListener;
import org.peterbaldwin.vlcremote.model.MediaServerListener;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.model.Status;
import org.peterbaldwin.vlcremote.model.Tags;
import org.peterbaldwin.vlcremote.net.MediaServer;
import org.peterbaldwin.vlcremote.util.FragmentUtil;
import org.peterbaldwin.vlcremote.widget.VolumePanel;

public class PlaybackActivity extends FragmentActivity implements TabHost.OnTabChangeListener,
        View.OnClickListener, UIVisibilityListener {

    private static final String TAG = "PlaybackActivity";
    
    private static final int REQUEST_PICK_SERVER = 1;

    private static final Uri URI_TROUBLESHOOTING = Uri.parse("http://code.google.com/p/android-vlc-remote/wiki/Troubleshooting");

    private static final int VOLUME_LEVEL_UNKNOWN = -1;

    private static final String STATE_INPUT = "vlc:input";
    private static final String STATE_TAB = "vlc:tab";

    private static final String TAB_MEDIA = "media";
    private static final String TAB_PLAYLIST = "playlist";
    private static final String TAB_BROWSE = "browse";
    private static final String TAB_NAVIGATION = "navigation";
    
    private static final int TAB_NAVIGATION_INDEX = 3;

    private static final int MAX_VOLUME = 1024;

    private final List<TabHost.TabSpec> mTabSpecList = new ArrayList<TabHost.TabSpec>();
    
    /**
     * This is used to store the value of the users preference before the 
     * pick server activity is created.
     */
    private boolean isHideDVDTab = false;
    
    private boolean isBottomActionbarVisible = false;
    
    private boolean isVolumeFragmentVisible = false;
    
    private ButtonVisibilityListener mButtonsVisibleListener;
    
    private MediaServer mMediaServer;

    private TabHost mTabHost;

    private PlaylistFragment mPlaylist;

    private BrowseFragment mBrowse;

    private VolumePanel mVolumePanel;

    private BroadcastReceiver mStatusReceiver;

    private int mVolumeLevel = VOLUME_LEVEL_UNKNOWN;

    private int mLastNonZeroVolume = VOLUME_LEVEL_UNKNOWN;

    private String mInput;

    private SlidingDrawer mDrawer;

    private ViewFlipper mFlipper;
    
    private List<MediaServerListener> mMediaServerListeners = new ArrayList<MediaServerListener>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.main);
        
        // Set the control stream to STREAM_MUSIC to suppress system beeps
        // that sound even when the activity handles volume key events.
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        String authority = Preferences.get(this).getAuthority();
        if (authority != null) {
            mMediaServer = new MediaServer(this, authority);
        }
        
        mFlipper = (ViewFlipper) findViewById(R.id.flipper);
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        
        FragmentUtil fu = new FragmentUtil(getSupportFragmentManager());
        fu.findOrAddFragment(Tags.FRAGMENT_STATUS, StatusFragment.class);        
        fu.findOrReplaceOptionalFragment(this, R.id.fragment_navigation, Tags.FRAGMENT_NAVIGATION, NavigationFragment.class);
        mPlaylist = fu.findOrReplaceFragment(R.id.fragment_playlist, Tags.FRAGMENT_PLAYLIST, PlaylistFragment.class);
        mBrowse = fu.findOrReplaceFragment(R.id.fragment_browse, Tags.FRAGMENT_BROWSE, BrowseFragment.class);
        mBrowse.registerObserver(mPlaylist);
        mVolumePanel = new VolumePanel(this);

        if(mTabHost == null) {
            fu.findOrReplaceFragment(R.id.fragment_playback, Tags.FRAGMENT_PLAYBACK, PlaybackFragment.class);
            fu.findOrReplaceFragment(R.id.fragment_info, Tags.FRAGMENT_INFO, InfoFragment.class);
            fu.findOrReplaceOptionalFragment(this, R.id.fragment_art, Tags.FRAGMENT_ART, ArtFragment.class);
            fu.findOrReplaceFragment(R.id.fragment_buttons, Tags.FRAGMENT_BUTTONS, ButtonsFragment.class);
            VolumeFragment mVolume = fu.findOrReplaceFragment(R.id.fragment_volume, Tags.FRAGMENT_VOLUME, VolumeFragment.class);
            setVolumeFragmentVisible(mVolume != null);
            onTabChanged(null);
        } else {
            if(savedInstanceState != null) {
                fu.removeFragmentsByTag(Tags.FRAGMENT_PLAYBACK, Tags.FRAGMENT_INFO, Tags.FRAGMENT_BUTTONS, Tags.FRAGMENT_VOLUME, Tags.FRAGMENT_BOTTOMBAR);
            }
            fu.findOrReplaceFragment(R.id.fragment_playing, Tags.FRAGMENT_PLAYING, PlayingFragment.class);
            setupTabHost();
        }

        mDrawer = (SlidingDrawer) findViewById(R.id.drawer);
        if (mDrawer != null) {
            assert Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
            BrowseDrawerListener listener = new BrowseDrawerListener(this, mDrawer, mBrowse);
            mDrawer.setOnDrawerOpenListener(listener);
            mDrawer.setOnDrawerCloseListener(listener);
        }

        if (savedInstanceState == null) {
            onNewIntent(getIntent());
        } else {
            notifyMediaServerListeners();
        }
    }
    
    private void setupTabHost() {
        mTabHost.setup();
        addTab(TAB_MEDIA, R.id.tab_media, R.string.nowplaying_title, R.drawable.ic_tab_artists);
        addTab(TAB_PLAYLIST, R.id.tab_playlist, R.string.tab_playlist,
                R.drawable.ic_tab_playlists);
        addTab(TAB_BROWSE, R.id.tab_browse, R.string.goto_start, R.drawable.ic_tab_playback);
        addTab(TAB_NAVIGATION, R.id.tab_navigation, R.string.tab_dvd, R.drawable.ic_tab_albums);
        if(Preferences.get(this).isHideDVDTabSet()) {
            mTabHost.getTabWidget().removeView(mTabHost.getTabWidget().getChildTabViewAt(TAB_NAVIGATION_INDEX));
        }
        mTabHost.setOnTabChangedListener(this);
        onTabChanged(mTabHost.getCurrentTabTag());
    }

    private void addTab(String tag, int content, int label, int icon) {
        if (mTabHost.findViewById(content) != null) {
            TabHost.TabSpec spec = mTabHost.newTabSpec(tag);
            spec.setContent(content);
            spec.setIndicator(getText(label), getResources().getDrawable(icon));
            mTabHost.addTab(spec);
            mTabSpecList.add(spec);
        }
    }
    
    public void updateTabs() {
        int curTab = mTabHost.getCurrentTab();
        mTabHost.setCurrentTab(0);
        mTabHost.clearAllTabs();
        for (int i = 0; i < mTabSpecList.size(); i++) {
            if(i == TAB_NAVIGATION_INDEX && Preferences.get(this).isHideDVDTabSet()) {
                continue;
            }
            mTabHost.addTab(mTabSpecList.get(i));
            mTabHost.setCurrentTab(i);
        }
        mTabHost.setCurrentTab(curTab);
    }

    public void onTabChanged(String tabId) {
        if (TAB_PLAYLIST.equals(tabId)) {
            mPlaylist.selectCurrentTrack();
            mBrowse.notifyPlaylistVisible();
        }
        mPlaylist.setHasOptionsMenu(TAB_PLAYLIST.equals(tabId) || mTabHost == null);
        mBrowse.setHasOptionsMenu(TAB_BROWSE.equals(tabId) && mDrawer == null);
        invalidateOptionsMenu();
    }

    /** {@inheritDoc} */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_navigation:
                if(mTabHost == null) {
                    mFlipper.showNext();
                    updateNavigationButton(v);
                } else {
                    mTabHost.setCurrentTab(TAB_NAVIGATION_INDEX);
                }
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
        boolean visible = tabId == null || TAB_MEDIA.equals(tabId);
        menu.findItem(R.id.menu_preferences).setVisible(visible);
        menu.findItem(R.id.menu_help).setVisible(visible);
        menu.setGroupVisible(R.id.group_vlc_actions, visible);
        if(isBottomActionbarVisible || (mButtonsVisibleListener != null && mButtonsVisibleListener.isAllButtonsVisible())) {
            menu.setGroupVisible(R.id.group_vlc_actions, false);
        }
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
            case R.id.menu_playlist_cycle_crop:
                mMediaServer.status().command.key("crop");
                return true;
            case R.id.menu_playlist_cycle_subtitles:
                mMediaServer.status().command.key("subtitle-track");
                return true;
            case R.id.menu_playlist_button_fullscreen:
                mMediaServer.status().command.fullscreen();
                return true;
            case R.id.menu_playlist_cycle_audio_track:
                mMediaServer.status().command.key("audio-track");
                return true;
            case R.id.menu_playlist_cycle_aspect_ratio:
                mMediaServer.status().command.key("aspect-ratio");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void setButtonVisibilityListener(ButtonVisibilityListener l) {
        mButtonsVisibleListener = l;
    }
    
    @Override
    public void setBottomActionbarFragmentVisible(boolean isVisible) {
        isBottomActionbarVisible = isVisible;
    }
    
    @Override
    public void setVolumeFragmentVisible(boolean isVisible) {
        isVolumeFragmentVisible = isVisible;
    }

    private void pickServer() {
        Preferences preferences = Preferences.get(this);
        ArrayList<String> remembered = preferences.getRememberedServers();
        isHideDVDTab = preferences.isHideDVDTabSet();
        Intent intent = new Intent(this, PickServerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
                } else {
                    mBrowse.reload();
                    mPlaylist.reload();
                }
                
                if(preferences.isHideDVDTabSet() != isHideDVDTab && mTabHost != null) {
                    updateTabs();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaServerListeners = null;
    }
    
    public MediaServer addMediaServerListener(MediaServerListener l) {
        mMediaServerListeners.add(l);
        return mMediaServer;
    }
    
    private void notifyMediaServerListeners() {
        for(MediaServerListener l : mMediaServerListeners) {
            l.onNewMediaServer(mMediaServer);
        }
    }

    private void changeServer(String authority) {
        mMediaServer = new MediaServer(this, authority);
        notifyMediaServerListeners();
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
        if (mMediaServer == null) {
            Log.w(TAG, "No server selected");
            return;
        }   
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
        if (!isVolumeFragmentVisible && mVolumeLevel != VOLUME_LEVEL_UNKNOWN && mVolumeLevel != volume) {
            mVolumePanel.onVolumeChanged(volume);
        }
        mVolumeLevel = volume;
        if (0 != volume) {
            mLastNonZeroVolume = volume;
        }
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
                mMediaServer.status().command.seek(Uri.encode("-".concat(Preferences.get(this).getSeekTime())));
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
                mMediaServer.status().command.seek(Uri.encode("+".concat(Preferences.get(this).getSeekTime())));
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

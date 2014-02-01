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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.intent.Intents;
import org.peterbaldwin.vlcremote.listener.ButtonVisibilityListener;
import org.peterbaldwin.vlcremote.listener.CommonPlaybackButtonsListener;
import org.peterbaldwin.vlcremote.listener.MediaServerListener;
import org.peterbaldwin.vlcremote.listener.UIVisibilityListener;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.model.Reloadable;
import org.peterbaldwin.vlcremote.model.Reloader;
import org.peterbaldwin.vlcremote.model.Status;
import org.peterbaldwin.vlcremote.model.Tags;
import org.peterbaldwin.vlcremote.net.MediaServer;

public final class ButtonsFragment extends MediaFragment implements View.OnClickListener,
        View.OnLongClickListener, MediaServerListener, ButtonVisibilityListener, Reloadable {
    
    private BroadcastReceiver mStatusReceiver;

    private CommonPlaybackButtonsListener listener;
    
    private ImageButton mButtonShuffle;
    private ImageButton mButtonRepeat;

    private boolean isAllButtonsVisible;
    
    private boolean mRandom;
    private boolean mRepeat;
    private boolean mLoop;

    @Override
    public void onNewMediaServer(MediaServer server) {
        super.onNewMediaServer(server);
        if(listener != null) {
            listener.setMediaServer(server);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((UIVisibilityListener) activity).setButtonVisibilityListener(this);
        ((Reloader) activity).addReloadable(Tags.FRAGMENT_BUTTONS, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.buttons_fragment, parent, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();

        listener = new CommonPlaybackButtonsListener(getMediaServer());
        listener.setUp(view);
        
        mButtonShuffle = (ImageButton) view.findViewById(R.id.playlist_button_shuffle);
        mButtonRepeat = (ImageButton) view.findViewById(R.id.playlist_button_repeat);
        ImageButton mButtonPlaylistSeekBackward = (ImageButton) view.findViewById(R.id.action_button_seek_backward);
        ImageButton mButtonPlaylistSeekForward = (ImageButton) view.findViewById(R.id.action_button_seek_forward);
        isAllButtonsVisible = view.findViewById(R.id.audio_player_buttons_second_row) != null;
        getActivity().invalidateOptionsMenu();

        setupImageButtonListeners(mButtonShuffle, mButtonRepeat, mButtonPlaylistSeekBackward, mButtonPlaylistSeekForward);
        
        if(getResources().getConfiguration().screenWidthDp >= 400) {
            // seek buttons are displayed in playback fragment if >= 400dp
            hideImageButton(mButtonPlaylistSeekBackward, mButtonPlaylistSeekForward);
        }
    }
    
    private void setupImageButtonListeners(ImageButton... imageButtons) {
        for(ImageButton b : imageButtons) {
            if(b != null) {
                b.setOnClickListener(this);
                b.setOnLongClickListener(this);
            }
        }
    }
    
    private void updateDVDButton() {
        if(getView() == null) {
            return;
        }
        ImageButton dvd = (ImageButton) getView().findViewById(R.id.button_navigation);
        if(dvd != null && dvd.getTag() == null) {
            dvd.setVisibility(Preferences.get(getActivity()).isHideDVDTabSet() ? View.GONE : View.VISIBLE);
        }
    }
    
    private void hideImageButton(ImageButton... imageButtons) {
        for(ImageButton b : imageButtons) {
            if(b != null) {
                b.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public boolean isAllButtonsVisible() {
        return isAllButtonsVisible;
    }

    @Override
    public void onResume() {
        super.onResume();
        mStatusReceiver = new StatusReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.ACTION_STATUS);
        getActivity().registerReceiver(mStatusReceiver, filter);
        updateDVDButton();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(mStatusReceiver);
        mStatusReceiver = null;
        super.onPause();
    }

    /** {@inheritDoc} */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.action_button_seek_backward:
                getMediaServer().status().command.seek(Uri.encode("-".concat(Preferences.get(getActivity()).getSeekTime())));
                break;
            case R.id.action_button_seek_forward:
                getMediaServer().status().command.seek(Uri.encode("+".concat(Preferences.get(getActivity()).getSeekTime())));
                break;
            case R.id.playlist_button_shuffle:
                getMediaServer().status().command.playback.random();
                mRandom = !mRandom;
                updateButtons();
                break;
            case R.id.playlist_button_repeat:
                // Order: Normal -> Loop -> Repeat
                if (mLoop) {
                    // Turn-on repeat
                    getMediaServer().status().command.playback.repeat();
                    mRepeat = true;
                    mLoop = false;
                } else if (mRepeat) {
                    // Turn-off repeat
                    getMediaServer().status().command.playback.repeat();
                    mRepeat = false;
                } else {
                    // Turn-on loop
                    getMediaServer().status().command.playback.loop();
                    mLoop = true;
                }
                updateButtons();
                break;
        }
    }
    
    private int getShuffleResId() {
        if (mRandom) {
            return R.drawable.ic_mp_shuffle_on_btn;
        } else {
            return R.drawable.ic_mp_shuffle_off_btn;
        }
    }

    private int getRepeatResId() {
        if (mRepeat) {
            return R.drawable.ic_mp_repeat_once_btn;
        } else if (mLoop) {
            return R.drawable.ic_mp_repeat_all_btn;
        } else {
            return R.drawable.ic_mp_repeat_off_btn;
        }
    }

    private void updateButtons() {
        mButtonShuffle.setImageResource(getShuffleResId());
        mButtonRepeat.setImageResource(getRepeatResId());
    }

    void onStatusChanged(Status status) {
        mRandom = status.isRandom();
        mLoop = status.isLoop();
        mRepeat = status.isRepeat();
        updateButtons();
    }

    public boolean onLongClick(View v) {
        Toast.makeText(getActivity(), v.getContentDescription(), Toast.LENGTH_SHORT).show();
        return true;
    }

    public void reload(Bundle args) {
        if(listener != null) {
            listener.setUp(getView());
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

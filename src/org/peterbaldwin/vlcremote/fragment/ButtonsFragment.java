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
import org.peterbaldwin.vlcremote.model.Status;
import org.peterbaldwin.vlcremote.net.MediaServer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

public final class ButtonsFragment extends Fragment implements View.OnClickListener {

    private MediaServer mMediaServer;

    private BroadcastReceiver mStatusReceiver;

    private ImageButton mButtonShuffle;
    private ImageButton mButtonRepeat;

    private boolean mRandom;
    private boolean mRepeat;
    private boolean mLoop;

    public void setMediaServer(MediaServer mediaServer) {
        mMediaServer = mediaServer;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frame_layout, parent, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();

        mButtonShuffle = (ImageButton) view.findViewById(R.id.playlist_button_shuffle);
        mButtonRepeat = (ImageButton) view.findViewById(R.id.playlist_button_repeat);

        mButtonShuffle.setOnClickListener(this);
        mButtonRepeat.setOnClickListener(this);

        view.findViewById(R.id.button_fullscreen).setOnClickListener(this);
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

    /** {@inheritDoc} */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_fullscreen:
                mMediaServer.status().command.fullscreen();
                break;
            case R.id.playlist_button_shuffle:
                mMediaServer.status().command.playback.random();
                mRandom = !mRandom;
                updateButtons();
                break;
            case R.id.playlist_button_repeat:
                // Order: Normal -> Repeat -> Loop
                if (mRepeat) {
                    // Switch to loop
                    if (mLoop) {
                        // Turn-off repeat
                        mMediaServer.status().command.playback.repeat();
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

                        // Turn-on loop then turn-off repeat shortly after.
                        mMediaServer.status().command.playback.loop().repeat();
                    }
                } else if (mLoop) {
                    // Switch to normal

                    // Turn-off loop
                    mMediaServer.status().command.playback.loop();
                    mLoop = false;
                } else {
                    // Turn-on repeat and turn-on loop to make the transition
                    // from repeat to loop one step instead of two steps.
                    // Loop has no effect when repeat is on.
                    mMediaServer.status().command.playback.repeat().loop();
                    mRepeat = true;
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

    private class StatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Status status = (Status) intent.getSerializableExtra(Intents.EXTRA_STATUS);
            onStatusChanged(status);
        }
    }
}

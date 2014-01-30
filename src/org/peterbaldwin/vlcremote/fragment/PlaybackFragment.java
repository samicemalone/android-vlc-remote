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
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.intent.Intents;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.model.Status;
import org.peterbaldwin.vlcremote.net.MediaServer.StatusRequest;
import org.peterbaldwin.vlcremote.net.MediaServer.StatusRequest.CommandInterface;
import org.peterbaldwin.vlcremote.net.MediaServer.StatusRequest.CommandInterface.PlaybackInterface;

/**
 * Controls playback and displays progress.
 */
public class PlaybackFragment extends MediaFragment implements View.OnClickListener,
        View.OnLongClickListener, OnSeekBarChangeListener {

    private BroadcastReceiver mStatusReceiver;

    private ImageButton mButtonPlaylistPause;

    private ImageButton mButtonPlaylistStop;

    private ImageButton mButtonPlaylistSkipForward;

    private ImageButton mButtonPlaylistSkipBackward;

    private ImageButton mButtonPlaylistSeekForward;

    private ImageButton mButtonPlaylistSeekBackward;

    private ImageButton mButtonPlaylistChapterNext;

    private ImageButton mButtonPlaylistChapterPrevious;

    private SeekBar mSeekPosition;

    private TextView mTextTime;

    private TextView mTextLength;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.audio_player_common, container, false);
        mButtonPlaylistPause = setupImageButton(v, R.id.button_pause);
        mButtonPlaylistStop = setupImageButton(v, R.id.button_stop);
        mButtonPlaylistSkipForward = setupImageButton(v, R.id.button_skip_forward);
        mButtonPlaylistSkipBackward = setupImageButton(v, R.id.button_skip_backward);
        mButtonPlaylistSeekForward = setupImageButton(v, R.id.button_seek_forward);
        mButtonPlaylistSeekBackward = setupImageButton(v, R.id.button_seek_backward);
        mButtonPlaylistChapterNext = setupImageButton(v, R.id.button_chapter_next);
        mButtonPlaylistChapterPrevious = setupImageButton(v, R.id.button_chapter_previous);

        mSeekPosition = (SeekBar) v.findViewById(R.id.seek_progress);
        mSeekPosition.setMax(100);
        mSeekPosition.setOnSeekBarChangeListener(this);

        mTextTime = (TextView) v.findViewById(R.id.text_time);
        mTextLength = (TextView) v.findViewById(R.id.text_length);
        return v;
    }

    private ImageButton setupImageButton(View v, int viewId) {
        ImageButton button = (ImageButton) v.findViewById(viewId);
        if (button != null) {
            button.setOnClickListener(this);
            button.setOnLongClickListener(this);
        }
        return button;
    }

    private StatusRequest status() {
        return getMediaServer().status();
    }

    private CommandInterface command() {
        return status().command;
    }

    private PlaybackInterface playlist() {
        return command().playback;
    }

    /** {@inheritDoc} */
    public void onClick(View v) {
        if (v == mButtonPlaylistPause) {
            playlist().pause();
        } else if (v == mButtonPlaylistStop) {
            playlist().stop();
        } else if (v == mButtonPlaylistSkipBackward) {
            playlist().previous();
        } else if (v == mButtonPlaylistSkipForward) {
            playlist().next();
        } else if (v == mButtonPlaylistSeekBackward) {
            command().seek(Uri.encode("-".concat(Preferences.get(getActivity()).getSeekTime())));
        } else if (v == mButtonPlaylistSeekForward) {
            command().seek(Uri.encode("+".concat(Preferences.get(getActivity()).getSeekTime())));
        } else if (v == mButtonPlaylistChapterPrevious) {
            command().key("chapter-prev");
        } else if (v == mButtonPlaylistChapterNext) {
            command().key("chapter-next");
        }
    }

    public boolean onLongClick(View v) {
        Toast.makeText(v.getContext(), v.getContentDescription(), Toast.LENGTH_SHORT).show();
        return true;
    }

    /** {@inheritDoc} */
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar == mSeekPosition) {
            if (fromUser) {
                seekPosition();
            }
        }
    }

    /** {@inheritDoc} */
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    /** {@inheritDoc} */
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (seekBar == mSeekPosition) {
            seekPosition();
        }
    }

    private void seekPosition() {
        int position = mSeekPosition.getProgress();
        command().seek(String.valueOf(position));
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

    void onStatusChanged(Status status) {
        int resId = status.isPlaying() ? R.drawable.ic_media_playback_pause
                : R.drawable.ic_media_playback_start;
        mButtonPlaylistPause.setImageResource(resId);

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
     * @param time the time (in seconds)
     * @return the formatted time.
     */
    private static String formatTime(int time) {
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

    private class StatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intents.ACTION_STATUS.equals(action)) {
                Status status = (Status) intent.getSerializableExtra(Intents.EXTRA_STATUS);
                onStatusChanged(status);
            }
        }
    }
}

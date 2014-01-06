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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.intent.Intents;
import org.peterbaldwin.vlcremote.model.File;
import org.peterbaldwin.vlcremote.model.Media;
import org.peterbaldwin.vlcremote.model.MediaDisplayInfo;
import org.peterbaldwin.vlcremote.model.Status;
import org.peterbaldwin.vlcremote.parser.MediaParser;

public class InfoFragment extends Fragment {

    private final MediaParser mMediaParser;
    private BroadcastReceiver mStatusReceiver;
    private TextView mArtist;
    private TextView mAlbum;
    private TextView mTrack;
    private String mCurrentFileName;

    public InfoFragment() {
        mMediaParser = new MediaParser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.info_fragment, root, false);
        mArtist = (TextView) view.findViewById(R.id.artist);
        mAlbum = (TextView) view.findViewById(R.id.album);
        mTrack = (TextView) view.findViewById(R.id.track);
        mCurrentFileName = null;
        return view;
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

    public void onStatusChanged(Context context, Status status) {
        if(status.getTrack().getName() == null) {
            if(status.getTrack().getTitle() == null) {
                if(mCurrentFileName != null) {
                    clearMediaDisplayInfo();
                    mCurrentFileName = null;
                }
                return;
            }
            if(status.getTrack().getTitle().equals(mCurrentFileName)) {
                return;
            }
            mCurrentFileName = status.getTrack().getTitle();
        } else {
            if(status.getTrack().getName().equals(mCurrentFileName)) {
                return;
            }
            mCurrentFileName = status.getTrack().getName();
        }
        // it is possible for a status change to be sent before vlc has fully read
        // the file metadata and not output any stream information.
        if(!status.getTrack().containsStream() || status.getTrack().hasVideoStream()) {
            Media media = mMediaParser.parse(mCurrentFileName);
            if(media != null) {
                setMediaDisplayInfo(media, mCurrentFileName);
                return;
            }
        }
        setMediaDisplayInfo(status.getTrack(), mCurrentFileName);
    }
    
    private void clearMediaDisplayInfo() {
        setText(mArtist, "");
        setText(mAlbum, "");
        setText(mTrack, "");
    }
    
    private void setMediaDisplayInfo(MediaDisplayInfo i, String fileName) {
        setText(mArtist, i.getMediaHeading());
        setText(mAlbum, i.getMediaFirstText());
        if(i.getMediaSecondText() == null || i.getMediaSecondText().isEmpty()) {
            setText(mTrack, File.baseName(fileName));
        } else {
            setText(mTrack, i.getMediaSecondText());
        }
    }

    private static void setText(TextView textView, String value) {
        textView.setText(value);
    }

    private class StatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intents.ACTION_STATUS.equals(action)) {
                // TODO: Filter by authority
                Status status = (Status) intent.getSerializableExtra(Intents.EXTRA_STATUS);
                onStatusChanged(context, status);
            }
        }
    }
}

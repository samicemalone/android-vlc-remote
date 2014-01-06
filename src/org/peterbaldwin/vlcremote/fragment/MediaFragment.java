/*
 * Copyright (C) 2013 Sam Malone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.peterbaldwin.vlcremote.fragment;

import android.app.Activity;
import android.support.v4.app.Fragment;
import org.peterbaldwin.vlcremote.app.PlaybackActivity;
import org.peterbaldwin.vlcremote.listener.MediaServerListener;
import org.peterbaldwin.vlcremote.net.MediaServer;

/**
 *
 * @author Sam Malone
 */
public class MediaFragment extends Fragment implements MediaServerListener {

    private MediaServer mMediaServer;
    
    public void onNewMediaServer(MediaServer server) {
        mMediaServer = server;
    }

    public MediaServer addMediaServerListener(PlaybackActivity activity) {
        return (activity == null) ? null : activity.addMediaServerListener(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mMediaServer = addMediaServerListener((PlaybackActivity) activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMediaServer = null;
    }

    /**
     * Get the media server instance
     * @return media server instance or null if none set
     */
    protected MediaServer getMediaServer() {
        return mMediaServer;
    }
    
}

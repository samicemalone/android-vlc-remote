/*-
 *  Copyright (C) 2013 Sam Malone
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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.listener.CommonPlaybackButtonsListener;
import org.peterbaldwin.vlcremote.model.Reloadable;
import org.peterbaldwin.vlcremote.model.Reloader;
import org.peterbaldwin.vlcremote.model.Tags;
import org.peterbaldwin.vlcremote.net.MediaServer;

public final class BottomActionbarFragment extends MediaFragment implements Reloadable {

    private CommonPlaybackButtonsListener listener;

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
        ((Reloader) activity).addReloadable(Tags.FRAGMENT_BOTTOMBAR, this);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playback_bottom, parent, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listener = new CommonPlaybackButtonsListener(getMediaServer());
        listener.setUp(getView());
    }

    public void reload(Bundle args) {
        if(listener != null) {
            listener.setUp(getView());
        }
    }

}

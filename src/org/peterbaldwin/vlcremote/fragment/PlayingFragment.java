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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.peterbaldwin.client.android.vlcremote.PlaybackActivity;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.listener.UIVisibilityListener;
import org.peterbaldwin.vlcremote.model.Tags;
import org.peterbaldwin.vlcremote.util.FragmentUtil;

/**
 *
 * @author Ice
 */
public class PlayingFragment extends Fragment {
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View v =  inflater.inflate(R.layout.playing_fragment, root, false);
        FragmentUtil fu = new FragmentUtil(getChildFragmentManager());
        fu.findOrReplaceFragment(R.id.fragment_playback, Tags.FRAGMENT_PLAYBACK, PlaybackFragment.class);
        fu.findOrReplaceFragment(R.id.fragment_info, Tags.FRAGMENT_INFO, InfoFragment.class);
        fu.findOrReplaceFragment(R.id.fragment_buttons, Tags.FRAGMENT_BUTTONS, ButtonsFragment.class);
        VolumeFragment mVolume = fu.findOrReplaceOptionalFragment(v, R.id.fragment_volume, Tags.FRAGMENT_VOLUME, VolumeFragment.class);
        BottomActionbarFragment mBottomActionBar = fu.findOrReplaceOptionalFragment(v, R.id.fragment_bottom_actionbar, Tags.FRAGMENT_BOTTOMBAR, BottomActionbarFragment.class);
        UIVisibilityListener ui = (PlaybackActivity) getActivity();
        // notify activity about optional fragments visibility
        ui.setBottomActionbarFragmentVisible(mBottomActionBar != null);
        ui.setVolumeFragmentVisible(mVolume != null);
        return v;
    }
    
}

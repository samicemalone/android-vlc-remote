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
package org.peterbaldwin.vlcremote.listener;

import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.model.Button;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.net.MediaServer;
import org.peterbaldwin.vlcremote.widget.Buttons;

/**
 *
 * @author Sam Malone
 */
public class CommonPlaybackButtonsListener implements View.OnClickListener, View.OnLongClickListener {
    
    private MediaServer mMediaServer;
    
    public CommonPlaybackButtonsListener(MediaServer server) {
        mMediaServer = server;
    }
    
    public void setMediaServer(MediaServer server) {
        mMediaServer = server;
    }
    
    public void setUp(View view) {
        ImageButton mButtonFirst = (ImageButton) view.findViewById(R.id.menu_action_button_first);
        ImageButton mButtonSecond = (ImageButton) view.findViewById(R.id.menu_action_button_second);
        ImageButton mButtonThird = (ImageButton) view.findViewById(R.id.menu_action_button_third);
        ImageButton mButtonFourth = (ImageButton) view.findViewById(R.id.menu_action_button_fourth);
        ImageButton mButtonFifth = (ImageButton) view.findViewById(R.id.menu_action_button_fifth);

        setupImageButtonListeners(mButtonFirst, mButtonSecond, mButtonThird, mButtonFourth, mButtonFifth);
    }
    
    private void setupImageButtonListeners(ImageButton... imageButtons) {
        for(ImageButton b : imageButtons) {
            if(b != null) {
                Button info = Buttons.getButton(b.getId(), Preferences.get(b.getContext()));
                b.setImageResource(info.getIconId());
                b.setContentDescription(b.getContext().getString(info.getContentDescriptionId()));
                b.setOnClickListener(this);
                b.setOnLongClickListener(this);
            }
        }
    }

    /** {@inheritDoc} */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.menu_action_button_first:
                Buttons.sendCommand(mMediaServer, v.getContext(), Preferences.KEY_BUTTON_FIRST);
                break;
            case R.id.menu_action_button_second:
                Buttons.sendCommand(mMediaServer, v.getContext(), Preferences.KEY_BUTTON_SECOND);
                break;
            case R.id.menu_action_button_third:
                Buttons.sendCommand(mMediaServer, v.getContext(), Preferences.KEY_BUTTON_THIRD);
                break;
            case R.id.menu_action_button_fourth:
                Buttons.sendCommand(mMediaServer, v.getContext(), Preferences.KEY_BUTTON_FOURTH);
                break;
            case R.id.menu_action_button_fifth:
                Buttons.sendCommand(mMediaServer, v.getContext(), Preferences.KEY_BUTTON_FIFTH);
                break;
        }
    }

    public boolean onLongClick(View v) {
        Toast.makeText(v.getContext(), v.getContentDescription(), Toast.LENGTH_SHORT).show();
        return true;
    }
    
}

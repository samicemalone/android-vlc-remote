/*
 * Copyright (C) 2014 Sam Malone
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

package org.peterbaldwin.vlcremote.model;

import android.content.Context;
import android.widget.Toast;
import org.peterbaldwin.vlcremote.net.MediaServer;

/**
 *
 * @author Sam Malone
 */
public abstract class DelayPresetButton extends Button {
    
    public DelayPresetButton(String button, int iconId, int contentDescriptionId) {
        super(button, null, iconId, contentDescriptionId);
    }

    @Override
    public void sendCommand(MediaServer server, Context context) {
        Preferences p = Preferences.get(context);
        boolean isPresetOn = p.isPresetDelayInUse(getButton());
        onSendCommand(server, p, context, isPresetOn);
        p.setPresetDelayInUse(getButton(), !isPresetOn);
    }
    
    /**
     * Send the command to the media server
     * @param m Media Server
     * @param p Preferences
     * @param context Context
     * @param isPresetOn whether the preset is on before sending the command
     */
    public abstract void onSendCommand(MediaServer m, Preferences p, Context context, boolean isPresetOn);
    
}

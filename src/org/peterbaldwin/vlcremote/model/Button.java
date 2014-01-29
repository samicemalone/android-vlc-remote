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
import org.peterbaldwin.vlcremote.net.MediaServer;

/**
 * Stores information about a button
 * @author Sam Malone
 */
public class Button {
    
    private final String button;
    private final String hotkey;
    private final int iconId;
    private final int contentDescriptionId;

    
    /**
     * Creates a new Button instance
     * @param button button name. See @array/buttonEntryValues
     * @param hotkey button command hotkey. See {@link Hotkeys}
     * @param iconId button icon resource id
     * @param contentDescriptionId button content description/title
     */
    public Button(String button, String hotkey, int iconId, int contentDescriptionId) {
        this.button = button;
        this.hotkey = hotkey;
        this.iconId = iconId;
        this.contentDescriptionId = contentDescriptionId;
    }

    /**
     * Get the button content description/title
     * @return button content description/title
     */
    public int getContentDescriptionId() {
        return contentDescriptionId;
    }

    /**
     * Get the button name
     * @return button name
     */
    public String getButton() {
        return button;
    }

    /**
     * Get the button icon resource id
     * @return button icon resource id
     */
    public int getIconId() {
        return iconId;
    }
    
    /**
     * Sends the command that this button performs, to the media server.
     * @param server Media Server
     * @param context context
     */
    public void sendCommand(MediaServer server, Context context) {
        server.status().command.key(hotkey);
    }
}

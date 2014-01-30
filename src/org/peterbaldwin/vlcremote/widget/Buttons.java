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

package org.peterbaldwin.vlcremote.widget;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.model.Button;
import org.peterbaldwin.vlcremote.model.DelayPresetButton;
import org.peterbaldwin.vlcremote.model.Hotkeys;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.net.MediaServer;

/**
 * 
 * @author Sam Malone
 */
public class Buttons {
    
    private static final String DEFAULT = "default";

    private static final String DEFAULT_FIRST = "crop";
    private static final String DEFAULT_SECOND = "subtitle";
    private static final String DEFAULT_THIRD = "fullscreen";
    private static final String DEFAULT_FOURTH = "audio_track";
    private static final String DEFAULT_FIFTH = "aspect_ratio";
    
    /**
     * Get the Button for the ordinal preference button key using the values
     * stored in preferences
     * @param key ordinal preference button key e.g. {@link Preferences#KEY_BUTTON_FIRST}
     * @param pref Preferences that stores the button action
     * @return Button or null if button not found
     */
    public static Button getButton(String key, Preferences pref) {
        return getButton(key, pref.getButton(key));
    }
    
    /**
     * Get the Button with the given button name or get the default button for
     * the ordinal preference button key
     * @param key ordinal preference button key e.g. {@link Preferences#KEY_BUTTON_FIRST}
     * @param button Button name. See @array/buttonEntryValues.
     * @return Button or null if button is not found
     */
    public static Button getButton(String key, String button) {
        if(DEFAULT.equals(button)) {
            if(Preferences.KEY_BUTTON_FIRST.equals(key)) {
                button = DEFAULT_FIRST;
            } else if(Preferences.KEY_BUTTON_SECOND.equals(key)) {
                button = DEFAULT_SECOND;
            } else if(Preferences.KEY_BUTTON_THIRD.equals(key)) {
                button = DEFAULT_THIRD;
            } else if(Preferences.KEY_BUTTON_FOURTH.equals(key)) {
                button = DEFAULT_FOURTH;
            } else if(Preferences.KEY_BUTTON_FIFTH.equals(key)) {
                button = DEFAULT_FIFTH;
            }
        }
        return getButton(button);
    }
    
    /**
     * Get the Button for the ordinal menu id
     * @param menuId ordinal menu id e.g. R.id.menu_action_button_first
     * @param pref Preferences where the action is stored
     * @return Button or null if menu id not found
     */
    public static Button getButton(int menuId, Preferences pref) {
        switch (menuId) {
            case R.id.menu_action_button_first:
                return getButton(Preferences.KEY_BUTTON_FIRST, pref);
            case R.id.menu_action_button_second:
                return getButton(Preferences.KEY_BUTTON_SECOND, pref);
            case R.id.menu_action_button_third:
                return getButton(Preferences.KEY_BUTTON_THIRD, pref);
            case R.id.menu_action_button_fourth:
                return getButton(Preferences.KEY_BUTTON_FOURTH, pref);
            case R.id.menu_action_button_fifth:
                return getButton(Preferences.KEY_BUTTON_FIFTH, pref);
        }
        return null;
    }
    
    /**
     * Get the Button with the given name
     * @param button Button name. See @array/buttonEntryValues
     * @return Button or null if not found
     */
    public static Button getButton(final String button) {        
        if("crop".equals(button)) {
            return new Button(button, Hotkeys.CROP, R.drawable.ic_menu_crop, R.string.crop);
        } else if("subtitle".equals(button)) {
            return new Button(button, Hotkeys.SUBTITLE_TRACK, R.drawable.ic_menu_start_conversation, R.string.subtitle_track);
        } else if("fullscreen".equals(button)) {
            return new Button(button, Hotkeys.FULLSCREEN, R.drawable.ic_media_fullscreen, R.string.toggle_fullscreen);
        } else if("audio_track".equals(button)) {
            return new Button(button, Hotkeys.AUDIO_TRACK, R.drawable.ic_media_cycle_audio_track, R.string.audio_track);
        } else if("aspect_ratio".equals(button)) {
            return new Button(button, Hotkeys.ASPECT_RATIO, R.drawable.ic_menu_chat_dashboard, R.string.aspect_ratio);
        } else if("chapter_prev".equals(button)) {
            return new Button(button, Hotkeys.CHAPTER_PREV, R.drawable.ic_media_previous_chapter, R.string.desc_button_chapter_previous);
        } else if("chapter_next".equals(button)) {
            return new Button(button, Hotkeys.CHAPTER_NEXT, R.drawable.ic_media_next_chapter, R.string.desc_button_chapter_next);
        } else if("subtitle_delay_increase".equals(button)) {
            return new Button(button, Hotkeys.SUBTITLE_DELAY_INCREASE, R.drawable.ic_menu_subtitle_delay_increase, R.string.desc_button_subtitle_delay_increase);
        } else if("subtitle_delay_decrease".equals(button)) {
            return new Button(button, Hotkeys.SUBTITLE_DELAY_DECREASE, R.drawable.ic_menu_subtitle_delay_decrease, R.string.desc_button_subtitle_delay_decrease);
        } else if("audio_delay_increase".equals(button)) {
            return new Button(button, Hotkeys.AUDIO_DELAY_INCREASE, R.drawable.ic_media_audio_delay_increase, R.string.desc_button_audio_delay_increase);
        } else if("audio_delay_decrease".equals(button)) {
            return new Button(button, Hotkeys.AUDIO_DELAY_DECREASE, R.drawable.ic_media_audio_delay_decrease, R.string.desc_button_audio_delay_decrease);
        }
        return getDelayPresetButton(button);
    }
    
    /**
     * Get the delay preset Button with the given name
     * @param button Button name. See @array/buttonEntryValues
     * @return Button or null if not found
     */
    public static Button getDelayPresetButton(String button) {
        if("subtitle_delay_toggle".equals(button)) {
            return new DelayPresetButton(button, R.drawable.ic_menu_subtitle_delay_cycle, R.string.desc_button_subtitle_delay_toggle) {
                @Override
                public void onSendCommand(MediaServer m, Preferences p, Context context, boolean isPresetOn) {
                    int delay = !isPresetOn ? p.getSubtitleDelayToggle() : 0;
                    m.status().command.playback.subtitleDelay(Float.valueOf(delay));
                    p.setPresetDelayInUse("delay_toggle", false);
                    Toast.makeText(context, String.format("Set subtitle delay at %d ms", delay), Toast.LENGTH_SHORT).show();
                }                
            };
        } else if("audio_delay_toggle".equals(button)) {
            return new DelayPresetButton(button, R.drawable.ic_media_audio_delay_preset_cycle, R.string.desc_button_audio_delay_toggle) {
                @Override
                public void onSendCommand(MediaServer m, Preferences p, Context context, boolean isPresetOn) {
                    int delay = !isPresetOn ? p.getAudioDelayToggle() : 0;
                    m.status().command.playback.audioDelay(Float.valueOf(delay));
                    p.setPresetDelayInUse("delay_toggle", false);
                    Toast.makeText(context, String.format("Set audio delay at %d ms", delay), Toast.LENGTH_SHORT).show();
                }
            };
        } else if("delay_toggle".equals(button)) {
            return new DelayPresetButton(button, R.drawable.ic_media_delay_preset_cycle, R.string.desc_button_delay_toggle) {
                @Override
                public void onSendCommand(MediaServer m, Preferences p, Context context, boolean isPresetOn) {
                    int subtitleDelay = !isPresetOn ? p.getSubtitleDelayToggle(): 0;
                    int audioDelay = !isPresetOn ? p.getAudioDelayToggle() : 0;
                    m.status().command.playback.subtitleDelay(Float.valueOf(subtitleDelay));
                    m.status().command.playback.audioDelay(Float.valueOf(audioDelay));
                    p.setPresetDelayInUse("subtitle_delay_toggle", !isPresetOn);
                    p.setPresetDelayInUse("audio_delay_toggle", !isPresetOn);
                    Toast.makeText(context, String.format("Set subtitle delay at %d ms and audio delay at %d ms", subtitleDelay, audioDelay), Toast.LENGTH_SHORT).show();
                }
            };
        }
        return null;
    }
    
    /**
     * Send the command performed by the given button
     * @param server Media server
     * @param context Context
     * @param key Preference button key e.g. {@link Preferences#KEY_BUTTON_FIRST}
     */
    public static void sendCommand(MediaServer server, Context context, String key) {
        getButton(key, Preferences.get(context)).sendCommand(server, context);
    }
    
    /**
     * Setup the Menu with an icon and title for each of the menu action buttons
     * @param menu Menu
     * @param pref Preferences to fetch the ordinal button actions
     */
    public static void setupMenu(Menu menu, Preferences pref) {
        setupMenuItem(menu.findItem(R.id.menu_action_button_first), pref);
        setupMenuItem(menu.findItem(R.id.menu_action_button_second), pref);
        setupMenuItem(menu.findItem(R.id.menu_action_button_third), pref);
        setupMenuItem(menu.findItem(R.id.menu_action_button_fourth), pref);
        setupMenuItem(menu.findItem(R.id.menu_action_button_fifth), pref);
    }
    
    /**
     * Setup the menu item with an icon and title based on the users button
     * preference
     * @param item Ordinal Menu Item
     * @param pref Preferences
     */
    private static void setupMenuItem(MenuItem item, Preferences pref) {
        Button b = getButton(item.getItemId(), pref);
        item.setIcon(b.getIconId());
        item.setTitle(b.getContentDescriptionId());
    }
    
}

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

import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.model.Preferences;

/**
 *
 * @author Sam Malone
 */
public class Buttons {
    
    private static final String DEFAULT = "default";
    private static final String CROP = "crop";
    private static final String SUBTITLE_TRACK = "subtitle";
    private static final String FULLSCREEN = "fullscreen";
    private static final String AUDIO_TRACK = "audio_track";
    private static final String ASPECT_RATIO = "aspect_ratio";
    private static final String SUBTITLE_DELAY_INCREASE = "subtitle_delay_increase";
    private static final String SUBTITLE_DELAY_DECREASE = "subtitle_delay_decrease";
    private static final String SUBTITLE_DELAY_TOGGLE = "subtitle_delay_toggle";
    private static final String AUDIO_DELAY_INCREASE = "audio_delay_increase";
    private static final String AUDIO_DELAY_DECREASE = "audio_delay_decrease";
    private static final String AUDIO_DELAY_TOGGLE = "audio_delay_toggle";
    private static final String DELAY_TOGGLE = "delay_toggle";
    
    private static final String DEFAULT_FIRST = CROP;
    private static final String DEFAULT_SECOND = SUBTITLE_TRACK;
    private static final String DEFAULT_THIRD = FULLSCREEN;
    private static final String DEFAULT_FOURTH = AUDIO_TRACK;
    private static final String DEFAULT_FIFTH = ASPECT_RATIO;
    
    private static final int INVALID_BUTTON = -1;
    
    public static int getDrawableResourceId(String key, String button) {
        if(DEFAULT.equals(button)) {
            if(Preferences.KEY_BUTTON_FIRST.equals(key)) {
                return getDrawableResourceId(DEFAULT_FIRST);
            } else if(Preferences.KEY_BUTTON_SECOND.equals(key)) {
                return getDrawableResourceId(DEFAULT_SECOND);
            } else if(Preferences.KEY_BUTTON_THIRD.equals(key)) {
                return getDrawableResourceId(DEFAULT_THIRD);
            } else if(Preferences.KEY_BUTTON_FOURTH.equals(key)) {
                return getDrawableResourceId(DEFAULT_FOURTH);
            } else if(Preferences.KEY_BUTTON_FIFTH.equals(key)) {
                return getDrawableResourceId(DEFAULT_FIFTH);
            }
        }
        return getDrawableResourceId(button);
    }
    
    private static int getDrawableResourceId(String button) {
        if(CROP.equals(button)) {
            return R.drawable.ic_menu_crop;
        } else if(SUBTITLE_TRACK.equals(button)) {
            return R.drawable.ic_menu_start_conversation;
        } else if(FULLSCREEN.equals(button)) {
            return R.drawable.ic_media_fullscreen;
        } else if(AUDIO_TRACK.equals(button)) {
            return R.drawable.ic_media_cycle_audio_track;
        } else if(ASPECT_RATIO.equals(button)) {
            return R.drawable.ic_menu_chat_dashboard;
        } else if(SUBTITLE_DELAY_INCREASE.equals(button)) {
            return R.drawable.ic_menu_subtitle_delay_increase;
        } else if(SUBTITLE_DELAY_DECREASE.equals(button)) {
            return R.drawable.ic_menu_subtitle_delay_decrease;
        } else if(SUBTITLE_DELAY_TOGGLE.equals(button)) {
            return R.drawable.ic_menu_subtitle_delay_cycle;
        } else if(AUDIO_DELAY_INCREASE.equals(button)) {
            return R.drawable.ic_media_audio_delay_increase;
        } else if(AUDIO_DELAY_DECREASE.equals(button)) {
            return R.drawable.ic_media_audio_delay_decrease;
        } else if(AUDIO_DELAY_TOGGLE.equals(button)) {
            return R.drawable.ic_media_audio_delay_preset_cycle;
        } else if(DELAY_TOGGLE.equals(button)) {
            return R.drawable.ic_media_delay_preset_cycle;
        }
        return INVALID_BUTTON;
    }
    
    
    
}

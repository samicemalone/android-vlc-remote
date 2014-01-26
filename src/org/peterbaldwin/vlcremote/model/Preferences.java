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

package org.peterbaldwin.vlcremote.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Convenience class for reading and writing application preferences.
 */
public final class Preferences {

    /**
     * The server authority preference key
     */
    public static final String KEY_SERVER = "server";
    public static final String KEY_REMEMBERED_SERVERS = "remembered_servers";
    public static final String KEY_BROWSE_DIRECTORY = "browse_directory";
    public static final String KEY_HOME_DIRECTORY = "home_directory";
    public static final String KEY_RESUME_ON_IDLE = "resume_on_idle";
    public static final String KEY_PARSE_PLAYLIST_ITEMS = "parse_playlist_items";
    public static final String KEY_HIDE_DVD_TAB = "hide_dvd_tab";
    public static final String KEY_SORT_DIRECTORIES_FIRST = "sort_directories_first";
    public static final String KEY_SEEK_TIME = "seek_time";
    public static final String KEY_TEXT_SIZE = "browse_text_size";
    public static final String KEY_SERVER_SUBTITLE = "server_subtitle";
    public static final String KEY_AUDIO_DELAY = "audio_delay";
    public static final String KEY_SUBTITLE_DELAY = "subtitle_delay";
    
    private static final String PREFERENCES = "preferences";

    private final SharedPreferences mPreferences;

    public final static int TEXT_SMALL = 0;
    public final static int TEXT_MEDIUM = 1;
    public final static int TEXT_LARGE = 2;
    
    public Preferences(SharedPreferences preferences) {
        mPreferences = preferences;
    }

    public static Preferences get(Context context) {
        return new Preferences(context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE));
    }

    public boolean setResumeOnIdle() {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putLong(KEY_RESUME_ON_IDLE, System.currentTimeMillis());
        return editor.commit();
    }

    /**
     * Checks if {@link #setResumeOnIdle()} was called in the last hour.
     * @return {@code true} if {@link #setResumeOnIdle()} was called in the last hour.
     */
    public boolean isResumeOnIdleSet() {
        long start = mPreferences.getLong(KEY_RESUME_ON_IDLE, 0L);
        long end = System.currentTimeMillis();
        return start < end && (end - start) < DateUtils.HOUR_IN_MILLIS;
    }
    
    public boolean clearResumeOnIdle() {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.remove(KEY_RESUME_ON_IDLE);
        return editor.commit();
    }
    
    public boolean isSortDirectoriesFirst() {
        return mPreferences.getBoolean(KEY_SORT_DIRECTORIES_FIRST, false);
    }
	
    public boolean setSortDirectoriesFirst(boolean sortDirectoriesFirst) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(KEY_SORT_DIRECTORIES_FIRST, sortDirectoriesFirst);
        return editor.commit();
    }
    
    public boolean isParsePlaylistItems() {
        return mPreferences.getBoolean(KEY_PARSE_PLAYLIST_ITEMS, false);
    }
	
    public boolean setParsePlaylistItems(boolean parsePlaylist) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(KEY_PARSE_PLAYLIST_ITEMS, parsePlaylist);
        return editor.commit();
    }
    
    public boolean isServerSubtitleSet() {
        return mPreferences.getBoolean(KEY_SERVER_SUBTITLE, false);
    }
	
    public boolean setServerSubtitle(boolean isServerSubtitle) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(KEY_SERVER_SUBTITLE, isServerSubtitle);
        return editor.commit();
    }
    
    public boolean isHideDVDTabSet() {
        return mPreferences.getBoolean(KEY_HIDE_DVD_TAB, false);
    }
	
    public boolean setHideDVDTab(boolean hideTab) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(KEY_HIDE_DVD_TAB, hideTab);
        return editor.commit();
    }
	
    public boolean setSeekTime(String seekTime) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(KEY_SEEK_TIME, seekTime);
        return editor.commit();
    }
    
    public int getAudioDelayToggle() {
        return mPreferences.getInt(KEY_AUDIO_DELAY, 0);
    }
	
    public boolean setAudioDelayToggle(int delay) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(KEY_AUDIO_DELAY, delay);
        return editor.commit();
    }
    
    public int getSubtitleDelayToggle() {
        return mPreferences.getInt(KEY_SUBTITLE_DELAY, 0);
    }
	
    public boolean setSubtitleDelayToggle(int delay) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(KEY_SUBTITLE_DELAY, delay);
        return editor.commit();
    }
    
    public String getSeekTime() {
        return mPreferences.getString(KEY_SEEK_TIME, "10");
    }
    
    public boolean setTextSize(int textSize) {
        return mPreferences.edit().putInt(KEY_TEXT_SIZE, textSize).commit();
    }
    
    public int getTextSize() {
        return mPreferences.getInt(KEY_TEXT_SIZE, TEXT_LARGE);
    }

    public String getAuthority() {
        return mPreferences.getString(KEY_SERVER, null);
    }

    public String getHomeDirectory() {
        return mPreferences.getString(KEY_HOME_DIRECTORY, "~");
    }

    public String getBrowseDirectory() {
        return mPreferences.getString(KEY_BROWSE_DIRECTORY, "~");
    }

    public boolean setAuthority(String authority) {
        return mPreferences.edit().putString(KEY_SERVER, authority).commit();
    }

    public boolean setHomeDirectory(String dir) {
        return mPreferences.edit().putString(KEY_HOME_DIRECTORY, dir).commit();
    }

    public boolean setBrowseDirectory(String dir) {
        return mPreferences.edit().putString(KEY_BROWSE_DIRECTORY, dir).commit();
    }
    
    public ArrayList<String> getRememberedServers() {
        return fromJSONArray(mPreferences.getString(KEY_REMEMBERED_SERVERS, "[]"));
    }

    public boolean setRememberedServers(List<String> rememberedServers) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(KEY_REMEMBERED_SERVERS, toJSONArray(rememberedServers));
        return editor.commit();
    }

    private static String toJSONArray(List<String> list) {
        JSONArray array = new JSONArray(list);
        return array.toString();
    }

    private static ArrayList<String> fromJSONArray(String json) {
        try {
            JSONArray array = new JSONArray(json);
            int n = array.length();
            ArrayList<String> list = new ArrayList<String>(n);
            for (int i = 0; i < n; i++) {
                String element = array.getString(i);
                list.add(element);
            }
            return list;
        } catch (JSONException e) {
            return new ArrayList<String>(0);
        }
    }
}

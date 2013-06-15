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

    private static final String PREFERENCES = "preferences";

    private static final String PREFERENCE_SERVER = "server";

    private static final String PREFERENCE_REMEMBERED_SERVERS = "remembered_servers";

    private static final String PREFERENCE_BROWSE_DIRECTORY = "browse_directory";

    private static final String PREFERENCE_HOME_DIRECTORY = "home_directory";

    private static final String PREFERENCE_RESUME_ON_IDLE = "resume_on_idle";
    
    private static final String PREFERENCE_PARSE_PLAYLIST_ITEMS = "parse_playlist_items";
    
    private static final String PREFERENCE_HIDE_DVD_TAB = "hide_dvd_tab";
    
    private static final String PREFERENCE_SORT_DIRECTORIES_FIRST = "sort_directories_first";

    private SharedPreferences mPreferences;

    public Preferences(SharedPreferences preferences) {
        mPreferences = preferences;
    }

    public static Preferences get(Context context) {
        return new Preferences(context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE));
    }

    public boolean setResumeOnIdle() {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putLong(PREFERENCE_RESUME_ON_IDLE, System.currentTimeMillis());
        return editor.commit();
    }

    /**
     * Returns {@code true} if {@link #setResumeOnIdle()} was called in the last
     * hour.
     */
    public boolean isResumeOnIdleSet() {
        long start = mPreferences.getLong(PREFERENCE_RESUME_ON_IDLE, 0L);
        long end = System.currentTimeMillis();
        return start < end && (end - start) < DateUtils.HOUR_IN_MILLIS;
    }
    
    public boolean clearResumeOnIdle() {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.remove(PREFERENCE_RESUME_ON_IDLE);
        return editor.commit();
    }
    
    public boolean isSortDirectoriesFirst() {
        return mPreferences.getBoolean(PREFERENCE_SORT_DIRECTORIES_FIRST, false);
    }
	
    public boolean setSortDirectoriesFirst(boolean sortDirectoriesFirst) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(PREFERENCE_SORT_DIRECTORIES_FIRST, sortDirectoriesFirst);
        return editor.commit();
    }
    
    public boolean isParsePlaylistItems() {
        return mPreferences.getBoolean(PREFERENCE_PARSE_PLAYLIST_ITEMS, false);
    }
	
    public boolean setParsePlaylistItems(boolean parsePlaylist) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(PREFERENCE_PARSE_PLAYLIST_ITEMS, parsePlaylist);
        return editor.commit();
    }
    
    public boolean isHideDVDTabSet() {
        return mPreferences.getBoolean(PREFERENCE_HIDE_DVD_TAB, false);
    }
	
    public boolean setHideDVDTab(boolean hideTab) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(PREFERENCE_HIDE_DVD_TAB, hideTab);
        return editor.commit();
    }

    public String getAuthority() {
        return mPreferences.getString(PREFERENCE_SERVER, null);
    }

    public String getHomeDirectory() {
        return mPreferences.getString(PREFERENCE_HOME_DIRECTORY, "~");
    }

    public String getBrowseDirectory() {
        return mPreferences.getString(PREFERENCE_BROWSE_DIRECTORY, "~");
    }

    public boolean setAuthority(String authority) {
        return mPreferences.edit().putString(PREFERENCE_SERVER, authority).commit();
    }

    public boolean setHomeDirectory(String dir) {
        return mPreferences.edit().putString(PREFERENCE_HOME_DIRECTORY, dir).commit();
    }

    public boolean setBrowseDirectory(String dir) {
        return mPreferences.edit().putString(PREFERENCE_BROWSE_DIRECTORY, dir).commit();
    }
    
    public ArrayList<String> getRememberedServers() {
        return fromJSONArray(mPreferences.getString(PREFERENCE_REMEMBERED_SERVERS, "[]"));
    }

    public boolean setRemeberedServers(List<String> rememberedServers) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(PREFERENCE_REMEMBERED_SERVERS, toJSONArray(rememberedServers));
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

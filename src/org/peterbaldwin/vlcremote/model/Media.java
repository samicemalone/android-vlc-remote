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
package org.peterbaldwin.vlcremote.model;

import java.io.Serializable;

/**
 *
 * @author Sam Malone
 */
public abstract class Media implements Serializable, PlaylistItem, MediaDisplayInfo {
    
    private static final long serialVersionUID = 1L;
    
    protected int mId;
    protected boolean mCurrent;
    protected String mUri;
    protected String mName;

    public int getId() {
        return mId;
    }
    
    public void setId(int id) {
        mId = id;
    }

    public String getName() {
        return mName;
    }
    
    public void setName(String name) {
        mName = name;
    }

    public String getUri() {
        return mUri;
    }
    
    public void setUri(String uri) {
        mUri = uri;
    }

    public boolean isCurrent() {
        return mCurrent;
    }
    
    public void setCurrent(boolean current) {
        mCurrent = current;
    }
    
    /**
     * Copy the values of ID, Name, Uri and whether the media is currently
     * playing from the PlaylistItem instance given
     * @param media PlaylistItem instance to copy values from
     */
    public final void copyPlaylistItemFrom(PlaylistItem media) {
        setId(media.getId());
        setCurrent(media.isCurrent());
        setName(media.getName());
        setUri(media.getUri());
    }
    
}

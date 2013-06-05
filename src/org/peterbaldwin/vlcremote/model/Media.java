/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.peterbaldwin.vlcremote.model;

/**
 *
 * @author Sam Malone
 */
public abstract class Media implements PlaylistItem {
    
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

    /** {@inheritDoc} */
    public abstract String getPlaylistHeading();

    /** {@inheritDoc} */
    public abstract String getPlaylistText();
    
}

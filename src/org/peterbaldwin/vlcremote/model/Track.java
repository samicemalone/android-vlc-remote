/*-
 *  Copyright (C) 2009 Peter Baldwin   
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

import android.text.TextUtils;
import java.io.Serializable;

public final class Track extends Media implements Serializable {
    
    private static final byte UNKNOWN_STREAM = 0;
    
    private static final byte AUDIO_STREAM_FLAG = 1;
    private static final byte VIDEO_STREAM_FLAG = 2;
    private static final byte SUBTITLE_STREAM_FLAG = 4;    

    private static final long serialVersionUID = 1L;
    
    private long mDuration;

    private String mTitle;

    private String mArtist;

    private String mGenre;

    private String mCopyright;

    private String mAlbum;

    private String mTrack;

    private String mDescription;

    private String mRating;

    private String mDate;

    private String mUrl;

    private String mLanguage;

    private String mNowPlaying;

    private String mPublisher;

    private String mEncodedBy;

    private String mArtUrl;

    private String mTrackId;
    
    /**
     * Bit Mask of the stream types the track holds.
     */
    private byte streamTypesMask;
    
    public String getPlaylistHeading() {
        return isNotEmpty(mTitle) ? mTitle : isNotEmpty(mName) ? mName : "";
    }

    public String getPlaylistText() {
        return isNotEmpty(mArtist) ? mArtist : isNotEmpty(mTitle) ? mName : "";
    }

    public long getDuration() {
        return mDuration;
    }

    public void setDuration(long duration) {
        mDuration = duration;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public String getArtist() {
        return mArtist;
    }

    public void setArtist(String artist) {
        this.mArtist = artist;
    }

    public String getGenre() {
        return mGenre;
    }

    public void setGenre(String genre) {
        this.mGenre = genre;
    }

    public String getCopyright() {
        return mCopyright;
    }

    public void setCopyright(String copyright) {
        this.mCopyright = copyright;
    }

    public String getAlbum() {
        return mAlbum;
    }

    public void setAlbum(String album) {
        this.mAlbum = album;
    }

    public String getTrack() {
        return mTrack;
    }

    public void setTrack(String track) {
        this.mTrack = track;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        this.mDescription = description;
    }

    public String getRating() {
        return mRating;
    }

    public void setRating(String rating) {
        this.mRating = rating;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        this.mDate = date;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    public String getLanguage() {
        return mLanguage;
    }

    public void setLanguage(String language) {
        this.mLanguage = language;
    }

    public String getNowPlaying() {
        return mNowPlaying;
    }

    public void setNowPlaying(String nowPlaying) {
        this.mNowPlaying = nowPlaying;
    }

    public String getPublisher() {
        return mPublisher;
    }

    public void setPublisher(String publisher) {
        this.mPublisher = publisher;
    }

    public String getEncodedBy() {
        return mEncodedBy;
    }

    public void setEncodedBy(String encodedBy) {
        this.mEncodedBy = encodedBy;
    }

    public String getArtUrl() {
        return mArtUrl;
    }

    public void setArtUrl(String art_url) {
        this.mArtUrl = art_url;
    }

    public String getTrackId() {
        return mTrackId;
    }

    public void setTrackId(String trackId) {
        this.mTrackId = trackId;
    }
    
    /**
     * Add a stream type for the Track.
     * @param streamType "Video" for a video track, "Audio" for an audio track
     * and "Subtitle" for a subtitle track.
     */
    public void addStreamType(String streamType) {
        if("Video".equals(streamType)) {
            streamTypesMask |= VIDEO_STREAM_FLAG;
            return;
        }
        if("Audio".equals(streamType)) {
            streamTypesMask |= AUDIO_STREAM_FLAG;
            return;
        }
        if("Subtitle".equals(streamType)) {
            streamTypesMask |= SUBTITLE_STREAM_FLAG;
        }
    }
    
    /**
     * Checks if the Track has a video stream.
     * @return true if track has a video stream, false otherwise
     */
    public boolean hasVideoStream() {
        return (streamTypesMask & VIDEO_STREAM_FLAG) == VIDEO_STREAM_FLAG;
    }
    
    /**
     * Checks if the Track has an audio stream.
     * @return true if track has an audio stream, false otherwise
     */
    public boolean hasAudioStream() {
        return (streamTypesMask & AUDIO_STREAM_FLAG) == AUDIO_STREAM_FLAG;
    }
    
    /**
     * Checks if the Track has a subtitle stream.
     * @return true if track has a subtitle stream, false otherwise
     */
    public boolean hasSubtitleStream() {
        return (streamTypesMask & SUBTITLE_STREAM_FLAG) == SUBTITLE_STREAM_FLAG;
    }
    
    /**
     * Checks if the Track contains a stream of any type (audio, video or 
     * subtitle).
     * @return true if track contains an audio, video or subtitle stream, false
     * otherwise
     */
    public boolean containsStream() {
        return streamTypesMask != UNKNOWN_STREAM;
    }

    @Override
    public String toString() {
        // XSPF playlists set the title, but use a URL for the name.
        // M3U playlists don't have a title, but set a good name.
        return mTitle != null ? mTitle : mName;
    }

    private static boolean isNotEmpty(CharSequence text) {
        return !TextUtils.isEmpty(text);
    }

    public String getMediaHeading() {
        return isNotEmpty(mArtist) ? mArtist : "";
    }

    public String getMediaFirstText() {
        return isNotEmpty(mAlbum) ? mAlbum : "";
    }

    public String getMediaSecondText() {
        return isNotEmpty(mTitle) ? mTitle : isNotEmpty(mName) ? mName : "";
    }
}

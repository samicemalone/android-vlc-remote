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

package org.peterbaldwin.client.android.vlcremote;

import java.io.Serializable;

class Track implements Serializable {

	private static final long serialVersionUID = 1L;

	private int mId;
	private boolean mCurrent;
	private String mUri;
	private String mName;
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

	public int getId() {
		return mId;
	}

	public void setId(int id) {
		mId = id;
	}

	public boolean isCurrent() {
		return mCurrent;
	}

	public void setCurrent(boolean current) {
		mCurrent = current;
	}

	public String getUri() {
		return mUri;
	}

	public void setUri(String uri) {
		mUri = uri;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		mName = name;
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

	@Override
	public String toString() {
		// XSPF playlists set the title, but use a URL for the name.
		// M3U playlists don't have a title, but set a good name.
		return mTitle != null ? mTitle : mName;
	}
}

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

class Status implements Serializable {

	private static final long serialVersionUID = 1L;

	private int mVolume;
	private int mLength;
	private int mTime;
	private String mState;
	private double mPosition;
	private boolean mFullscreen;
	private boolean mRandom;
	private boolean mLoop;
	private boolean mRepeat;

	private final Track mTrack = new Track();

	public int getVolume() {
		return mVolume;
	}

	/**
	 * Returns the length of the media in seconds.
	 */
	public int getLength() {
		return mLength;
	}

	public int getTime() {
		return mTime;
	}

	public boolean isPlaying() {
		return "playing".equals(mState);
	}

	public boolean isPaused() {
		return "paused".equals(mState);
	}

	public boolean isStopped() {
		return "stop".equals(mState);
	}

	public String getState() {
		return mState;
	}

	/**
	 * Returns the playback position as a percentage.
	 */
	public double getPosition() {
		return mPosition;
	}

	public boolean isFullscreen() {
		return mFullscreen;
	}

	public boolean isRandom() {
		return mRandom;
	}

	public boolean isLoop() {
		return mLoop;
	}

	public boolean isRepeat() {
		return mRepeat;
	}

	public void setVolume(int volume) {
		mVolume = volume;
	}

	public void setLength(int length) {
		mLength = length;
	}

	public void setTime(int time) {
		mTime = time;
	}

	public void setState(String state) {
		mState = state;
	}

	public void setPosition(double position) {
		mPosition = position;
	}

	public void setFullscreen(boolean fullscreen) {
		mFullscreen = fullscreen;
	}

	public void setRandom(boolean random) {
		mRandom = random;
	}

	public void setLoop(boolean loop) {
		mLoop = loop;
	}

	public void setRepeat(boolean repeat) {
		mRepeat = repeat;
	}

	public Track getTrack() {
		return mTrack;
	}
}

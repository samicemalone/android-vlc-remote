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

public final class Status implements Serializable {

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
        return "stopped".equals(mState) || "stop".equals(mState);
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
    
    /**
     * Check if the given state matches the state of this status
     * The status is considered to be equal if the state (playing, paused, 
     * stopped) and the file name are equal.
     * @param fileName file name
     * @param state state. see {@link #getState()}
     * @return true if the track filename and the state are the equal, false
     * otherwise
     */
    public boolean equalsState(String fileName, String state) {
        return TextUtils.equals(fileName, getTrack().getName()) &&
               TextUtils.equals(state, mState);
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

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

package org.peterbaldwin.vlcremote.model;

import java.io.Serializable;

/**
 *
 * @author Sam Malone
 */
public class Chapters implements Serializable {
    
    public static final int UNKNOWN = -1;
    
    private static final long serialVersionUID = 1L;
    
    private int mChapter = UNKNOWN;
    private int mLastChapter;
    
    /**
     * Get chapter if known
     * @return chapter if known, or {@link #UNKNOWN}
     */
    public int getChapter() {
        return mChapter;
    }

    /**
     * Get the last chapter
     * @return last chapter or 0 if no chapters
     */
    public int getLastChapter() {
        return mLastChapter;
    }

    /**
     * Set the current chapter
     * @param mChapter Chapter or {@link #UNKNOWN} 
     */
    public void setChapter(int mChapter) {
        this.mChapter = mChapter;
    }
    
    /**
     * Set the last chapter
     * @param mLastChapter Last chapter or 0 if no chapter
     */
    public void setLastChapter(int mLastChapter) {
        this.mLastChapter = mLastChapter;
    }
    
    /**
     * Check if the chapters can be navigated (i.e. next, prev)
     * @return true if chapters can be navigated, false otherwise
     */
    public boolean canNavigateChapters() {
        return hasChapters() && mChapter != UNKNOWN;
    }
    
    /**
     * Check if the chapters are known
     * @return true if the chapters are known, false otherwise
     */
    public boolean hasChapters() {
        return mLastChapter != 0;
    }
    
    /**
     * Get the next chapter
     * @return the next chapter or the same chapter if its the last one
     */
    public int getNextChapter() {
        return Math.min(mChapter + 1, mLastChapter);
    }
    
    /**
     * Get the previous chapter
     * @return the previous chapter or the first chapter if its already the first one
     */
    public int getPreviousChapter() {
        return Math.max(0, mChapter - 1);
    }
    
    
}

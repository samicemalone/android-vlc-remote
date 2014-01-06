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
package org.peterbaldwin.vlcremote.listener;

/**
 *
 * @author Sam Malone
 */
public interface ProgressListener {
    
    public final static int START = 0;
    public final static int MAX = 9999;
    public final static int FINISHED = 10000;
    
    /**
     * Called periodically when progress is updated.
     * @param progress 0 - 9999 is the range of progress. 10000 will hide
     * the progress bar. See {@link #FINISHED}, {@link #START}, {@link #MAX} 
     */
    public void onProgress(int progress);
    
}

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

/**
 * Abstraction that allows any type of media (music, tv, movies etc...)
 * to be displayed as a playlist item via the interface.
 * @author Sam Malone
 */
public interface PlaylistDisplayItem {
    
    /**
     * Get the heading for the playlist display item
     * @return playlist display item heading or empty string
     */
    public String getPlaylistHeading();

    /**
     * Get the text for the playlist display item
     * @return playlist display item text or empty string
     */
    public String getPlaylistText();
}

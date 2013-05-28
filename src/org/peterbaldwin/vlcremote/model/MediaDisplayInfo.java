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
 *
 * @author Abstraction that allows any type of media (music, tv, movies etc...)
 * to be displayed on screen via the interface
 */
public interface MediaDisplayInfo {
    
    /**
     * Gets the heading for the media display item. This will generally be a
     * larger text size than the other text items.
     * @return media display item or empty string
     */
    public String getMediaHeading();
    
    /**
     * Get the first text media display item
     * @return media display item or empty string
     */
    public String getMediaFirstText();
    
    /**
     * Get the second text media display item
     * @return media display item or empty string
     */
    public String getMediaSecondText();
    
}

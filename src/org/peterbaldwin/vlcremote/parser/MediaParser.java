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
package org.peterbaldwin.vlcremote.parser;

import org.peterbaldwin.vlcremote.model.Media;

/**
 * 
 * @author Sam Malone
 */
public class MediaParser {
    
    private EpisodeParser mEpisodeParser;
    private MovieParser mMovieParser;
    
    public MediaParser() {
        mEpisodeParser = new EpisodeParser();
        mMovieParser = new MovieParser();
    }
    
    /**
     * Parse a file path into a media item
     * @param path File path to parse
     * @return Media or null if unable to parse path
     */
    public Media parse(String path) {
        Media media;
        if((media = mEpisodeParser.parse(path)) != null) {
            return media;
        }
        if((media = mMovieParser.parse(path)) != null) {
            return media;
        }
        return null;
    }
    
}

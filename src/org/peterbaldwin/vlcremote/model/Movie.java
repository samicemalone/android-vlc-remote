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
 * @author Sam Malone
 */
public class Movie implements MediaDisplayInfo {
    
    public final static int UNKNOWN_YEAR = 0;
    
    private String movieName;
    private String quality;
    private String source;
    private int year;

    public Movie(String movieName, String quality, String source, int year) {
        this.movieName = movieName;
        this.quality = quality;
        this.source = source;
        this.year = year;
    }

    public Movie() {
        year = UNKNOWN_YEAR;
    }

    /**
     * Get the movie name
     * @return movie name or null if not set
     */
    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    /**
     * Get the quality of the movie.
     * @return quality or null if not set
     */
    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    /**
     * Get the source of the movie
     * @return Source or null if not set
     */
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Get the year of the movie.
     * @return year or UNKNOWN_YEAR if not set
     */
    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getMediaHeading() {
        return movieName;
    }

    public String getMediaFirstText() {
        if(year == UNKNOWN_YEAR) {
            return "";
        }
        return String.valueOf(year);
    }

    public String getMediaSecondText() {
        String text = "";
        if(quality != null) {
            text = quality + " ";
        }
        if(source != null) {
            return text.concat(source);
        }
        return text;
    }
    
}

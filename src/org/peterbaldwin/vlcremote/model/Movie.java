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
        
    }

    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

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

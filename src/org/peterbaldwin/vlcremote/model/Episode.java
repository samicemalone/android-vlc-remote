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
public class Episode extends Media {
    
    public final static int NO_EPISODE = -1;
    
    private String show;
    private String episodeName;
    private int seasonNo;
    private int episodeNo;

    /**
     * Creates an instance of Episode
     * @param show TV Show
     * @param seasonNo Season number
     * @param episodeNo Episode number
     * @param episodeName Episode Name
     */
    public Episode(String show, int seasonNo, int episodeNo, String episodeName) {
        this.show = show;
        this.seasonNo = seasonNo;
        this.episodeNo = episodeNo;
        this.episodeName = episodeName;
    }
    
    /**
     * Creates an empty instance of Episode
     */
    public Episode() {
        episodeNo = NO_EPISODE;
    }
    
    /**
     * Gets the episode number
     * @return Episode number or NO_EPISODE if not set
     */
    public int getEpisodeNo() {
        return episodeNo;
    }

    /**
     * Sets the episode number
     * @param episodeNo Episode number
     */
    public void setEpisodeNo(int episodeNo) {
        this.episodeNo = episodeNo;
    }

    /**
     * Gets the season number
     * @return Season number
     */
    public int getSeasonNo() {
        return seasonNo;
    }

    /**
     * Sets the season number
     * @param seasonNo Season number
     */
    public void setSeasonNo(int seasonNo) {
        this.seasonNo = seasonNo;
    }

    /**
     * Sets the TV Show
     * @return 
     */
    public String getShow() {
        return show;
    }

    /**
     * Gets the TV Show
     * @param show 
     */
    public void setShow(String show) {
        this.show = show;
    }
    
    /**
     * Sets the TV Show
     * @return Episode Name or empty string if not set
     */
    public String getEpisodeName() {
        if(episodeName == null) {
            return "";
        }
        return episodeName;
    }

    /**
     * Gets the TV Show
     * @param Episode Name 
     */
    public void setEpisodeName(String episodeName) {
        this.episodeName = episodeName;
    }
    
    /**
     * Gets the formatted season number and episode
     * @return formatted string e.g. 2x01
     */
    public String getFormattedEpisode() {
        if(episodeNo == NO_EPISODE) {
            return String.format("S%02d", seasonNo);
        }
        return String.format("%dx%02d", seasonNo, episodeNo);
    }

    @Override
    public String toString() {
        return String.format("%s S%02dE%02d", show, seasonNo, episodeNo);
    }

    public String getMediaHeading() {
        return show;
    }

    public String getMediaFirstText() {
        return getFormattedEpisode();
    }

    public String getMediaSecondText() {
        return getEpisodeName();
    }

    @Override
    public String getPlaylistHeading() {
        return String.format("%s - %s", show, getFormattedEpisode());
    }
    @Override
    public String getPlaylistText() {
        return getEpisodeName();
    }
    
}

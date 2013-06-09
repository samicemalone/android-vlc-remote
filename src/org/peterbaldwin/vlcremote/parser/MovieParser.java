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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.peterbaldwin.vlcremote.model.File;
import org.peterbaldwin.vlcremote.model.Movie;
import org.peterbaldwin.vlcremote.model.VideoSources;
import org.peterbaldwin.vlcremote.util.StringUtil;

/**
 *
 * @author Sam Malone
 */
public class MovieParser {
    
    private static final String quality = "(?:480|720|1080)[ip]";
    private static final String sources = "a?hdtv|pdtv|web[- ]*dl|bluray|dvdr(?:ip)?|bdrip|brrip|hddvd|ts|tc|(?:dvd)?scr|cam|rc|r5|hdrip";
    private static final String repacks = "proper|repack|real|rerip|dubbed|subbed|internal|int|oar|ppv|convert|native|readnfo";
    private static final String versions = "ws|uncut|extended|theatrical|limited|retail|dc|se|(?:un)?rated|stv|dl|pal|ntsc";
    private static final String fixes = "(?:nfo|dir|sample|sync|proof|rar)[ -._]*fix";
    
    private final String[] regexList = new String[] {
        // strictest regex. scene variants. matches year (4 groups)
        "^(.*)[ -._]+([0-9]{4})[^ip][ -._]*(?:(?:"+repacks+"|"+versions+"|"+fixes+")[ -._]*)*(?:("+quality+")[ -._]*)*("+sources+")*.*?$",
        // scene variants but without year (3 groups)
        "^(.*?)[ -._]+(?:(?:"+repacks+"|"+versions+"|"+fixes+")[ -._]*)*(?:("+quality+")[ -._]*)*("+sources+")[ -._]+.*?$",
        // scene variants but without year and source (2 groups)
        "^(.*)[ -._]+(?:(?:"+repacks+"|"+versions+"|"+fixes+")[ -._]*)*(?:("+quality+")[ -._]*)+.*?$"
    };
    
    private Pattern[] patternList;
    
    public MovieParser() {
        patternList = new Pattern[regexList.length];
        for(int i = 0; i < regexList.length; i++) {
            patternList[i] = Pattern.compile(regexList[i], Pattern.CASE_INSENSITIVE);
        }
    }
    
    public Movie parse(String path) {
        Movie movie;
        String fileName = File.baseName(path);
        for(Pattern p : patternList) {
            Matcher m = p.matcher(fileName);
            if (m.find()) {
                movie = new Movie();
                movie.setMovieName(StringUtil.formatMatch(m.group(1)));
                if(m.groupCount() == 4) {
                    movie.setYear(Integer.valueOf(m.group(2)));
                    movie.setQuality(m.group(3));
                    movie.setSource(VideoSources.getFormattedSource(m.group(4)));
                } else if(m.groupCount() == 3) {
                    movie.setQuality(m.group(2));
                    movie.setSource(VideoSources.getFormattedSource(m.group(3)));
                } else {
                    movie.setQuality(m.group(2));
                }
                return movie;
            }
        }
        return null;
    }
    
}

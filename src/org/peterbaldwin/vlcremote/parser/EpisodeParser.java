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
import org.peterbaldwin.vlcremote.model.Episode;
import org.peterbaldwin.vlcremote.model.File;
import org.peterbaldwin.vlcremote.util.StringUtil;

/**
 *
 * @author Sam Malone
 */
public class EpisodeParser {
    
    private static final String quality = "(?:480|720|1080)[ip]";
    private static final String sources = "a?hdtv|pdtv|web[- ]*dl|bluray|dvdrip|bdrip|brrip|hddvd";
    private static final String repacks = "proper|repack|real|uncut|dubbed|subbed|internal|oar|ppv|convert|native";
    private static final String fixes = "(?:nfo|dir|sample|sync|proof)[ -._]*fix";
    
    /**
     * Array of regular expressions to match an episode against. Lowest index
     * is the strictest regex. It is not recommended to use individual regex
     * array elements in isolation because the less strict regular expressions 
     * may match incorrect values that should have been caught by a stricter 
     * regex. Instead, iterate the array until a match is found. 
     */
    private final static String[] regexList = new String[] {
        // scene regex (and web-dl). tries to match episode name. matches s01e02, 1x02
        "^(.*?)[ -._]+s?([0-9]+)[ -._]*[xe]([0-9]+)[ -._]+(.*?)[ -._]*(?:"+repacks+"[ -._]*)*(?:"+fixes+"[ -._]*)*(?:"+quality+"[ -._]*)*(?:"+sources+")+",
        // scene tv special - show.name.s01.special.name.etc.. (no episode set) group(3) = name
        "^(.*?)[ -._]+s([0-9]+)[ -._]+(.*?)[ -._]*(?:"+repacks+"[ -._]*)*(?:"+fixes+"[ -._]*)*(?:"+quality+"[ -._]*)*(?:"+sources+")+",
        // scene variant with no source, quality etc.. - show.name.1x01.name.ext
        "^(.*?)[ -._]+s?([0-9]+)[ -._]*[xe]([0-9]+)[ -._]*(.*?)\\.[A-Za-z0-9]+$"
    };
    
    private Pattern[] patternList;

    public EpisodeParser() {
        patternList = new Pattern[regexList.length];
        for(int i = 0; i < regexList.length; i++) {
            patternList[i] = Pattern.compile(regexList[i], Pattern.CASE_INSENSITIVE);
        }
    }
    
    /**
     * Attempt to parse the given file path into an Episode object
     * @param path Episode Path
     * @return Parsed Episode or null
     */
    public Episode parse(String path) {
        Episode e;
        String fileName = File.baseName(path);
        for(Pattern p : patternList) {
            Matcher m = p.matcher(fileName);
            if (m.find()) {
                e = new Episode();
                e.setShow(StringUtil.formatMatch(m.group(1)));
                e.setSeasonNo(Integer.valueOf(m.group(2)));
                try {
                    e.setEpisodeNo(Integer.valueOf(m.group(3)));
                } catch(NumberFormatException ex) {
                    // no episode. group(3) should be episode name
                    e.setEpisodeName(StringUtil.formatMatch(m.group(3)));
                }
                if(m.groupCount() == 4) {
                    e.setEpisodeName(StringUtil.formatMatch(m.group(4)));
                }
                return e;
            }
        }
        return null;
    }
    
}

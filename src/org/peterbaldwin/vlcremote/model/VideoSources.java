/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.peterbaldwin.vlcremote.model;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Sam Malone
 */
public class VideoSources {
    
    private static final Map<String, String> sourceMap;
    static {
        sourceMap = new HashMap<String, String>();
        sourceMap.put("ahdtv", "AHDTV");
        sourceMap.put("hdtv", "HDTV");
        sourceMap.put("pdtv", "PDTV");
        sourceMap.put("webdl", "WEB-DL");
        sourceMap.put("web-dl", "WEB-DL");
        sourceMap.put("web dl", "WEB-DL");
        sourceMap.put("bluray", "Bluray");
        sourceMap.put("dvdr", "DVDR");
        sourceMap.put("dvdrip", "DVDRip");
        sourceMap.put("bdrip", "BDRip");
        sourceMap.put("brrip", "BRRip");
        sourceMap.put("hddvd", "HDDVD");
        sourceMap.put("ts", "TS");
        sourceMap.put("tc", "TC");
        sourceMap.put("dvdscr", "DVDSCR");
        sourceMap.put("scr", "SCR");
        sourceMap.put("cam", "CAM");
        sourceMap.put("rc", "RC");
        sourceMap.put("r5", "R5");
        sourceMap.put("hdrip", "HDRip");
    }
    
    /**
     * Gets a formatted video source, or return the same source if a matching
     * source was not found.
     * @param source Video Source
     * @return Formatted source if source exists. Empty string if source is 
     * null. If a formatted source does not exist, then the original "source" 
     * will be returned. E.g. dvdrip = DVDRip
     */
    public static String getFormattedSource(String source) {
        if(source == null) {
            return "";
        }
        String formatted = sourceMap.get(source.toLowerCase());
        if(formatted == null) {
            return source;
        }
        return formatted;
    }
    
}

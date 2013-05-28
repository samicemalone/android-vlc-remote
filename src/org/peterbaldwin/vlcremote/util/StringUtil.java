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
package org.peterbaldwin.vlcremote.util;

/**
 *
 * @author Sam Malone
 */
public class StringUtil {
    
    /**
     * Converts a string into title case. e.g. this string => This String
     * @param s String to be converted to title case
     * @return String s in title case
     */
    public static String toTitleCase(String s) {
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;
        for (char c : s.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }
            titleCase.append(c);
        }
        return titleCase.toString();
    }
    
    /**
     * Formats a string by replacing delimeter characters (._) with spaces
     * and converts into title case. e.g. this.string_here => This String Here
     * @param matchedString
     * @return 
     */
    public static String formatMatch(String matchedString) {
        return StringUtil.toTitleCase(matchedString.replaceAll("\\.|_", " "));
    }
    
}

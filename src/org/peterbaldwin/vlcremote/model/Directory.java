/*-
 *  Copyright (C) 2009 Peter Baldwin   
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

import java.util.ArrayList;
import java.util.Comparator;

@SuppressWarnings("serial")
public final class Directory extends ArrayList<File> implements Comparator<File> {
    
    public static final String UNIX_DIRECTORY = "/";
    public static final String WINDOWS_ROOT_DIRECTORY = "";
    public static String ROOT_DIRECTORY = UNIX_DIRECTORY;

    public Directory() {
    }

    public Directory(int capacity) {
        super(capacity);
    }

    /**
     * Get the path to the current directory.
     * The path is determined from existing files in the directory or the parent
     * entry if it exists. If there are no items in the directory and there is
     * no parent entry, then the ROOT_DIRECTORY will be returned.
     * @return current directory path or ROOT_DIRECTORY
     */
    public String getPath() {
        String tmpRoot = null;
        for (File file : this) {
            String path = file.getPath();
            if (file.isParent()) {
                if(path != null && path.endsWith("..")) {
                    final int length = path.length() - "..".length();
                    return File.getNormalizedPath(path.substring(0, length));
                }
            } else {
                path = File.getNormalizedPath(file.getPath().concat("/.."));
                if(tmpRoot == null) {
                    tmpRoot = path; // ensure two directory entries are checked
                    continue;       // for same root. if not then root is drive
                }
                return tmpRoot.equals(path) ? path : ROOT_DIRECTORY;
            }
        }
        return ROOT_DIRECTORY;
    }

    /**
     * Compares two Files that are to be sorted with directories being displayed
     * before files. The parent entry will be first if present, then the
     * directories and then files.
     * @param firstFile
     * @param secondFile
     * @return a negative integer, zero, or a positive integer as the first 
     * argument is less than, equal to, or greater than the second.
     */
    public int compare(File firstFile, File secondFile) {
        // parent always first
        if(firstFile.isDirectory() && firstFile.isParent() && secondFile.isDirectory() && secondFile.isParent()) {
            return 0;
        }
        if(firstFile.isDirectory() && firstFile.isParent()) {
            return -1;
        }
        if(secondFile.isDirectory() && secondFile.isParent()) {
            return 1;
        }
        // then directories next
        if(firstFile.isDirectory() && !secondFile.isDirectory()) {
            return -1;
        }
        if(secondFile.isDirectory() && !firstFile.isDirectory()) {
            return 1;
        }
        // then files
        return firstFile.getName().compareToIgnoreCase(secondFile.getName());
    }
    
    public Comparator<File> getCaseInsensitiveComparator() {
        return new CaseInsensitiveComparator();
    }
    
    private final static class CaseInsensitiveComparator implements Comparator<File> {

        public int compare(File lhs, File rhs) {
            return lhs.getName().compareToIgnoreCase(rhs.getName());
        }
        
    }
    
}

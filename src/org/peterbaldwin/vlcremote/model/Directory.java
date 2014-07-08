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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
public class Directory extends ArrayList<File> implements Comparator<File> {
    
    public static final String UNIX_DIRECTORY = "/";
    public static final String WINDOWS_ROOT_DIRECTORY = "";
    public static String ROOT_DIRECTORY = UNIX_DIRECTORY;
    
    private final Map<String, Set<String>> realDirMap;
    
    public Directory() {
        realDirMap = new HashMap<String, Set<String>>();
    }

    public Directory(int capacity) {
        super(capacity);
        realDirMap = new HashMap<String, Set<String>>();
    }
    
    /**
     * Adds a file to the directory. If the file is a library, a library name or
     * a library directory, then the real directory given will be cached.
     * @param file File to add
     * @param realDir real directory of file, can be null if the file's path is
     * already valid (e.g. for a standard file or directory)
     */
    public void addFile(File file, String realDir) {
        if(file.isLibraryDir() || file.isLibrary() || file.isLibraryName()) {
            boolean keyExists = realDirMap.containsKey(file.getName());
            if(!keyExists) {
                realDirMap.put(file.getName(), new HashSet<String>(4));
            }
            realDirMap.get(file.getName()).add(File.getNormalizedPath(realDir));
            if(keyExists) {
                return;
            }
        }
        add(file);
    }
    
    public Set<String> getRealPaths(String dirName) {
        return realDirMap.get(dirName);
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
    @Override
    public int compare(File firstFile, File secondFile) {
        if((firstFile.isLibrary() || firstFile.isParent()) && !secondFile.isLibrary()) {
            return -1;
        }
        if(!firstFile.isLibrary() && (secondFile.isLibrary() || secondFile.isParent())) {
            return 1;
        }
        boolean isFirstDir = firstFile.isDirectory() || firstFile.isLibraryDir();
        boolean isSecondDir = secondFile.isDirectory() || secondFile.isLibraryDir();
        // parent always first
        if(isFirstDir && firstFile.isParent() && isSecondDir && secondFile.isParent()) {
            return 0;
        }
        if(isFirstDir && firstFile.isParent()) {
            return -1;
        }
        if(isSecondDir && secondFile.isParent()) {
            return 1;
        }
        // then directories next
        if(isFirstDir && !isSecondDir) {
            return -1;
        }
        if(isSecondDir && !isFirstDir) {
            return 1;
        }
        // then files
        return firstFile.getName().compareToIgnoreCase(secondFile.getName());
    }
    
    public Comparator<File> getCaseInsensitiveComparator() {
        return new CaseInsensitiveComparator();
    }
    
    private final static class CaseInsensitiveComparator implements Comparator<File> {
        @Override
        public int compare(File lhs, File rhs) {
            if((lhs.isLibrary() || lhs.isParent()) && !rhs.isLibrary()) {
                return -1;
            }
            if(!lhs.isLibrary() && (rhs.isLibrary() || rhs.isParent())) {
                return 1;
            }
            return lhs.getName().compareToIgnoreCase(rhs.getName());
        }
    }
    
}

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

@SuppressWarnings("serial")
public final class Directory extends ArrayList<File> {

    public Directory() {
    }

    public Directory(int capacity) {
        super(capacity);
    }

    public String getPath() {
        // Compute the path from the .. entry
        for (File file : this) {
            String name = file.getName();
            String path = file.getPath();
            if (name != null && path != null && name.equals("..") && path.endsWith("..")) {
                int length = path.length();
                length -= "..".length();
                return path.substring(0, length);
            }
        }
        return null;
    }
}

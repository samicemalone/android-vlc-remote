/*-
 *  Copyright (C) 2011 Peter Baldwin   
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

package org.peterbaldwin.vlcremote.net;

import android.sax.Element;
import android.sax.RootElement;
import android.sax.StartElementListener;
import java.io.IOException;
import java.net.URLConnection;
import org.peterbaldwin.vlcremote.model.Directory;
import org.peterbaldwin.vlcremote.model.File;
import org.xml.sax.Attributes;

final class DirectoryContentHandler extends XmlContentHandler<Directory> {
    
    private static Directory DIRECTORY;

    private File createFile(Attributes attributes) {
        String type = attributes.getValue("", "type");
        String sizeString = attributes.getValue("", "size");
        Long size = null;
        try {
            if (sizeString != null && !sizeString.equals("unknown")) {
                size = Long.parseLong(sizeString);
            }
        } catch (NumberFormatException e) {
            // Ignore unexpected value
        }
        String date = attributes.getValue("", "date");
        String path = attributes.getValue("", "path");
        String name = attributes.getValue("", "name");
        String extension = attributes.getValue("", "extension");
        if (File.PATH_TYPE == File.PATH_WINDOWS) {
            // Work-around: Replace front-slash
            // appended by server with back-slash.
            path = path.replace('/', '\\');
        }
        return new File(type, size, date, path, name, extension);
    }

    @Override
    public Object getContent(URLConnection connection) throws IOException {
        DIRECTORY = new Directory();
        RootElement root = new RootElement("", "root");
        Element element = root.getChild("", "element");
        element.setStartElementListener(new StartElementListener() {
            /** {@inheritDoc} */
            public void start(Attributes attributes) {
                final String path = attributes.getValue("", "path");
                if (path != null && !path.startsWith("/")) {
                    File.PATH_TYPE = File.PATH_WINDOWS;
                } else {
                    File.PATH_TYPE = File.PATH_UNIX;
                }
                File file = createFile(attributes);
                DIRECTORY.add(file);
            }
        });
        parse(connection, root.getContentHandler());
        Directory.ROOT_DIRECTORY = (File.PATH_TYPE == File.PATH_WINDOWS) ? Directory.WINDOWS_ROOT_DIRECTORY : Directory.UNIX_DIRECTORY;
        if(Directory.ROOT_DIRECTORY.equals(DIRECTORY.getPath())) {
            hideParent();
        } else {
            setParentTop();
        }
        return DIRECTORY;
    }
    
    /**
     * Hide the parent directory item if one exists
     */
    private void hideParent() {
        for(int i = 0; i < DIRECTORY.size(); i++) {
            if(DIRECTORY.get(i).isParent()) {
                DIRECTORY.remove(i);
                return;
            }
        }
    }
    
    /**
     * Set the parent directory item at the top of the list. If no parent entry
     * exists, one will be added with the default path being the root directory
     */
    private void setParentTop() {
        for(int i = 0; i < DIRECTORY.size(); i++) {
            if(DIRECTORY.get(i).isParent()) {
                if(i != 0) {
                    DIRECTORY.add(0, DIRECTORY.remove(i));
                }
                return;
            }
        }
        DIRECTORY.add(0, new File("dir", 0L, null, Directory.ROOT_DIRECTORY, "..", null));
    }
}

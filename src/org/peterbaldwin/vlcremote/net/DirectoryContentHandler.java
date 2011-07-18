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

import org.peterbaldwin.vlcremote.model.Directory;
import org.peterbaldwin.vlcremote.model.File;
import org.xml.sax.Attributes;

import android.sax.Element;
import android.sax.RootElement;
import android.sax.StartElementListener;

import java.io.IOException;
import java.net.URLConnection;

final class DirectoryContentHandler extends XmlContentHandler<Directory> {

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
        if (path != null && !path.startsWith("/")) { // Windows path
            // Work-around: Replace front-slash
            // appended by server with back-slash.
            path = path.replace('/', '\\');
        }
        return new File(type, size, date, path, name, extension);
    }

    @Override
    public Object getContent(URLConnection connection) throws IOException {
        final Directory directory = new Directory();
        RootElement root = new RootElement("", "root");
        Element element = root.getChild("", "element");
        element.setStartElementListener(new StartElementListener() {
            /** {@inheritDoc} */
            public void start(Attributes attributes) {
                File file = createFile(attributes);
                directory.add(file);
            }
        });
        parse(connection, root.getContentHandler());
        return directory;
    }
}

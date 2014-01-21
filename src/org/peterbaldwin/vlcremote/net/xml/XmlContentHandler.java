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

package org.peterbaldwin.vlcremote.net.xml;

import android.util.Xml;
import java.io.IOException;
import java.io.InputStream;
import java.net.ContentHandler;
import java.net.HttpURLConnection;
import java.net.URLConnection;

/**
 * Reads an XML response from an {@link HttpURLConnection}.
 * 
 * @param <T>
 */
public abstract class XmlContentHandler<T> extends ContentHandler {
    
    public static final String ERROR_INVALID_XML = "Invalid XML";

    protected final void parse(URLConnection connection, org.xml.sax.ContentHandler handler)
            throws IOException {
        InputStream input = connection.getInputStream();
        try {
            // The server sends UTF-8 instead of the HTTP default (ISO-8859-1).
            Xml.Encoding encoding = Xml.Encoding.UTF_8;
            Xml.parse(input, encoding, handler);
        } catch (Exception e) {
            throw new IOException(ERROR_INVALID_XML, e);
        } finally {
            input.close();
        }
    }
}

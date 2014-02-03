/*
 * Copyright (C) 2014 Sam Malone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.peterbaldwin.vlcremote.net.json;

import android.util.JsonReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ContentHandler;
import java.net.URLConnection;

/**
 * Reads a JSON response from a {@link URLConnection}.
 * @author Sam Malone
 * @param <T>
 */
public abstract class JsonContentHandler<T> extends ContentHandler {
    
    public static final String FILE_NOT_FOUND = "JSON_FILE_NOT_FOUND";
    
    public abstract Object parse(JsonReader reader) throws IOException;
    
    @Override
    public Object getContent(URLConnection connection) throws IOException {
        return parse(connection);
    }
    
    protected final Object parse(URLConnection connection) throws IOException {
        InputStream input;
        try {
            input = connection.getInputStream();
        } catch(FileNotFoundException ex) {
            throw new FileNotFoundException(FILE_NOT_FOUND);
        }
        JsonReader reader = new JsonReader(new InputStreamReader(input, "UTF-8"));
        try {
            return parse(reader);
        } finally {
            reader.close();
        }
    }
    
}

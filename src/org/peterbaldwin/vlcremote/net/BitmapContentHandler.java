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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.ContentHandler;
import java.net.URLConnection;

final class BitmapContentHandler extends ContentHandler {
    private static final int TIMEOUT = 2000;

    @Override
    public Bitmap getContent(URLConnection connection) throws IOException {
        // In some versions of VLC, album art requests can take a long time
        // to return if there is no album art available for the current track.
        // Set a short timeout to prevent a backlog of requests in this queue.
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);

        InputStream input = connection.getInputStream();
        try {
            input = new BlockingFilterInputStream(input);
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            if (bitmap == null) {
                throw new IOException("Decoding failed");
            }
            return bitmap;
        } finally {
            input.close();
        }
    }
}

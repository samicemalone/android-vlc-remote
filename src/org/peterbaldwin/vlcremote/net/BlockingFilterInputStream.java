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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Work-around for <a
 * href="http://code.google.com/p/android/issues/detail?id=6066">Issue 6066</a>
 */
final class BlockingFilterInputStream extends FilterInputStream {

    public BlockingFilterInputStream(InputStream input) {
        super(input);
    }

    @Override
    public int read(byte[] buffer, int offset, int count) throws IOException {
        int total = 0;
        while (total < count) {
            int read = super.read(buffer, offset + total, count - total);
            if (read == -1) {
                return (total != 0) ? total : -1;
            }
            total += read;
        }
        return total;
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        int total = 0;
        while (total < buffer.length) {
            int offset = total;
            int count = buffer.length - total;
            int read = super.read(buffer, offset, count);
            if (read == -1) {
                return (total != 0) ? total : -1;
            }
            total += read;
        }
        return total;
    }

    @Override
    public long skip(long count) throws IOException {
        long total = 0L;
        while (total < count) {
            long skipped = super.skip(count - total);
            if (skipped == 0L) {
                int b = super.read();
                if (b < 0) {
                    break;
                } else {
                    skipped += 1;
                }
            }
            total += skipped;
        }
        return total;
    }
}

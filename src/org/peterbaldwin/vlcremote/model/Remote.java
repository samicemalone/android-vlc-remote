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

package org.peterbaldwin.vlcremote.model;

/**
 * Light-weight wrapper for data that is loaded from a remote server.
 * <p>
 * Holds the remote data, or an error if there was a problem receiving the data.
 */
public final class Remote<T> {
    public final T data;

    public final Throwable error;

    private Remote(T data, Throwable error) {
        this.data = data;
        this.error = error;
    }

    public static <T> Remote<T> data(T data) {
        return new Remote<T>(data, null);
    }

    public static <T> Remote<T> error(Throwable t) {
        return new Remote<T>(null, t);
    }
}

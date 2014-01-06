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

package org.peterbaldwin.vlcremote.model;

import android.os.Bundle;

/**
 *
 * @author Sam Malone
 */
public interface Reloader {
    public void addReloadable(String tag, Reloadable r);
    public void reload(String tag, Bundle args);
    
    /**
     * Reload after the given delay. This method is non-blocking
     * @param tag Tag to identify reloadable
     * @param args Bundle to pass to the reloadable
     * @param delayMillis delay in milliseconds
     */
    public void reloadDelayed(final String tag, final Bundle args, long delayMillis);
}

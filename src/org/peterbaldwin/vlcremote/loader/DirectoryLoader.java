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

package org.peterbaldwin.vlcremote.loader;

import android.content.Context;
import java.util.Collections;
import org.peterbaldwin.vlcremote.model.Directory;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.model.Remote;
import org.peterbaldwin.vlcremote.net.MediaServer;

public class DirectoryLoader extends ModelLoader<Remote<Directory>> {

    private final MediaServer mMediaServer;

    private final String mDir;

    public DirectoryLoader(Context context, MediaServer mediaServer, String dir) {
        super(context);
        mMediaServer = mediaServer;
        mDir = dir;
    }

    @Override
    public Remote<Directory> loadInBackground() {
        if(mMediaServer == null) {
            return Remote.error(new NullPointerException("Unable to load the media server"));
        }
        Remote<Directory> remote = mMediaServer.browse(mDir).load();
        if(remote.data != null) {
            boolean dirSort = Preferences.get(getContext()).isSortDirectoriesFirst();
            Collections.sort(remote.data, dirSort ? remote.data : remote.data.getCaseInsensitiveComparator());
        }
        return remote;
    }
}

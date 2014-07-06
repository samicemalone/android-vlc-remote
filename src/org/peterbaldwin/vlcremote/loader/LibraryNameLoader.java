/*-
 *  Copyright (C) 2011 Sam Malone  
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
import org.peterbaldwin.vlcremote.model.File;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.model.Remote;
import org.peterbaldwin.vlcremote.net.MediaServer;

public class LibraryNameLoader extends ModelLoader<Remote<Directory>> {

    private final MediaServer mMediaServer;
    
    public LibraryNameLoader(Context context, MediaServer server) {
        super(context);
        mMediaServer = server;
    }

    @Override
    public Remote<Directory> loadInBackground() {
        // fetch home directory so the root path can be determined
        Remote<Directory> home = mMediaServer.browse("~").load();
        if(home.data == null) {
            return home;
        }
        Preferences p = Preferences.get(getContext());
        Directory d = new Directory();
        d.add(new File(File.Type.DIRECTORY, 0L, null, Directory.ROOT_DIRECTORY, "..", null));
        for(String libraryName : p.getLibraries()) {
            File library = File.getLibrary(libraryName);
            for(String dir : p.getLibraryDirectories(libraryName)) {
                d.addFile(library, dir);
            }
        }
        Collections.sort(d, d);
        return Remote.data(d);
    }
}

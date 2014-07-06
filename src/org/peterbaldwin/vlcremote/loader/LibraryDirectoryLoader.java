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
import java.io.FileNotFoundException;
import java.util.Collections;
import org.peterbaldwin.vlcremote.model.Directory;
import org.peterbaldwin.vlcremote.model.File;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.model.Remote;
import org.peterbaldwin.vlcremote.net.MediaServer;
import org.peterbaldwin.vlcremote.net.xml.XmlContentHandler;

public class LibraryDirectoryLoader extends ModelLoader<Remote<Directory>> {

    private final MediaServer mMediaServer;
    
    private final Preferences mPreferences;

    private final String mDir;

    public LibraryDirectoryLoader(Context context, MediaServer mediaServer, String dir) {
        super(context);
        mPreferences = Preferences.get(context);
        mMediaServer = mediaServer;
        mDir = dir;
    }

    @Override
    public Remote<Directory> loadInBackground() {
        if(mMediaServer == null) {
            return Remote.error(new NullPointerException("Unable to load the media server"));
        }
        Directory d = new Directory();
        addParent(d);
        // library[0] = library name, library[1] = path
        String[] libraryParts = mDir.substring("library://".length()).split("(\\\\|/)+", 2);
        boolean isAllError = true;
        for(String libraryDir : mPreferences.getLibraryDirectories(libraryParts[0])) {
            String path = libraryParts.length == 1 || libraryParts[1] == null ? "" : libraryParts[1];
            Remote<Directory> remote = mMediaServer.browse(libraryDir + '/' + path).load();
            if(remote.data != null) {
                for(File file : remote.data) {
                    if(!file.isParent()) {
                        String realPath = file.getNormalizedPath();
                        if(file.isDirectory()) {
                            String relativeEnd = file.getPath().substring(libraryDir.length());
                            file.setPath("library://" + libraryParts[0] + relativeEnd);
                            file.setType(File.Type.LIBRARY_DIRECTORY);
                        }
                        d.addFile(file, realPath);
                    }
                }
                isAllError = false;
            }
        }
        if(isAllError) {
            return Remote.error(new FileNotFoundException(XmlContentHandler.ERROR_INVALID_XML));
        }
        Collections.sort(d, mPreferences.isSortDirectoriesFirst() ? d : d.getCaseInsensitiveComparator());
        return Remote.data(d);
    }
    
    private void addParent(Directory d) {
        String path = File.getNormalizedPath(mDir.concat("/.."));
        File.Type type = path.equals("library://") ? File.Type.LIBRARY : File.Type.LIBRARY_DIRECTORY;
        File parent = new File(type, 0L, null, path, "..", null);
        d.add(parent);
//        if(parent.isLibrary()) {
//            d.add(parent);
//        } else {
//            String[] libraryParts = path.substring("library://".length()).split("(\\\\|/)+", 2);
//            String relPath = libraryParts.length == 1 || libraryParts[1] == null ? "" : libraryParts[1];
//            for(String libraryDir : mPreferences.getLibraryDirectories(libraryParts[0])) {
//                d.addFile(parent, libraryDir + '/' + relPath);
//            }
//        }
    }

}

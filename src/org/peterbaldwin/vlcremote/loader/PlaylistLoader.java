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

import org.peterbaldwin.vlcremote.listener.ProgressListener;
import android.content.Context;
import org.peterbaldwin.vlcremote.model.Media;
import org.peterbaldwin.vlcremote.model.Playlist;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.model.Remote;
import org.peterbaldwin.vlcremote.model.Track;
import org.peterbaldwin.vlcremote.net.MediaServer;
import org.peterbaldwin.vlcremote.parser.MediaParser;

public class PlaylistLoader extends ModelLoader<Remote<Playlist>> {

    private final MediaServer mMediaServer;

    private final String mSearch;

    private final MediaParser mMediaParser;
    
    private final ProgressListener mListener;
    
    private boolean mIsCancelled = false;
    
    public PlaylistLoader(Context context, MediaServer mediaServer, String search, ProgressListener listen) {
        super(context);
        mMediaServer = mediaServer;
        mSearch = search;
        mMediaParser = new MediaParser();
        mListener = listen;
    }

    @Override
    public Remote<Playlist> loadInBackground() {
        mListener.onProgress(ProgressListener.START);
        Remote<Playlist> p = mMediaServer == null ? null : mMediaServer.playlist(mSearch).load();
        if(p == null || p.data == null || p.data.isEmpty() || !Preferences.get(getContext()).isParsePlaylistItems()) {
            mListener.onProgress(ProgressListener.FINISHED);
            return p;
        }
        int progress = ProgressListener.MAX/10;
        mListener.onProgress(progress);
        final int increment = (ProgressListener.MAX - progress) / p.data.size();
        for(int i = 0; i < p.data.size(); i++) {
            if(mIsCancelled) {
                return null;
            }
            if(p.data.get(i) instanceof Track) {
                Track track = (Track) p.data.get(i);
                Media media = mMediaParser.parse(track.getUri());
                if(media != null) {
                    media.copyPlaylistItemFrom(track);
                    p.data.set(i, media);
                }
            }
            mListener.onProgress(progress + (increment * i));
        }
        mListener.onProgress(ProgressListener.FINISHED);
        return p;
    }
    
    public void cancelBackgroundLoad() {
        mIsCancelled = true;
    }

}

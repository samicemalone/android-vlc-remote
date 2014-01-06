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

package org.peterbaldwin.vlcremote.fragment;

import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.intent.Intents;
import org.peterbaldwin.vlcremote.loader.ImageLoader;
import org.peterbaldwin.vlcremote.model.Status;
import org.peterbaldwin.vlcremote.model.Track;
import org.peterbaldwin.vlcremote.net.MediaServer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import org.peterbaldwin.vlcremote.loader.ArtLoader;

public class ArtFragment extends MediaFragment implements LoaderCallbacks<Drawable> {

    private static final int LOADER_IMAGE = 1;

    private BroadcastReceiver mStatusReceiver;

    private ImageView mImageView;

    private String mArtUrl;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.art_fragment, root, false);
        mImageView = (ImageView) view.findViewById(android.R.id.icon);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getMediaServer() != null) {
            getLoaderManager().initLoader(LOADER_IMAGE, Bundle.EMPTY, this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mStatusReceiver = new StatusReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.ACTION_STATUS);
        getActivity().registerReceiver(mStatusReceiver, filter);
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(mStatusReceiver);
        mStatusReceiver = null;
        super.onPause();
    }

    /** {@inheritDoc} */
    public Loader<Drawable> onCreateLoader(int id, Bundle args) {
        Context context = getActivity();
        Uri uri = mArtUrl != null ? Uri.parse(mArtUrl) : null;
        if (uri != null) {
            uri = resizeImage(uri);
            if("file".equals(uri.getScheme())) {
                return new ArtLoader(context, getMediaServer());
            }
        }
        return new ImageLoader(context, getMediaServer(), uri);
    }

    /** {@inheritDoc} */
    public void onLoadFinished(Loader<Drawable> loader, Drawable data) {
        mImageView.setImageDrawable(data);
    }

    /** {@inheritDoc} */
    public void onLoaderReset(Loader<Drawable> loader) {
        mImageView.setImageResource(R.drawable.albumart_mp_unknown);
    }

    @Override
    public void onNewMediaServer(MediaServer server) {
        super.onNewMediaServer(server);
        if (getMediaServer() != null) {
            getLoaderManager().restartLoader(LOADER_IMAGE, Bundle.EMPTY, this);
        }
    }

    private void onStatusChanged(Status status) {
        Track track = status.getTrack();
        String artUrl = track.getArtUrl();
        if (mArtUrl == null || !mArtUrl.equals(artUrl)) {
            mArtUrl = artUrl;
            getLoaderManager().restartLoader(LOADER_IMAGE, null, this);
        }
    }

    private static Uri resizeImage(Uri uri) {
        if (isJamendoImage(uri)) {
            return resizeJamendoImage(uri);
        } else {
            return uri;
        }
    }

    private static boolean isJamendoImage(Uri uri) {
        return "imgjam.com".equals(uri.getAuthority()) && uri.getPathSegments().size() != 0
                && uri.getLastPathSegment().matches("1\\.\\d+\\.jpg");
    }

    private static Uri resizeJamendoImage(Uri uri) {
        String path = uri.getPath();
        path = path.replace("/" + uri.getLastPathSegment(), "/1.400.jpg");
        return uri.buildUpon().path(path).build();
    }

    private class StatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (this != mStatusReceiver) {
                // TODO: Determine why this receiver is not unregistered
                return;
            }
            String action = intent.getAction();
            if (Intents.ACTION_STATUS.equals(action)) {
                Status status = (Status) intent.getSerializableExtra(Intents.EXTRA_STATUS);
                onStatusChanged(status);
            }
        }
    }
}

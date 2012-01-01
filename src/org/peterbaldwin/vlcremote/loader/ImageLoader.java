
package org.peterbaldwin.vlcremote.loader;

import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.net.MediaServer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import java.io.IOException;

public class ImageLoader extends ModelLoader<Drawable> {

    private final MediaServer mMediaServer;

    private final Uri mUri;

    public ImageLoader(Context context, MediaServer mediaServer, Uri uri) {
        super(context);
        mMediaServer = mediaServer;
        mUri = uri;
    }

    @Override
    public Drawable loadInBackground() {
        Resources res = getContext().getResources();
        if (mUri != null && "http".equals(mUri.getScheme())) {
            try {
                return new BitmapDrawable(res, mMediaServer.image(mUri).read());
            } catch (IOException e) {
                return res.getDrawable(R.drawable.albumart_mp_unknown);
            }
        } else {
            return res.getDrawable(R.drawable.albumart_mp_unknown);
        }
    }
}

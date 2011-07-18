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
import org.peterbaldwin.vlcremote.net.MediaServer;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class NavigationFragment extends Fragment implements View.OnTouchListener,
        GestureDetector.OnGestureListener {

    private MediaServer mMediaServer;
    private GestureDetector mGestureDetector;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.navigation_fragment, root, false);
        Context context = view.getContext();
        GestureDetector.OnGestureListener listener = this;
        mGestureDetector = new GestureDetector(context, listener);
        view.findViewById(R.id.overlay).setOnTouchListener(this);
        return view;
    }

    public void setMediaServer(MediaServer mediaServer) {
        mMediaServer = mediaServer;
    }

    /** {@inheritDoc} */
    public boolean onTouch(View v, MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    /** {@inheritDoc} */
    public boolean onSingleTapUp(MotionEvent e) {
        mMediaServer.status().command.key("nav-activate");
        vibrate();
        return true;
    }

    /** {@inheritDoc} */
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            if (velocityX > 0) {
                mMediaServer.status().command.key("nav-right");
                vibrate();
                return true;
            } else if (velocityX < 0) {
                mMediaServer.status().command.key("nav-left");
                vibrate();
                return true;
            }
        } else if (Math.abs(velocityY) > Math.abs(velocityX)) {
            if (velocityY > 0) {
                mMediaServer.status().command.key("nav-down");
                vibrate();
                return true;
            } else if (velocityY < 0) {
                mMediaServer.status().command.key("nav-up");
                vibrate();
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private <T> T getSystemService(String name) {
        Context context = getActivity();
        return (T) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    private void vibrate() {
        Vibrator v = getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(100);
        }
    }

    /** {@inheritDoc} */
    public boolean onDown(MotionEvent e) {
        return false;
    }

    /** {@inheritDoc} */
    public void onShowPress(MotionEvent e) {
    }

    /** {@inheritDoc} */
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    /** {@inheritDoc} */
    public void onLongPress(MotionEvent e) {
    }
}

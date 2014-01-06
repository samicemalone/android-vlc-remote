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

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import org.peterbaldwin.client.android.vlcremote.R;

public class NavigationFragment extends MediaFragment implements View.OnTouchListener,
        GestureDetector.OnGestureListener {
    
    private static final String ARG_IS_LOCKABLE = "isLockable";
    
    public static NavigationFragment lockableInstance() {
        NavigationFragment f = new NavigationFragment();
        Bundle b = new Bundle();
        b.putBoolean(ARG_IS_LOCKABLE, true);
        f.setArguments(b);
        return f;
    }

    private GestureDetector mGestureDetector;
    private boolean isLockable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.navigation_fragment, root, false);
        mGestureDetector = new GestureDetector(view.getContext(), this);
        view.findViewById(R.id.overlay).setOnTouchListener(this);
        isLockable = getArguments() != null ? getArguments().getBoolean(ARG_IS_LOCKABLE, false) : false;
        if(!isLockable) {
            view.findViewById(R.id.pager_lock).setVisibility(View.GONE);
        }
        return view;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        getMediaServer().status().command.key("nav-activate");
        vibrate();
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            if (velocityX > 0) {
                getMediaServer().status().command.key("nav-right");
                vibrate();
                return true;
            } else if (velocityX < 0) {
                getMediaServer().status().command.key("nav-left");
                vibrate();
                return true;
            }
        } else if (Math.abs(velocityY) > Math.abs(velocityX)) {
            if (velocityY > 0) {
                getMediaServer().status().command.key("nav-down");
                vibrate();
                return true;
            } else if (velocityY < 0) {
                getMediaServer().status().command.key("nav-up");
                vibrate();
                return true;
            }
        }
        return false;
    }

    private void vibrate() {
        Vibrator v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(100);
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {}

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {}
}

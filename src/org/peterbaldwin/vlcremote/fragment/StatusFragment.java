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


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;

/**
 * Polls the server for status updates.
 */
public class StatusFragment extends MediaFragment implements Handler.Callback {

    private static final int TIMER = 1;

    private static final long INTERVAL = DateUtils.SECOND_IN_MILLIS;

    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        startTimer();
    }

    @Override
    public void onPause() {
        stopTimer();
        super.onPause();
    }

    private void startTimer() {
        mHandler.sendEmptyMessage(TIMER);
    }

    private void stopTimer() {
        mHandler.removeMessages(TIMER);
    }

    /** {@inheritDoc} */
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case TIMER:
                onTimerEvent();
                return true;
            default:
                return false;
        }
    }

    private void onTimerEvent() {
        if (getMediaServer() != null) {
            getMediaServer().status().programmatic().get();
        }

        // Schedule the next timer event
        mHandler.sendEmptyMessageDelayed(TIMER, INTERVAL);
    }
}

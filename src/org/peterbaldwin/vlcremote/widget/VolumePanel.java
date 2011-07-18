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

package org.peterbaldwin.vlcremote.widget;

import org.peterbaldwin.client.android.vlcremote.R;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * Displays the current volume level as a {@link Toast}.
 */
public class VolumePanel {

    private final Toast mToast;
    private final View mView;
    private final ImageView mIcon;
    private final ProgressBar mProgress;

    public VolumePanel(Context context) {
        mToast = new Toast(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        mView = inflater.inflate(R.layout.volume_adjust, null);
        mIcon = (ImageView) mView.findViewById(android.R.id.icon);
        mProgress = (ProgressBar) mView.findViewById(android.R.id.progress);
    }

    public void onVolumeChanged(int level) {
        mIcon.setImageResource(level == 0 ? R.drawable.ic_volume_off_small
                : R.drawable.ic_volume_small);
        mProgress.setProgress(level);
        mToast.setView(mView);
        mToast.setDuration(Toast.LENGTH_LONG);
        mToast.setGravity(Gravity.TOP, 0, 0);
        mToast.show();
    }
}

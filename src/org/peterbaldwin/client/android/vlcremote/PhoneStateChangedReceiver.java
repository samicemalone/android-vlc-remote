/*-
 *  Copyright (C) 2010 Peter Baldwin   
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

package org.peterbaldwin.client.android.vlcremote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

/**
 * Automatically pauses media when there is an incoming call.
 */
public class PhoneStateChangedReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		VLC vlc = new VLC(context);
		String action = intent.getAction();
		if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
			String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
			if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
				Intent command = vlc.command("command=pl_pause");
				command.putExtra(StatusService.EXTRA_ONLY_IF_PLAYING, true);
				command.putExtra(StatusService.EXTRA_SET_RESUME_ON_IDLE, true);
				context.startService(command);
			} else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
				if (vlc.isResumeOnIdleSet()) {
					Intent command = vlc.command("command=pl_pause");
					command.putExtra(StatusService.EXTRA_ONLY_IF_PAUSED, true);
					context.startService(command);
					vlc.clearResumeOnIdle();
				}
			}
		}
	}
}

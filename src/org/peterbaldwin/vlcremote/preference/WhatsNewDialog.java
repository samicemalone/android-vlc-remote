/*
 * Copyright (C) 2014 Sam Malone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.peterbaldwin.vlcremote.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.Html;
import android.widget.ScrollView;
import android.widget.TextView;
import org.peterbaldwin.client.android.vlcremote.R;

/**
 *
 * @author Sam Malone
 */
public class WhatsNewDialog {
    
    private final static String SHARED_PREFERENCES_NAME = "whats_new";
    
    private final SharedPreferences mPrefs;
    
    private final String mVersion;
    private final Context mContext;

    public WhatsNewDialog(Context context) {
        mPrefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        mContext = context;
        mVersion = getVersionString(context);
    }
    
    private String getVersionString(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch(PackageManager.NameNotFoundException ex) {
            return null;
        }
    }
    
    public boolean hasUserSeenDialog() {
        return mVersion == null || mPrefs.getBoolean(mVersion, false);
    }
    
    public void setDialogAsSeen() {
        if(mVersion != null) {
            mPrefs.edit().clear().putBoolean(mVersion, true).apply();
        }
    }
    
    public AlertDialog build() {
        TextView tv = new TextView(mContext);
        tv.setText(Html.fromHtml(getMessage()));
        tv.setPadding(10, 10, 10, 10);
        ScrollView sv = new ScrollView(mContext);
        sv.addView(tv);
        return new AlertDialog.Builder(mContext)
            .setTitle(R.string.title_dialog_whats_new)
            .setView(sv)
            .setCancelable(false)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton("View in Google Play", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("market://details?id=uk.co.samicemalone.stream.client.android.vlcremote"));
                    mContext.startActivity(intent);
                    dialog.dismiss();
                }
            })
            .create();
    }
    
    private String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>v").append(mVersion).append("</h2>");
        for(String item : getNewItems()) {
            sb.append("<p><b>**</b> ").append(item).append("</p>");
        }
        sb.append("</p>");
        sb.append("<p>I have released another application: <b>Remote for VLC (Stream Fork)</b> which allows you to stream media from VLC to your Android device.</p>");
        sb.append("<p>This app will always stay free and the versions only differ in the streaming feature. They will both receive the same bug/feature updates.</p>");
        sb.append("<p>If you are interested in streaming support, you can use the button below to view the app on Google Play.</p>");
        return sb.toString();
    }
    
    private String[] getNewItems() {
        return new String[] {
            "Added library support for viewing multiple directories under one directory (via context menu). This is similar to Windows Libraries",
            "Fixed crashing occasionally when trying restore the search query",
            "Updated FAQ / Install Guide"
        };
    }
    
}

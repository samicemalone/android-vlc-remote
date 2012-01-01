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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.AdapterView;

public final class HotkeyDialog extends DialogFragment implements AdapterView.OnItemClickListener {

    private String[] mCodes;
    
    private HotkeyListener mHotkeyListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCodes = getResources().getStringArray(R.array.hotkey_codes);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setItems(R.array.hotkey_labels, null);
        builder.setNeutralButton(R.string.close, null);
        AlertDialog dialog = builder.create();

        // Handle ListView item clicks directly so that dialog is not dismissed
        dialog.getListView().setOnItemClickListener(this);

        return dialog;
    }
    
    public void setHotkeyListener(HotkeyListener hotkeyListener) {
        mHotkeyListener = hotkeyListener;
    }

    /** {@inheritDoc} */
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mHotkeyListener != null) {
            mHotkeyListener.onHotkey(mCodes[position]);
        }
    }
}

/*-
 *  Copyright (C) 2013 Sam Malone
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.model.Server;

/**
 *
 * @author Sam Malone
 */
public class ServerInfoDialog extends DialogFragment implements View.OnClickListener {
    
    public interface ServerInfoDialogListener {
        public void onAddServer(Server server);
        public void onEditServer(Server newServer, String oldServerKey);
    }
    
    private ServerInfoDialogListener mListener;
    
    private EditText mEditHostname;
    private EditText mEditPort;
    private EditText mEditUser;
    private EditText mEditPassword;
    
    public static ServerInfoDialog addServerInstance() {
        return newInstance(R.string.add_server, null);
    }
    
    public static ServerInfoDialog editServerInstance(String currentServerKey) {
        return newInstance(R.string.edit_server, currentServerKey);
    }
    
    public static ServerInfoDialog newInstance(int titleRes, String currentServerKey) {
        ServerInfoDialog dialog = new ServerInfoDialog();
        Bundle args = new Bundle();
        args.putInt("titleRes", titleRes);
        if(currentServerKey != null) {
            args.putString("currentKey", currentServerKey);
        }
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getArguments().getInt("titleRes"));
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.add_server, null);
        setupViews(view);
        builder.setView(view);
        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        // override positive button so the dialog can be closed after validation
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            public void onShow(DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(ServerInfoDialog.this);
            }
        });
        return dialog;
    }
    
    /**
     * Sets up the mEditHostname, mEditPort, mEditUser and mEditPassword views
     * with their initial data.
     * @param view Inflated root of the view hierarchy
     */
    public void setupViews(View view) {
        mEditHostname = (EditText) view.findViewById(R.id.edit_hostname);
        mEditPort = (EditText) view.findViewById(R.id.edit_port);
        mEditUser = (EditText) view.findViewById(R.id.edit_user);
        mEditPassword = (EditText) view.findViewById(R.id.edit_password);
        if(getArguments().getString("currentKey") != null) {
            Server server = Server.fromKey(getArguments().getString("currentKey"));
            mEditHostname.setText(server.getHost());
            mEditPort.setText(String.valueOf(server.getPort()));
            mEditUser.setText(server.getUser());
            mEditPassword.setText(server.getPassword());
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ServerInfoDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ServerInfoDialogListener");
        }
    }

    @Override
    public void onClick(View view) {
        if(!validateInput()) {
            return;
        }
        if(getArguments().getString("currentKey") == null) {
            mListener.onAddServer(createServerFromInput());
            dismiss();
            return;
        }
        Server server = createServerFromInput();
        if(!getArguments().getString("currentKey").equals(server.toKey())) {
            mListener.onEditServer(server, getArguments().getString("currentKey"));
        }
        dismiss();
    }
    
    private int getPort() {
        if(mEditPort.getText().toString().isEmpty()) {
            return Server.DEFAULT_PORT;
        }
        return Integer.valueOf(mEditPort.getText().toString());
    }
    
    private Server createServerFromInput() {
        return new Server(
            mEditHostname.getText().toString(),
            getPort(),
            mEditUser.getText().toString(),
            mEditPassword.getText().toString()
        );
    }
    
    private boolean validateInput() {
        if(!mEditPort.getText().toString().isEmpty()) {
            try {
                Integer.valueOf(mEditPort.getText().toString());
            } catch(NumberFormatException ex) {
                Toast.makeText(getActivity(), "The port number must be numberic", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        if(mEditHostname.getText().toString().isEmpty()) {
            Toast.makeText(getActivity(), "The host cannot be empty", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    
}

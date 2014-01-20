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
import org.peterbaldwin.vlcremote.net.ServerConnectionTest;

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
    
    private final static int ADD_TYPE = 0;
    private final static int EDIT_TYPE = 1;
    
    private boolean requiresAuthentication;
    private int dialogType;
    
    private EditText mEditNickname;
    private EditText mEditHostname;
    private EditText mEditPort;
    private EditText mEditUser;
    private EditText mEditPassword;
    
    /**
     * Creates a new ServerInfoDialog instance for adding a new server.
     * Username and password fields are optional
     * @return 
     */
    public static ServerInfoDialog addServerInstance() {
        return newInstance(ADD_TYPE, R.string.add_server, null, false);
    }
    
    /**
     * Creates a new ServerInfoDialog instance for adding a new server that
     * requires authorization.
     * @param currentServerKey The Server key for the existing server
     * @return 
     */
    public static ServerInfoDialog addAuthServerInstance(String currentServerKey) {
        return newInstance(ADD_TYPE, R.string.add_server, currentServerKey, true);
    }
    
    /**
     * Creates a new ServerInfoDialog instance for editing an existing server.
     * Username and password fields are optional
     * @param currentServerKey The Server key for the existing server.
     * @return 
     */
    public static ServerInfoDialog editServerInstance(String currentServerKey) {
        return newInstance(EDIT_TYPE, R.string.edit_server, currentServerKey, false);
    }
    
    /**
     * Creates a new instance of ServerInfoDialog.
     * @param dialogType ADD_TYPE or EDIT_TYPE
     * @param titleRes The title String resource id
     * @param currentServerKey The Server key for the existing server. This is
     * used to populate the fields with existing values.
     * @param requiresAuth if true, the username or password will be required.
     * If false, the username and password will be optional
     * @return 
     */
    public static ServerInfoDialog newInstance(int dialogType, int titleRes, String currentServerKey, boolean requiresAuth) {
        ServerInfoDialog dialog = new ServerInfoDialog();
        Bundle args = new Bundle();
        args.putInt("titleRes", titleRes);
        args.putBoolean("auth", requiresAuth);
        args.putInt("dialogType", dialogType);
        if(currentServerKey != null) {
            args.putString("currentKey", currentServerKey);
        }
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        requiresAuthentication = getArguments().getBoolean("auth", false);
        dialogType = getArguments().getInt("dialogType", ADD_TYPE);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getArguments().getInt("titleRes"));
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.add_server, null);
        setupViews(view);
        builder.setView(view);
        builder.setPositiveButton(R.string.ok, null);
        builder.setNeutralButton(R.string.test_server_button, null);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        // override positive button so the dialog can be closed after validation
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            public void onShow(DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTag(AlertDialog.BUTTON_POSITIVE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(ServerInfoDialog.this);
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTag(AlertDialog.BUTTON_NEUTRAL);
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(ServerInfoDialog.this);
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
        mEditNickname = (EditText) view.findViewById(R.id.edit_nickname);
        mEditHostname = (EditText) view.findViewById(R.id.edit_hostname);
        mEditPort = (EditText) view.findViewById(R.id.edit_port);
        mEditUser = (EditText) view.findViewById(R.id.edit_user);
        mEditPassword = (EditText) view.findViewById(R.id.edit_password);
        if(getArguments().getString("currentKey") != null) {
            Server server = Server.fromKey(getArguments().getString("currentKey"));
            mEditNickname.setText(server.getNickname());
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
      
    public void onTestServer(Server server) {
        new ServerConnectionTest(getActivity()).execute(server);
    }

    /**
     * This is the onShowListener for when the positive or neutral button is 
     * clicked.
     * @param view Positive/Neutral button
     */
    @Override
    public void onClick(View view) {
        if(!validateInput()) {
            return;
        }
        if(view.getTag().equals(AlertDialog.BUTTON_NEUTRAL)) {
            onTestServer(createServerFromInput());
            return;
        }
        switch(dialogType) {
            case ADD_TYPE:
                mListener.onAddServer(createServerFromInput());
                break;
            case EDIT_TYPE:
                Server server = createServerFromInput();
                if(!getArguments().getString("currentKey").equals(server.toKey())) {
                    mListener.onEditServer(server, getArguments().getString("currentKey"));
                }
                break;
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
            mEditNickname.getText().toString(),
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
                Toast.makeText(getActivity(), R.string.validate_port, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        if(mEditHostname.getText().toString().isEmpty()) {
            Toast.makeText(getActivity(), R.string.validate_host, Toast.LENGTH_SHORT).show();
            return false;
        }
        if(requiresAuthentication) {
            if(mEditUser.getText().toString().isEmpty() && mEditPassword.getText().toString().isEmpty()) {
                Toast.makeText(getActivity(), R.string.validate_auth, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }
    
}

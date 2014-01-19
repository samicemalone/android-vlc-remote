/*-
 *  Copyright (C) 2009 Peter Baldwin   
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

package org.peterbaldwin.vlcremote.app;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.fragment.PickServerFragment;
import org.peterbaldwin.vlcremote.fragment.ServerInfoDialog;
import org.peterbaldwin.vlcremote.model.Server;
import org.peterbaldwin.vlcremote.sweep.PortSweeper;

public final class PickServerActivity extends FragmentActivity implements ServerInfoDialog.ServerInfoDialogListener {
    
    private static final String TAG = "PickServer";

    public static final String STATE_HOSTS = "hosts";

    public static final int DEFAULT_WORKERS = 16;

    private static final int MENU_SCAN = Menu.FIRST;

    private static byte[] toByteArray(int i) {
        int i4 = (i >> 24) & 0xFF;
        int i3 = (i >> 16) & 0xFF;
        int i2 = (i >> 8) & 0xFF;
        int i1 = i & 0xFF;
        return new byte[] {
            (byte) i1, (byte) i2, (byte) i3, (byte) i4
        };
    }

    private PortSweeper mPortSweeper;

    private BroadcastReceiver mReceiver;
    
    private ServerInfoDialog.ServerInfoDialogListener mServerInfoListener;

    private String mFile;
    private int mPort;
    private int mWorkers;
    private long mCreateTime;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PickServerFragment fragment = findOrReplaceFragment(android.R.id.content, TAG, PickServerFragment.class);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mPort = getIntent().getIntExtra(PortSweeper.EXTRA_PORT, 0);
        if (mPort == 0) {
            throw new IllegalArgumentException("Port must be specified");
        }

        mFile = getIntent().getStringExtra(PortSweeper.EXTRA_FILE);
        if (mFile == null) {
            throw new IllegalArgumentException("File must be specified");
        }

        mWorkers = getIntent().getIntExtra(PortSweeper.EXTRA_WORKERS, DEFAULT_WORKERS);

        mPortSweeper = createPortSweeper(fragment);
        startSweep();  

        // Registering the receiver triggers a broadcast with the initial state.
        // To tell the difference between a broadcast triggered by registering a
        // receiver and a broadcast triggered by a true network event, note the
        // time and ignore all broadcasts for one second.
        mCreateTime = SystemClock.uptimeMillis();

        mReceiver = new MyBroadcastReceiver();

        // For robustness, update the connection status for all types of events.
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);
    }

    private boolean isInitialBroadcast() {
        return (SystemClock.uptimeMillis() - mCreateTime) < 1000;
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        mReceiver = null;
        if (mPortSweeper != null) {
            mPortSweeper.destroy();
        }
        super.onDestroy();
    }

    private PortSweeper createPortSweeper(PortSweeper.Callback callback) {
        return new PortSweeper(mPort, mFile, mWorkers, callback, Looper.myLooper());
    }

    private WifiInfo getConnectionInfo() {
        Object service = getSystemService(WIFI_SERVICE);
        WifiManager manager = (WifiManager) service;
        WifiInfo info = manager.getConnectionInfo();
        if (info != null) {
            SupplicantState state = info.getSupplicantState();
            if (state.equals(SupplicantState.COMPLETED)) {
                return info;
            }
        }
        return null;
    }

    private byte[] getIpAddress() {
        WifiInfo info = getConnectionInfo();
        if (info != null) {
            return toByteArray(info.getIpAddress());
        }
        return null;
    }

    private void startSweep() {
        byte[] ipAddress = getIpAddress();
        if (ipAddress != null) {
            mPortSweeper.sweep(ipAddress);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem scan = menu.add(0, MENU_SCAN, 0, R.string.scan);
        scan.setIcon(R.drawable.ic_menu_scan_network);
        scan.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SCAN:
                startSweep();
                return true;
            case android.R.id.home:
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    public void setServerInfoDialogListener(ServerInfoDialog.ServerInfoDialogListener l) {
        mServerInfoListener = l;
    }

    public void onAddServer(Server server) {
        mServerInfoListener.onAddServer(server);
    }

    public void onEditServer(Server newServer, String oldServerKey) {
        mServerInfoListener.onEditServer(newServer, oldServerKey);
        startSweep();
    }
    
    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle extras = intent.getExtras();
            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo networkInfo = extras.getParcelable(WifiManager.EXTRA_NETWORK_INFO);
                NetworkInfo.State state = networkInfo.getState();
                if (state == NetworkInfo.State.CONNECTED) {
                    if (isInitialBroadcast()) {
                        // Don't perform a sweep if the broadcast was triggered
                        // as a result of a receiver being registered.
                    } else {
                        startSweep();
                    }
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T extends android.app.Fragment> T findOrReplaceFragment(int res, String tag, Class<T> fragmentClass) {
        try {
            FragmentManager fm = getFragmentManager();
            T fragment = (T) fm.findFragmentByTag(tag);
            if (fragment == null) {
                fragment = fragmentClass.newInstance();
                FragmentTransaction fragmentTransaction = fm.beginTransaction();
                fragmentTransaction.replace(res, fragment, tag);
                fragmentTransaction.commit();
                fm.executePendingTransactions();
            }
            return fragment;
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

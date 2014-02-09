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

package org.peterbaldwin.vlcremote.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.intent.Intents;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.model.Status;
import org.peterbaldwin.vlcremote.net.MediaServer;
import org.peterbaldwin.vlcremote.net.json.JsonContentHandler;
import org.peterbaldwin.vlcremote.widget.NotificationControls;

/**
 * Sends commands to a VLC server and receives &amp; broadcasts the status.
 */
public class StatusService extends Service implements Handler.Callback {

    public static boolean USE_XML_STATUS = false;
    
    private static final String TAG = "StatusService";

    private static final int HANDLE_STATUS = 1;
    private static final int HANDLE_ALBUM_ART = 2;
    private static final int HANDLE_STOP = 3;
    private static final int HANDLE_NOTIFICATION = 4;
    private static final int HANDLE_NOTIFICATION_CREATE = 5;

    private static boolean isCommand(Uri uri) {
        return !uri.getQueryParameters("command").isEmpty();
    }

    /**
     * Erases any commands from the URI.
     */
    private static Uri readOnly(Uri uri) {
        Uri.Builder builder = uri.buildUpon();
        builder.encodedQuery("");
        return builder.build();
    }

    private static boolean isSeek(Uri uri) {
        return "seek".equals(uri.getQueryParameter("command"));
    }

    private static boolean isVolume(Uri uri) {
        return "volume".equals(uri.getQueryParameter("command"));
    }

    private static boolean isAbsoluteValue(Uri uri) {
        String value = uri.getQueryParameter("val");
        return value != null && !value.startsWith("+") && !value.startsWith("-");
    }
    
    private String mAuthority;

    private Handler mStatusHandler;

    private Handler mAlbumArtHandler;

    private Handler mCommandHandler;

    private Handler mNotificationHandler;

    private AtomicInteger mSequenceNumber;
    
    private StatusReceiver mStatusReceiver;
    
    private String mLastFileName;
    
    private boolean mUpdateNotification;
    
    private boolean mWasPaused;
    
    @Override
    public void onCreate() {
        super.onCreate();

        mSequenceNumber = new AtomicInteger();
        
        mNotificationHandler = startHandlerThread("NotificationThread");

        mStatusHandler = startHandlerThread("StatusThread");

        // Create a separate thread for album art requests
        // because the request can be very slow.
        mAlbumArtHandler = startHandlerThread("AlbumArtThread");

        // Create a separate thread for commands to improve latency
        // (commands shouldn't have to wait for partially complete reads).
        mCommandHandler = startHandlerThread("CommandThread");
        
        mStatusReceiver = new StatusReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.ACTION_STATUS);
        registerReceiver(mStatusReceiver, filter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mStatusReceiver);
        stopHandlerThread(mNotificationHandler);
        stopHandlerThread(mStatusHandler);
        stopHandlerThread(mCommandHandler);
        stopHandlerThread(mAlbumArtHandler);
        super.onDestroy();
    }

    private Handler startHandlerThread(String name) {
        HandlerThread thread = new HandlerThread(name, Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        Looper looper = thread.getLooper();
        Handler.Callback callback = this;
        return new Handler(looper, callback);
    }

    private void stopHandlerThread(Handler handler) {
        Looper looper = handler.getLooper();
        looper.quit();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = (intent != null) ? intent.getAction() : null;
        Uri uri = (intent != null) ? intent.getData() : null;
        if (Intents.ACTION_STATUS.equals(action) && uri != null) {
            if (isCommand(uri)) {
                // A command will change the status,
                // so cancel any unsent requests to
                // query the status
                mStatusHandler.removeMessages(HANDLE_STATUS);
            }

            if (isSeek(uri) || isVolume(uri)) {
                if (isAbsoluteValue(uri)) {
                    // Seeking to an absolute position or volume
                    // invalidates any existing requests to change
                    // the position or volume.
                    mCommandHandler.removeMessages(HANDLE_STATUS);
                }
            }

            Handler handler = isCommand(uri) ? mCommandHandler : mStatusHandler;
            if (isCommand(uri) || !handler.hasMessages(HANDLE_STATUS)) {
                int sequenceNumber = isCommand(uri) ? mSequenceNumber.incrementAndGet()
                        : mSequenceNumber.get();
                int extraFlags = intent.getIntExtra(Intents.EXTRA_FLAGS, 0);
                Message msg = handler.obtainMessage(HANDLE_STATUS, sequenceNumber, extraFlags, uri);
                handler.sendMessage(msg);
            }
        } else if (Intents.ACTION_ART.equals(action) && uri != null) {
            int sequenceNumber = mSequenceNumber.get();
            Message msg = mAlbumArtHandler.obtainMessage(HANDLE_ALBUM_ART, sequenceNumber, -1, uri);
            msg.sendToTarget();
        } else if (Intents.ACTION_NOTIFICATION_CANCEL.equals(action)) {
            NotificationControls.cancel(this);
        } else if (Intents.ACTION_NOTIFICATION_CREATE.equals(action)) {
            mNotificationHandler.obtainMessage(HANDLE_NOTIFICATION_CREATE).sendToTarget();
        }
        // Stop the service if no new Intents are received for 20 seconds
        Handler handler = mCommandHandler;
        Message msg = handler.obtainMessage(HANDLE_STOP, startId, -1);
        handler.sendMessageDelayed(msg, 20 * 1000);     
        return START_STICKY;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case HANDLE_STATUS: {
                Uri uri = (Uri) msg.obj;
                MediaServer server = new MediaServer(this, uri);
                if(!server.getAuthority().equals(mAuthority)) {
                    mAuthority = server.getAuthority();
                    USE_XML_STATUS = false; // new server so try json first
                }
                int sequenceNumber = msg.arg1;
                int flags = msg.arg2;
                if (sequenceNumber == mSequenceNumber.get()) {
                    boolean setResumeOnIdle = ((flags & Intents.FLAG_SET_RESUME_ON_IDLE) != 0);
                    boolean onlyIfPlaying = ((flags & Intents.FLAG_ONLY_IF_PLAYING) != 0);
                    boolean onlyIfPaused = ((flags & Intents.FLAG_ONLY_IF_PAUSED) != 0);
                    boolean conditional = onlyIfPlaying || onlyIfPaused;
                    try {
                        if (conditional) {
                            Status status = server.status().read();
                            if (onlyIfPlaying && !status.isPlaying()) {
                                return true;
                            }
                            if (onlyIfPaused && !status.isPaused()) {
                                return true;
                            }
                        }
                        Status status = server.status(uri).read();
                        if (sequenceNumber == mSequenceNumber.get()) {
                            sendBroadcast(Intents.status(status));
                            if (isCommand(uri)) {
                                // Check the status again after the command
                                // has had time to take effect.
                                msg = mStatusHandler.obtainMessage(HANDLE_STATUS, sequenceNumber,
                                        0, readOnly(uri));
                                mStatusHandler.sendMessageDelayed(msg, 500);
                            }
                        } else {
                            Log.d(TAG, "Dropped stale status response: " + uri);
                        }
                        if (setResumeOnIdle) {
                            Preferences.get(this).setResumeOnIdle();
                        }
                    } catch (Throwable tr) {
                        if(JsonContentHandler.FILE_NOT_FOUND.equals(tr.getMessage())) {
                            USE_XML_STATUS = true;
                        }
                        String message = String.valueOf(tr);
                        Log.e(TAG, message, tr);
                        Intent broadcast = Intents.error(tr);
                        broadcast.putExtra(Intents.EXTRA_FLAGS, flags);
                        sendBroadcast(broadcast);
                    }
                } else {
                    Log.d(TAG, "Dropped stale status request: " + uri);
                }
                return true;
            }
            case HANDLE_ALBUM_ART: {
                Uri uri = (Uri) msg.obj;
                MediaServer server = new MediaServer(this, uri);
                int sequenceNumber = msg.arg1;
                if (sequenceNumber == mSequenceNumber.get()) {
                    try {
                        Bitmap bitmap = server.image(uri).read();
                        if (sequenceNumber == mSequenceNumber.get()) {
                            sendBroadcast(Intents.art(bitmap));
                        } else {
                            Log.d(TAG, "Dropped stale album art response: " + uri);
                        }
                    } catch (Throwable tr) {
                        String message = String.valueOf(tr);
                        Log.e(TAG, message, tr);
                        sendBroadcast(Intents.error(tr));
                    }
                } else {
                    Log.d(TAG, "Dropped stale album art request: " + uri);
                }
                return true;
            }
            case HANDLE_NOTIFICATION_CREATE: {
                new NotificationControls(this).showLoading();
                mUpdateNotification = true;
                return true;
            }
            case HANDLE_NOTIFICATION: {
                int sequenceNumber = msg.arg1;
                if (sequenceNumber == mSequenceNumber.get()) {
                    MediaServer server = new MediaServer(this, Preferences.get(this).getAuthority());
                    Bitmap bitmap;
                    try {
                        bitmap = server.art().read();
                    } catch(IOException ex) {
                        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.albumart_mp_unknown);
                    }
                    new NotificationControls(this).show((Status) msg.obj, bitmap);
                }
                return true;
            }
            case HANDLE_STOP: {
                int startId = msg.arg1;
                stopSelf(startId);
                return true;
            }
            default:
                return false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private final class StatusReceiver extends BroadcastReceiver {    

        @Override
        public void onReceive(Context context, Intent intent) {
            if(mUpdateNotification || Preferences.get(context).isNotificationSet()) {
                Status status = (Status) intent.getSerializableExtra(Intents.EXTRA_STATUS);
                if(mUpdateNotification || mWasPaused != status.isPaused() || !TextUtils.equals(status.getTrack().getName(), mLastFileName)) {
                    mWasPaused = status.isPaused();
                    mLastFileName = status.getTrack().getName();
                    int seq = mSequenceNumber.get();
                    Message msg = mNotificationHandler.obtainMessage(HANDLE_NOTIFICATION, seq, -1, status);
                    msg.sendToTarget();
                }
                mUpdateNotification = false;
            }
        }
        
    }
}

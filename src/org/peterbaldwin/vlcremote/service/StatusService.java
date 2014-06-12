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
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.peterbaldwin.client.android.vlcremote.MediaAppWidgetProvider;
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
    
    private static boolean HAS_CANCELLED_NOTIFICATION;
    
    private static final String TAG = "StatusService";

    private static final int REMOTE_STATUS = 0;
    private static final int REMOTE_ERROR = -1;
    
    private static final int HANDLE_STATUS = 1;
    private static final int HANDLE_ALBUM_ART = 2;
    private static final int HANDLE_STOP = 3;
    private static final int HANDLE_REMOTE_VIEWS = 4;
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

    private Handler mRemoteViewsHandler;

    private AtomicInteger mSequenceNumber;
    
    private String mLastState;
    
    private String mLastFileName;
    
    private boolean mUpdateRemoteViews;
    
    @Override
    public void onCreate() {
        super.onCreate();

        mSequenceNumber = new AtomicInteger();
        
        mRemoteViewsHandler = startHandlerThread("RemoteViewsThread");

        mStatusHandler = startHandlerThread("StatusThread");

        // Create a separate thread for album art requests
        // because the request can be very slow.
        mAlbumArtHandler = startHandlerThread("AlbumArtThread");

        // Create a separate thread for commands to improve latency
        // (commands shouldn't have to wait for partially complete reads).
        mCommandHandler = startHandlerThread("CommandThread");
    }

    @Override
    public void onDestroy() {
        stopHandlerThread(mRemoteViewsHandler);
        stopHandlerThread(mStatusHandler);
        stopHandlerThread(mCommandHandler);
        stopHandlerThread(mAlbumArtHandler);
        super.onDestroy();
    }

    private Handler startHandlerThread(String name) {
        HandlerThread thread = new HandlerThread(name, Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        return new Handler(thread.getLooper(), this);
    }

    private void stopHandlerThread(Handler handler) {
        handler.getLooper().quit();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = (intent != null) ? intent.getAction() : null;
        Uri uri = (intent != null) ? intent.getData() : null;
        if (Intents.ACTION_STATUS.equals(action) && uri != null) {
            if (isCommand(uri)) {
                // A command will change the status, so cancel any unsent
                // requests to query the status
                mStatusHandler.removeMessages(HANDLE_STATUS);
            }

            if (isSeek(uri) || isVolume(uri)) {
                if (isAbsoluteValue(uri)) {
                    // Seeking to an absolute position or volume invalidates
                    // any existing requests to change the position or volume
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
            int seqNumber = mSequenceNumber.get();
            mAlbumArtHandler.obtainMessage(HANDLE_ALBUM_ART, seqNumber, -1, uri).sendToTarget();
        } else if (Intents.ACTION_NOTIFICATION_CANCEL.equals(action)) {
            NotificationControls.cancel(this);
            HAS_CANCELLED_NOTIFICATION = true;
        } else if (Intents.ACTION_NOTIFICATION_CREATE.equals(action)) {
            mRemoteViewsHandler.obtainMessage(HANDLE_NOTIFICATION_CREATE).sendToTarget();
        } else if (Intents.ACTION_PROGRAMMATIC_APPWIDGET_UPDATE.equals(action)) {
            mUpdateRemoteViews = true;
            sendStatusRequest();
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
            case HANDLE_STATUS:
                handleStatus(msg);
                return true;
            case HANDLE_ALBUM_ART:
                handleArt(msg);
                return true;
            case HANDLE_NOTIFICATION_CREATE:
                NotificationControls.showLoading(this);
                mUpdateRemoteViews = true;
                HAS_CANCELLED_NOTIFICATION = false;
                sendStatusRequest();
                return true;
            case HANDLE_REMOTE_VIEWS:
                handleRemoteViews(msg);
                return true;
            case HANDLE_STOP:
                int startId = msg.arg1;
                stopSelf(startId);
                return true;
            default:
                return false;
        }
    }

    private void handleStatus(Message msg) {
        Uri uri = (Uri) msg.obj;
        MediaServer server = new MediaServer(this, uri);
        if(!server.getAuthority().equals(mAuthority)) {
            mAuthority = server.getAuthority();
            USE_XML_STATUS = false; // new server so try json first
        }
        int seqNumber = msg.arg1;
        int flags = msg.arg2;
        if (seqNumber == mSequenceNumber.get()) {
            boolean setResumeOnIdle = ((flags & Intents.FLAG_SET_RESUME_ON_IDLE) != 0);
            boolean onlyIfPlaying = ((flags & Intents.FLAG_ONLY_IF_PLAYING) != 0);
            boolean onlyIfPaused = ((flags & Intents.FLAG_ONLY_IF_PAUSED) != 0);
            try {
                if (onlyIfPlaying || onlyIfPaused) {
                    Status status = server.status().read();
                    if (onlyIfPlaying && !status.isPlaying()) {
                        return;
                    }
                    if (onlyIfPaused && !status.isPaused()) {
                        return;
                    }
                }
                Preferences pref = Preferences.get(this);
                Status status = server.status(uri).read();
                if (seqNumber == mSequenceNumber.get()) {
                    sendBroadcast(Intents.status(status));
                    Message n = mRemoteViewsHandler.obtainMessage(HANDLE_REMOTE_VIEWS, seqNumber, REMOTE_STATUS, status);
                    n.sendToTarget();
                    if (isCommand(uri)) {
                        // Check the status again after the command has had time to take effect.
                        msg = mStatusHandler.obtainMessage(HANDLE_STATUS, seqNumber, 0, readOnly(uri));
                        mStatusHandler.sendMessageDelayed(msg, 500);
                    }
                } else {
                    Log.d(TAG, "Dropped stale status response: " + uri);
                }
                if (setResumeOnIdle) {
                    pref.setResumeOnIdle();
                }
            } catch (Throwable tr) {
                if(JsonContentHandler.FILE_NOT_FOUND.equals(tr.getMessage())) {
                    USE_XML_STATUS = true;
                }
                Log.e(TAG, "Error: " + tr.getMessage());
                Intent broadcast = Intents.error(tr);
                broadcast.putExtra(Intents.EXTRA_FLAGS, flags);
                sendBroadcast(broadcast);
                mRemoteViewsHandler.obtainMessage(HANDLE_REMOTE_VIEWS, seqNumber, REMOTE_ERROR, tr).sendToTarget();
            }
        } else {
            Log.d(TAG, "Dropped stale status request: " + uri);
        }
    }
    
    private void handleArt(Message msg) {
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
    }
    
    private void handleRemoteViews(Message msg) {
        int seqNumber = msg.arg1;
        if (seqNumber != mSequenceNumber.get()) {
            return;
        }
        Preferences pref = Preferences.get(this);
        if(REMOTE_STATUS == msg.arg2) {
            Status status = (Status) msg.obj;
            MediaAppWidgetProvider.scheduleUpdate(this, status);
            handleRemoteViewsStatus(status, pref);
        } else if(REMOTE_ERROR == msg.arg2) {
            handleRemoteViewsError((Throwable) msg.obj, pref);
        }
        mUpdateRemoteViews = false;
    }
    
    private void handleRemoteViewsStatus(Status status, Preferences pref) {
        if(!checkStatusChanged(status) && !mUpdateRemoteViews) {
            return; 
        }
        int[] widgetIds = MediaAppWidgetProvider.getWidgetIds(this);
        if(widgetIds.length == 0 && (!pref.isNotificationSet() || HAS_CANCELLED_NOTIFICATION)) {
            return;
        }
        MediaServer server = new MediaServer(this, pref.getAuthority());
        Bitmap bitmap;
        try {
            bitmap = server.art().read();
        } catch(IOException ex) {
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.albumart_mp_unknown);
        }
        if(pref.isNotificationSet() && !HAS_CANCELLED_NOTIFICATION) {
            NotificationControls.show(this, status, bitmap);
        }
        if(widgetIds.length != 0) {
            MediaAppWidgetProvider.update(this, status, bitmap);
        }
    }
    
    private void handleRemoteViewsError(Throwable tr, Preferences pref) {
        MediaAppWidgetProvider.cancelPendingUpdate(this);
        int[] widgetIds = MediaAppWidgetProvider.getWidgetIds(this);
        if(widgetIds.length == 0 && (!pref.isNotificationSet() || HAS_CANCELLED_NOTIFICATION)) {
            return;
        }
        if(pref.isNotificationSet() && !HAS_CANCELLED_NOTIFICATION) {
            NotificationControls.showError(this, tr);
        }
        if(widgetIds.length != 0) {
            MediaAppWidgetProvider.update(this, tr);
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void sendStatusRequest() {
        MediaServer server = new MediaServer(this, Preferences.get(this).getAuthority());
        if(server.getAuthority() != null) {
            server.status().get();
        }
    }
    
    /**
     * Check if the playback status has changed. If the status has changed,
     * the new state will be stored.
     * @param status Status
     * @return true if status was changed, false otherwise
     */
    private boolean checkStatusChanged(Status status) {
        if(!status.equalsState(mLastFileName, mLastState)) {
            mLastFileName = status.getTrack().getName();
            mLastState = status.getState();
            return true;
        }
        return false;
     }    

}

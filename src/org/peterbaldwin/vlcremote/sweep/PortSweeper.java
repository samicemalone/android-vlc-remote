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

package org.peterbaldwin.vlcremote.sweep;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class PortSweeper {

    public interface Callback {
        void onHostFound(String hostname, int responseCode);

        void onProgress(int progress, int max);
    }
    
    public static final String EXTRA_PORT = "org.peterbaldwin.portsweep.intent.extra.PORT";
    public static final String EXTRA_FILE = "org.peterbaldwin.portsweep.intent.extra.FILE";
    public static final String EXTRA_WORKERS = "org.peterbaldwin.portsweep.intent.extra.WORKERS";

    private static final String TAG = "Scanner";

    private static final int HANDLE_SCAN = 1;

    private static final int HANDLE_START = 1;
    private static final int HANDLE_REACHABLE = 2;
    private static final int HANDLE_UNREACHABLE = 3;
    private static final int HANDLE_COMPLETE = 4;

    private final Queue<byte[]> mAddressQueue;

    /**
     * Queue for scan requests.
     */
    private final Handler mScanHandler;

    /**
     * Processes scan requests.
     * <p>
     * No real work is done on this thread; it just waits for the workers to
     * finish using {@link Thread#join()}.
     */
    private final HandlerThread mScanThread;

    /**
     * Dispatches callbacks on the main thread.
     */
    private final Handler mCallbackHandler;

    /**
     * Provides work for worker threads.
     */
    private final Worker.Manager mWorkerManager;

    /**
     * Handles results from worker threads.
     */
    private final Worker.Callback mWorkerCallback;

    /**
     * The port to scan.
     */
    private final int mPort;

    /**
     * The HTTP path to scan.
     */
    private final String mPath;

    /**
     * The number of workers to allocate.
     */
    private final int mWorkerCount;

    /**
     * Callback for port sweep progress and results.
     */
    private Callback mCallback;

    /**
     * The current progress.
     */
    private int mProgress;

    /**
     * The maximum progress.
     */
    private int mMax;

    /**
     * Indicates that the sweep is complete.
     */
    private boolean mComplete;

    public PortSweeper(int port, String file, int threadCount, Callback callback, Looper looper) {
        mPort = port;
        mPath = file;
        mWorkerCount = threadCount;
        mCallback = callback;

        mAddressQueue = new ConcurrentLinkedQueue<byte[]>();

        mWorkerManager = new MyWorkerManager();

        mWorkerCallback = new MyWorkerCallback();

        mScanThread = new HandlerThread("Scanner", Process.THREAD_PRIORITY_BACKGROUND);
        mScanThread.start();

        Handler.Callback callbackHandlerCallback = new MyCallbackHandlerCallback();
        mCallbackHandler = new Handler(looper, callbackHandlerCallback);

        Handler.Callback scanHandlerCallback = new MyScanHandlerCallback();
        mScanHandler = new Handler(mScanThread.getLooper(), scanHandlerCallback);
    }
    
    public void setCallback(Callback callback) {
        mCallback = callback;
        if (mCallback != null) {
            // Replay progress for new callback receiver
            mCallback.onProgress(0, mMax);
            mCallback.onProgress(mProgress, mMax);
        }
    }

    /**
     * Schedule a new sweep. The new sweep will not start until all previous
     * sweeps have been fully aborted.
     * @param ipAddress Interface IP address
     */
    public void sweep(byte[] ipAddress) {
        abort();
        mScanHandler.obtainMessage(HANDLE_SCAN, ipAddress).sendToTarget();
    }

    public void abort() {
        // Clear pending jobs
        mScanHandler.removeMessages(HANDLE_SCAN);

        // Abort the job in progress
        mAddressQueue.clear();
    }

    public void destroy() {
        abort();
        Looper looper = mScanThread.getLooper();
        looper.quit();
    }

    /**
     * Scans all local IP addresses using a pool of worker threads and waits for
     * the all of the workers to finish scanning before returning.
     */
    private void handleScan(byte[] interfaceAddress) {
        Worker[] workers = new Worker[mWorkerCount];
        for (int i = 0; i < workers.length; i++) {
            Worker worker = workers[i] = new Worker(mPort, mPath);
            worker.setPriority(Thread.MIN_PRIORITY);
            worker.setManager(mWorkerManager);
            worker.setCallback(mWorkerCallback);
        }
        int count = 0;

        // Scan outwards from the interface IP address for best results
        // with DHCP servers that allocate addresses sequentially.
        byte start = interfaceAddress[interfaceAddress.length - 1];
        for (int delta = 1; delta < 128; delta++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                int b = (256 + start + sign * delta) % 256;
                if (b != 0) {
                    byte[] ipAddress = interfaceAddress.clone();
                    ipAddress[ipAddress.length - 1] = (byte) b;
                    mAddressQueue.add(ipAddress);
                    count += 1;
                } else {
                    // Skip broadcast address
                }
            }
        }
        mCallbackHandler.obtainMessage(HANDLE_START, 0, count).sendToTarget();
        for(Worker worker : workers) {
            worker.start();
        }
        try {
            for(Worker worker : workers) {
                worker.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mCallbackHandler.sendEmptyMessage(HANDLE_COMPLETE);
        }
    }

    private class MyScanHandlerCallback implements Handler.Callback {
        /** {@inheritDoc} */
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_SCAN:
                    byte[] interfaceAddress = (byte[]) msg.obj;
                    handleScan(interfaceAddress);
                    return true;
                default:
                    return false;
            }

        }
    }

    private class MyWorkerManager implements Worker.Manager {
        /** {@inheritDoc} */
        public byte[] pollIpAddress() {
            return mAddressQueue.poll();
        }
    }

    private class MyWorkerCallback implements Worker.Callback {

        /** {@inheritDoc} */
        public void onReachable(InetAddress address, int port, String hostname, int responseCode) {
            Message m = mCallbackHandler.obtainMessage(HANDLE_REACHABLE);
            m.obj = hostname;
            m.arg1 = responseCode;
            m.sendToTarget();
        }

        /** {@inheritDoc} */
        public void onUnreachable(byte[] ipAddress, int port, IOException e) {
            Message m = mCallbackHandler.obtainMessage(HANDLE_UNREACHABLE);
            m.obj = e;
            m.sendToTarget();
        }
    }

    private class MyCallbackHandlerCallback implements Handler.Callback {
        /** {@inheritDoc} */
        public boolean handleMessage(Message msg) {
            if (mComplete && msg.what != HANDLE_START) {
                Log.w(TAG, "unexpected callback");
                return true;
            }
            try {
                switch (msg.what) {
                    case HANDLE_START:
                        mComplete = false;
                        mProgress = msg.arg1;
                        mMax = msg.arg2;
                        return true;
                    case HANDLE_REACHABLE:
                        String hostname = (String) msg.obj;
                        int responseCode = msg.arg1;
                        Log.d(TAG, "found: " + hostname);
                        mCallback.onHostFound(hostname, responseCode);
                        mProgress++;
                        return true;
                    case HANDLE_UNREACHABLE:
                        IOException e = (IOException) msg.obj;
                        Log.d(TAG, "unreachable: " + e.getMessage());
                        mProgress++;
                        return true;
                    case HANDLE_COMPLETE:
                        mComplete = true;
                        mProgress = mMax;
                        return true;
                    default:
                        return false;
                }
            } finally {
                mProgress = Math.min(mProgress, mMax);
                mCallback.onProgress(mProgress, mMax);
            }
        }
    }
}

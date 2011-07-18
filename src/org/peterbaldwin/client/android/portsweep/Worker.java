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

package org.peterbaldwin.client.android.portsweep;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

class Worker extends Thread {
	public interface Callback {
		byte[] pollIpAddress();

		void onReachable(InetAddress address, int port, String hostname, int responseCode);

		void onUnreachable(byte[] ipAddress, int port, IOException e);
	}

	private final int mPort;
	private final String mFile;
	private Callback mCallback;

	public Worker(int port, String file) {
		super();
		mPort = port;
		mFile = file;
	}

	public Callback getCallback() {
		return mCallback;
	}

	public void setCallback(Callback callback) {
		this.mCallback = callback;
	}

	private static URI createUri(String scheme, String host, int port,
			String path) {
		try {
			return new URI("http", null, host, port, path, null, null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public void run() {
		// Notes:
		//
		// 1. InetAddress#isReachable(int) always returns false for Windows
		// hosts because applications do not have permission to perform ICMP
		// echo requests.
		//
		// 2. HttpURLConnection takes a long time to fail if the host does not
		// exist and the current implementation does not respect connection
		// timeouts.

		HttpClient client = new DefaultHttpClient();
		for (;;) {
			byte[] ipAddress = mCallback.pollIpAddress();
			if (ipAddress == null) {
				break;
			}
			try {
				InetAddress address = InetAddress.getByAddress(ipAddress);
				String hostAddress = address.getHostAddress();
				URI uri = createUri("http", hostAddress, mPort, mFile);
				HttpUriRequest request = new HttpGet(uri);
				HttpResponse response = client.execute(request);
				
				StatusLine statusLine = response.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					entity.consumeContent();
				}
				
				String hostname = address.getHostName();
				mCallback.onReachable(address, mPort, hostname, statusCode);
			} catch (IOException e) {
				mCallback.onUnreachable(ipAddress, mPort, e);
			}
		}
	}
}

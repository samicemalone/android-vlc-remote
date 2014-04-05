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
package org.peterbaldwin.vlcremote.model;

import android.net.Uri;
import android.text.TextUtils;
import java.net.HttpURLConnection;

/**
 *
 * @author Sam Malone
 */
public class Server {
    
    public final static int DEFAULT_PORT = 8080;
    public final static int DEFAULT_RESPONSE_CODE = HttpURLConnection.HTTP_OK;
    
    private String nickname;
    private String user;
    private String password;
    private Uri uri;
    private int responseCode;
    
    public Server(String nickname, String host, int port, String user, String password, int responseCode) {
        this.nickname = nickname != null ? nickname : "";
        this.user = user != null ? user : "";
        this.password = password != null ? password : "";
        StringBuilder authority = new StringBuilder();
        if(!this.user.isEmpty()) {
            authority.append(user);
        }
        if(!this.password.isEmpty()) {
            authority.append(':').append(password);
        }
        if(hasUserInfo()) {
            authority.append('@');
        }
        authority.append(host).append(':').append(port);
        uri = Uri.parse("http://" + authority.toString());
        this.responseCode = responseCode;
    }
    
    public Server(String nickname, String host, int port, String user, String password) {
        this(nickname, host, port, user, password, DEFAULT_RESPONSE_CODE);
    }
    
    public Server(String nickname, String authority, int responseCode) {
        uri = Uri.parse("http://" + authority);
        setUserInfo(uri.getUserInfo());
        this.responseCode = responseCode;
        this.nickname = nickname;
    }
    
    public Server(String authority, int responseCode) {
        this("", authority, responseCode);
    }
    
    public Server(String authority) {
        this(authority, DEFAULT_RESPONSE_CODE);
    }
    
    /**
     * Set the user authentication information
     * @param userInfo String containing username and password separated by a
     * colon. e.g. user:pass.
     */
    private void setUserInfo(String userInfo) {
        if(userInfo == null) {
            return;
        }
        int passDelim = userInfo.indexOf(':');
        if(passDelim < 0) {
            user = userInfo;
        } else if(passDelim == 0) {
            password = userInfo.substring(passDelim + 1, userInfo.length());
        } else {
            user = userInfo.substring(0, passDelim);
            password = userInfo.substring(passDelim + 1, userInfo.length());
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return uri.getHost();
    }
    
    public String getHostAndPort() {
        return String.format("%s:%d", getHost(), getPort());
    }

    /**
     * Get the port number for the server
     * @return Port number if set, otherwise DEFAULT_PORT
     */
    public int getPort() {
        if(uri.getPort() == -1) {
            return DEFAULT_PORT;
        }
        return uri.getPort();
    }

    /**
     * Get the response code that the server returned when trying to connect
     * @return response code
     */
    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int reponseCode) {
        this.responseCode = reponseCode;
    }
    
    /**
     * Checks if the server has user information set
     * @return true if the username or password is set (and non empty), false 
     * otherwise
     */
    public final boolean hasUserInfo() {
        return !TextUtils.isEmpty(user) || !TextUtils.isEmpty(password);
    }
    
    public Uri getUri() {
        return uri;
    }
    
    /**
     * Creates a key that denotes the server information. The format is
     * authority#responseCode;nickname
     * @return key to represent server information
     */
    public String toKey() {
        return String.format("%s#%d;%s", uri.getAuthority(), responseCode, nickname);
    }
    
    /**
     * Creates an instance of server with the information represented in key
     * @param key Server key.
     * @see toKey();
     * @return Server
     */
    public static Server fromKey(String key) {
        int responseDelim = key.lastIndexOf('#');
        if(responseDelim < 0) {
            return new Server(key);
        } else if(responseDelim == 0) {
            return null;
        }
        int nicknameDelim = key.lastIndexOf(';');
        String nickname = nicknameDelim > 0 ? key.substring(nicknameDelim + 1) : "";
        int response = Integer.valueOf(key.substring(responseDelim + 1, nicknameDelim));
        return new Server(nickname, key.substring(0, responseDelim), response);
    }
    
}

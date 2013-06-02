/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.peterbaldwin.vlcremote.model;

import android.net.Uri;
import java.net.HttpURLConnection;

/**
 *
 * @author Sam Malone
 */
public class Server {
    
    public final static int DEFAULT_PORT = 8080;
    public final static int DEFAULT_RESPONSE_CODE = HttpURLConnection.HTTP_OK;
    
    private String user;
    private String password;
    private Uri uri;
    private int reponseCode;
    
    public Server(String authority, int responseCode) {
        uri = Uri.parse("http://" + authority);
        setUserInfo(uri.getUserInfo());
        this.reponseCode = responseCode;
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
        return reponseCode;
    }

    public void setReponseCode(int reponseCode) {
        this.reponseCode = reponseCode;
    }
    
    /**
     * Checks if the server has user information set
     * @return true if the username or password is set, false otherwise
     */
    public boolean hasUserInfo() {
        return user != null && password != null;
    }
    
    public Uri getUri() {
        return uri;
    }
    
    /**
     * Creates a key that denotes the server information. The format is
     * authority#responseCode
     * @return key to represent server information
     */
    public String toKey() {
        return String.format("%s#%d", uri.getAuthority(), reponseCode);
    }
    
    /**
     * Creates an instance of server with the information represented in key
     * @param key Server key.
     * @see toKey();
     * @return Server
     */
    public static Server fromKey(String key) {
        int responseDelim = key.indexOf('#');
        if(responseDelim < 0) {
            return new Server(key);
        } else if(responseDelim == 0) {
            return null;
        }
        return new Server(key.substring(0, responseDelim), Integer.valueOf(key.substring(responseDelim + 1)));
    }
    
}

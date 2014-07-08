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

package org.peterbaldwin.vlcremote.model;

import android.content.Intent;
import android.net.Uri;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.peterbaldwin.vlcremote.util.MimeTypeMap;

public final class File {
    
    public enum Type {
        DIRECTORY(0),
        FILE(1),
        LIBRARY(2),
        LIBRARY_NAME(3),
        LIBRARY_DIRECTORY(4);
        
        private final int type;

        private Type(int type) {
            this.type = type;
        }

        public int getId() {
            return type;
        }
    }
    
    public static File getLibrary(String libraryName) {
        return new File(File.Type.LIBRARY_NAME, 0L, null, "library://" + libraryName, libraryName, null);
    }

    public static final File LIBRARIES = new File(Type.LIBRARY, 0L, null, "library://", "Libraries", null);
    
    public static final int PATH_UNIX = 0;
    public static final int PATH_WINDOWS = 1;
    public static int PATH_TYPE = PATH_UNIX;

    private static final MimeTypeMap sMimeTypeMap = MimeTypeMap.getSingleton();
    private static final Pattern schemedPathPattern = Pattern.compile("^([A-Za-z]+://)?(.*)$");

    private static String parseExtension(String path) {
        int index = path.lastIndexOf('.');
        if (index != -1) {
            return path.substring(index + 1);
        } else {
            return null;
        }
    }
    
    public static String baseName(String path) {
        if(path == null) {
            return null;
        }
        String fileProtocol = "file:///";
        int offset = 0;
        if(path.startsWith(fileProtocol)) {
            offset = fileProtocol.length();
        }
        int bslash = path.lastIndexOf('\\');
        int fslash = path.substring(offset).lastIndexOf('/') + offset;
        if(fslash == -1 && bslash == -1) {
            return path;
        }
        return path.substring(Math.max(bslash, fslash) + 1);
    }

    private Type mType;
    private Long mSize;
    private String mDate;
    private String mPath;
    private String mName;
    private String mExtension;

    public File(Type type, Long size, String date, String path, String name, String extension) {
        mType = type;
        mSize = size;
        mDate = date;
        mPath = path;
        mName = name;
        mExtension = extension != null ? extension : path != null ? parseExtension(path) : null;
    }

    public Type getType() {
        return mType;
    }

    public void setType(Type type) {
        mType = type;
    }
    
    public boolean isType(Type type) {
        return mType == type;
    }

    public boolean isDirectory() {
        // Type is "directory" in VLC 1.0 and "dir" in VLC 1.1
        return mType == Type.DIRECTORY;
    }
    
    public boolean isLibrary() {
        return mType == Type.LIBRARY;
    }
    
    public boolean isLibraryName() {
        return mType == Type.LIBRARY_NAME;
    }
    
    public boolean isLibraryDir() {
        return mType == Type.LIBRARY_DIRECTORY;
    }
    
    public boolean isBrowsable() {
        return isLibrary() || isDirectory() || isLibraryName() || isLibraryDir();
    }
    
    /**
     * Checks if this File is a parent entry (name is ..)
     * @return true if this File is a parent entry, false otherwise. 
     */
    public boolean isParent() {
        return "..".equals(mName);
    }

    public static boolean isImage(String ext) {
        String mimeType = getMimeType(ext);
        return mimeType != null && mimeType.startsWith("image/");
    }

    public Long getSize() {
        return mSize;
    }

    public void setSize(Long size) {
        mSize = size;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        mDate = date;
    }

    public String getPath() {
        return mPath;
    }
    
    public String getNormalizedPath() {
        return getNormalizedPath(mPath);
    }
    
    /**
     * Get the normalized path for the given file path.
     * Any parent directories (..) will be resolved.
     * @param file file path
     * @return 
     */
    public static String getNormalizedPath(String file) {
        Matcher m = schemedPathPattern.matcher(file);
        if(!m.find()) {
            return Directory.ROOT_DIRECTORY;
        }
        String scheme = m.group(1);
        String path = m.group(2);
        if(scheme == null) {
            scheme = "";
        }
        String[] st = path.split("(\\\\|/)+");
        ArrayDeque<String> segmentList = new ArrayDeque<String>();
        for(String segment : st) {
            if("..".equals(segment)) {
                segmentList.pollFirst();
                continue;
            }
            segmentList.offerFirst(segment);
        }
        if(segmentList.isEmpty() && scheme.isEmpty()) {
            return Directory.ROOT_DIRECTORY;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(scheme);
        while(!segmentList.isEmpty()) {
            sb.append(segmentList.pollLast());
            if(segmentList.peekLast() != null) {
                sb.append('/');
            }
        }
        return sb.length() == 0 ? Directory.ROOT_DIRECTORY : sb.toString();
    }

    public String getMrl() {
        return getMrl(mPath, mExtension);
    }
    
    public static String getMrl(String path, String extension) {
        if (isImage(extension)) {
            return "fake://";
        } else {
            return Uri.fromFile(new java.io.File(path)).toString();
        }
    }

    public List<String> getOptions() {
        if (isImage(mExtension)) {
            return Collections.singletonList(":fake-file=" + getPath());
        } else {
            return Collections.emptyList();
        }
    }

    public List<String> getStreamingOptions() {
        List<String> options = new ArrayList<String>(getOptions());
        String mimeType = getMimeType();
        if (mimeType != null && mimeType.startsWith("audio/")) {
            options.add(":sout=#transcode{acodec=vorb,ab=128}:standard{access=http,mux=ogg,dst=0.0.0.0:8000}");
        } else {
            options.add(":sout=#transcode{vcodec=mp4v,vb=384,acodec=mp4a,ab=64,channels=2,fps=25,venc=x264{profile=baseline,keyint=50,bframes=0,no-cabac,ref=1,vbv-maxrate=4096,vbv-bufsize=1024,aq-mode=0,no-mbtree,partitions=none,no-weightb,weightp=0,me=dia,subme=0,no-mixed-refs,no-8x8dct,trellis=0,level1.3},vfilter=canvas{width=320,height=180,aspect=320:180,padd},senc,soverlay}:rtp{sdp=rtsp://0.0.0.0:5554/stream.sdp,caching=4000}}");
        }
        return options;
    }

    public Intent getIntentForStreaming(String authority) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String mimeType = getMimeType();
        if (mimeType != null && mimeType.startsWith("audio/")) {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("rtsp");
            builder.encodedAuthority(swapPortNumber(authority, 5554));
            builder.path("stream.sdp");
            Uri data = builder.build();
            intent.setData(data);
        } else {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("http");
            builder.encodedAuthority(swapPortNumber(authority, 8000));
            Uri data = builder.build();
            intent.setDataAndType(data, "application/ogg");
        }
        return intent;
    }

    public void setPath(String path) {
        mPath = path;
        if (mExtension == null && path != null) {
            mExtension = parseExtension(path);
        }
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getExtension() {
        return mExtension;
    }

    public void setExtension(String extension) {
        mExtension = extension;
    }

    public String getMimeType() {
        return getMimeType(mExtension);
    }

    public static String getMimeType(String ext) {
        if (ext != null) {
            // See http://code.google.com/p/android/issues/detail?id=8806
            String extension = ext.toLowerCase();
            return sMimeTypeMap.getMimeTypeFromExtension(extension);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return mName;
    }

    private static String removePortNumber(String authority) {
        int index = authority.lastIndexOf(':');
        if (index != -1) {
            // Remove port number
            authority = authority.substring(0, index);
        }
        return authority;
    }

    private static String swapPortNumber(String authority, int port) {
        return removePortNumber(authority) + ":" + port;
    }
}

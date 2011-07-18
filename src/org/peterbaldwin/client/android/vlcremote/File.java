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

package org.peterbaldwin.client.android.vlcremote;

import android.webkit.MimeTypeMap;

class File {
    
    private static final MimeTypeMap sMimeTypeMap = MimeTypeMap.getSingleton();

    private static String parseExtension(String path) {
        int index = path.lastIndexOf('.');
        if (index != -1) {
            return path.substring(index + 1);
        } else {
            return null;
        }
    }
    
	private String mType;
	private Long mSize;
	private String mDate;
	private String mPath;
	private String mName;
	private String mExtension;

	public File(String type, Long size, String date, String path, String name,
			String extension) {
		mType = type;
		mSize = size;
		mDate = date;
		mPath = path;
		mName = name;
        mExtension = extension != null ? extension : path != null ? parseExtension(path) : null;
	}

	public String getType() {
		return mType;
	}

	public void setType(String type) {
		mType = type;
	}

	public boolean isDirectory() {
		// Type is "directory" in VLC 1.0 and "dir" in VLC 1.1
		return "directory".equals(mType) || "dir".equals(mType);
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
        if (mExtension != null) {
            // See http://code.google.com/p/android/issues/detail?id=8806
            String extension = mExtension .toLowerCase();
            return sMimeTypeMap.getMimeTypeFromExtension(extension);
        } else {
            return null;
        }
    }

	@Override
	public String toString() {
		return mName;
	}	
}

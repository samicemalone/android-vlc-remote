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

package org.peterbaldwin.vlcremote.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.model.Directory;
import org.peterbaldwin.vlcremote.model.File;
import org.peterbaldwin.vlcremote.model.Preferences;

public class DirectoryAdapter extends ArrayAdapter<File> implements SectionIndexer {

    private Object[] mSections = new Object[0];

    private Integer[] mPositionForSection = new Integer[0];

    private Integer[] mSectionForPosition = new Integer[0];
    
    private Directory mDirectory;

    public DirectoryAdapter(Context context) {
        super(context, R.layout.file_list_item, android.R.id.text1);
    }

    /** {@inheritDoc} */
    public int getPositionForSection(int section) {
        if (section < 0) {
            section = 0;
        }
        if (section >= mPositionForSection.length) {
            section = mPositionForSection.length - 1;
        }
        return mPositionForSection[section];
    }

    /** {@inheritDoc} */
    public int getSectionForPosition(int position) {
        if (position < 0) {
            position = 0;
        }
        if (position >= mSectionForPosition.length) {
            position = mSectionForPosition.length - 1;
        }
        return mSectionForPosition[position];
    }

    /** {@inheritDoc} */
    public Object[] getSections() {
        return mSections;
    }

    @Override
    public void add(File object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        super.clear();
        mSections = new Object[0];
        mPositionForSection = new Integer[0];
        mSectionForPosition = new Integer[0];
        mDirectory = null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);
        File file = getItem(position);
        ImageView icon = (ImageView) v.findViewById(android.R.id.icon);
        if (file.isDirectory() || file.isLibraryDir()) {
            icon.setImageResource("..".equals(file.getName()) ? R.drawable.ic_up : R.drawable.ic_directory);
        } else if(file.isLibrary() || file.isLibraryName()) {
            icon.setImageResource(R.drawable.ic_library);
        } else {
            String contentType = file.getMimeType();
            if (contentType != null) {
                contentType = contentType.toLowerCase();
                if (contentType.startsWith("audio/")) {
                    icon.setImageResource(R.drawable.ic_mime_audio);
                } else if (contentType.startsWith("image/")) {
                    icon.setImageResource(R.drawable.ic_mime_image);
                } else if (contentType.startsWith("video/")) {
                    icon.setImageResource(R.drawable.ic_mime_video);
                } else {
                    icon.setImageResource(R.drawable.ic_file);
                }
            } else {
                icon.setImageResource(R.drawable.ic_file);
            }
        }
        TextView tv = (TextView) v.findViewById(android.R.id.text1);
        int size = Preferences.get(getContext()).getTextSize();
        if(size == Preferences.TEXT_LARGE) {
            tv.setTextAppearance(getContext(), android.R.style.TextAppearance_Large);
        } else if(size == Preferences.TEXT_MEDIUM) {
            tv.setTextAppearance(getContext(), android.R.style.TextAppearance_Medium);
        } else {
            tv.setTextAppearance(getContext(), android.R.style.TextAppearance_Small);
        }
        return v;
    }

    private String getSection(File file) {
        String name = file.getName();
        if (name.equals("..")) {
            Context context = getContext();
            return context.getString(R.string.section_parent);
        } else if (name.length() > 0) {
            char c = name.charAt(0);
            c = Character.toUpperCase(c);
            return String.valueOf(c);
        } else {
            // This shouldn't happen
            return "";
        }
    }
    
    public Set<String> getRealPaths(File file) {
        Preferences p = Preferences.get(getContext());
        if(file.isLibraryDir() || file.isLibraryName()) {
            return mDirectory.getRealPaths(file.getName());
        }
        Set<String> s = new TreeSet<String>();
        if(file.isLibrary()) {
            for(String library : p.getLibraries()) {
                s.addAll(p.getLibraryDirectories(library));
            }
            return s;
        }
        s.add(file.getPath());
        return s;
    }

    public void setDirectory(Directory items) {
        super.clear();
        if (items != null) {
            mDirectory = items;
            int count = items.size();
            // Space for every letter and digit, plus a couple symbols
            int capacity = 48;
            Integer lastSection = null;
            List<String> sections = new ArrayList<String>(capacity);
            List<Integer> positionForSection = new ArrayList<Integer>(capacity);
            List<Integer> sectionForPosition = new ArrayList<Integer>(count);

            for (int position = 0; position < count; position++) {
                File file = items.get(position);
                String section = getSection(file);
                if (!sections.contains(section)) {
                    lastSection = Integer.valueOf(sections.size());
                    sections.add(section);
                    positionForSection.add(Integer.valueOf(position));
                }
                sectionForPosition.add(lastSection);
                super.add(file);
            }

            mSections = sections.toArray();

            mPositionForSection = toArray(positionForSection);
            mSectionForPosition = toArray(sectionForPosition);
        } else {
            mSections = new Object[0];
            mPositionForSection = new Integer[0];
            mSectionForPosition = new Integer[0];
            mDirectory = null;
        }
    }

    private static Integer[] toArray(List<Integer> list) {
        return list.toArray(new Integer[list.size()]);
    }
}

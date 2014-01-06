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

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.model.File;
import org.peterbaldwin.vlcremote.model.Media;
import org.peterbaldwin.vlcremote.model.PlaylistItem;

public final class PlaylistAdapter extends BaseAdapter implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public static class ViewHolder {
        public TextView playlistHeading;
        public TextView playlistText;
        public View icon;
    }

    private List<PlaylistItem> mItems;
    
    /**
     * The position of the items marked as current in the playlist. Ideally
     * there should only be one current item but VLC has a bug which compares
     * tracks names instead of track id's so files with the same names are all
     * marked as current.
     */
    private final HashSet<Integer> mCurrentPositions;
    
    public PlaylistAdapter() {
        mCurrentPositions = new HashSet<Integer>(4);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.playlist_list_item, parent, false);
            holder = new ViewHolder();
            holder.playlistHeading = (TextView) convertView.findViewById(android.R.id.text1);
            holder.playlistText = (TextView) convertView.findViewById(android.R.id.text2);
            holder.icon = convertView.findViewById(android.R.id.icon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        setPlaylistDisplayInfo(holder, getItem(position));
        return convertView;
    }
    
    private void setPlaylistDisplayInfo(ViewHolder holder, PlaylistItem item) {
        holder.playlistHeading.setText(item.getPlaylistHeading());
        if(TextUtils.isEmpty(item.getPlaylistText())) {
            holder.playlistText.setText(File.baseName(item.getUri()));
        } else {
            holder.playlistText.setText(item.getPlaylistText());
        }
        if(holder.playlistHeading.getText().equals(holder.playlistText.getText())) {
            holder.playlistText.setText("");
        }
        holder.icon.setVisibility(item.isCurrent() ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getCount() {
        return mItems != null ? mItems.size() : 0;
    }

    @Override
    public PlaylistItem getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        if (position < getCount()) {
            PlaylistItem item = getItem(position);
            return item.getId();
        }
        return AdapterView.INVALID_ROW_ID;
    }

    public void setItems(List<PlaylistItem> items) {
        mItems = items;
        if (mItems != null) {
            populateCurrentItems();
            notifyDataSetChanged();
        } else {
            notifyDataSetInvalidated();
        }
    }
    
    private void populateCurrentItems() {
        mCurrentPositions.clear();
        for(int i = 0; i < mItems.size(); i++) {
            if(mItems.get(i).isCurrent()) {
                mCurrentPositions.add(i);
            }
        }
    }
    
    public void setCurrentItem(int position) {
        if(position >= 0 && position < mItems.size()) {
            if(mItems.get(position) instanceof Media) {
                for (Iterator<Integer> it = mCurrentPositions.iterator(); it.hasNext();) {
                    ((Media) mItems.get(it.next())).setCurrent(false);
                }
                ((Media) mItems.get(position)).setCurrent(true);
                mCurrentPositions.clear();
                mCurrentPositions.add(position);
                notifyDataSetChanged();
            }
        }
    }
    
    /**
     * Get the set of item positions of current tracks
     * @return item positions of current tracks
     */
    public HashSet<Integer> getCurrentItems() {
        return mCurrentPositions;
    }
    
    public PlaylistItem remove(int position) {
        PlaylistItem item =  mItems.remove(position);
        notifyDataSetChanged();
        return item;
    }

    public List<PlaylistItem> getItems() {
        int count = getCount();
        List<PlaylistItem> items = new ArrayList<PlaylistItem>(count);
        for (int position = 0; position < count; position++) {
            PlaylistItem item = getItem(position);
            items.add(item);
        }
        return items;
    }
}

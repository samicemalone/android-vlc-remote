
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
import java.util.List;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.model.File;
import org.peterbaldwin.vlcremote.model.PlaylistItem;

public final class PlaylistAdapter extends BaseAdapter implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public static class ViewHolder {
        public TextView playlistHeading;
        public TextView playlistText;
        public View icon;
    }

    private List<PlaylistItem> mItems;

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    public int getCount() {
        return mItems != null ? mItems.size() : 0;
    }

    /** {@inheritDoc} */
    public PlaylistItem getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    /** {@inheritDoc} */
    public long getItemId(int position) {
        if (position < getCount()) {
            PlaylistItem item = getItem(position);
            return item.getId();
        } else {
            return AdapterView.INVALID_ROW_ID;
        }
    }

    public void setItems(List<PlaylistItem> items) {
        mItems = items;
        if (mItems != null) {
            notifyDataSetChanged();
        } else {
            notifyDataSetInvalidated();
        }
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

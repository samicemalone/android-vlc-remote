
package org.peterbaldwin.vlcremote.widget;

import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.model.PlaylistItem;
import org.peterbaldwin.vlcremote.model.Track;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class PlaylistAdapter extends BaseAdapter {

    private List<PlaylistItem> mItems;

    /** {@inheritDoc} */
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(R.layout.playlist_list_item, parent, false);
        }
        TextView text1 = (TextView) view.findViewById(android.R.id.text1);
        TextView text2 = (TextView) view.findViewById(android.R.id.text2);
        View icon = view.findViewById(android.R.id.icon);
        PlaylistItem item = getItem(position);
        if (item instanceof Track) {
            Track track = (Track) item;
            if (!TextUtils.isEmpty(track.getTitle())) {
                text1.setText(track.getTitle());
                text2.setText(track.getArtist());
            } else {
                text1.setText(item.getName());
                text2.setText("");
            }
            icon.setVisibility(track.isCurrent() ? View.VISIBLE : View.GONE);
        } else {
            text1.setText(item.getName());
            text2.setText("");
            icon.setVisibility(View.GONE);
        }
        return view;
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

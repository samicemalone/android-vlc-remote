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

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.sax.Element;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.text.TextUtils;
import android.util.Xml;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BrowseActivity extends ListActivity implements
		View.OnClickListener, View.OnLongClickListener {

	public static final int RESULT_DOES_NOT_EXIST = 1;
	public static final int RESULT_CLOSED = 2;

	private FileAdapter mAdapter;

	private Button mButtonHome;
	private Button mButtonClose;

	@Override
	@SuppressWarnings("unchecked")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Window window = getWindow();
		window.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.browse);
		
		mAdapter = new FileAdapter(this);
		setListAdapter(mAdapter);
		ListView listView = getListView();
		registerForContextMenu(listView);
		mButtonHome = (Button) findViewById(R.id.browse_button_home);
		mButtonClose = (Button) findViewById(R.id.browse_button_close);
		mButtonHome.setOnClickListener(this);
		mButtonHome.setOnLongClickListener(this);
		mButtonClose.setOnClickListener(this);
		
		View titleView = findViewById(android.R.id.title);
		if (titleView instanceof TextView) {
			TextView title = (TextView) titleView;
			title.setEllipsize(TextUtils.TruncateAt.MIDDLE);
		}
		
		List<File> files = (List<File>) getLastNonConfigurationInstance();
		if (files != null) {
			setItems(files);
		} else {
			updateData();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		clearData();
		setIntent(intent);
		updateData();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		int count = mAdapter.getCount();
		if (count != 0) {
			return mAdapter.getItems();
		} else {
			return null;
		}
	}

	private void clearData() {
		mAdapter.clear();
	}

	private void updateData() {
		BrowseTask task = new BrowseTask();
		Intent intent = getIntent();
		Uri uri = intent.getData();
		task.execute(uri);
	}
	
	private boolean isDirectory(ContextMenuInfo menuInfo) {
		if (menuInfo instanceof AdapterContextMenuInfo) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
			File file = mAdapter.getItem(info.position);
			return file.isDirectory();
		} else {
			return false;
		}
	}

	private void openDirectory(String path) {
		Uri.Builder builder = getIntent().getData().buildUpon();

		// Replace the dir parameter
		builder.query("");
		builder.appendQueryParameter("dir", path);

		Uri data = builder.build();
		Intent intent = new Intent(this, BrowseActivity.class);
		intent.setData(data);
		startActivityForResult(intent, 0);
	}
	
	private Uri createStreamingVideoUri() {
	    Uri browse = getIntent().getData();
	    
	    String authority = browse.getAuthority();
	    int index = authority.lastIndexOf(':');
	    if (index != -1) {
	        authority = authority.substring(0, index);
	    }
	    
        Uri.Builder builder = browse.buildUpon();
        builder.scheme("rtsp");
        builder.encodedAuthority(authority + ":5554");
	    builder.path("stream.sdp");
	    builder.query("");
	    builder.fragment("");
	    
	    return builder.build();
	}
    
    private Uri createStreamingAudioUri() {
        Uri browse = getIntent().getData();
        
        String authority = browse.getAuthority();
        int index = authority.lastIndexOf(':');
        if (index != -1) {
            authority = authority.substring(0, index);
        }
        
        Uri.Builder builder = browse.buildUpon();
        builder.scheme("http");
        builder.encodedAuthority(authority + ":8000");
        builder.path("");
        builder.query("");
        builder.fragment("");
        
        return builder.build();
    }

	private void selectFile(String action, File file) {
        Intent result = new Intent(action);
        
		Uri.Builder builder = getIntent().getData().buildUpon();

		// Add the file parameter in addition to the dir parameter
		String path = file.getPath();
        String mrl = VLC.fileUri(path);
        if (VLC.ACTION_STREAM.equals(action)) {
            String mimeType = file.getMimeType();
            
            if (mimeType != null && mimeType.startsWith("audio/")) {
                mrl += " :sout=#transcode{acodec=vorb,ab=128}:standard{access=http,mux=ogg,dst=0.0.0.0:8000}";
                result.putExtra(VLC.EXTRA_STREAM_DATA, createStreamingAudioUri());
                result.putExtra(VLC.EXTRA_STREAM_TYPE, "application/ogg");
            } else {
                mrl += " :sout=#transcode{vcodec=mp4v,vb=384,acodec=mp4a,ab=64,channels=2,fps=25,venc=x264{profile=baseline,keyint=50,bframes=0,no-cabac,ref=1,vbv-maxrate=4096,vbv-bufsize=1024,aq-mode=0,no-mbtree,partitions=none,no-weightb,weightp=0,me=dia,subme=0,no-mixed-refs,no-8x8dct,trellis=0,level1.3},vfilter=canvas{width=320,height=180,aspect=320:180,padd},senc,soverlay}:rtp{sdp=rtsp://0.0.0.0:5554/stream.sdp,caching=4000}}";
                result.putExtra(VLC.EXTRA_STREAM_DATA, createStreamingVideoUri());
            }
		}
		builder.appendQueryParameter("mrl", mrl);
		Uri data = builder.build();
		result.setData(data);
		setResult(RESULT_OK, result);
		finish();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		File file = mAdapter.getItem(position);
		if (file.isDirectory()) {
			openDirectory(file.getPath());
		} else {
			selectFile(VLC.ACTION_PLAY, file);
		}
	}
	
	private String getHome() {
		SharedPreferences preferences = getSharedPreferences(VLC.PREFERENCES,
				MODE_PRIVATE);
		return preferences.getString(VLC.PREFERENCE_HOME_DIRECTORY, "~");
	}
	
	private void setHome(String home) {
		if (home == null) {
			throw new NullPointerException();
		}
		SharedPreferences preferences = getSharedPreferences(VLC.PREFERENCES,
				MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(VLC.PREFERENCE_HOME_DIRECTORY, home);
		editor.commit();
	}

	/** {@inheritDoc} */
	public void onClick(View v) {
		if (v == mButtonHome) {
			String home = getHome();
			openDirectory(home);
		} else if (v == mButtonClose) {
			setResult(RESULT_CLOSED);
			finish();
		}
	}
	
	/** {@inheritDoc} */
	public boolean onLongClick(View v) {
		if (v == mButtonHome) {
			Uri uri = getIntent().getData();
			String dir = uri.getQueryParameter("dir");
			setHome(dir);
			
			Context context = this;
			CharSequence text = getString(R.string.sethome, dir);
			Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
			toast.show();
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		Context context = this;
		MenuInflater inflater = new MenuInflater(context);
		inflater.inflate(R.menu.browse_context, menu);
		MenuItem openItem = menu.findItem(R.id.context_open);
		openItem.setVisible(isDirectory(menuInfo));
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ContextMenuInfo menuInfo = item.getMenuInfo();
		if (menuInfo instanceof AdapterContextMenuInfo) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
			File file = mAdapter.getItem(info.position);
			switch (item.getItemId()) {
			case R.id.context_open:
				openDirectory(file.getPath());
				return true;
			case R.id.context_play:
				selectFile(VLC.ACTION_PLAY, file);
				return true;
            case R.id.context_stream:
                selectFile(VLC.ACTION_STREAM, file);
                return true;
			case R.id.context_enqueue:
				selectFile(VLC.ACTION_ENQUEUE, file);
				return true;
			default:
				return super.onContextItemSelected(item);
			}
		} else {
			return super.onContextItemSelected(item);
		}
	}

	private void showError(CharSequence message) {
		Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
		toast.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_CANCELED) {
			setResult(resultCode, data);
			finish();			
		}
	}

	protected void setItems(List<File> result) {
		mAdapter.setItems(result);
		for (File file : result) {
			String name = file.getName();
			String path = file.getPath();
			if (name != null && path != null && name.equals("..")
					&& path.endsWith("..")) {
				int length = path.length();
				length -= "..".length();
				String title = path.substring(0, length);
				setTitle(title);
				break;
			}
		}
	}
	
	private static class FileAdapter extends ArrayAdapter<File> implements SectionIndexer {

		private Object[] mSections = new Object[0];
		private Integer[] mPositionForSection = new Integer[0];
		private Integer[] mSectionForPosition = new Integer[0];
		
		public FileAdapter(Context context) {
			super(context, R.layout.file_list_item, android.R.id.text1);
		}

		/** {@inheritDoc} */
		public int getPositionForSection(int section) {
			return mPositionForSection[section];
		}

		/** {@inheritDoc} */
		public int getSectionForPosition(int position) {
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
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = super.getView(position, convertView, parent);
			File file = getItem(position);
			ImageView icon = (ImageView) v.findViewById(android.R.id.icon);
			if (file.isDirectory()) {
				String name = file.getName();
				if ("..".equals(name)) {
					icon.setImageResource(R.drawable.ic_up);
				} else {
					icon.setImageResource(R.drawable.ic_directory);
				}
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
		
		public void setItems(List<File> items) {
			super.clear();
			int count = items.size();
			int capacity = 48; // Space for every letter and digit, plus a couple symbols
			Integer lastSection = null;
			List<String> sections = new ArrayList<String>(capacity);
			List<Integer> positionForSection = new ArrayList<Integer>(capacity);
			List<Integer> sectionForPosition = new ArrayList<Integer>(count);
			
			for (int position = 0; position < count; position++) {
				File file = items.get(position);
				String section = getSection(file);
				if (!sections.contains(section)) {
					sections.add(section);
					positionForSection.add(Integer.valueOf(position));
					lastSection = Integer.valueOf(position);
				}
				sectionForPosition.add(lastSection);
				super.add(file);
			}
			
			mSections = sections.toArray();
			
			mPositionForSection = new Integer[positionForSection.size()];
			positionForSection.toArray(mPositionForSection);
			
			mSectionForPosition = new Integer[sectionForPosition.size()];
			sectionForPosition.toArray(mSectionForPosition);
		}
		
		public List<File> getItems() {
			int count = getCount();
			List<File> items = new ArrayList<File>(count);
			for (int position = 0; position < count; position++) {
				File item = getItem(position);
				items.add(item);
			}
			return items;
		}
	}

	private class BrowseTask extends AsyncTask<Uri, Integer, Directory> {

		private final Directory mDirectory = new Directory();
		private Throwable mError;

		@Override
		protected void onPreExecute() {
			Window window = getWindow();
			window.setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
					Window.PROGRESS_VISIBILITY_ON);
		}

		private File createFile(Attributes attributes) {
			String type = attributes.getValue("", "type");
			String sizeString = attributes.getValue("", "size");
			Long size = null;
			try {
				if (sizeString != null && !sizeString.equals("unknown")) {
					size = Long.parseLong(sizeString);
				}
			} catch (NumberFormatException e) {
				// Ignore unexpected value
			}
			String date = attributes.getValue("", "date");
			String path = attributes.getValue("", "path");
			String name = attributes.getValue("", "name");
			String extension = attributes.getValue("", "extension");
			if (path != null && !path.startsWith("/")) { // Windows path
				// Work-around: Replace front-slash appended by server with back-slash.
				path = path.replace('/', '\\');
			}
			return new File(type, size, date, path, name, extension);
		}

		@Override
		protected Directory doInBackground(Uri... params) {
			Uri uri = params[0];
			try {
				String uriString = uri.toString();
				URL url = new URL(uriString);
				InputStream in = url.openStream();
				try {
					RootElement root = new RootElement("", "root");
					Element element = root.getChild("", "element");
					element.setStartElementListener(new StartElementListener() {
						/** {@inheritDoc} */
						public void start(Attributes attributes) {
							File file = createFile(attributes);
							mDirectory.add(file);
						}
					});
					ContentHandler contentHandler = root.getContentHandler();
					Xml.parse(in, Xml.Encoding.UTF_8, contentHandler);
					return mDirectory;
				} finally {
					in.close();
				}
			} catch (IOException e) {
				mError = e;
			} catch (SAXException e) {
				mError = e;
			} catch (RuntimeException e) {
				mError = e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Directory result) {
			Window window = getWindow();
			window.setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
					Window.PROGRESS_VISIBILITY_OFF);
			clearData();
			if (mError == null) {
				if (result.isEmpty()) {
					// If the directory does not exist, the response will be
					// totally empty (i.e., no ".." for parent directory).
					setResult(RESULT_DOES_NOT_EXIST);
					finish();
				} else {
					setItems(result);
				}
			} else {
				CharSequence message = String.valueOf(mError);
				showError(message);
			}
		}
	}
}

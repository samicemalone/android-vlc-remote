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
import org.xml.sax.helpers.DefaultHandler;

import android.text.Html;

class PlaylistResponseHandler extends DefaultHandler implements
		XmlResponseHandler<Playlist> {

	private static final String[] TEXT_ELEMENTS = { "title", "artist", "genre",
			"copyright", "album", "track", "description", "rating", "date",
			"url", "language", "now_playing", "publisher", "encoded_by",
			"art_url", "track_id" };
	private final Playlist mPlaylist = new Playlist();
	private final StringBuilder mBuilder = new StringBuilder();
	private boolean mCapture = false;
	private Track mTrack;

	@Override
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		super.startElement(uri, localName, name, attributes);
		if ("leaf".equals(localName)
				&& !"vlc://nop".equals(attributes.getValue("uri"))) {
			mTrack = createTrack(attributes);
		} else if (mTrack != null && isTextElement(localName)) {
			mBuilder.setLength(0);
			mCapture = true;
		}
	}

	private static String unescape(CharSequence text) {
		// TODO: Do this more efficiently
		return Html.fromHtml(text.toString()).toString();
	}

	private String getText() {
		if (mBuilder.length() == 0) {
			return null;
		} else {
			if (mBuilder.indexOf("&") != -1) {
				// Text is escaped twice so that it can be used in HTML.
				return unescape(mBuilder);
			} else {
				return mBuilder.toString();
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {
		super.endElement(uri, localName, name);
		if (mTrack != null) {
			if ("leaf".equals(localName)) {
				mPlaylist.add(mTrack);
				mTrack = null;
			} else if ("title".equals(localName)) {
				mTrack.setTitle(getText());
			} else if ("artist".equals(localName)) {
				mTrack.setArtist(getText());
			} else if ("genre".equals(localName)) {
				mTrack.setGenre(getText());
			} else if ("copyright".equals(localName)) {
				mTrack.setCopyright(getText());
			} else if ("album".equals(localName)) {
				mTrack.setAlbum(getText());
			} else if ("track".equals(localName)) {
				mTrack.setTrack(getText());
			} else if ("description".equals(localName)) {
				mTrack.setDescription(getText());
			} else if ("rating".equals(localName)) {
				mTrack.setRating(getText());
			} else if ("date".equals(localName)) {
				mTrack.setDate(getText());
			} else if ("url".equals(localName)) {
				mTrack.setUrl(getText());
			} else if ("language".equals(localName)) {
				mTrack.setLanguage(getText());
			} else if ("now_playing".equals(localName)) {
				mTrack.setNowPlaying(getText());
			} else if ("publisher".equals(localName)) {
				mTrack.setPublisher(getText());
			} else if ("encoded_by".equals(localName)) {
				mTrack.setEncodedBy(getText());
			} else if ("art_url".equals(localName)) {
				mTrack.setArtUrl(getText());
			} else if ("track_id".equals(localName)) {
				mTrack.setTrackId(getText());
			}
		}
		mCapture = false;
	}

	private static boolean isTextElement(String localName) {
		for (int i = 0; i < TEXT_ELEMENTS.length; i++) {
			if (TEXT_ELEMENTS[i].equals(localName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		super.characters(ch, start, length);
		if (mCapture) {
			mBuilder.append(ch, start, length);
		}
	}

	private static Track createTrack(Attributes attributes) {
		Track track = new Track();

		int id = Integer.parseInt(attributes.getValue("", "id"));
		track.setId(id);

		boolean current = "current".equals(attributes.getValue("", "current"));
		track.setCurrent(current);

		String uri = attributes.getValue("", "uri");
		track.setUri(uri);

		String name = attributes.getValue("", "name");
		track.setName(name);

		long duration = Long.parseLong(attributes.getValue("", "duration"));
		track.setDuration(duration);

		return track;
	}

	/** {@inheritDoc} */
	public ContentHandler getContentHandler() {
		return this;
	}

	/** {@inheritDoc} */
	public Playlist getResponse() {
		return mPlaylist;
	}
}
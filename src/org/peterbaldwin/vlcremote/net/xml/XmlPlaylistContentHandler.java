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

package org.peterbaldwin.vlcremote.net.xml;

import android.net.Uri;
import android.text.Html;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Stack;
import org.peterbaldwin.vlcremote.model.Playlist;
import org.peterbaldwin.vlcremote.model.Track;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public final class XmlPlaylistContentHandler extends XmlContentHandler<Playlist> implements ContentHandler {

    private static final String[] TEXT_ELEMENTS = {
            "title", "artist", "genre", "copyright", "album", "track", "description", "rating",
            "date", "url", "language", "now_playing", "publisher", "encoded_by", "art_url",
            "track_id"
    };

    private static final Playlist EMPTY_PLAYLIST = new Playlist(1, "Undefined");

    private final Stack<Playlist> mNodeStack;

    private final StringBuilder mBuilder;

    private Playlist mRoot;

    private boolean mCapture = false;

    private Track mTrack;

    public XmlPlaylistContentHandler() {
        mNodeStack = new Stack<Playlist>();
        mBuilder = new StringBuilder();
    }

    /** {@inheritDoc} */
    public void startElement(String uri, String localName, String name, Attributes attributes)
            throws SAXException {
        if ("leaf".equals(localName)) {
            mTrack = createTrack(attributes);
            if ("vlc://nop".equals(attributes.getValue("uri"))) {
                // Don't include nop tracks in the output
            } else {
                mRoot.add(mTrack);
            }
        } else if ("node".equals(localName)) {
            Playlist playlist = createPlaylist(attributes);
            if (mNodeStack.isEmpty()) {
                mRoot = playlist;
            }
            mNodeStack.push(playlist);
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

    /** {@inheritDoc} */
    public void endElement(String uri, String localName, String name) throws SAXException {
        if ("node".equals(localName)) {
            mNodeStack.pop();
        } else if ("leaf".equals(localName)) {
            mTrack = null;
        } else if (mTrack != null) {
            if ("title".equals(localName)) {
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

    /** {@inheritDoc} */
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (mCapture) {
            mBuilder.append(ch, start, length);
        }
    }

    /** {@inheritDoc} */
    public void endDocument() throws SAXException {

    }

    /** {@inheritDoc} */
    public void endPrefixMapping(String prefix) throws SAXException {
    }

    /** {@inheritDoc} */
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    }

    /** {@inheritDoc} */
    public void processingInstruction(String target, String data) throws SAXException {
    }

    /** {@inheritDoc} */
    public void setDocumentLocator(Locator locator) {
    }

    /** {@inheritDoc} */
    public void skippedEntity(String name) throws SAXException {
    }

    /** {@inheritDoc} */
    public void startDocument() throws SAXException {
    }

    /** {@inheritDoc} */
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    private static Playlist createPlaylist(Attributes attributes) {
        int id = Integer.parseInt(attributes.getValue("", "id"));
        String name = attributes.getValue("", "name");
        return new Playlist(id, name);
    }

    private static Track createTrack(Attributes attributes) {
        Track track = new Track();

        int id = Integer.parseInt(attributes.getValue("", "id"));
        track.setId(id);

        boolean current = "current".equals(attributes.getValue("", "current"));
        track.setCurrent(current);

        String uri = attributes.getValue("", "uri");
        track.setUri(Uri.decode(uri));

        String name = attributes.getValue("", "name");
        track.setName(name);

        long duration = Long.parseLong(attributes.getValue("", "duration"));
        track.setDuration(duration);

        return track;
    }

    @Override
    public Object getContent(URLConnection connection) throws IOException {
        parse(connection, this);
        return (mRoot != null) ? mRoot : EMPTY_PLAYLIST;
    }
}

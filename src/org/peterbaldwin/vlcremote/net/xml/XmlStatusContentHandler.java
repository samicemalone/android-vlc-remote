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

import org.peterbaldwin.vlcremote.model.Status;
import org.peterbaldwin.vlcremote.model.Track;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

import android.sax.Element;
import android.sax.ElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.TextElementListener;
import android.text.Html;

import java.io.IOException;
import java.net.URLConnection;

public final class XmlStatusContentHandler extends XmlContentHandler<Status> {
    private static String unescape(String text) {
        // The response text is escaped twice so that it can be used in HTML.
        if (text.indexOf("&") != -1) {
            // TODO: Use more efficient unescaping
            return Html.fromHtml(text.toString()).toString();
        } else {
            return text;
        }
    }

    private static boolean parseBoolean(String text) {
        try {
            // Booleans are represented as integers in VLC 1.0
            return Integer.parseInt(text) != 0;
        } catch (NumberFormatException e) {
            // Booleans are represented as strings in VLC 1.1
            return Boolean.parseBoolean(text);
        }
    }

    private Status mStatus = new Status();

    private Track mTrack = mStatus.getTrack();

    private String mCategoryName;

    private String mInfoName;

    public ContentHandler getContentHandler() {
        RootElement root = new RootElement("", "root");
        root.getChild("", "volume").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                int volume = Integer.parseInt(body);
                mStatus.setVolume(volume);
            }
        });
        root.getChild("", "length").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                int length = Integer.parseInt(body);
                mStatus.setLength(length);
            }
        });
        root.getChild("", "time").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                int time = Integer.parseInt(body);
                mStatus.setTime(time);
            }
        });
        root.getChild("", "state").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                mStatus.setState(body);
            }
        });
        root.getChild("", "position").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                double position = Double.parseDouble(body);
                mStatus.setPosition(position);
            }
        });

        root.getChild("", "fullscreen").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                boolean fullscreen = parseBoolean(body);
                mStatus.setFullscreen(fullscreen);
            }
        });
        root.getChild("", "random").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                boolean random = parseBoolean(body);
                mStatus.setRandom(random);
            }
        });
        root.getChild("", "loop").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                boolean loop = parseBoolean(body);
                mStatus.setLoop(loop);
            }
        });
        root.getChild("", "repeat").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                boolean repeat = parseBoolean(body);
                mStatus.setRepeat(repeat);
            }
        });
        Element information = root.getChild("", "information");
        Element meta = information.getChild("", "meta-information");
        meta.getChild("", "title").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                mTrack.setTitle(unescape(body));
            }
        });
        meta.getChild("", "artist").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                mTrack.setArtist(unescape(body));
            }
        });
        meta.getChild("", "genre").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                mTrack.setGenre(unescape(body));
            }
        });
        meta.getChild("", "copyright").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                mTrack.setCopyright(unescape(body));
            }
        });
        meta.getChild("", "album").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                mTrack.setAlbum(unescape(body));
            }
        });
        meta.getChild("", "track").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                mTrack.setTrack(unescape(body));
            }
        });
        meta.getChild("", "description").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                mTrack.setDescription(unescape(body));
            }
        });
        meta.getChild("", "rating").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                mTrack.setRating(unescape(body));
            }
        });
        meta.getChild("", "date").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                mTrack.setDate(unescape(body));
            }
        });
        meta.getChild("", "url").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                mTrack.setUrl(unescape(body));
            }
        });
        meta.getChild("", "language").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                mTrack.setLanguage(unescape(body));
            }
        });
        meta.getChild("", "now_playing").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                mTrack.setNowPlaying(unescape(body));
            }
        });
        meta.getChild("", "publisher").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                mTrack.setPublisher(unescape(body));
            }
        });
        meta.getChild("", "encoded_by").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                mTrack.setEncodedBy(unescape(body));
            }
        });
        meta.getChild("", "art_url").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                mTrack.setArtUrl(unescape(body));
            }
        });
        meta.getChild("", "track_id").setEndTextElementListener(new EndTextElementListener() {
            /** {@inheritDoc} */
            public void end(String body) {
                mTrack.setTrackId(unescape(body));
            }
        });

        // VLC 1.1
        Element category = information.getChild("", "category");
        category.setElementListener(new ElementListener() {
            /** {@inheritDoc} */
            public void start(Attributes attributes) {
                mCategoryName = attributes.getValue("", "name");
            }

            /** {@inheritDoc} */
            public void end() {
                mCategoryName = null;
            }
        });
        Element info = category.getChild("", "info");
        info.setTextElementListener(new TextElementListener() {
            /** {@inheritDoc} */
            public void start(Attributes attributes) {
                mInfoName = attributes.getValue("", "name");
            }

            /** {@inheritDoc} */
            public void end(String body) {
                if ("meta".equalsIgnoreCase(mCategoryName)) {
                    if ("artist".equalsIgnoreCase(mInfoName)) {
                        mTrack.setArtist(unescape(body));
                    } else if ("title".equalsIgnoreCase(mInfoName)) {
                        mTrack.setTitle(unescape(body));
                    } else if ("album".equalsIgnoreCase(mInfoName)) {
                        mTrack.setAlbum(unescape(body));
                    } else if ("genre".equalsIgnoreCase(mInfoName)) {
                        mTrack.setGenre(unescape(body));
                    } else if ("description".equalsIgnoreCase(mInfoName)) {
                        mTrack.setDescription(unescape(body));
                    } else if ("filename".equalsIgnoreCase(mInfoName)) {
                        mTrack.setName(unescape(body));
                    } else if ("artwork_url".equalsIgnoreCase(mInfoName)) {
                        mTrack.setArtUrl(unescape(body));
                    }
                }
                if (mCategoryName != null && mCategoryName.startsWith("Stream")) {
                    if ("Type".equalsIgnoreCase(mInfoName)) {
                        mTrack.addStreamType(unescape(body));
                    }
                }
                mInfoName = null;
            }
        });
        return root.getContentHandler();
    }

    @Override
    public Object getContent(URLConnection connection) throws IOException {
        parse(connection, getContentHandler());
        return mStatus;
    }
}

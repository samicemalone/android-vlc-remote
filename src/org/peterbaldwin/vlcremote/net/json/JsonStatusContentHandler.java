/*
 * Copyright (C) 2014 Sam Malone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.peterbaldwin.vlcremote.net.json;

import android.util.JsonReader;
import android.util.JsonToken;
import java.io.IOException;
import org.peterbaldwin.vlcremote.model.Status;

/**
 *
 * @author Sam Malone
 */
public class JsonStatusContentHandler extends JsonContentHandler<Status> {

    private final Status mStatus = new Status();
    
    private JsonReader reader;
    
    @Override
    public Object parse(JsonReader reader) throws IOException {
        this.reader = reader;
        reader.beginObject();
        while(reader.hasNext()) {
            parseName(reader.nextName());
        }
        reader.endObject();
        return mStatus;
    }
    
    private boolean parseBoolean(JsonToken token) throws IOException {
        if(JsonToken.NUMBER.equals(token)) {
            return reader.nextInt() == 0;
        }
        return reader.nextBoolean();
    }
    
    
    private void parseName(String name) throws IOException {
        if("fullscreen".equals(name)) {
            mStatus.setFullscreen(parseBoolean(reader.peek()));
        } else if("information".equals(name)) {
            parseInformation();
        } else if("time".equals(name)) {
            mStatus.setTime(reader.nextInt());
        } else if("volume".equals(name)) {
            mStatus.setVolume(reader.nextInt());
        } else if("length".equals(name)) {
            mStatus.setLength(reader.nextInt());
        } else if("random".equals(name)) {
            mStatus.setRandom(parseBoolean(reader.peek()));
        } else if("state".equals(name)) {
            mStatus.setState(reader.nextString());
        } else if("loop".equals(name)) {
            mStatus.setLoop(parseBoolean(reader.peek()));
        } else if("position".equals(name)) {
            mStatus.setPosition(reader.nextDouble());
        } else if("repeat".equals(name)) {
            mStatus.setRepeat(parseBoolean(reader.peek()));
        } else {
            reader.skipValue();
        }
    }
    
    private void parseCategory() throws IOException {
        reader.beginObject();
        while(reader.hasNext()) {
            String name = reader.nextName();
            if("meta".equals(name)) {
                parseMeta();
            } else if(name.startsWith("Stream ")) {
                parseStream();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }
    
    private void parseInformation() throws IOException {
        reader.beginObject();
        while(reader.hasNext()) {
            String name = reader.nextName();
            if("category".equals(name)) {
                parseCategory();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }
    
    private void parseMeta() throws IOException {
        reader.beginObject();
        while(reader.hasNext()) {
            String name = reader.nextName();
            if("filename".equals(name)) {
                mStatus.getTrack().setName(reader.nextString());
            } else if("title".equals(name)) {
                mStatus.getTrack().setTitle(reader.nextString());
            } else if("description".equals(name)) {
                mStatus.getTrack().setDescription(reader.nextString());
            } else if("artwork_url".equals(name)) {
                mStatus.getTrack().setArtUrl(reader.nextString());
            } else if("artist".equals(name)) {
                mStatus.getTrack().setArtist(reader.nextString());
            } else if("genre".equals(name)) {
                mStatus.getTrack().setGenre(reader.nextString());
            } else if("album".equals(name)) {
                mStatus.getTrack().setAlbum(reader.nextString());
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }
    
    private void parseStream() throws IOException {
        reader.beginObject();
        while(reader.hasNext()) {
            if("Type".equals(reader.nextName())) {
                mStatus.getTrack().addStreamType(reader.nextString());
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }
    
}

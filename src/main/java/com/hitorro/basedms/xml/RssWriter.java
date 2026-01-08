/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.basedms.xml;

import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.base.objects.ExternalContent;
import com.hitorro.conversationtracker.Log;
import com.hitorro.conversationtracker.rss.RssIteratorGenerator;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

/**
 * <p/>
 */
public class RssWriter extends XWriter {
    public RssWriter(JsonNode params) {
        super(params);
    }

    public boolean requiresFlush() {
        return true;
    }

    public void generateFeed(Writer writerx, Iterator<com.hitorro.basedms.xml.RssItemInterface> items, int maxItems, int window)
            throws IOException {
        int counter = 0;
        _writer = writerx;
        _depth = 0;
        int maxWords = RssIteratorGenerator.TopWordsKey.apply(params);
        _writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        _writer.write(Fmt.S("<!-- generator=\"%s\" -->", getRssGenerator()));
        writeTag("rss", false, "version", "2.0",
                "xmlns:content", "http://purl.org/rss/1.0/modules/content/",
                "xmlns:wfw", "http://wellformedweb.org/CommentAPI/",
                "xmlns:dc", "http://purl.org/dc/elements/1.1/",
                "xmlns:itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd",
                "xmlns:dtvmedia", "http://participatoryculture.org/RSSModules/dtv/1.0",
                "xmlns:media", "http://search.yahoo.com/mrss"
        );

        writeTag("channel", false);
        writeTag("title", false);
        _writer.write(StringUtil.encodeForXML(getFeedName()));
        closeTag("title");
        writeTag("link", true);
        writeTag("description", true);
        writeTag("pubDate", true);
        writeTag("generator", true);
        writeTag("language", true);
        writeTag("copyright", true);
        writeTag("managingEditor", true);
        writeTag("webMaster", true);
        writeTag("category", true);
        if (isShowItunes()) {
            for (String cat : getITunesCategories()) {
                writeTag("itunes:category", true, "text", cat);
            }
            writeTag("itunes:subtitle", true);
            writeTag("itunes:summary", true);
            writeTag("itunes:author", true);
            writeTag("itunes:owner", true);
            writeTag("itunes:explicit", true);
            writeTag("image", true);
            writeTag("itunes:image", true);
        }

        // we want dates written out in the format: Wed, 20 Jun 2007 22:03:43 +0000
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");

        if (window > 0) {

            int m = (window * maxItems);
            while (items.hasNext() && counter < m) {
                com.hitorro.basedms.xml.RssItemInterface pi = items.next();
                String guid = pi.getGuid();

                // prevent dupes in stream.
                String guidDupe = dupeMap.get(guid);
                if (guidDupe != null) {
                    Log.conversationtracker.debug("Dupe id in feed: (%s)", guid);
                    continue;
                }
                counter++;

                dupeMap.put(guid, guid);

                if (pi == null) {
                    continue;
                }
            }
            maxItems = maxItems + (window * maxItems);
        }

        while (items.hasNext() && counter < maxItems) {
            com.hitorro.basedms.xml.RssItemInterface pi = items.next();
            String guid = pi.getGuid();

            // prevent dupes in stream.
            String guidDupe = dupeMap.get(guid);
            if (guidDupe != null) {
                Log.conversationtracker.debug("Dupe id in feed: (%s)", guid);
                continue;
            }
            counter++;

            dupeMap.put(guid, guid);

            if (pi == null) {
                continue;
            }
            writeTag("item", false);

            String imageUrl = pi.getImageUrl();
            if (imageUrl == null) {
                imageUrl = "";
            }
            String title = pi.getTitle();
            if (title == null) {
                title = "";
            }
            String titleline = null;
            if (debug) {
                String name = pi.getFeedContactAuthorDisplayName();
                titleline = Fmt.S("<title imageURL=\"%s\">%s (Shared by: %s)</title>\n", StringUtil.encodeForXML(imageUrl),
                        StringUtil.encodeForXML(title), name);
            } else {
                titleline = Fmt.S("<title imageURL=\"%s\">%s</title>\n", StringUtil.encodeForXML(imageUrl),
                        StringUtil.encodeForXML(title));
            }

            _writer.write(titleline);
            writeTag("link", false);
            String link = pi.getLink();
            if (link == null) {
                link = "";
            }
            _writer.write(StringUtil.encodeUrl(link));
            closeTag("link");
            writeTag("comments", false);
            String comments = StringUtil.getValueDefault(pi.getComments(), "");

            _writer.write(comments);
            closeTag("comments");
            writeTag("pubDate", false);
            _writer.write(dateFormat.format(pi.getPostDate()));
            closeTag("pubDate");
            writeTag("dc:creator", false);

            String user = pi.getAuthorDisplayName();
            if (user == null) {
                user = "";
            }
            _writer.write(StringUtil.encodeForXML(user.trim()));

            closeTag("dc:creator");
            List<String> tws = pi.getTopWords();
            int words = Math.min(maxWords, tws.size());
            for (int i = 0; i < words; i++) {
                String word = tws.get(i);
                writeTag("dc:topword", false);
                _writer.write(StringUtil.encodeForXML(word));
                closeTag("dc:topword");
            }
            // categories.
            for (String cat : pi.getCategoriesList()) {
                writeTag("dc:category", false);
                _writer.write(StringUtil.encodeForXML(cat));
                closeTag("dc:category");
            }


            writeTag("dc:type", pi.getType(), false);
            writeTag("dc:tld", pi.getTLD(), true);
            writeTag("dc:altQuery", pi.getAlternateQuery(), true);
            if (pi.hasAd()) {
                writeTag("ad", true,
                        "url", StringUtil.encodeForXML(pi.getAdUrl()), "text", pi.getAdText());
                writeTag("sponsored", true, "text", pi.getSponsored());
            }

            writeTag("guid", false);
            _writer.write(pi.getGuid());
            closeTag("guid");
            writeTag("description", false);
            _writer.write("<![CDATA[");
            String desc = StringUtil.getValueDefault(pi.getDescription(), "");
            desc = StringUtil.cutToLength(desc, 5 * 1024);
            //desc = StringUtil.encodeForXML(desc);
            _writer.write(desc);
            _writer.write("]]>");
            closeTag("description");
            writeTag("content:encoded", false);
            _writer.write("<![CDATA[");
            _writer.write(desc);
            _writer.write("]]>");
            closeTag("content:encoded");
            writeTag("wfw:commentRSS", true);
            if (pi.getEnclosures() != null) {
                List<ExternalContent> encs = pi.getEnclosures();
                for (ExternalContent ec : encs) {
                    if (ec.getUrl() != null && ec.getType() != null) {
                        writeTag("enclosure", true,
                                "url", StringUtil.encodeForXML(ec.getUrl()),
                                "type", ec.getType(),
                                "length", Long.toString(ec.getPlayLength()));
                    }
                }
            }

            String clicktext = StringUtil.encodeForXML(pi.getClickThroughText());
            String clickUrl = StringUtil.encodeForXML(pi.getClickThroughUrl());
            if (clicktext != null && clickUrl != null) {
                writeTag("clickthrough", false, "url", clickUrl);
                _writer.write(clicktext);
                closeTag("clickthrough");
            }
            closeTag("item");
        }
        closeTag("channel");
        closeTag("rss");
    }

    private void writeTag(String tag, String content, boolean encode) throws IOException {
        writeTag(tag, false);
        if (encode) {
            _writer.write(StringUtil.encodeForXML(content));
        } else {
            _writer.write(content);
        }
        closeTag(tag);
    }

    private void writeTag(String tag, boolean closed, String... attributes)
            throws IOException {

        _writer.write("<");
        _writer.write(tag);
        int nAttr = attributes.length / 2;
        for (int ii = 0; ii < nAttr; ii++) {
            _writer.write(" ");
            _writer.write(attributes[ii * 2]);
            _writer.write("=\"");
            _writer.write(attributes[ii * 2 + 1]);
            _writer.write("\"");
        }
        if (closed) {
            _writer.write("/>");
        } else {
            _writer.write(">");
            _depth++;
        }
    }

    private void closeTag(String tag)
            throws IOException {
        _depth--;
        _writer.write("</");
        _writer.write(tag);
        _writer.write(">\n");
    }

    public boolean isShowItunes() {
        return showItunes;
    }

    public void setShowItunes(boolean showItunes) {
        this.showItunes = showItunes;
    }

    public List<String> getITunesCategories() {
        return iTunesCategories;
    }

    public void setITunesCategories(List<String> iTunesCategories) {
        this.iTunesCategories = iTunesCategories;
    }

    public String getRssGenerator() {
        return rssGenerator;
    }

    public void setRssGenerator(String rssGenerator) {
        this.rssGenerator = rssGenerator;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }
}

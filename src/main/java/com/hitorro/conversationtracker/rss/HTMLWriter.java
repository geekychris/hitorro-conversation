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
package com.hitorro.conversationtracker.rss;

import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.base.objects.ExternalContent;
import com.hitorro.base.objects.ObjectFetcher;
import com.hitorro.basedms.ObjectFetcherRPC;
import com.hitorro.basedms.xml.RssItemInterface;
import com.hitorro.basedms.xml.XWriter;
import com.hitorro.conversationtracker.Log;
import com.hitorro.network.rpc.RPCMessage;
import com.hitorro.network.rpc.RPCService;
import com.hitorro.util.core.Console;
import com.hitorro.util.core.ListUtil;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.HTSerializable;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <p/>
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Sep 28, 2005 Time: 3:15:51 PM
 */
public class HTMLWriter extends XWriter {
    private static final String flash = "<embed jwcid=\"@Any\" type=\"application/x-shockwave-flash\" src=\"http://www.HiTorro.net/player/HiTorro-player.swf?bc=3F34K2L1\"" +
            "id=\"prueba\" name=\"prueba\" bgcolor=\"#FFFFFF\" quality=\"high\" menu=\"false\" flashvars=\"%s\" height=\"269\" width=\"320\" allowScriptAccess=\"always\" />";
    private boolean showItunes = false;
    private List<String> iTunesCategories = new ArrayList<String>();
    private Writer _writer;
    private int _depth;
    private boolean debug = false;
    private Map<String, String> dupeMap = new HashMap<String, String>();

    public HTMLWriter(JsonNode params) {
        super(params);
    }

    public void setDebug(boolean flag) {
        debug = flag;
    }

    private void writeBold(String text) throws IOException {
        writeTag("b", false);
        _writer.write(text);
        closeTag("b");
    }

    private void write(String str, String... args) throws IOException {
        _writer.write(Fmt.Sargs(str, args));
    }

    public String getFlashvars(ExternalContent enc) {
        String content = enc.getUrl();
        String time = new Date().toString();
        String title = "This title";
        String excerpt = "this excerpt";
        String postUrl = "http://HiTorro.net";
        String permaLink = postUrl;
        String adText = "Visit HiTorro.net";
        String adUrl = "http://www.HiTorro.net";
        String imageUrl = "http://www.HiTorro.net/player/mp3-screen.jpg";

        String breadCrumb = "3F34K2L1";

        //return Fmt.S("content=%s&totalTime=%s&title=%s&excerpt=%s&postURL=%s&adText=%s&adURL=%s&imageURL=%s&permalink=%s&breadcrumb=%s&autoplay=false&bufferTime=5&wmode=opaque",
        //             content, time, title, excerpt, postUrl, adText, adUrl, imageUrl, permaLink, breadCrumb);

        return Fmt.S("content=%s&totalTime=%s&title=%s&excerpt=%s&postURL=%s&adText=%s&adURL=%s&mp3ImageSrc=%s&permalink=%s&breadcrumb=%s&autoplay=false&bufferTime=5&wmode=opaque",
                content, time, title, excerpt, postUrl, adText, adUrl, imageUrl, permaLink, breadCrumb);
    }

    public String getEnc(ExternalContent enc) {
        return Fmt.S(flash, getFlashvars(enc));
    }

    private void test(List<String> guids) {
        ObjectFetcher of = new ObjectFetcher();
        of.setGuid(guids);
        of.setAdapterMethod(RssFeedGenerator.RssItemCacheName);

        RPCMessage message = RPCService.s_service.getMessageToSend("localhost",
                6050,
                ObjectFetcherRPC.ObjectFetchRPCKey,
                true);

        message.setPayload(of);
        RPCMessage resp = null;
        try {
            resp = message.sendSyncMessage();
        } catch (StoreException e) {
            com.hitorro.util.core.Log.util.error("Exception %s %e", e, e);
        } catch (ClassNotFoundException e) {
            com.hitorro.util.core.Log.util.error("Exception %s %e", e, e);
        } catch (IOException e) {
            Log.util.error("Exception %s %e", e, e);
        }
        if (resp != null) {
            of = (ObjectFetcher) resp.getPayload();
            if (of != null) {
                for (HTSerializable pts : of.getResults()) {
                    Console.println("%s", pts);
                }
            }
        }
    }

    public void generateFeed(Writer writerx, Iterator<RssItemInterface> items, int maxItems, int window)
            throws IOException {
        int counter = 0;
        _writer = writerx;
        _depth = 0;
        //List<String> guids = new ArrayList<String>();
        write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
        write("<html>\n");
        write("<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n");
        write("<body>\n");
        write("<title>%s</title>\n", this.feedName);

        // we want dates written out in the format: Wed, 20 Jun 2007 22:03:43 +0000
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");

        boolean full = RssIteratorGenerator.FullKey.apply(params);

        if (window > 0) {

            int m = (window * maxItems);
            while (items.hasNext() && counter < m) {
                RssItemInterface pi = items.next();
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
            RssItemInterface pi = items.next();
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

            _writer.write("<hr/>\n");

            writeBold("Guid");
            _writer.write(guid);
            //guids.put(guid);

            String maxPostDays = "180";

            String enc = StringUtil.encodeForXML(pi.getAuthorDisplayName());
            write("<br>Author: <A HREF=\"rssquery?html=true&&algorithm=text&maxpostage=%s&author=%s\">%s</a><br>\n", maxPostDays, enc, enc);
            write("Feed: (%s)<br>\n", StringUtil.encodeForXML(pi.getFeedContactAuthorDisplayName()));
            write("Degree: (%s)<br>\n", Integer.toString(pi.getDegree()));
            write("Post Date: (%s)<br>\n", pi.getPostDate().toString());
            if (!full) {
                write("<A HREF=\"rssquery?algorithm=show&guid=%s&full=true&html=true&topwords=10\">", guid);
            } else {
                write("<A HREF=\"%s\">\n", StringUtil.encodeForXML(pi.getLink()));
            }
            write("<h1>%s</h1>\n", StringUtil.encodeForXML(pi.getTitle()));
            /*write("<br><A HREF=\"rssquery?html=true&&algorithm=text&maxpostage=%s&author=%s\"><h1>%s</h1>\n</a><br>\n",
                  maxPostDays,
                  StringUtil.encodeForXML(pi.getLink()),
                  StringUtil.encodeForXML(pi.getTitle())); */
            write("</A>");

            if (full) {
                multiQuery("<br><b>Categories: </b>", "<A HREF=\"rssquery?algorithm=text&category=%s&html=true&maxpostage=%s\">%s</A> ", pi.getCategoriesList(), maxPostDays);
                write("Type: (%s)<br>\n", StringUtil.encodeForXML(pi.getType()));
                write("TLD: (%s)<br>\n", StringUtil.encodeForXML(pi.getTLD()));
                write("Alt Query: (%s)<br>\n", StringUtil.encodeForXML(pi.getAlternateQuery()));
                write("%s", pi.getDescription());
                write("<br><A HREF=\"rssquery?algorithm=similar&guid=%s&html=true&maxpostage=%s\">similar</A>", guid, maxPostDays);
                write("<br><A HREF=\"rssquery?algorithm=referencedby&guid=%s&html=true\">referrers</A>", guid);
                multiQuery("<br><b>Top Words: </b>", "<A HREF=\"rssquery?algorithm=text&body=%s&html=true&maxpostage=%s\">%s</A> ", pi.getTopWords(), maxPostDays);
                List<ExternalContent> encs = pi.getEnclosures();
                if (encs != null) {
                    for (ExternalContent c : encs) {
                        write(getEnc(c));
                    }
                }

            }
        }

        _writer.write("</body>\n");
        _writer.write("</head>\n");
        _writer.write("</html>\n");
        //test(guids);
    }

    private void multiQuery(String title, String query, List<String> l, String maxPostDays) throws IOException {
        if (!ListUtil.nullOrEmpty(l)) {
            String toks = StringUtil.mergeWithJoinToken(l, " ");
            write(title);
            for (String tok : l) {
                write(query, tok, maxPostDays, tok);
            }
            write("<br>");
        }
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

    public String getContentType() {
        return com.hitorro.basedms.Constants.HTMLMimeType;
    }
}

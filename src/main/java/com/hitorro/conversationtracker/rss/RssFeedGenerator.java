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
import com.hitorro.base.typesystem.btadapter.DBBaseAdapterItemCacheFetcher;
import com.hitorro.base.typesystem.btadapter.rssadapters.VersionableObjectRssItem;
import com.hitorro.basedms.RssItemCacheProxy;
import com.hitorro.basedms.xml.RssItemInterface;
import com.hitorro.basedms.xml.RssWriter;
import com.hitorro.basedms.xml.XWriter;
import com.hitorro.conversationtracker.Log;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.JVSUtils;
import com.hitorro.network.servlet.ServletUtil;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.keystore.KeyStore;
import com.hitorro.util.typesystem.btadapter.BaseAdapterItemCache;
import com.hitorro.util.typesystem.btadapter.BaseAdapterItemCacheSet;

import jakarta.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

/**
 * <p/>
 * forum      = <forum_name>                - constrain by a forum orderby    = <authoredDate|createDate>   - for
 * aggregated feeds like TechOne, we dont really want the post date as they were not added to the share in the authored
 * date order. However for a normal forum we probably do want them in regular authoredDate order maxresults = <count> -
 * Maximum amount of results returned.
 */
public class RssFeedGenerator extends HttpServlet {
    public static final String RssItemCacheName = "rssiteminterface";
    private static final long serialVersionUID = 4793110215928860139L;
    private int maxCache = 10000;
    private BaseAdapterItemCache cache = null;

    public RssFeedGenerator() {
        KeyStore<RssItemInterface> store = null;
        RssItemProxy proxy = new RssItemProxy();
        /**
         * Construct a item cache that will potentially persist in a btree and if not found in a btree
         * will retrieve from the underlying db.
         */
        cache = new BaseAdapterItemCache(true, new RssItemCacheProxy(),
                maxCache, VersionableObjectRssItem.AdapterGroup,
                "rssitemcache", store, proxy, new DBBaseAdapterItemCacheFetcher());

        BaseAdapterItemCacheSet.setCache(RssItemCacheName, cache);
        BaseAdapterItemCacheSet.setProxy(RssItemCacheName, proxy);
    }

    protected long getLastModified(javax.servlet.http.HttpServletRequest httpServletRequest) {
        //return System.currentTimeMillis() - Constants.getMillis(0,0,10,0);
        return -1;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        //Log.conversationtracker.setLevel(Level.DEBUG);
        String verb = request.getPathInfo();
        Map<String, String[]> p = request.getParameterMap();
        Map<String, String> paramsTmp = ServletUtil.getArgs(p);
        JVS params = JVSUtils.convertMapToJVS(paramsTmp);
        XWriter rssWriter;
        if (RssIteratorGenerator.HtmlKey.apply(params)) {
            rssWriter = new HTMLWriter(params.getJsonNode());
        } else {
            rssWriter = new RssWriter(params.getJsonNode());
        }

        RssIteratorGenerator gen = new RssIteratorGenerator(cache);
        Writer writer = null;
        try {
            IteratorRequest r = new IteratorRequest();
            Iterator<RssItemInterface> iter = gen.generate(params.getJsonNode(), r);
            if (iter == null) {
                response.sendError(404);
                return;
            }
            if (r.getCacheTimeInMillis() != -1) {
                long current = System.currentTimeMillis();
                response.setDateHeader("Date", current);
                response.setDateHeader("Last-Modified", current);
                response.addHeader("Cache-Control", Fmt.S("s-maxage=120, public", r.getCacheTimeInMillis()));
                response.addHeader("Cache-Control", "proxy-revalidate");
            }
            writer = writeOutResponse(params.getJsonNode(), rssWriter, response, writer, gen, iter);
        } finally {
            gen.close();
            if (writer != null) {
                if (rssWriter.requiresFlush()) {
                    writer.flush();
                }
                writer.close();
            }
        }
    }

    private Writer writeOutResponse(JsonNode params, XWriter rssWriter, HttpServletResponse response, Writer writer, RssIteratorGenerator gen, Iterator<RssItemInterface> iter) throws IOException {
        boolean debug = RssIteratorGenerator.DebugKey.apply(params);
        rssWriter.setDebug(debug);
        writer = new OutputStreamWriter(response.getOutputStream(), "UTF8");

        try {
            response.setContentType(rssWriter.getContentType());
            int maxResults = RssIteratorGenerator.MaxResultsKey.apply(params);
            int window = RssIteratorGenerator.ResultsWindowKey.apply(params);
            rssWriter.setTitle(gen.getTitle());
            rssWriter.generateFeed(writer, iter, maxResults, window);
        } catch (IOException e) {
            Log.util.error("Exception %s %e", e, e);
        }
        return writer;
    }

}
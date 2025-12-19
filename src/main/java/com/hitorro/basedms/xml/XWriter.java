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

import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * <p/>
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Sep 28, 2005 Time: 3:17:22 PM
 */
public abstract class XWriter {
    protected boolean showItunes = false;
    protected List<String> iTunesCategories = new ArrayList<String>();
    protected String rssGenerator = "HiTorro Dynamic RSS";
    protected String feedName = "HiTorro Dynamic Feed";
    protected Writer _writer;
    protected int _depth;
    protected JsonNode params;


    protected boolean debug = false;
    protected Map<String, String> dupeMap = new HashMap<String, String>();

    public XWriter(JsonNode params) {
        this.params = params;
    }

    public void setTitle(String title) {
        feedName = title;
    }

    public boolean requiresFlush() {
        return false;
    }

    public void setDebug(boolean flag) {
        debug = flag;
    }

    public abstract void generateFeed(Writer writerx, Iterator<com.hitorro.basedms.xml.RssItemInterface> items, int maxItems, int window)
            throws IOException;

    public String getContentType() {
        return com.hitorro.basedms.Constants.XMLMimeType;
    }
}

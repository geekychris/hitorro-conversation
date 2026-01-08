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

import com.hitorro.basedms.xml.RssItemInterface;
import com.hitorro.conversationtracker.Log;
import com.hitorro.obj.core.FontColorCoder;
import com.hitorro.util.html.HTMLParser;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * <p/>
 */
public class TopWordsIterator implements Iterator<RssItemInterface> {
    private Iterator<RssItemInterface> iter;
    private int topNWords;
    private HTMLParser parser = new HTMLParser();

    public TopWordsIterator(Iterator<RssItemInterface> iter, int topNWords) {
        this.iter = iter;
        this.topNWords = topNWords;
    }

    public boolean hasNext() {
        return iter.hasNext();
    }

    public RssItemInterface next() {
        RssItemInterface rii = iter.next();
        if (rii != null) {
            List<String> t = rii.getTopWords();
            if (t == null || t.size() >= topNWords) {
                return rii;
            }
            String desc = rii.getDescription();
            String title = rii.getTitle();
            StringBuilder sb = new StringBuilder();
            if (desc != null) {
                try {
                    parser.setHtmlPage(desc);
                    sb.append(parser.getBodyText());
                } catch (IOException e) {
                    Log.util.error("Exception %s %e", e, e);
                }

            }
            sb.append(" ");
            if (title != null) {
                sb.append(title);
            }
            try {
                List<String> l = FontColorCoder.getTopNWords(sb.toString(), topNWords, true);
                rii.setTopWords(l);
            } catch (IOException e) {
                Log.util.error("Exception %s %e", e, e);
            }
        }
        return rii;
    }

    public void remove() {

    }
}
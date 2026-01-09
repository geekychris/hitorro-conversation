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
package com.hitorro.analysis.linkengine;

import com.hitorro.util.html.Link;
import com.hitorro.util.html.LinkSet;

import java.util.Iterator;
import java.util.List;

/**
 * <p/>
 * Adapter iterator that takes a LinkSet iterator and provides an iterator of Links
 */
public class LinkSetToLinkIterator implements Iterator<Link> {
    private Iterator<LinkSet> m_iter;
    private Iterator<Link> linkIter;
    private Link m_link;

    public LinkSetToLinkIterator(Iterator<LinkSet> iter) {
        m_iter = iter;
        m_link = advance();
    }

    public boolean hasNext() {
        return m_link != null;
    }

    public Link next() {
        Link returnMe = m_link;
        m_link = advance();
        return returnMe;
    }

    public void remove() {
        // not implemented
    }

    private Link advance() {
        if (linkIter != null) {
            if (linkIter.hasNext()) {
                return linkIter.next();
            }
        }
        while (m_iter.hasNext()) {
            LinkSet ls = m_iter.next();
            List<Link> list = null;
            if (ls != null) {
                list = ls.getLinks();
            }
            if (list != null && list.size() > 0) {
                linkIter = list.iterator();
                if (linkIter.hasNext()) {
                    return linkIter.next();
                }
            }
        }
        return null;
    }
}

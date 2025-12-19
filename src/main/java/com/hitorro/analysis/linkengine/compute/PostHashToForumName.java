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
package com.hitorro.analysis.linkengine.compute;

import com.hitorro.util.core.string.Fmt;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved.
 * <p/>
 * User: chris
 */
public class PostHashToForumName {
    private long m_hash;
    private String m_link;
    private String m_forum;
    private int m_count;

    public PostHashToForumName(Long hash, String link, String forum) {
        m_hash = hash.longValue();
        m_link = link;
        m_forum = forum;
    }

    public PostHashToForumName(long hash, String link, String forum) {
        m_hash = hash;
        m_link = link;
        m_forum = forum;
    }

    public long getHash() {
        return m_hash;
    }

    public String getLink() {
        return m_link;
    }

    public String getForum() {
        return m_forum;
    }

    public String toString() {
        return Fmt.S("%s.%s.%s: %s", m_hash, m_link, m_forum, m_count);
    }

    public int getCount() {
        return m_count;
    }

    public void setCount(int count) {
        m_count = count;
    }

}

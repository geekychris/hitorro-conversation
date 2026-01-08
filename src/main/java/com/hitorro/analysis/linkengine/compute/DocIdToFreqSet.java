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


import gnu.trove.map.hash.TLongByteHashMap;
import com.hitorro.util.core.queue.PriorityQueue;
import com.hitorro.util.core.string.Fmt;


public class DocIdToFreqSet {
    private PriorityQueue<DocIdToFreq> m_pq;
    private TLongByteHashMap m_map = new TLongByteHashMap();
    private DocIdToFreq m_prev = null;

    public DocIdToFreqSet(int size) {
        m_pq = new PriorityQueue<DocIdToFreq>(new CountComparator(), size);
    }

    public void clear() {
        m_map.clear();
        m_pq.clear();
    }

    public void add(long docId, int freq) {
        if (m_map.containsKey(docId)) {
            return;
        }
        m_map.put(docId, (byte) 1);
        DocIdToFreq f;
        if (m_prev != null) {
            f = m_prev;
        } else {
            f = new DocIdToFreq();
        }
        f.m_docId = docId;
        f.m_freq = freq;
        m_prev = m_pq.insert(f);
    }

    public PriorityQueue<DocIdToFreq> getPriorityQueue() {
        return m_pq;
    }
}

class DocIdToFreq {
    public long m_docId;
    public int m_freq;

    public String toString() {
        return Fmt.S("docID: %s, freq:%s", m_docId, m_freq);
    }
}


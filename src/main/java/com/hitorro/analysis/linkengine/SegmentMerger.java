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

import com.hitorro.analysis.linkengine.tuple.LinkTupleReader;
import com.hitorro.analysis.linkengine.tuple.RLEByteArrayTupleLinkReader;
import com.hitorro.util.core.queue.PriorityQueue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <p/>
 * The merger is given n segments and will apply them into a target segment.  Once complete, the old segments will be
 * removed from the index and substituted with the merged segment.
 */
class SegmentMerger extends TuplePool {
    private PriorityQueue<RLEByteArrayTupleLinkReader> m_pq =
            new PriorityQueue<RLEByteArrayTupleLinkReader>(new SegmentMergerComparator(), 100);
    private ArrayList<RLEByteArrayTupleLinkReader> inUse =
            new ArrayList<RLEByteArrayTupleLinkReader>();
    private List<LinkSegment> m_segments = new ArrayList<LinkSegment>();

    private LinkEngine m_engine;
    private int inUseCount = 0;

    public SegmentMerger(LinkEngine engine) {
        m_engine = engine;
    }

    public void add(LinkSegment segment) throws IOException {
        byte[] array = segment.getArray();
        RLEByteArrayTupleLinkReader reader = this.getRLEByteReader();
        reader.set(array, 0);
        reader.readDocId();
        m_pq.put(reader);
        m_segments.add(segment);
        inUseCount++;
    }

    public LinkSegment merge() throws IOException {
        LinkSegmentWriter writer = m_engine.getWriter();
        writer.open();
        long docId = 0;
        int postings = 0;
        while (inUseCount > 0) {
            // keep doing this while we have elements in the queue

            if ((m_pq.size() == 0 || m_pq.top().m_docId != docId) && getQueueSize() > 0) {
                // We have stuff to copy out in the queue since the current item in the queue is greater...
                LinkTupleReader copyMe = this.popQueue();
                // write out the posting.
                if (copyMe != null) {
                    postings++;
                    while (copyMe.readNext()) {
                        writer.write(docId, copyMe.readerDocumentId, copyMe.m_typeCode, copyMe.m_metric, copyMe.m_date);
                    }

                    // return the select tree back.  We do infact return the byte readers back to the pool
                    // though we are actually ok since we never do any further allocs of them here.
                    copyMe.free(m_pool);
                    for (RLEByteArrayTupleLinkReader rb : inUse) {
                        // put the used readers back on the listFiles.
                        rb.readDocId();
                        m_pq.insert(rb);
                    }
                    inUse.clear();
                }
            } else {
                // item at the top of the queue
                RLEByteArrayTupleLinkReader reader = m_pq.pop();
                if (reader != null && reader.m_docId == -1) {
                    // reader is at the end, dont requeue
                    inUseCount--;
                    continue;
                } else {
                    docId = reader.m_docId;
                    // to apply a select tree
                    this.enQueue(reader);
                    // listFiles to track what we took off the queue, so we can advance them and put them back on the queue
                    inUse.add(reader);
                }
            }
        }
        return writer.close();
    }
}


class SegmentMergerComparator implements Comparator<RLEByteArrayTupleLinkReader> {
    public int compare(RLEByteArrayTupleLinkReader reader, RLEByteArrayTupleLinkReader reader1) {
        if (reader.m_docId > reader1.m_docId) {
            return 1;
        }
        if (reader.m_docId < reader1.m_docId) {
            return -1;
        }
        return 0;
    }
}
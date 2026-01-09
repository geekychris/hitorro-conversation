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
import com.hitorro.analysis.linkengine.tuple.TupleANDSelectTreeReader;
import com.hitorro.analysis.linkengine.tuple.TupleORSelectTreeReader;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

/**
 * <p/>
 * Encapsulate the management of a pool of tuple readers and the ability to create an or tree from raw readers.  Used
 * for normal queries and for merging segments.
 */
public class TuplePool {
    public static final int RLEByteReader = 0;
    public static final int ORReader = 1;
    public static final int ANDReader = 2;
    public static final int MaxPoolSlot = ANDReader;

    protected LinkTupleReader m_pool[] = new LinkTupleReader[MaxPoolSlot];
    private Queue<LinkTupleReader> readerQueue = new LinkedList<LinkTupleReader>();


    void enQueue(LinkTupleReader reader) {
        if (reader == null) {
            return;
        }
        readerQueue.add(reader);
    }

    /**
     * If the index has more than 1 tier, we need to apply the collections together. All the RLEByteArray readers are on
     * the stack, we take them off in twos and or them together that resultant or is placed back on the queue until
     * there is 1 man left.  That should give us an evenly balanced tree thus, for four segments a, b, c, d we would
     * have:
     * <p/>
     * reader(a) / or - reader(b) / or \ or - reader(c) \ reader(d)
     *
     * @return
     * @throws java.io.IOException
     */
    LinkTupleReader popQueue() throws IOException {

        int size = readerQueue.size();
        if (size == 0) {
            // didnt really expect that since we must of found the doc entry in the listFiles
            return null;
        }

        while (readerQueue.size() > 1) {
            TupleORSelectTreeReader reader = this.getORReader();
            reader.set(readerQueue.remove(), readerQueue.remove());
            readerQueue.add(reader);
        }
        LinkTupleReader reader = readerQueue.remove();
        reader.reset();
        return reader;
    }

    int getQueueSize() {
        return readerQueue.size();
    }

    /**
     * Return the query tree back to the pool
     *
     * @param reader
     */
    public void freeExpressionTree(LinkTupleReader reader) {
        if (reader == null) {
            return;
        }
        reader.free(m_pool);
    }

    //*************************** POOL MANAGEMENT ****************************

    RLEByteArrayTupleLinkReader getRLEByteReader() {
        RLEByteArrayTupleLinkReader reader = (RLEByteArrayTupleLinkReader) m_pool[RLEByteReader];
        if (reader != null) {
            // one exists in pool return it.
            m_pool[RLEByteReader] = reader.m_next;
            reader.m_next = null;

        } else {
            reader = new RLEByteArrayTupleLinkReader();
            reader.poolIndex = RLEByteReader;
        }
        return reader;
    }

    TupleORSelectTreeReader getORReader() {
        TupleORSelectTreeReader reader = (TupleORSelectTreeReader) m_pool[ORReader];
        if (reader != null) {
            // one exists in pool return it.
            m_pool[ORReader] = reader.m_next;
            reader.m_next = null;
        } else {
            reader = new TupleORSelectTreeReader();
            reader.poolIndex = ORReader;
        }
        return reader;
    }

    TupleANDSelectTreeReader getANDReader() {
        TupleANDSelectTreeReader reader = (TupleANDSelectTreeReader) m_pool[ANDReader];
        if (reader != null) {
            // one exists in pool return it.
            m_pool[ANDReader] = reader.m_next;
            reader.m_next = null;
        } else {
            reader = new TupleANDSelectTreeReader();
            reader.poolIndex = ANDReader;
        }
        return reader;
    }
}

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
package com.hitorro.analysis.linkengine.tuple;


import gnu.trove.map.hash.TObjectByteHashMap;
import com.hitorro.util.core.Console;

import java.io.IOException;

/**
 * <p/>
 * A tuple posting looks like: <docId><typeCode><date>[<metric>]
 */
public abstract class LinkTupleReader {
    /**
     * the type code consists of two parts: type number that defines the type of relationship extra bits that identify:
     * - more directed edges for same vertex pair to come - item deleted (if we go back and mark a bit as deleted
     * without merging it out....this could occur if its - held in memory
     * <p/>
     * Also that has to be encoded in the type is if it has a metric (so that we can save a byte per edge that doesnt).
     * This could be encoded simply as "link doesnt", "similarity does"....or we could say that even numbers are with,
     * odd without.
     * <p/>
     * <p/>
     * bits:
     * <p/>
     * inbound (a <- b) / 7 6 5 4 3 2 1 0 \    \-------\ \ \        \      set if the type has metric value. \        \
     * \        remainder of the types space (bits 1 - 5) \ set if there are
     * <p/>
     * With bits 1-5 reserved for type, considering bit 0 is "has metric" we have 2^5 (32 min) < 2^6 (64 max link types)
     * Thats 64 if we found as many link types that need a metric as do not.
     */

    public static final byte TypeBit_HasMore = (byte) (1 << 7);
    public static final byte TypeBit_Inbound = (byte) (1 << 6);
    public static final byte TypeBit_HasMetric = (byte) (1 << 0);
    public static final byte TypeHRef = 2;                                    // (2)
    public static final String TypeHRef_Key = "href";
    public static final byte TypeTextualSimilarity = 2 | TypeBit_HasMetric;   // (3 METRIC)
    private static final TObjectByteHashMap s_typeNameToByte = getTypeNameToByte();
    public static final String TypeTextualSimilarity_Key = "similar";
    public static final byte TypeComment = 4;                                 // (4)
    public static final String TypeComment_Key = "comment";
    public static final byte TypeRssShare = 6;                                // (6)
    public static final String TypeRssShare_Key = "rssshare";
    public long m_docId;
    //
    public LinkTupleReader m_next;
    public int poolIndex = -1;
    public long readerDocumentId;
    public byte m_typeCode;
    public short m_date;
    public byte m_metric;
    public long m_seek;
    protected boolean m_skipDeleted;

    /**
     * Get the type code from the name
     *
     * @param name
     * @return
     */
    public static final byte getTypeByteFromName(String name) {
        name = name.toLowerCase();
        return s_typeNameToByte.get(name);
    }


    private static final TObjectByteHashMap getTypeNameToByte() {
        TObjectByteHashMap map = new TObjectByteHashMap();
        map.put(TypeHRef_Key, TypeHRef);
        map.put(TypeComment_Key, TypeComment);
        map.put(TypeRssShare_Key, TypeRssShare);
        map.put(TypeTextualSimilarity_Key, TypeTextualSimilarity);


        return map;
    }

    /**
     * Determines from the type code if there is a metric to be associated with this tuple
     *
     * @param typeCode
     * @return true indicates there is a metric.
     */
    public static boolean hasMetric(byte typeCode) {
        return (typeCode & TypeBit_HasMetric) != 0;
    }

    /**
     * Determines if this type has more follow on edges between the same vertices, if so then we only need to store the
     * edge type and the metric
     *
     * @param typeCode
     * @return
     */
    public static boolean hasMoreRefs(byte typeCode) {
        return (typeCode & TypeBit_HasMore) != 0;
    }

    public static void dumpReader(String title, LinkTupleReader reader) throws IOException {
        Console.println(title);
        Console.println();
        while (reader.readNext()) {
            if (LinkTupleReader.hasMetric(reader.m_typeCode)) {
                Console.println("DocID: %s, type: %s, metric: %s , date: %s", reader.getDocumentId(),
                        reader.m_typeCode,
                        reader.getMetric(),
                        reader.getDate());
            } else {
                Console.println("DocID: %s, type: %s , date: %s", reader.getDocumentId(), reader.m_typeCode, reader.getDate());
            }
        }
        Console.println();
    }

    /**
     * Free's the current element (returns it to the pool), poolIndex must of been set during newing the non abstract
     * object.
     *
     * @param pool
     */
    public void free(LinkTupleReader[] pool) {
        if (pool[poolIndex] != null) {
            m_next = pool[poolIndex];
        } else {
            m_next = null;
        }
        pool[poolIndex] = this;
    }

    public abstract void reset() throws IOException;

    public long getDocumentId() {
        return readerDocumentId;
    }

    public byte getMetric() {
        return m_metric;
    }

    public long getDate() {
        return m_date;
    }

    public abstract boolean readNext() throws IOException;

    //********************* UTIL METHODS FOR DEBUGGING *******************************************

    /**
     * This reader can be slightly smarter.  If we are reading postings we dont have to read the date or the metric.
     *
     * @param docId
     * @return
     */
    public boolean readToDocId(long docId) throws IOException {
        while (readNext()) {
            if (docId == readerDocumentId) {
                // we found our link
                return true;
            }
        }
        // we didnt find our link.
        return false;
    }

}

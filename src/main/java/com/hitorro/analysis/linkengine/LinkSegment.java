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


import gnu.trove.map.hash.TLongLongHashMap;
import com.hitorro.analysis.linkengine.tuple.LinkTupleReader;
import com.hitorro.analysis.linkengine.tuple.RLEByteArrayTupleLinkReader;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.core.map.TroveMapUtil;
import com.hitorro.util.io.largedata.compressedstreams.FSOutputStream;

import java.io.IOException;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Feb 16, 2005 Time: 8:18:00 AM encapsulation
 * of the set of index parts that make up a segment.
 * <p/>
 * The DocId mapping here is ignorant to whether this is a hash based id or some ephemeral id, it simply is a key to
 * offset
 */
public class LinkSegment {
    public static final String SegmentExtension = "segment";
    public static final String DocToPositionExtension = "positionals";
    private TLongLongHashMap m_docIdToPosition = new TLongLongHashMap();
    private byte m_array[];
    private BaseFile m_dir;
    private String m_segmentName;

    private BaseFile m_segFile;
    private BaseFile m_positionalsFile;

    public LinkSegment(BaseFile dir, String segmentName) {
        m_dir = dir;
        m_segmentName = segmentName;
        m_segFile = m_dir.getChild("%s.%s", m_segmentName, SegmentExtension);
        m_positionalsFile = m_dir.getChild("%s.%s", m_segmentName, DocToPositionExtension);
    }

    /**
     * How many postings there are in this segment.
     *
     * @return
     */
    public int getSize() {
        return m_docIdToPosition.size();
    }

    public String getName() {
        return m_segmentName;
    }

    byte[] getArray() {
        return m_array;
    }

    public void delete() throws IOException {
        m_segFile.delete();
        m_positionalsFile.delete();
    }

    /**
     * Get a reader for the document id.  Note that we use the query compContext as the pool.  You cant actually get a
     * reader directly from this guy, you must go through the compContext.
     *
     * @param context
     * @param docId
     * @return reader, or null if docid is not available.
     */
    LinkTupleReader getReaderForDocId(LinkQueryContext context, long docId) {
        long pos = m_docIdToPosition.get(docId);
        if (pos == 0) {
            return null;
        }
        RLEByteArrayTupleLinkReader reader = context.getRLEByteReader();
        reader.set(m_array, pos);
        reader.m_docId = docId;
        return reader;
    }

    public void load() throws IOException {
        m_array = m_segFile.getBytes();
        m_docIdToPosition = TroveMapUtil.getTLongLongFromFile(m_positionalsFile, m_docIdToPosition);
    }

    void setOffset(long docid, long offset) {
        m_docIdToPosition.put(docid, offset);
    }

    void writePositional() throws IOException {
        int size = m_docIdToPosition.size();
        TroveMapUtil.writeTLongLongMapToFile(m_positionalsFile, m_docIdToPosition);
    }

    FSOutputStream getOutputStream()
            throws IOException {
        m_segFile.getParent().mkdir();
        return new FSOutputStream(m_segFile);
    }
}

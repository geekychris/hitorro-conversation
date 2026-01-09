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
 * of the set of index parts that make up a segment.
 * <p/>
 * The DocId mapping here is ignorant to whether this is a hash based id or some ephemeral id, it simply is a key to
 * offset
 */
public class LinkSegment {
    public static final String SegmentExtension = "segment";
    public static final String DocToPositionExtension = "positionals";
    private TLongLongHashMap docIdToPosition = new TLongLongHashMap();
    private byte m_array[];
    private BaseFile m_dir;
    private String segmentName;

    private BaseFile segFile;
    private BaseFile positionalsFile;

    public LinkSegment(BaseFile dir, String segmentName) {
        m_dir = dir;
        this.segmentName = segmentName;
        segFile = m_dir.getChild("%s.%s", this.segmentName, SegmentExtension);
        positionalsFile = m_dir.getChild("%s.%s", this.segmentName, DocToPositionExtension);
    }

    /**
     * How many postings there are in this segment.
     *
     * @return
     */
    public int getSize() {
        return docIdToPosition.size();
    }

    public String getName() {
        return segmentName;
    }

    byte[] getArray() {
        return m_array;
    }

    public void delete() throws IOException {
        segFile.delete();
        positionalsFile.delete();
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
        long pos = docIdToPosition.get(docId);
        if (pos == 0) {
            return null;
        }
        RLEByteArrayTupleLinkReader reader = context.getRLEByteReader();
        reader.set(m_array, pos);
        reader.m_docId = docId;
        return reader;
    }

    public void load() throws IOException {
        m_array = segFile.getBytes();
        docIdToPosition = TroveMapUtil.getTLongLongFromFile(positionalsFile, docIdToPosition);
    }

    void setOffset(long docid, long offset) {
        docIdToPosition.put(docid, offset);
    }

    void writePositional() throws IOException {
        int size = docIdToPosition.size();
        TroveMapUtil.writeTLongLongMapToFile(positionalsFile, docIdToPosition);
    }

    FSOutputStream getOutputStream()
            throws IOException {
        segFile.getParent().mkdir();
        return new FSOutputStream(segFile);
    }
}

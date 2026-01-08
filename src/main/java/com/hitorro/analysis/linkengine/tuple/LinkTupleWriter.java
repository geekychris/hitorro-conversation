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

import java.io.IOException;

/**
 * <p/>
 * The tuple writer has to deal with writing out tuples, however it has to hold back the previous tuple till it figures
 * back if the current tuple has the same doc id.  If it does, it has to mark in the previous that the following tuple
 * only has metrics and type info (no docid or date).  This slows down the writer a little bit compared to the reader
 * (and makes it a tad more complex), however writing will be an orders of magnitude less than reading.
 */
public abstract class LinkTupleWriter {
    protected long m_prevDocId;
    protected byte m_type;
    protected byte m_metric;
    protected short m_date;

    protected boolean m_sameAsPrevious;

    public void reset() {
        m_prevDocId = 0;
        m_sameAsPrevious = false;
    }

    public boolean writeTuple(long docId, byte type, byte metric, short date) throws IOException {
        // if the doc ids are the same then we only write out the type, metric info.

        writeOutPrev(docId == m_prevDocId, m_sameAsPrevious);
        m_sameAsPrevious = (docId == m_prevDocId);
        m_prevDocId = docId;
        m_type = type;
        m_metric = metric;
        m_date = date;
        return true;
    }

    private boolean writeOutPrev(boolean nextIsSame, boolean thisIsFollowOn) throws IOException {
        if (m_prevDocId == 0) {
            // this was the first time called and we should write out null.
            return true;
        }
        return writeTupleAux(m_prevDocId, m_type, m_metric, m_date, nextIsSame, thisIsFollowOn);
    }

    /**
     * End of stream, so we can write out the last item in the stream
     */
    public boolean close() throws IOException {

        boolean returnVal = writeOutPrev(false, m_sameAsPrevious);
        writeEndOfStream();
        reset();
        return returnVal;
    }

    protected abstract boolean writeEndOfStream() throws IOException;

    protected abstract boolean writeTupleAux(long docId,
                                             byte type,
                                             byte metric,
                                             short date,
                                             boolean sameDocIdAsNext,
                                             boolean thisIsFollowOn)
            throws IOException;
}

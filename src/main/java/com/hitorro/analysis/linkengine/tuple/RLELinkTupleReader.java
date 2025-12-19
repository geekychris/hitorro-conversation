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
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Feb 13, 2005 Time: 3:25:11 PM
 */
public abstract class RLELinkTupleReader extends LinkTupleReader {
    private static final byte SansTypeCode = 0xff ^ 1 << 7;
    protected boolean m_hasMoreReferences = false;
    protected byte m_typeCodeUnmuted;

    public boolean readNext() throws IOException {
        if (hasMoreRefs(m_typeCodeUnmuted)) {
            // this is a follow on reference between the same vertices.
            m_typeCodeUnmuted = readByte();
            m_typeCode = (byte) (m_typeCodeUnmuted & SansTypeCode);

            if (hasMetric(m_typeCode)) {
                m_metric = readByte();
            }
        } else {
            long tempId = this.readVLong();
            if (tempId == 0) {
                m_readerDocumentId = 0;
                // end of postings;
                return false;
            }

            m_readerDocumentId += tempId;
            m_typeCodeUnmuted = readByte();
            m_typeCode = (byte) (m_typeCodeUnmuted & SansTypeCode);

            this.m_date = readShort();
            if (hasMetric(m_typeCode)) {
                m_metric = readByte();
            }
        }
        return true;
    }

    /**
     * Not currently used as we only save 3 bytes being read.
     *
     * @param docId
     * @return
     * @throws IOException
     */
    private boolean readToDocIdAux(long docId) throws IOException {
        if (hasMoreRefs(m_typeCode)) {
            // this is a follow on reference between the same vertices.
            m_typeCodeUnmuted = readByte();
            m_typeCode = (byte) (m_typeCodeUnmuted & SansTypeCode);
            if (hasMetric(m_typeCode)) {
                m_metric = readByte();
            }
        } else {
            long tempId = readVLong();
            if (tempId == 0) {
                // end of postings;
                return false;
            }

            m_readerDocumentId += tempId;
            m_typeCode = readByte();

            if (docId == m_readerDocumentId) {
                // we want to read the full thing
                this.m_date = readShort();
                if (hasMetric(m_typeCode)) {
                    m_metric = readByte();
                }
            } else {
                // skip three bytes
                advanceNBytes(3);
            }
        }
        return true;
    }

    protected abstract byte readByte() throws IOException;

    protected abstract short readShort() throws IOException;

    protected abstract int readVInt() throws IOException;

    protected abstract long readVLong() throws IOException;


    protected abstract void advanceNBytes(int size) throws IOException;
}

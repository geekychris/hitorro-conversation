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

import com.hitorro.util.io.largedata.compressedstreams.COutputStream;

import java.io.IOException;


/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Feb 13, 2005 Time: 3:40:40 PM
 */
public class RLELinkTupleWriter extends LinkTupleWriter {
    private COutputStream m_os;
    private long m_docId;

    public void setOutputStream(COutputStream os) {
        m_os = os;
        reset();
    }

    public void reset() {
        m_docId = 0;
        super.reset();
    }

    protected boolean writeEndOfStream() throws IOException {
        // end of stream
        m_os.writeVInt(0);
        return true;
    }

    protected boolean writeTupleAux(long docId,
                                    byte type,
                                    byte metric,
                                    short date,
                                    boolean sameDocIdAsNext,
                                    boolean thisIsFollowOn)
            throws IOException {
        if (sameDocIdAsNext) {
            // record there is more to follow
            type = (byte) (type | LinkTupleReader.TypeBit_HasMore);
        }
        if (thisIsFollowOn) {

            m_os.writeByte(type);
            if (LinkTupleReader.hasMetric(type)) {
                m_os.writeByte(metric);
            }
        } else {
            m_os.writeVLong(docId - m_docId);
            m_docId = docId;
            m_os.writeByte(type);
            m_os.writeShort(date);
            if (LinkTupleReader.hasMetric(type)) {
                m_os.writeByte(metric);
            }
        }

        return true;
    }
}

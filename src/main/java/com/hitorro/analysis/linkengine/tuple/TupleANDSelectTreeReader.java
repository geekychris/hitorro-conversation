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
 * And two Tuple Readers together (intersection). That is, we will only output a stream where the document id's are
 * provided in both left and right readers.
 */

public class TupleANDSelectTreeReader extends TupleLogicSelectTreeReader {
    public TupleANDSelectTreeReader() {

    }

    public TupleANDSelectTreeReader(LinkTupleReader left, LinkTupleReader right) throws IOException {
        set(left, right);
    }

    public final boolean readNext() throws IOException {
        while (true) {
            switch (readTupleAux()) {
                case -1:
                    // either left or right doesnt have data anymore
                    return false;
                case 0:
                    // we have to advance the left or the right but ignore the value
                    // since we dont have the same id's in the left and the right.
                    continue;
                case 1:
                    // have test of both channels
                    return m_readerDocumentId > 0;
            }
        }
    }

    protected final int readTupleAux() throws IOException {
        if (m_left.m_readerDocumentId != 0 && m_right.m_readerDocumentId != 0) {
            if (m_left.m_readerDocumentId == m_right.m_readerDocumentId) {
                // items same, we should taken in order of type
                takeLeft();
                byte lTC = m_left.m_typeCode;
                byte rTC = m_right.m_typeCode;
                if (lTC < rTC) {
                    takeLeft();
                    m_left.readNext();
                } else if (lTC > rTC) {
                    // right is smaller, take it
                    takeRight();
                    m_right.readNext();
                } else {
                    // both same, reduce to left
                    takeLeft();
                    m_left.readNext();
                    m_right.readNext();
                }
                return 1;
            } else if (m_left.m_readerDocumentId < m_right.m_readerDocumentId) {
                m_left.readNext();
                return 0;
            } else {
                // right must be smaller
                m_right.readNext();
                return 0;
            }
        } else {
            return -1;
        }
    }

}

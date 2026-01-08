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
 * Select tree is responsible for providing a merged view of multiple  Tuple Reader's. This class however only does two
 * Readers....Put them in a call tree which gives Log2n performance (where n is the number of channels).
 * <p/>
 * LEFT TAKES PRESIDENCE OVER RIGHT.  So provide LEFT as the newer tier.
 */

public class TupleORSelectTreeReader extends TupleLogicSelectTreeReader {

    public TupleORSelectTreeReader() {
    }

    public TupleORSelectTreeReader(LinkTupleReader left, LinkTupleReader right) throws IOException {
        set(left, right);
    }

    @Override
    public final boolean readNext() throws IOException {
        if (m_left.m_readerDocumentId != 0) {
            if (m_right.m_readerDocumentId != 0) {
                if (m_left.m_readerDocumentId < m_right.m_readerDocumentId) {
                    // we use the lefts value to pass back
                    takeLeft();
                    m_left.readNext();
                } else if (m_left.m_readerDocumentId == m_right.m_readerDocumentId) {
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

                } else {
                    takeRight();
                    m_right.readNext();
                }
            } else {
                // we dont have right so we just provide left data!
                takeLeft();
                m_left.readNext();
            }
        } else {
            //  we dont have right so we just provide left data!
            if (m_right.m_readerDocumentId != 0) {
                takeRight();
                m_right.readNext();
            } else {
                m_readerDocumentId = 0;
            }
        }
        return m_readerDocumentId > 0;
    }
}

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
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Feb 13, 2005 Time: 3:40:20 PM
 */

public abstract class TupleLogicSelectTreeReader extends LinkTupleReader {
    protected LinkTupleReader m_left;
    protected LinkTupleReader m_right;

    public void free(LinkTupleReader[] pool) {
        m_left.free(pool);
        m_right.free(pool);
        if (pool[m_poolIndex] != null) {
            m_next = pool[m_poolIndex];
        } else {
            m_next = null;
        }
        pool[m_poolIndex] = this;
    }

    public final void set(LinkTupleReader left, LinkTupleReader right) throws IOException {

        set(left, right, true);
    }

    public void reset() throws IOException {
        m_readerDocumentId = 0;
        m_left.reset();
        m_right.reset();
        primePump();
    }

    /**
     * Set the left and right readers and also state if we want to ignore the deleted entries.
     *
     * @param left
     * @param right
     * @param skipDeletes
     */
    public final void set(LinkTupleReader left, LinkTupleReader right, boolean skipDeletes) {
        m_left = left;
        m_right = right;
        m_skipDeleted = skipDeletes;
    }

    public final void primePump() throws IOException {
        m_left.readNext();
        m_right.readNext();
    }

    protected final void takeLeft() {
        m_readerDocumentId = m_left.m_readerDocumentId;
        m_typeCode = m_left.m_typeCode;
        m_date = m_left.m_date;
        m_metric = m_left.m_metric;
    }

    protected final void takeRight() {
        m_readerDocumentId = m_right.m_readerDocumentId;
        m_typeCode = m_right.m_typeCode;
        m_date = m_right.m_date;
        m_metric = m_right.m_metric;
    }
}

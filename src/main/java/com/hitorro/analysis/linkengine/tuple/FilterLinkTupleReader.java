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
 * Abstract class for filtering out tuples we dont care about
 */
public class FilterLinkTupleReader extends LinkTupleReader {

    private LinkTupleReader m_reader;
    private TupleReaderFilter m_filter;

    public FilterLinkTupleReader(TupleReaderFilter filter) {
        m_filter = filter;
    }

    public void setReader(LinkTupleReader reader) {
        m_reader = reader;
    }

    public void reset() throws IOException {
        m_reader.reset();
    }

    public boolean readNext() throws IOException {
        while (m_reader.readNext()) {
            if (m_filter.isValid(m_reader)) {
                m_readerDocumentId = m_reader.m_readerDocumentId;
                m_typeCode = m_reader.m_typeCode;
                m_metric = m_reader.m_metric;
                m_date = m_reader.m_date;
                return true;
            }
        }
        return false;
    }
}

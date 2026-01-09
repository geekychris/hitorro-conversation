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

import com.hitorro.util.io.largedata.compressedstreams.CInputStream;

import java.io.IOException;

/**
 */
public class RLEISLinkTupleReader extends RLELinkTupleReader {
    private CInputStream m_is;

    public void setInputStream(CInputStream is, long seek) throws IOException {
        m_is = is;
        m_seek = seek;
        reset();
    }

    public void reset() throws IOException {
        readerDocumentId = 0;
        typeCodeUnmuted = 0;
        m_typeCode = 0;
        m_is.seek(m_seek);
    }

    protected byte readByte() throws IOException {
        return m_is.readByte();
    }

    protected short readShort() throws IOException {
        return m_is.readShort();
    }

    protected int readVInt() throws IOException {
        return m_is.readVInt();
    }

    protected long readVLong() throws IOException {
        return m_is.readVLong();
    }

    protected void advanceNBytes(int size) throws IOException {
        m_is.seek(m_is.getFilePointer() + size);

    }
}

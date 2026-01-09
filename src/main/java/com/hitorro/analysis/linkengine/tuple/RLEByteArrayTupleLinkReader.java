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
 */
public class RLEByteArrayTupleLinkReader extends RLELinkTupleReader {
    private byte m_array[];
    private int m_offset;

    private int m_bufferPosition;
    private int m_bufferLength;

    /**
     * Method used for iterating through postings one by one without the need for lookup in the docid ->offset table
     *
     * @return
     */
    public boolean readDocId() throws IOException {
        if (m_bufferPosition >= m_bufferLength) {
            // hit the end.
            m_docId = -1;
            return false;
        }
        m_docId = readLong();
        // reset the base offset based upon the current buffer position (we can reset within the posting, not
        // within the whole segment).
        m_offset = m_bufferPosition;
        return true;
    }

    public void set(byte[] array, long offset) {
        m_array = array;
        m_offset = (int) offset;
        m_bufferLength = m_array.length;
        reset();
    }

    public void reset() {
        m_bufferPosition = m_offset;
        readerDocumentId = 0;
    }

    protected byte readByte() throws IOException {

        if (m_bufferPosition >= m_bufferLength) {
            throw new IOException("end of buffer");
        }
        return m_array[m_bufferPosition++];
    }


    /**
     * Reads four bytes and returns an int.
     *
     * @see COutputStream#writeInt(int)
     */
    public final int readInt() throws IOException {
        return ((readByte() & 0xFF) << 24) | ((readByte() & 0xFF) << 16)
                | ((readByte() & 0xFF) << 8) | (readByte() & 0xFF);
    }

    /**
     * Reads four bytes and returns an int.
     *
     * @see COutputStream#writeInt(int)
     */
    public short readShort() throws IOException {
        int result = ((readByte() & 0xFF) << 8) | (readByte() & 0xFF);
        return (short) result;
    }

    /**
     * Reads an int stored in variable-length format.  Reads between one and five bytes.  Smaller values take fewer
     * bytes.  Negative numbers are not supported.
     *
     * @see COutputStream#writeVInt(int)
     */
    public final int readVInt() throws IOException {
        byte b = readByte();
        int i = b & 0x7F;
        for (int shift = 7; (b & 0x80) != 0; shift += 7) {
            b = readByte();
            i |= (b & 0x7F) << shift;
        }
        return i;
    }


    protected void advanceNBytes(int size) {
        m_bufferPosition += size;
    }

    /**
     * Reads eight bytes and returns a long.
     *
     * @see COutputStream#writeLong(long)
     */
    public final long readLong() throws IOException {
        return (((long) readInt()) << 32) | (readInt() & 0xFFFFFFFFL);
    }

    /**
     * Reads a long stored in variable-length format.  Reads between one and nine bytes.  Smaller values take fewer
     * bytes.  Negative numbers are not supported.
     */
    public final long readVLong() throws IOException {
        byte b = readByte();
        long i = b & 0x7F;
        for (int shift = 7; (b & 0x80) != 0; shift += 7) {
            b = readByte();
            i |= (b & 0x7FL) << shift;
        }
        return i;
    }

}

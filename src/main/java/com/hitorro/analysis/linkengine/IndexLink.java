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

import com.hitorro.util.core.UTCDateUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import java.io.IOException;

/**
 * <p/>
 */
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.IndexLink,
        isView = false,
        isPersisted = false,
        schemaVersion = IndexLink.SerialVersion)
public class IndexLink implements HTSerializable, Comparable<IndexLink> {
    public static final int SerialVersion = 1;
    public long m_fromDocId;
    public long m_toDocId;
    public byte m_type;
    public byte m_metric;
    public short m_date;

    public void setHashes(long from, long to) {
        m_fromDocId = from;
        m_toDocId = to;
    }

    public void setDate(long d) {
        m_date = UTCDateUtil.timeMillisToShortDate(d);
    }

    public void setMetric(byte b) {
        m_metric = b;
    }

    public void setType(byte type) {
        m_type = type;
    }

    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        os.writeLong(m_fromDocId);
        os.writeLong(m_toDocId);
        os.writeByte(m_type);
        os.writeByte(m_metric);
        os.writeShort(m_date);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();

        m_fromDocId = os.readLong();
        m_toDocId = os.readLong();
        m_type = os.readByte();
        m_metric = os.readByte();
        m_date = os.readShort();
    }


    public int getSerializationVersion() {
        return SerialVersion;
    }

    public boolean isPersisted() {
        return false;
    }

    public boolean hasGuid() {
        return false;
    }

    public boolean hasSoftGuid() {
        return false;
    }

    public int compareTo(IndexLink indexLink) {
        return 0;
    }
}

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
package com.hitorro.analysis.linkengine.compute;

import com.hitorro.util.core.Console;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import java.io.IOException;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved.
 * <p/>
 * User: chris
 */
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.MemoryConversation,
        isView = false,
        isPersisted = false,
        schemaVersion = ReferringPost.SerialVersion)
public class ReferringPost implements HTSerializable {
    public static final int SerialVersion = 1;
    public String m_guid;
    public long m_hash;
    public String m_moreLink;
    public String m_userGuid;
    public String m_user;
    public String m_forumGuid;
    public String m_forum;

    public ReferringPost(long hash) {
        m_hash = hash;
    }

    public void dump() {
        Console.println(">r> guid: %s, hash: %s, link: %s, user: %s", m_guid, m_hash, m_moreLink, m_user);
    }

    public void serialize(HTObjectOutputStream os) throws IOException {
        os.writeInt(getSerializationVersion());
        os.writeString(m_guid);
        os.writeString(m_moreLink);
        os.writeString(m_userGuid);
        os.writeString(m_user);
        os.writeString(m_forumGuid);
        os.writeString(m_forum);
        os.writeLong(m_hash);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException {
        int version = os.readInt();
        m_guid = os.readString();
        m_moreLink = os.readString();
        m_userGuid = os.readString();
        m_user = os.readString();
        m_forumGuid = os.readString();
        m_forum = os.readString();
        m_hash = os.readLong();
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
}
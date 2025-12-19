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
package com.hitorro.conversationtracker.rss;

import com.hitorro.base.objects.VersionableObject;
import com.hitorro.base.typesystem.btadapter.rssadapters.VersionableObjectRssItem;
import com.hitorro.basedms.xml.RssItemInterface;
import com.hitorro.util.typesystem.Type;
import com.hitorro.util.typesystem.TypeManager;
import com.hitorro.util.typesystem.btadapter.BaseTypeAdapter;

import java.util.Iterator;

/**
 * <p/>
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Oct 4, 2005 Time: 2:46:15 PM
 */
public class AdapterIterator<E extends VersionableObject> implements Iterator<RssItemInterface> {
    private Iterator<E> iterIn;

    public AdapterIterator(Iterator<E> iterIn) {
        this.iterIn = iterIn;
    }

    public boolean hasNext() {
        return iterIn.hasNext();
    }

    public RssItemInterface next() {
        E so = iterIn.next();
        if (so == null) {
            return null;
        }
        Type type = TypeManager.getTypeManager().getTypeFromGuid(so.getGuid());
        if (type == null) {
            return null;
        }
        BaseTypeAdapter<E, RssItemInterface> bta = type.getBTAdapter(VersionableObjectRssItem.AdapterGroup);
        return bta.getObject(so);
    }

    public void remove() {

    }
}


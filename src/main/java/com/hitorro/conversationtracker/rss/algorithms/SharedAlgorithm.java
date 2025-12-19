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
package com.hitorro.conversationtracker.rss.algorithms;

import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.analysis.linkengine.LinkEngine;
import com.hitorro.analysis.linkengine.LinkEngineSingletonMapper;
import com.hitorro.analysis.linkengine.LinkQueryContext;
import com.hitorro.analysis.linkengine.tuple.FilterLinkTupleReader;
import com.hitorro.analysis.linkengine.tuple.LinkTupleReader;
import com.hitorro.analysis.linkengine.tuple.filters.ANDTupleReaderFilter;
import com.hitorro.analysis.linkengine.tuple.filters.DateRangeFilter;
import com.hitorro.analysis.linkengine.tuple.filters.TypeFilter;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.basedms.xml.RssItemInterface;
import com.hitorro.conversationtracker.Log;
import com.hitorro.conversationtracker.rss.IteratorRequest;
import com.hitorro.conversationtracker.rss.RssIteratorGenerator;
import com.hitorro.util.core.Console;
import com.hitorro.util.core.UTCDateUtil;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.btadapter.BaseAdapterItemCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * <p/>
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Oct 4, 2005 Time: 5:15:13 PM
 */
public class SharedAlgorithm implements RssAlgorithm {
    public static Iterator<RssItemInterface> getReferrers(long hash, Date startDate, Date endDate, byte type,
                                                          RssIteratorGenerator generator) throws IOException {
        LinkQueryContext c = null;
        LinkEngine le = LinkEngineSingletonMapper.getSingleton().get();
        short sD = UTCDateUtil.dateToShortDate(startDate);
        short eD = UTCDateUtil.dateToShortDate(endDate);
        DateRangeFilter dateRangeFilter = new DateRangeFilter(sD, eD);
        ANDTupleReaderFilter backwardFilter = new ANDTupleReaderFilter(new TypeFilter((byte) (LinkTupleReader.TypeBit_Inbound | type)),
                dateRangeFilter);

        LinkTupleReader backReader = null;
        List<Long> l = new ArrayList<Long>();
        try {
            c = le.getQueryContext();
            backReader = c.getReaderForHash(hash);
            if (backReader != null) {
                FilterLinkTupleReader reverseFilterReader = new FilterLinkTupleReader(backwardFilter);
                reverseFilterReader.setReader(backReader);
                reverseFilterReader.reset();
                while (reverseFilterReader.readNext()) {
                    long docId = reverseFilterReader.m_docId;
                    l.add(docId);
                    Console.println("DocId: %s", docId);
                }

                RssIterFromHash<RssItemInterface> it = new RssIterFromHash<RssItemInterface>(l.iterator(),
                        generator.getCache());
                return it;
            }
        } finally {
            c.freeExpressionTree(backReader);
            if (c != null) {
                le.release(c);
            }
        }

        return null;
    }

    public String getName() {
        return AlgorithmSharedByQuery;
    }

    public boolean get(JsonNode params, RssIteratorGenerator generator, IteratorRequest req) {
        BaseSession sess = DMSSessionFactory.getFactory().getSession();
        try {
            String guid = RssIteratorGenerator.GuidKey.apply(params);
            if (!StringUtil.nullOrEmptyString(guid)) {
                VersionableObject so = (VersionableObject) sess.getObjectFromGuid(guid);
                if (so != null) {
                    Date s = new Date();
                    Date e = UTCDateUtil.getCalendarLastWeek().getTime();
                    try {
                        generator.setIter(getReferrers(so.getIdentityHash(), s, e,
                                LinkTupleReader.TypeRssShare, generator));
                        return true;
                    } catch (IOException e1) {
                        Log.util.error("Exception %s %e", e1, e1);
                    }
                }
            }
        } finally {
            DMSSessionFactory.getFactory().rollbackClose(sess);
        }
        return false;
    }
}

class RssIterFromHash<OUT> implements Iterator<OUT> {
    private Iterator<Long> iterIn;
    private BaseSession session;
    private boolean flush = true;
    private int count = 0;
    private BaseAdapterItemCache<OUT> cache;

    public RssIterFromHash(Iterator<Long> in, BaseAdapterItemCache<OUT> cache) {
        iterIn = in;
        this.cache = cache;
    }

    public boolean hasNext() {
        return iterIn.hasNext();
    }

    public OUT next() {
        return map();
    }

    public void remove() {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    public void flushAfterOnNext(boolean flag) {
        flush = flag;
    }

    public OUT map() {
        if (session == null) {
            session = DMSSessionFactory.getFactory().getSession();
        }

        while (iterIn.hasNext()) {
            long hash = iterIn.next();

            if (flush) {
                session.rollback();
            }


            String guid = session.getObjectGuidFromHash(hash, false);
            if (guid == null) {
                continue;
            }

            return cache.get(guid);
        }
        return null;
    }

}

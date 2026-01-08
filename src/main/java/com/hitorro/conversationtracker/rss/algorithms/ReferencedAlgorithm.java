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
import com.hitorro.analysis.linkengine.tuple.LinkTupleReader;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.conversationtracker.Log;
import com.hitorro.conversationtracker.rss.IteratorRequest;
import com.hitorro.conversationtracker.rss.RssIteratorGenerator;
import com.hitorro.util.core.Constants;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.typesystem.BaseSession;

import java.io.IOException;
import java.util.Date;

/**
 * <p/>
 */
public class ReferencedAlgorithm implements RssAlgorithm {
    public String getName() {
        return AlgorithmReferencedByQuery;
    }

    public boolean get(JsonNode params, RssIteratorGenerator generator, IteratorRequest req) {

        BaseSession sess = DMSSessionFactory.getFactory().getSession();
        try {
            String guid = RssIteratorGenerator.GuidKey.apply(params);
            if (!StringUtil.nullOrEmptyString(guid)) {
                VersionableObject so = (VersionableObject) sess.getObjectFromGuid(guid);
                if (so != null) {
                    Date s = new Date();
                    Date e = new Date(s.getTime() - Constants.getMillis(RssIteratorGenerator.MaxDaysAgeKey.apply(params), 0, 0, 0));
                    try {
                        generator.setIter(SharedAlgorithm.getReferrers(so.getIdentityHash(), s, e,
                                LinkTupleReader.TypeHRef, generator));
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

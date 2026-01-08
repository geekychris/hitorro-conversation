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
import com.hitorro.base.objects.User;
import com.hitorro.base.objects.UserMark;
import com.hitorro.base.objects.UserPreferences;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.base.typesystem.btadapter.rssadapters.VersionableObjectRssItem;
import com.hitorro.basedms.SORetrievalAdapter;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.basedms.session.GuidListResultObjectAdapterDriver;
import com.hitorro.basedms.xml.RssItemInterface;
import com.hitorro.conversationtracker.rss.IteratorRequest;
import com.hitorro.conversationtracker.rss.RssIteratorGenerator;
import com.hitorro.util.core.string.StringBuilderUtil;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.typesystem.BaseSession;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class BookmarksAlgorithm implements RssAlgorithm {
    public String getName() {
        return AlgorithmBookmarks;
    }

    public boolean get(JsonNode params, RssIteratorGenerator generator, IteratorRequest req) {
        StringBuilder sb = new StringBuilder();
        String user = RssIteratorGenerator.UserKey.apply(params);

        if (!StringUtil.nullOrEmptyOrBlankString(user)) {
            StringBuilderUtil.appendIfNotNull(sb, "user: ", user, " and ");
            BaseSession sess = DMSSessionFactory.getFactory().getSession();
            List<String> guids = new ArrayList<String>();

            try {
                User u = User.getUserForName(sess, user);
                UserPreferences up = u.getUserPreferences();
                for (UserMark um : up.getUserMarks()) {
                    guids.add(um.getId());
                }
            } finally {
                DMSSessionFactory.getFactory().rollbackClose(sess);
            }
            generator.setAdapter(new SORetrievalAdapter<VersionableObject, RssItemInterface>(VersionableObjectRssItem.AdapterGroup, generator.getCache()));
            generator.getAdapter().flushAfterOnNext(true);
            generator.setTitle(sb.toString());
            generator.setIter(new GuidListResultObjectAdapterDriver(generator.getAdapter(), guids.iterator()));
            return true;
        }
        return false;
    }
}

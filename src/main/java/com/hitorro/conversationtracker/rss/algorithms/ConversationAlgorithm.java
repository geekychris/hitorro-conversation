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
import com.hitorro.analysis.linkengine.compute.ConversationRankerContext;
import com.hitorro.analysis.linkengine.compute.RankByParticipantSpread;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.base.typesystem.btadapter.rssadapters.VersionableObjectRssItem;
import com.hitorro.basedms.SORetrievalAdapter;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.basedms.xml.RssItemInterface;
import com.hitorro.conversationtracker.Conversation;
import com.hitorro.conversationtracker.rss.AdapterIterator;
import com.hitorro.conversationtracker.rss.IteratorRequest;
import com.hitorro.conversationtracker.rss.RssIteratorGenerator;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringBuilderUtil;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTSerializable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p/>
 */
public class ConversationAlgorithm implements RssAlgorithm {
    private int MaxConversationCount = 20;

    public String getName() {
        return AlgorithmConversationQuery;
    }

    public boolean get(JsonNode params, RssIteratorGenerator generator, IteratorRequest req) {
        StringBuilder sb = new StringBuilder();
        String guid = RssIteratorGenerator.GuidKey.apply(params);
        if (StringUtil.nullOrEmptyString(guid)) {
            StringBuilderUtil.appendIfNotNull(sb, "all conversations ", "", " and ");
            List<Conversation> convs = getConversations(generator.getSession());
            generator.setIter(new AdapterIterator(convs.iterator()));
        } else {
            HTSerializable pts = generator.getSession().getObjectFromGuid(guid);
            if (pts instanceof Conversation) {
                Conversation conv = (Conversation) pts;
                StringBuilderUtil.appendIfNotNull(sb, "Conversation: ", conv.getTitle(), " and ");
            }
            String s = Fmt.S("select post.guid from Conversation conv join conv.posts post where conv.guid='%s' order by cnv.lastPostDate desc", guid);

            generator.setAdapter(new SORetrievalAdapter<VersionableObject, RssItemInterface>(VersionableObjectRssItem.AdapterGroup, generator.getCache()));
            generator.getAdapter().flushAfterOnNext(true);
            generator.setTitle(sb.toString());
            generator.setIter(((DMSSession) generator.getSession()).getIteratorFromQuery(generator.getAdapter(), s));
        }
        return true;
    }

    private List<Conversation> getConversations(BaseSession session) {
        List<Conversation> list = getConversationObjects(session);
        ConversationRankerContext context = ConversationRankerContext.getContext();
        RankByParticipantSpread ranker = new RankByParticipantSpread();
        list = context.rank(list, RankByParticipantSpread.Key);
        return list;
    }

    private List<Conversation> getConversationObjects(BaseSession session) {
        List<Conversation> cnvs = new ArrayList<Conversation>();

        Iterator itr = session.getIteratorFromQuery("select guid from Conversation order by lastPostDate desc");
        int count = 0;
        while (itr.hasNext() && count < MaxConversationCount) {
            count++;
            String guid = (String) itr.next();
            Conversation conv = (Conversation) session.getObjectFromGuid(guid);
            if (conv.getPrimaryPersonalityWithFailover() != null) {
                cnvs.add(conv);
            }
        }

        return cnvs;
    }

}

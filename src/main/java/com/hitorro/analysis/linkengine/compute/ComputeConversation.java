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

import gnu.trove.map.hash.TIntIntHashMap;
import com.hitorro.analysis.linkengine.LinkEngine;
import com.hitorro.analysis.linkengine.LinkEngineSingletonMapper;
import com.hitorro.analysis.linkengine.LinkQueryContext;
import com.hitorro.analysis.linkengine.tuple.CountDistinctId;
import com.hitorro.analysis.linkengine.tuple.FilterLinkTupleReader;
import com.hitorro.analysis.linkengine.tuple.LinkTupleReader;
import com.hitorro.analysis.linkengine.tuple.filters.ANDTupleReaderFilter;
import com.hitorro.analysis.linkengine.tuple.filters.DateRangeFilter;
import com.hitorro.analysis.linkengine.tuple.filters.TypeFilter;
import com.hitorro.base.objects.Post;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.basetext.indexer.SearcherCache;
import com.hitorro.basetext.termmatch.TermMatchQuery;
import com.hitorro.conversationtracker.Conversation;
import com.hitorro.obj.core.Log;
import com.hitorro.obj.core.LuceneResultIterator;
import com.hitorro.obj.core.LuceneUtils;
import com.hitorro.util.core.Console;
import com.hitorro.util.core.Timer;
import com.hitorro.util.core.UTCDateUtil;
import com.hitorro.util.core.queue.PriorityQueue;
import com.hitorro.util.html.Link;
import com.hitorro.util.html.LinkSet;
import com.hitorro.util.typesystem.BaseSession;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;
import java.util.*;

/**
 * <p/>
 * Compute a conversation from a listFiles of seed nodes.
 */
public class ComputeConversation {

    public static final String BasicAreaToPostQuery = " from User as user, RssFeedIn as feed, Forum as forum, SubjectArea as area, Post as post left join post.containers as p " +
            "where user.rssFeedIn = feed and " +
            "feed.forum = forum and " +
            "p.id = forum.id and " +
            "forum.area = area and area.name= :a and post.authoredDate >= ? and post.authoredDate <= ?";

    public static final String BasicAreaToPostQuery_Count = "select count(*)" + BasicAreaToPostQuery;

    public static final String BasicAreaToPostQuery_PostHash_Forum = "select post.identityHash, post.moreLink, forum.name" + BasicAreaToPostQuery;

    public List<MemoryConversation> getConversationsSimilarTo(TermMatchQuery query,
                                                              int maxTerms,
                                                              int maxDocsFromLucene,
                                                              int minParticipants,
                                                              int maxConversations,
                                                              float minRank,
                                                              String subjectArea)
            throws IOException {
        List<String> termsUsed = new ArrayList<String>();
        BooleanQuery.Builder q = LuceneUtils.getBooleanQueryFromTermMatchQuery(query,
                maxTerms,
                termsUsed, "all", "contenttext", BooleanClause.Occur.SHOULD);

        String indexName = "fs_store1";
        IndexSearcher s = SearcherCache.getCache().get(indexName);
        if (s == null) {
            return null;
        }
        QueryParser qp = LuceneUtils.getQueryParser();
        if (qp == null) {
            return null;
        }
        PostHashToForumNameLuceneAdapter adapter = new PostHashToForumNameLuceneAdapter();
        Iterator<PostHashToForumName> iter = new LuceneResultIterator(adapter,
                maxDocsFromLucene, minRank, s, q.build());

        return computeFromPostHashToForumNameIterator(subjectArea, iter, minParticipants, maxConversations, null, null);
    }

    public int computeFromSubjectAreaCreatePersistedConversation(String area,
                                                                 Date startDate,
                                                                 Date endDate,
                                                                 int minPosters,
                                                                 int maxPriorityQueueSize) throws IOException {

        List<MemoryConversation> list = computeFromSubjectArea(area, startDate,
                endDate, minPosters,
                maxPriorityQueueSize);

        for (MemoryConversation conv : list) {
            conv.createOrUpdatePersistedConv(startDate, endDate);
        }
        return list.size();
    }


    public List<MemoryConversation> computeFromSubjectArea(String area,
                                                           Date startDate,
                                                           Date endDate,
                                                           int minPosters,
                                                           int maxPriorityQueueSize)
            throws IOException {
        BaseSession session = DMSSessionFactory.getFactory().getSession();
        try {
            PostHashToForumNameAdapter adapter = new PostHashToForumNameAdapter();
            Iterator<PostHashToForumName> iter = ((DMSSession) session).getIteratorFromQuery(adapter, BasicAreaToPostQuery_PostHash_Forum, area, startDate, endDate);
            return computeFromPostHashToForumNameIterator(area, iter, minPosters, maxPriorityQueueSize, startDate, endDate);
        } finally {
            DMSSessionFactory.getFactory().rollbackClose(session);
        }
    }

    /**
     * Compute a listFiles of in memory conversations from an iterator of hashes.
     *
     * @param area
     * @param iter
     * @param minPosters
     * @param maxPriorityQueueSize
     * @return
     * @throws IOException
     */
    public List<MemoryConversation> computeFromPostHashToForumNameIterator(String area,
                                                                           Iterator<PostHashToForumName> iter,
                                                                           int minPosters,
                                                                           int maxPriorityQueueSize,
                                                                           Date startDate,
                                                                           Date endDate)
            throws IOException {
        LinkQueryContext c = null;
        LinkEngine le = LinkEngineSingletonMapper.getSingleton().get();

        try {
            List<MemoryConversation> convList = new ArrayList<MemoryConversation>();

            if (iter != null) {

                List<PostHashToForumName> list = new ArrayList<PostHashToForumName>();
                c = le.getQueryContext();
                Timer t = new Timer();
                CountDistinctId countDistinct = new CountDistinctId();

                TIntIntHashMap map = new TIntIntHashMap();
                short sD = UTCDateUtil.dateToShortDate(startDate);
                short eD = UTCDateUtil.dateToShortDate(endDate);
                DateRangeFilter dateRangeFilter = new DateRangeFilter(sD, eD);
                ANDTupleReaderFilter forwardFilter = new ANDTupleReaderFilter(new TypeFilter(LinkTupleReader.TypeHRef), dateRangeFilter);
                ANDTupleReaderFilter backwardFilter = new ANDTupleReaderFilter(new TypeFilter((byte) (LinkTupleReader.TypeBit_Inbound | LinkTupleReader.TypeHRef)),
                        dateRangeFilter);
                FilterLinkTupleReader forwardFilterReader = new FilterLinkTupleReader(forwardFilter);

                FilterLinkTupleReader reverseFilterReader = new FilterLinkTupleReader(backwardFilter);
                countDistinct.set(reverseFilterReader);
                DocIdToFreqSet set = new DocIdToFreqSet(maxPriorityQueueSize);
                while (iter.hasNext()) {
                    PostHashToForumName p = iter.next();
                    LinkTupleReader reader = c.getReaderForHash(p.getHash());
                    if (reader != null) {
                        forwardFilterReader.setReader(reader);
                        forwardFilterReader.reset();
                        while (forwardFilterReader.readNext()) {
                            LinkTupleReader backReader = c.getReaderForDocId(forwardFilterReader.readerDocumentId);
                            if (backReader != null) {
                                reverseFilterReader.setReader(backReader);
                                reverseFilterReader.reset();
                                int count = countDistinct.getInt();
                                if (count > minPosters) {
                                    set.add(forwardFilterReader.readerDocumentId, count);
                                    count++;
                                }
                                // count of things pointing to this article
                                c.freeExpressionTree(backReader);
                            }
                        }
                    }
                    c.freeExpressionTree(reader);
                }
                t.stop();

                PriorityQueue<DocIdToFreq> pq = set.getPriorityQueue();

                // XXX TODO TEMPORARY REMOVAL
                deleteConversations();

                // now we have a prioritized listFiles of documents that have n posts pointing to it.
                // we now want to get the posts that point to that and possibly the person associated with that post.
                //
                createMemoryConversationsFromPriorityQueue(pq, convList, le, c, reverseFilterReader);
            }
            return convList;
        } finally {
            if (c != null) {
                le.release(c);
            }
        }
    }

    private void createMemoryConversationsFromPriorityQueue(PriorityQueue<DocIdToFreq> pq,
                                                            List<MemoryConversation> convList,
                                                            LinkEngine le,
                                                            LinkQueryContext c,
                                                            FilterLinkTupleReader reverseFilterReader)
            throws IOException {
        BaseSession session = DMSSessionFactory.getFactory().getSession();
        try {
            while (pq.size() > 0) {
                DocIdToFreq p = pq.pop();


                session.rollback();
                Post post = Post.getPostByMoreLinkHash(session, p.m_docId);

                if (post != null) {
                    MemoryConversation conv = new MemoryConversation();
                    conv.setHash(p.m_docId);
                    convList.add(conv);
                    conv.setPostDetails(post);
                    LinkTupleReader backReader = c.getReaderForDocId(p.m_docId);

                    if (backReader != null) {
                        reverseFilterReader.setReader(backReader);
                        reverseFilterReader.reset();
                        while (reverseFilterReader.readNext()) {

                            conv.addReferrer(new ReferringPost(reverseFilterReader.readerDocumentId));
                        }
                    }
                } else {
                    Log.util.error("Cant find hash: %s", p.m_docId);
                }
            }
        } finally {
            DMSSessionFactory.getFactory().rollbackClose(session);
        }
    }

    private void deleteConversations() {
        BaseSession qSession = DMSSessionFactory.getFactory().getSession();
        try {
            Iterator<Conversation> iter = qSession.getIteratorFromQuery("from Conversation");
            while (iter.hasNext()) {
                Conversation conv = iter.next();
                conv.delete(qSession);
            }
        } finally {
            DMSSessionFactory.getFactory().commitAndClose(qSession);
        }
    }

    private void dumpPost(long hash) {
        BaseSession session = DMSSessionFactory.getFactory().getSession();
        try {
            Post p = Post.getPostByMoreLinkHash(session, hash);
            if (p != null) {
                LinkSet ls = p.getReferenceLinksNew();
                if (ls != null) {
                    List<Link> links = ls.getLinks();
                    for (Link link : links) {
                        Console.println("Links to: %s to %s", link.getSourceUrl(), link.getUrl());
                    }
                }
            }
        } finally {
            DMSSessionFactory.getFactory().rollbackClose(session);
        }

    }
}


class CountComparator implements Comparator<DocIdToFreq> {

    public int compare(DocIdToFreq a, DocIdToFreq b) {
        return a.m_freq - b.m_freq;
    }
}









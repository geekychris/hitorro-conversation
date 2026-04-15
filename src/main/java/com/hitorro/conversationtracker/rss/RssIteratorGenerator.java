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

import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.base.typesystem.btadapter.rssadapters.VersionableObjectRssItem;
import com.hitorro.basedms.SORetrievalAdapter;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.basedms.session.GuidListResultObjectAdapterDriver;
import com.hitorro.basedms.xml.RssItemInterface;
import com.hitorro.basetext.indexer.IndexerFieldAdapter;
import com.hitorro.conversationtracker.rss.algorithms.BookmarksAlgorithm;
import com.hitorro.conversationtracker.rss.algorithms.ConversationAlgorithm;
import com.hitorro.conversationtracker.rss.algorithms.PostAlgorithm;
import com.hitorro.conversationtracker.rss.algorithms.ReferencedAlgorithm;
import com.hitorro.conversationtracker.rss.algorithms.RssAlgorithm;
import com.hitorro.conversationtracker.rss.algorithms.SharedAlgorithm;
import com.hitorro.conversationtracker.rss.algorithms.ShowAlgorithm;
import com.hitorro.conversationtracker.rss.algorithms.SimilarAlgorithm;
import com.hitorro.conversationtracker.rss.algorithms.TextAlgorithm;
import com.hitorro.conversationtracker.rss.algorithms.*;
import com.hitorro.obj.core.LuceneUtils;
import com.hitorro.util.core.Constants;
import com.hitorro.util.core.ListUtil;
import com.hitorro.util.core.iterator.FilteringIterator;
import com.hitorro.util.core.opers.HTPredicate;
import com.hitorro.util.core.opers.LogicalAndOperator;
import com.hitorro.util.json.keys.BooleanProperty;
import com.hitorro.util.json.keys.IntegerProperty;
import com.hitorro.util.json.keys.StringProperty;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;
import com.hitorro.util.typesystem.btadapter.BaseAdapterItemCache;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.util.BytesRef;

import java.util.*;

/**
 * To get a window (windows are counted from):
 * <p/>
 * rss: http://localhost:8072/external/rssquery?maxresults=10&windownumber=1 html:
 * http://localhost:8072/external/rssquery?html=true&maxresults=10&windownumber=1
 * <p/>
 * This returns 10 results from the second window
 * <p/>
 * To get a single element:
 * <p/>
 * http://localhost:8072/external/rssquery?algorithm=show&guid=34:4bd9f:fe8f7
 * <p/>
 * To get a listFiles of top keywords used in the text:
 * <p/>
 * http://localhost:8072/external/rssquery?algorithm=show&guid=34:4bd9f:fe8f7&topwords=n
 * <p/>
 * where n is the amount of words.
 * <p/>
 * Simple text search:
 * <p/>
 * http://localhost:8072/external/rssquery?html=true&algorithm=text&body=robert+scoble+iphone&title=bla
 * <p/>
 * just HTPosts:
 * <p/>
 * http://localhost:8072/external/rssquery?algorithm=text&html=true&body=robert+scoble&objecttype=3c
 */
public class RssIteratorGenerator {
    public static final String OBKAuthoredDate = "authoredDate";
    public static final String OBKCreatedDate = "createdDate";

    public static IntegerProperty MaxDegreeKey = new IntegerProperty("maxdegree", "Maximum results to return", 1);

    public static StringProperty TitleSearchKey = new StringProperty("title", "title", null);
    public static StringProperty BodySearchKey = new StringProperty("body", "body", null);

    public static StringProperty ObjectTypeKey = new StringProperty("objecttype", "objecttype", TypeClassMetaInfo.Post);

    public static StringProperty ForumKey = new StringProperty("forum", "ForumName", "TechOne");
    public static StringProperty AuthorKey = new StringProperty("author", "author", null);
    public static StringProperty CategoryKey = new StringProperty("category", "author", null);

    public static StringProperty CelebKey = new StringProperty("celeb", "Celebrity Name", "Robert");
    public static StringProperty AlgorithmKey = new StringProperty("algorithm", "Algorithm name", RssAlgorithm.AlgorithmPostQuery);
    public static StringProperty GuidKey = new StringProperty("guid", "guid of a document driven query", null);
    public static StringProperty OrderByKey = new StringProperty("orderby", "authoredDate|modfiedDate", OBKAuthoredDate);

    public static StringProperty UserKey = new StringProperty("user", "Username", null);


    public static BooleanProperty IncludeSubtypeKey = new BooleanProperty("includesubtype", "", false);

    public static BooleanProperty DebugKey = new BooleanProperty("debug", "", false);
    public static BooleanProperty HtmlKey = new BooleanProperty("html", "", false);
    public static BooleanProperty FullKey = new BooleanProperty("full", "", false);
    public static IntegerProperty MaxResultsKey = new IntegerProperty("maxresults", "Maximum results to return", 20);
    public static IntegerProperty ResultsWindowKey = new IntegerProperty("windownumber", "result window to return", 0);
    public static IntegerProperty MaxDaysAgeKey = new IntegerProperty("maxpostage", "maximum age of post", 2);

    public static IntegerProperty TopWordsKey = new IntegerProperty("topwords", "top n terms in text", 0);

    public static ContainsFilter cdataConstraint = new ContainsFilter("<![CDATA[");


    private static final Map<String, RssAlgorithm> algorithms = initAlgorithms();

    private BaseSession session;
    private HTPredicate<RssItemInterface> constraint = null;
    private SORetrievalAdapter<VersionableObject, RssItemInterface> adapter = null;
    private Iterator<RssItemInterface> iter = null;

    private BaseAdapterItemCache cache;

    private String title;
    private List<HTPredicate<RssItemInterface>> constraints = null;

    public RssIteratorGenerator(BaseAdapterItemCache cache) {
        this.setCache(cache);
    }

    private static Map<String, RssAlgorithm> initAlgorithms() {
        Map<String, RssAlgorithm> algorithms = new HashMap<String, RssAlgorithm>();
        addAlgo(algorithms, new SimilarAlgorithm());
        addAlgo(algorithms, new TextAlgorithm());
        addAlgo(algorithms, new PostAlgorithm());
        addAlgo(algorithms, new ConversationAlgorithm());
        addAlgo(algorithms, new ReferencedAlgorithm());
        addAlgo(algorithms, new SharedAlgorithm());
        addAlgo(algorithms, new ShowAlgorithm());
        addAlgo(algorithms, new BookmarksAlgorithm());
        return algorithms;
    }

    private static void addAlgo(Map<String, RssAlgorithm> map, RssAlgorithm algo) {
        map.put(algo.getName(), algo);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Iterator<RssItemInterface> generate(JsonNode params, IteratorRequest req) {
        setSession(DMSSessionFactory.getFactory().getSession());

        String algorithm = AlgorithmKey.apply(params);

        RssAlgorithm algo = algorithms.get(algorithm);
        if (algo != null) {
            if (!algo.get(params, this, req)) {
                return null;
            }
        }
        // always put the cdata constraint.
        appendConstraint(cdataConstraint);

        if (getConstraint() != null) {
            setIter(new FilteringIterator(getIter(), getConstraint()));
        }
        if (TopWordsKey.apply(params) > 0) {
            setIter(new TopWordsIterator(getIter(), TopWordsKey.apply(params)));
        }


        req.setIter(getIter());
        return getIter();
    }

    public void addLuceneDateRange(JsonNode params, BooleanQuery.Builder q) {
        Date endDate = new Date();
        int days = MaxDaysAgeKey.apply(params);
        Date startDate = new Date(endDate.getTime() - Constants.getMillis(days, 0, 0, 0));
        String fieldName = VersionableObject.AuthoredDateKey;
        String lowerVal = DateTools.dateToString(startDate, IndexerFieldAdapter.IndexingDateResolution);
        String upperVal = DateTools.dateToString(endDate, IndexerFieldAdapter.IndexingDateResolution);
        // this was changed from another query type....not sure if this will work correctly!!!
        //String field, BytesRef lowerTerm, BytesRef upperTerm, boolean includeLower, boolean includeUpper
        TermRangeQuery dateQuery = new TermRangeQuery(fieldName,
                new BytesRef(lowerVal.getBytes()),
                new BytesRef(upperVal.getBytes()),
                true, true);

        q.add(dateQuery, BooleanClause.Occur.MUST);
    }

    public BooleanQuery.Builder getBasicLuceneParams(JsonNode params) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        String objectType = ObjectTypeKey.apply(params);
        boolean includeSubtype = IncludeSubtypeKey.apply(params);
        LuceneUtils.addTypeMustBeClassOrSubclassOf(builder, objectType, includeSubtype);

        // getDegree 0
        LuceneUtils.addMaxPostdegree(builder, MaxDegreeKey.apply(params));
        return builder;
    }

    public void iteratorFromGuidList(List<String> l) {
        setAdapter(new SORetrievalAdapter<VersionableObject, RssItemInterface>(VersionableObjectRssItem.AdapterGroup, getCache()));
        getAdapter().flushAfterOnNext(true);
        setIter(new GuidListResultObjectAdapterDriver(adapter, l.iterator()));
    }

    public void close() {
        DMSSessionFactory.getFactory().rollbackClose(getSession());
        if (getAdapter() != null) {
            getAdapter().close();
        }
    }

    public BaseSession getSession() {
        return session;
    }

    public void setSession(BaseSession session) {
        this.session = session;
    }

    public HTPredicate<RssItemInterface> getConstraint() {
        if (constraint != null) {
            return constraint;
        }
        if (ListUtil.nullOrEmpty(constraints)) {
            return null;
        }
        HTPredicate<RssItemInterface> arr[] = constraints.toArray(new HTPredicate[constraints.size()]);
        constraint = new LogicalAndOperator<RssItemInterface>(arr);
        return constraint;
    }

    public void appendConstraint(HTPredicate<RssItemInterface> constraint) {
        if (constraints == null) {
            constraints = new ArrayList<HTPredicate<RssItemInterface>>();
        }
        constraints.add(constraint);
        constraint = null;
    }

    public SORetrievalAdapter<VersionableObject, RssItemInterface> getAdapter() {
        return adapter;
    }

    public void setAdapter(SORetrievalAdapter<VersionableObject, RssItemInterface> adapter) {
        this.adapter = adapter;
    }

    public Iterator<RssItemInterface> getIter() {
        return iter;
    }

    public void setIter(Iterator<RssItemInterface> iter) {
        this.iter = iter;
    }

    public BaseAdapterItemCache getCache() {
        return cache;
    }

    public void setCache(BaseAdapterItemCache cache) {
        this.cache = cache;
    }
}
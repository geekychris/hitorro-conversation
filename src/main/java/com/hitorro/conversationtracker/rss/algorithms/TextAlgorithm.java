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
import com.hitorro.base.objects.Category;
import com.hitorro.base.objects.Post;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.base.typesystem.btadapter.rssadapters.VersionableObjectRssItem;
import com.hitorro.basedms.SORetrievalAdapter;
import com.hitorro.basedms.session.GuidListResultObjectAdapterDriver;
import com.hitorro.basedms.xml.RssItemInterface;
import com.hitorro.basetext.indexer.SearcherCache;
import com.hitorro.conversationtracker.Log;
import com.hitorro.conversationtracker.rss.IteratorRequest;
import com.hitorro.conversationtracker.rss.ResultMapper;
import com.hitorro.conversationtracker.rss.RssIteratorGenerator;
import com.hitorro.obj.core.LuceneResultIterator;
import com.hitorro.obj.core.LuceneUtils;
import com.hitorro.util.core.string.StringBuilderUtil;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.objects.DomainValue;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

import java.io.IOException;

/**
 * <p/>
 */
public class TextAlgorithm implements RssAlgorithm {
    public String getName() {
        return TextQuery;
    }

    public boolean get(JsonNode params, RssIteratorGenerator generator, IteratorRequest req) {
        String title = RssIteratorGenerator.TitleSearchKey.apply(params);
        String body = RssIteratorGenerator.BodySearchKey.apply(params);
        String auth = RssIteratorGenerator.AuthorKey.apply(params);
        String category = RssIteratorGenerator.CategoryKey.apply(params);

        BooleanQuery.Builder q1 = generator.getBasicLuceneParams(params);

        String indexName = "fs_store1";
        IndexSearcher s = SearcherCache.getCache().get(indexName);
        if (s != null) {
            QueryParser qp = LuceneUtils.getQueryParser();
            if (qp != null) {
                StringBuilder sb = new StringBuilder();
                try {
                    BooleanQuery.Builder q = new BooleanQuery.Builder();
                    generator.addLuceneDateRange(params, q);
                    ResultMapper mapper = new ResultMapper();
                    if (!StringUtil.nullOrEmptyString(title) || !StringUtil.nullOrEmptyString(body)) {
                        StringBuilderUtil.appendIfNotNull(sb, "title: ", title, " and ");
                        StringBuilderUtil.appendIfNotNull(sb, "body: ", body, " and ");
                        BooleanQuery.Builder qPrime = LuceneUtils.getBooleanQueryFreeText("title", title, "contenttext", body, BooleanClause.Occur.SHOULD);
                        q.add(qPrime.build(), BooleanClause.Occur.MUST);
                    }
                    if (!StringUtil.nullOrEmptyString(auth)) {

                        StringBuilderUtil.appendIfNotNull(sb, "author: ", auth, " and ");
                        //Query pq = LuceneUtils.getPhraseQuery(Post.OriginalAuthorKey, auth);
                        BooleanQuery.Builder pq = LuceneUtils.getQuery(Post.OriginalAuthorKey, auth, BooleanClause.Occur.MUST);
                        if (pq != null) {
                            q.add(pq.build(), BooleanClause.Occur.MUST);
                        }
                    }
                    if (!StringUtil.nullOrEmptyString(category)) {                                   //
                        // We have a category
                        StringBuilderUtil.appendIfNotNull(sb, "category: ", category, " and ");
                        BooleanQuery.Builder a1 = new BooleanQuery.Builder();
                        Query ptec = LuceneUtils.getStringLiteralQuery(VersionableObject.CategoriesKey,
                                DomainValue.getConcatinated(Category.HTEXTERNALCATEGORY, category), false);
                        Query ptc = LuceneUtils.getStringLiteralQuery(VersionableObject.CategoriesKey,
                                DomainValue.getConcatinated(Category.HTCATEGORY, category), false);
                        a1.add(ptec, BooleanClause.Occur.SHOULD);
                        a1.add(ptc, BooleanClause.Occur.SHOULD);
                        q.add(a1.build(), BooleanClause.Occur.MUST);
                    }
                    // XXX TODO
                    q.add(q1.build(), BooleanClause.Occur.MUST);
                    LuceneResultIterator luceneIter = new LuceneResultIterator(mapper, -1, (float) 0.0, s, q.build());


                    generator.setAdapter(new SORetrievalAdapter<VersionableObject, RssItemInterface>(VersionableObjectRssItem.AdapterGroup, generator.getCache()));
                    generator.getAdapter().flushAfterOnNext(true);
                    generator.setTitle(sb.toString());
                    generator.setIter(new GuidListResultObjectAdapterDriver(generator.getAdapter(), luceneIter));
                    return true;

                } catch (IOException e) {
                    Log.conversationtracker.error("Could not search index %s, %s %e", indexName, e, e);
                }
            }
        }
        return false;
    }
}

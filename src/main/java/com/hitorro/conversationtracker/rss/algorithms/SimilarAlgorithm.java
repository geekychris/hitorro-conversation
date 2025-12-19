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
import com.hitorro.basetext.termmatch.GuidTermMatchQuery;
import com.hitorro.basetext.termmatch.Ranker;
import com.hitorro.basetext.termmatch.RankerResult;
import com.hitorro.basetext.termmatch.RankerResultRow;
import com.hitorro.conversationtracker.rss.IteratorRequest;
import com.hitorro.conversationtracker.rss.RssIteratorGenerator;
import com.hitorro.util.core.string.StringBuilderUtil;
import com.hitorro.util.core.string.StringUtil;
import org.apache.lucene.search.BooleanQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * <p/>
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Oct 4, 2005 Time: 5:07:16 PM
 */
public class SimilarAlgorithm implements RssAlgorithm {
    public String getName() {
        return "similar";
    }

    public boolean get(JsonNode params, RssIteratorGenerator generator, IteratorRequest req) {
        BooleanQuery.Builder q1 = generator.getBasicLuceneParams(params);

        generator.addLuceneDateRange(params, q1);
        String guid = RssIteratorGenerator.GuidKey.apply(params);

        if (!StringUtil.nullOrEmptyString(guid)) {
            StringBuilder sb = new StringBuilder();
            GuidTermMatchQuery query = new GuidTermMatchQuery();
            query.setQuery(guid);
            if (guid.indexOf(":") == -1) {
                query.setPostId(true);
            }
            query.setMasterQuery(q1.build());

            Ranker ranker = new Ranker();

            RankerResult result = ranker.rank(query, RssIteratorGenerator.MaxResultsKey.apply(params), 0.3F, 5);
            if (result == null) {
                return false;
            }
            if (!StringUtil.nullOrEmptyString(result.getError())) {
                return false;
            }
            List<RankerResultRow> rows = result.getRows();
            List<String> l = new ArrayList<String>();
            for (RankerResultRow row : rows) {
                l.add(row.getGuid());
            }
            generator.iteratorFromGuidList(l);
            StringBuilderUtil.appendIfNotNull(sb, "Similar to ", query.getTitle(), " and ");
            generator.setTitle(sb.toString());
        }
        return true;
    }
}

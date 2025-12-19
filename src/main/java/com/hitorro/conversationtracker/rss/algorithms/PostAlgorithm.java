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
import com.hitorro.base.objects.Forum;
import com.hitorro.base.objects.Reference;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.base.typesystem.btadapter.rssadapters.VersionableObjectRssItem;
import com.hitorro.basedms.SORetrievalAdapter;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.basedms.xml.RssItemInterface;
import com.hitorro.conversationtracker.rss.IteratorRequest;
import com.hitorro.conversationtracker.rss.MinDateSize;
import com.hitorro.conversationtracker.rss.MinDescriptionSize;
import com.hitorro.conversationtracker.rss.RssIteratorGenerator;
import com.hitorro.util.core.opers.LogicalAndOperator;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringBuilderUtil;
import com.hitorro.util.core.string.StringUtil;

/**
 * <p/>
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Oct 4, 2005 Time: 5:16:47 PM
 */
public class PostAlgorithm implements RssAlgorithm {
    private static final int PostCacheTime = 120;

    public String getName() {
        return AlgorithmPostQuery;
    }

    public boolean get(JsonNode params, RssIteratorGenerator generator, IteratorRequest req) {
        StringBuilder sb = new StringBuilder();
        generator.appendConstraint(new LogicalAndOperator<RssItemInterface>(new MinDescriptionSize(), new MinDateSize(RssIteratorGenerator.MaxDaysAgeKey.apply(params))));
        String orderBy = null;
        String obk = RssIteratorGenerator.OrderByKey.apply(params);
        String author = RssIteratorGenerator.AuthorKey.apply(params);
        String s = null;
        if (!StringUtil.nullOrEmptyOrBlankString(author)) {
            StringBuilderUtil.appendIfNotNull(sb, "author: ", author, " and ");
            String postType = "Post";
            s = Fmt.S("select guid from %s where originalAuthor='%s' order by creationDate desc", postType, StringUtil.replace(author, "'", "''"));
        } else {
            // normal "forum" query, decending in time.
            String forumName = RssIteratorGenerator.ForumKey.apply(params);
            Forum forum = Forum.getForumForName(generator.getSession(), forumName);
            StringBuilderUtil.appendIfNotNull(sb, "Recent in ", forumName, " and ");
            //String s = Fmt.S("select post.guid from Forum as forum, Post as post left join post.containers as p where p.id = forum.id and forum.name ='%s' %s", forumName, orderBy);
            if (forum != null) {
                s = Fmt.S("select fromGuid from Reference where refType='%s' and toGuid='%s' order by date desc", Reference.ShareReference, forum.getGuid());
            } else {
                return false;
            }
        }


        generator.setAdapter(new SORetrievalAdapter<VersionableObject, RssItemInterface>(VersionableObjectRssItem.AdapterGroup, generator.getCache()));
        generator.getAdapter().flushAfterOnNext(true);
        generator.setTitle(sb.toString());
        generator.setIter(((DMSSession) generator.getSession()).getIteratorFromQuery(generator.getAdapter(), s));
        req.setCacheTimeInMillis(PostCacheTime);
        return true;
    }
}

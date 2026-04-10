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
package com.hitorro.conversationtracker.feeds;

import com.hitorro.base.objects.Forum;
import com.hitorro.base.objects.Post;
import com.hitorro.basedms.csvconsumers.PostDumpCSVConsumer;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.Console;
import com.hitorro.util.core.Constants;
import com.hitorro.util.core.Env;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.csv.CSVFileWriter;
import com.hitorro.util.io.csv.CSVWriter;
import com.hitorro.util.job.Job;
import com.hitorro.util.job.JobExecutionResult;
import com.hitorro.util.job.JobParameters;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.BaseSessionFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;


public class WritePostsToCSVJob extends Job {
    public static final String JobName = "writeposts";
    private static final String ColumnNames[] = {PostDumpCSVConsumer.TitleColumn,
            PostDumpCSVConsumer.LinkColumn,
            PostDumpCSVConsumer.DateColumn,
            PostDumpCSVConsumer.FeedNameColumn,
            PostDumpCSVConsumer.BodyColumn};
    private static final String PostQuery = "select pst.title, pst.moreLink, pst.authoredDate, forum.name, pst.guid from " +
            Post.class.getCanonicalName() + " as pst, " +
            Forum.class.getCanonicalName() + " as forum " +
            "where pst.owningContainer = forum " +
            "and pst.state = :a and forum.state = :b " +
            "order by forum.name, pst.moreLink, pst.authoredDate";

    /**
     * This job must run on the db leader.
     *
     * @return
     */
    public boolean getMustRunOnDBLeader() {
        return true;
    }

    public String getName() {
        return JobName;
    }

    public boolean needsSession() {
        return true;
    }

    public JobExecutionResult doAction(JobParameters parameters) {
        // no parameters - just write out all the files to a fixed location
        BaseSession session = getSession();

        // get session for fetching content
        BaseSessionFactory sFactory = DMSSessionFactory.getFactory();
        BaseSession session2 = sFactory.getSession();

        File outFile = new File(Env.getHome(), StringUtil.strcat("output", File.separator, "posts.csv"));
        CSVWriter writer = new CSVFileWriter(outFile, StringUtil.UTF8Encoding, ColumnNames);

        SimpleDateFormat dfmt = new SimpleDateFormat(PostDumpCSVConsumer.DateFormat);

        Iterator itr = session.getIteratorFromQueryArgs(PostQuery, Constants.ActiveState, Constants.ActiveState);
        int count = 0;
        String lastMore = null;
        while (itr.hasNext()) {
            Object obj = itr.next();
            Object[] row = (Object[]) obj;

            // make sure we have a link to the post, bail if not.  Also make sure this isn't a repeat
            String link = (String) row[1];
            if (link == null) {
                continue;
            }
            if (link.equals(lastMore)) {
                continue;
            }
            lastMore = link;

            // need to clean newlines out of title
            row[0] = removeNewlines((String) row[0]);

            // format the date
            String dateText = dfmt.format((Date) row[2]);
            row[2] = dateText;

            // replace the guid with the body text of the post
            Post post = (Post) session2.getObjectFromGuid((String) row[4]);
            if (post == null) {
                continue;
            }
            String body = post.getRssFeedBody();
            row[4] = removeNewlines(body);

            writer.writeRow(row);

            count++;
            if (count % 200 == 0) {
                // try and clear out objects so we don't kill our memory
                session.rollback();
                session.clearCaches();
                sFactory.close(session2);
                session2 = sFactory.getSession();
                Console.println("Written " + count + " posts");
            }
        }

        sFactory.close(session2);

        writer.close();
        return JobExecutionResult.Executed;
    }

    private String removeNewlines(String in) {
        if (in.indexOf("\n") >= 0) {
            in = in.replace('\n', ' ');
        }
        if (in.indexOf("\r") >= 0) {
            in = in.replace('\r', ' ');
        }

        return in;
    }
}

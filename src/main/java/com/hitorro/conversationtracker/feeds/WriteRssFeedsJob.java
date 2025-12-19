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

import com.hitorro.util.core.Constants;
import com.hitorro.util.core.Env;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.csv.CSVFileWriter;
import com.hitorro.util.io.csv.CSVWriter;
import com.hitorro.util.job.Job;
import com.hitorro.util.job.JobExecutionResult;
import com.hitorro.util.job.JobParameters;
import com.hitorro.util.typesystem.BaseSession;

import java.io.File;
import java.util.Iterator;

/**
 * Write out all of the active RssFeeds.
 */
public class WriteRssFeedsJob extends Job {
    public static final String WriteRssFeeds = "writerssfeeds";
    private static final String ColumnNames[] = {"Name", "WebSite", "ContactName",
            "ContactEmail", "RssUrl", "StartGroup"};

    /**
     * This job must run on the db leader.
     *
     * @return
     */
    public boolean getMustRunOnDBLeader() {
        return true;
    }

    public String getName() {
        return WriteRssFeeds;
    }

    public boolean needsSession() {
        return true;
    }

    public JobExecutionResult doAction(JobParameters parameters) {
        // no parameters - just write out all the files to a fixed location
        BaseSession session = getSession();

        File outFile = new File(Env.getHome(), StringUtil.strcat("output", File.separator, "feeds.csv"));
        CSVWriter writer = new CSVFileWriter(outFile, ColumnNames);

        String query = "select feed.name, feed.siteUrl, feed.contactName, feed.contactEmail, feed.uri, feedgroup.name " +
                "from RssFeedIn as feed, RssFeedGroup as feedgroup where feed.state= :a " +
                "and feed.owningContainer = feedgroup " +
                "order by feed.name";

        Iterator itr = session.getIteratorFromQueryArgs(query, Constants.ActiveState);
        while (itr.hasNext()) {
            Object obj = itr.next();
            Object[] row = (Object[]) obj;
            // convertToPdf Tech to Technology
            String groupName = (String) row[5];
            if ("Tech".equals(groupName)) {
                groupName = "Technology";
            }
            row[5] = groupName;
            writer.writeRow(row);
        }

        writer.close();
        return JobExecutionResult.Executed;
    }
}

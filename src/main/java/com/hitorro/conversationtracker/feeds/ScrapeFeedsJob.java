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
import com.hitorro.base.objects.RssFeedIn;
import com.hitorro.base.objects.SubjectArea;
import com.hitorro.basedms.rss.FeedFetcherDatum;
import com.hitorro.basedms.rss.FeedRetrieval;
import com.hitorro.util.core.Constants;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.html.HTMLParser;
import com.hitorro.util.io.net.NetUtil;
import com.hitorro.util.job.Job;
import com.hitorro.util.job.JobExecutionResult;
import com.hitorro.util.job.JobParameters;
import com.hitorro.util.typesystem.BaseSession;
import org.apache.log4j.Level;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used to find new RSS feeds.
 */
public class ScrapeFeedsJob extends Job {
    public static final String ScrapeFeeds = "scrapefeeds";

    public static boolean addNewFeed(BaseSession session, String title, String uri, String description,
                                     String siteUrl, SubjectArea area) {
        uri = uri.trim();
        title = title.trim();

        // we want the feed to have a unique url and name
        RssFeedIn byUrl = RssFeedIn.getRssFeedInForUri(session, uri);
        if (byUrl != null) {
            return false;
        }

        RssFeedIn byName = RssFeedIn.getRssFeedInForName(session, title);
        if (byName != null) {
            return false;
        }

        // create the new feed
        RssFeedIn newFeed = new RssFeedIn();
        newFeed.setName(StringUtil.cutToLength(title.trim(), 80));
        newFeed.setUri(uri.trim());
        newFeed.setState(Constants.ActiveState);
        newFeed.setDescription(StringUtil.cutToLength(description, 250));
        newFeed.setSiteUrl(siteUrl);
        session.persist(newFeed);

        // create a corresponding new forum
        Forum forum = new Forum();
        forum.setName(newFeed.getName());
        session.persist(forum);
        newFeed.setForum(forum);

        if (area == null) {
            // put to the default area
            area = SubjectArea.getSubjectAreaForName(session, SubjectArea.DefaultArea);
        }
        forum.setArea(area);

        return true;
    }

    /**
     * This job must run on the db leader.
     *
     * @return
     */
    public boolean getMustRunOnDBLeader() {
        return true;
    }

    public String getName() {
        return ScrapeFeeds;
    }

    public boolean needsSession() {
        return true;
    }

    public JobExecutionResult doAction(JobParameters parameters) {
        if (!(parameters instanceof ScrapeFeedsJobParameters)) {
            // we only work off parameters
            return new JobExecutionResult(Level.ERROR, "ScrapeFeedsJobParameters were not provided, got %s",
                    parameters.getClass().getCanonicalName());
        }

        ScrapeFeedsJobParameters params = (ScrapeFeedsJobParameters) parameters;

        BaseSession session = getSession();

        SubjectArea area = params.getArea();
        /*
        RssFeedGroup feedGroup = RssFeedGroup.getRssFeedGroupForName(session, groupName);
        if (feedGroup == null)
        {
            //Console.println("Creating group " + groupName);
            // make the group
            feedGroup = new RssFeedGroup();
            feedGroup.setName(groupName);
            feedGroup.setState(Constants.ActiveState);
            session.persist(feedGroup);
            session.commit();
        }
        */

        // the url field make contain several urls separated by semicolons
        String urls = params.getReadUrl();
        String[] theUrls = StringUtil.tokenizeFromSingleChar(urls, ";");
        for (String ur : theUrls) {
            process(session, area, ur);
        }
        return JobExecutionResult.Executed;
    }

    private void process(BaseSession session, SubjectArea area, String readUrl) {
        // read the input html page
        String inputPage = readPage(readUrl);

        // get the links
        List<String> links = getLinks(inputPage);

        // look for feeds & save them
        for (String link : links) {
            findFeed(session, link, area);
        }
    }

    private String readPage(String url) {
        StringBuilder page = NetUtil.readUrl(url, true);
        return page.toString();
    }

    private List<String> getLinks(String pageText) {
        List<String> refLinks = new ArrayList<String>();
        try {
            HTMLParser hparser = new HTMLParser(pageText);
            refLinks = hparser.getLinks(HTMLParser.PAGE_HTML);
        } catch (IOException exc) {
            //Console.println("Trouble: " + exc);
        }

        return refLinks;
    }

    private boolean findFeed(BaseSession session, String link, SubjectArea area) {
        boolean success;

        if (!link.startsWith("http://")) {
            return false;
        }

        String feed = NetUtil.findRssUrlFromUrl(link);
        if (feed == null) {
            return false;
        }

        // try to read the feed - and get its title
        try {
            FeedFetcherDatum fb = new FeedFetcherDatum();
            fb.setURL(new URL(feed));
            FeedRetrieval fr = new FeedRetrieval();

            // read the feed
            FeedFetcherDatum output = fr.getFeed(fb);
            if (output == null) {
                return false;
            }
            String title = output.getTitle();
            if (title == null || title.length() < 3 || title.startsWith("An error has")) {
                // no title?  or we could read the feed..
                return false;
            }

            if (!addNewFeed(session, title, feed, output.getDescription(),
                    output.getLink(), area)) {
                // was a duplicate
                return false;
            } else {
                success = true;
                session.commit();
            }
        } catch (MalformedURLException exc) {
            return false;
        }

        return success;
    }
}

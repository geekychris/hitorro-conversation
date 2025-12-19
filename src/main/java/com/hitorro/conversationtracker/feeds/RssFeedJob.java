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


import gnu.trove.map.hash.TLongLongHashMap;
import com.hitorro.basedms.PostUtil;
import com.hitorro.basedms.rss.FeedFetcherDatum;
import com.hitorro.basedms.rss.ItemBean;
import com.hitorro.basedms.rss.RssItemStore;
import com.hitorro.basedms.rss.RssReadImpl;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.Constants;
import com.hitorro.util.core.ListUtil;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.hash.FPHash64;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.core.thread.HTThread;
import com.hitorro.util.html.HTMLPage;
import com.hitorro.util.job.Job;
import com.hitorro.util.job.JobExecutionResult;
import com.hitorro.util.job.JobParameters;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.urlparser.URLUtil;
import org.hibernate.exception.GenericJDBCException;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


/**
 * Scheduled task which reads the active RssFeeds.
 */
public class RssFeedJob extends Job {
    public static final String RssFeedJob = "rssfeedjob";
    private static final int MaxRead = 10;
    private static final String AnyAreaFeedQuery = "select guid from RssFeedIn where state = :a and nextRead < :b";
    private static final String OneAreaFeedQuery = "select rfi.guid " +
            "from RssFeedIn as rfi, Forum as forum " +
            "where rfi.forum = forum " +
            "and rfi.state= :a and forum.state= :b " +
            "and forum.area= :c " +
            "and nextRead < :d";
    private TLongLongHashMap globalDupeMap = new TLongLongHashMap();

    private void setInfo(String info, String... args) {
        Thread t = Thread.currentThread();
        if (t instanceof HTThread) {
            HTThread pt = (HTThread) t;
            pt.setDescription(Fmt.Sargs(info, args));
        }
    }

    /**
     * This job must run on the db leader.
     *
     * @return
     */
    public boolean getMustRunOnDBLeader() {
        return true;
    }

    public JobExecutionResult doAction(JobParameters parameters) {
        //XXX
        //Log.util.setLevel(Level.DEBUG);
        if (parameters != null && parameters.getJobName() != null) {
            Log.util.info("Executing RssFeedJob %s", parameters.getJobName());
        } else {
            Log.util.info("Executing RssFeedJob with no parameters");
        }

        BaseSession session = getSession();

        // get the guids of the active feeds that are ready to read
        // we query the guids rather than the objects because this is a long-running task and it is not unlikely
        // that some other thread will be accessing & changing the RssFeedIn object, and we want to avoid
        // staleness issues
        Date now = new Date();
        Iterator itr;
        if (!(parameters instanceof RssFeedJobParameters)) {
            // read all groups
            itr = session.getIteratorFromQueryArgs(AnyAreaFeedQuery, Constants.ActiveState, now);
        } else {
            // read feeds from a single group
            parameters.setEditingSession(session);
            com.hitorro.base.objects.SubjectArea area = ((RssFeedJobParameters) parameters).getSubjectArea();
            itr = session.getIteratorFromQueryArgs(OneAreaFeedQuery,
                    Constants.ActiveState, Constants.ActiveState, area, now);
        }

        // get our own copy of the results, because the iterator will go stale
        List<String> guids = new ArrayList<String>();
        while (itr.hasNext()) {
            guids.add((String) itr.next());
        }

        // for every feed run the feed reader if it is time
        // we are reading the feeds sequentially - if we have too many we should break into multiple threads
        // we'll read up to a maximum number of feeds to stagger the feed reading
        int nRead = 0;
        FeedStorer storer = new FeedStorer(session, now, globalDupeMap);
        RssReadImpl reader = new RssReadImpl();
        for (String guid : guids) {
            storer.reset();
            com.hitorro.base.objects.RssFeedIn inf = (com.hitorro.base.objects.RssFeedIn) session.getObjectFromGuid(guid);
            // do another date check, because another thread may have read the feed since we did our query
            if (inf != null && now.after(inf.getNextRead())) {
                // set the next read time in the future so that no other feed-reading thread will try to
                // simultaneously read this feed

                inf.updateNextRead(inf.getReadInMinutes());
                session.commit();
                if (inf.getForumType() == com.hitorro.base.objects.RssFeedIn.AggregateForum) {
                    Log.util.info("Aggregate forum RssFeedJob reading %s", inf.getName());
                }
                Log.util.debug("RssFeedJob reading %s", inf.getName());
                storer.setSource(inf);
                setInfo("Reading: %s at %s", inf.getUri(), new Date().toString());
                FeedFetcherDatum feedBean = reader.read(inf.getUri(), inf.getLastModified(), storer);
                setInfo("Read: %s at %s", inf.getUri(), new Date().toString());
                // update the feed with the new "lastModified" and with the next read time
                // get a fresh copy of the feed since it has been awhile
                com.hitorro.base.objects.RssFeedIn inf2 = (com.hitorro.base.objects.RssFeedIn) session.getObjectFromGuid(guid);
                if (guid == null || inf2 == null || feedBean == null) {
                    Log.httpfetcher.error("Unable to read %s", inf.getUri());
                    break;
                }
                String lastMod = feedBean.getLastModified();

                inf2.setLastModified(lastMod);

                inf2.updateNextRead(0);

                // commit for each feed (may have created multiple items)
                session.commit();

                if (feedBean.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    // we skipped reading this because the rss feed has not been updated, so we didn't do any work
                } else {
                    nRead++;
                }
                Log.util.info("Forum RssFeedJob %s, read %s items", inf.getName(), storer.getStoredCount());

                if (nRead >= MaxRead) {
                    break;
                }
            }
        }
        // things are good
        Log.util.info("Finished with RssFeedJob");
        return JobExecutionResult.Executed;
    }

    public String getName() {
        return RssFeedJob;
    }

    public boolean needsSession() {
        return true;
    }

    public static class FeedStorer implements RssItemStore {
        private BaseSession _session;
        private com.hitorro.base.objects.RssFeedIn _source;
        private Date _now;
        private String _siteDomain;
        private int storedCount = 0;
        private TLongLongHashMap dupeMap = new TLongLongHashMap();
        private TLongLongHashMap globalDupeMap = new TLongLongHashMap();


        public FeedStorer(BaseSession session, Date now, TLongLongHashMap globalDupeMap) {
            //_session = session;
            _now = now;
            this.globalDupeMap = globalDupeMap;
        }

        public void setSource(com.hitorro.base.objects.RssFeedIn source) {
            _source = source;
            String siteUrl = _source.getSiteUrl();
            _siteDomain = siteUrl; // in case we can't figure out anything else
            if (_siteDomain == null) {
                return;
            }
            // siteUrl will be something like http://xx.yy.zz/blah
            // the domain is the first chunk of that
            int indx = siteUrl.indexOf("//");
            if (indx >= 0) {
                indx = siteUrl.indexOf('/', indx + 2);
                if (indx >= 0) {
                    _siteDomain = siteUrl.substring(0, indx);
                }
            }
        }

        public Object store(ItemBean ib, String originalUrl) {
            _session = DMSSessionFactory.getFactory().getSession();
            String title = ib.getTitle();
            // for the body text we prefer "content", but we'll take "description" if there is no content
            String body = ib.getContent();
            if (body == null || body.length() < 1) {
                body = ib.getDescription();
            }
            String link = ib.getLink();
            if (StringUtil.nullOrEmptyString(link)) {
                Log.util.warn(
                        "Post does not have a valid url to use for dupe detection, ignoring post %s in feed %s",
                        title,
                        _source.getName());
                return null;
            }
            link = URLUtil.cleanupUrlAndCutToLength(link, PostUtil.MaxUrlLength);

            // see if we already have this item (same title in this feed)

            String origAuthor = "";
            List<String> authors = ib.getAuthors();
            if (!ListUtil.nullOrEmpty(authors)) {

                for (int i = 0; i < authors.size(); i++) {
                    origAuthor = ib.getAuthors().get(i);
                    if (!StringUtil.nullOrEmptyOrBlankString(origAuthor)) {
                        break;
                    }
                }
            }
            try {
                com.hitorro.base.objects.Forum container = _source.getForum();
                // prevent this feed from providing duplicate posts.
                long hash = FPHash64.getFP(link);

                if (dupeMap.contains(hash)) {
                    Log.util.debug("RssFeed: %s provided the permalink: %s more than once", container.getName(), link);
                    return null;
                }

                boolean gotDupe = false;
                if (globalDupeMap.contains(hash)) {
                    Log.util.debug("RssFeed(Global for Run): %s provided the permalink: %s more than once", container.getName(), link);
                    //return null;
                    gotDupe = true;
                }
                dupeMap.put(hash, hash);
                globalDupeMap.put(hash, hash);
                com.hitorro.base.objects.Post checkEntry = com.hitorro.base.objects.Post.getPostByMoreLinkHash(_session, hash);
                if (gotDupe && checkEntry != null) {
                    Log.util.debug("Got Dupe in rssjob guid: %s", checkEntry.getGuid());
                }
                if (checkEntry == null && gotDupe) {
                    Log.util.error("Noted duplicate Post, but not in DB: %s", link);
                }

                com.hitorro.base.objects.Forum sharedContainer = _source.getSharedForum();
                List<com.hitorro.base.objects.Forum> forums = new ArrayList<com.hitorro.base.objects.Forum>();
                if (container != null) {
                    forums.add(container);
                }
                if (sharedContainer != null) {
                    forums.add(sharedContainer);
                }

                if (checkEntry != null && com.hitorro.base.objects.Post.PostType.RSSFeedType.equals(checkEntry.getPostType())) {

                    Log.util.debug("There is a test of a previous post %s / %s", title, checkEntry.getTitle());
                    // ensure its in our container
                    addMissingForums(forums, checkEntry, title, container);
                } else {
                    // we ended up here because we either dont have the post, or we have the post and its currently not
                    // marked as from an rss feed (we are about to promote it.
                    HTMLPage page = new HTMLPage();
                    Date pDate = ib.getPubDate();
                    Date now = new Date();
                    pDate = setCommonFields(page, link, body, pDate, now);

                    if (checkEntry != null) {
                        Log.util.debug("Promoting Post: %s to RssFeedType", checkEntry.getGuid());
                        checkEntry.setPostType(com.hitorro.base.objects.Post.PostType.RSSFeedType);
                    }

                    String feedUserName = this._source.getContactName();
                    com.hitorro.base.objects.User author = container.getUser(_session);
                    storedCount++;

                    return PostUtil.createNewPost(_session, pDate,
                            link, title,
                            page, body, false, true,
                            forums, author,
                            _siteDomain, 1,
                            com.hitorro.base.objects.Post.PostType.RSSFeedType.getOrdinal(), checkEntry,
                            ib.getEnclosures(),
                            origAuthor, feedUserName, ib.getCategories());


                }

            } catch (GenericJDBCException exc) {
                Log.util.error(exc, "Error looking up feed item or persisting post in feed %s with title %s: ",
                        _source.getId(), title);
                throw exc;
            } catch (Exception e) {
                Log.util.error("Exception %s %e", e, e);
            } finally {
                DMSSessionFactory.getFactory().rollbackCloseSession(_session);
            }

            return null;
        }

        private Date setCommonFields(HTMLPage page, String link, String body, Date pDate, Date now) {
            page.setUrl(link);
            // wrap body in html tags to make the html parser happy

            page.setSourceFromPlainText(body);

            // make sure we have a valid date (existing and not in the future)
            if (pDate == null || pDate.after(now)) {
                pDate = now;
            }

            page.setPublishedDate(pDate.getTime());
            return pDate;
        }

        private void addMissingForums(List<com.hitorro.base.objects.Forum> forums, com.hitorro.base.objects.Post checkEntry, String title, com.hitorro.base.objects.Forum container) {
            boolean newForum = false;
            for (com.hitorro.base.objects.Forum f : forums) {
                if (!checkEntry.getContainers().contains(f)) {
                    // XXX TODO TEST CODE REMOVE
                    com.hitorro.base.objects.Reference.assertReference(checkEntry.getGuid(), f.getGuid(), com.hitorro.base.objects.Reference.ShareReference);
                    // we currently are not in this container so lets put it.
                    Log.util.info("There is a test of a previous post %s / %s, but was not in forum %s, adding",
                            title, checkEntry.getTitle(), container.getName());
                    String g = container.getGuid();
                    f = (com.hitorro.base.objects.Forum) _session.getHTSerializableFromGUID(g);
                    checkEntry.addContainer(f);
                    storedCount++;
                    com.hitorro.base.objects.Reference.createReference(checkEntry, f, com.hitorro.base.objects.Reference.ShareReference, null, _session);

                    newForum = true;
                }
            }
            if (newForum == true) {
                storedCount++;
                _session.commit();
            }
        }

        public int getStoredCount() {
            return storedCount;
        }

        protected void setStoredCount(int storedCount) {
            this.storedCount = storedCount;
        }

        public void reset() {
            setStoredCount(0);
            dupeMap.clear();
        }
    }
}

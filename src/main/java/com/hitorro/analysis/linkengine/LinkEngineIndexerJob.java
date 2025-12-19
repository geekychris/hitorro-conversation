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
package com.hitorro.analysis.linkengine;

import com.hitorro.util.basefile.tools.BaseFileLastTimeMarker;
import com.hitorro.util.core.UTCDateUtil;
import com.hitorro.util.core.map.LockBox;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.startupframework.ServiceContext;
import com.hitorro.basetext.indexer.Log;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.io.IOException;
import java.util.Date;

/**
 * <p/>
 */
public class LinkEngineIndexerJob implements Job {
    private static LockBox s_lockBox = new LockBox();

    public LinkEngineIndexerJob() {
    }

    public final void execute(JobExecutionContext jobExecutionContext) {
        Log.indexer.info("LinkEngineIndexerJob");
        if (!ServiceContext.getSC().isServiceInitialized(LinkEngineService.class)) {
            return;
        }
        Object lock = null;
        try {

            JobDetail detail = jobExecutionContext.getJobDetail();
            JobDataMap dmap = detail.getJobDataMap();

            String indexName = (String) dmap.get("name");
            if (StringUtil.nullOrEmptyOrBlankString(indexName)) {
                Log.indexer.error("No index name provided in LinkEngineIndexerJob");
                return;
            }
            lock = s_lockBox.getLock(indexName);
            synchronized (lock) {
                if (indexFromDb()) {
                    return;
                }
            }
        } finally {
            // there is no need to release the lock as this is just a lock object and the fact we have exited the
            // synchronized block means we have released the lock.
            Log.indexer.info("LinkEngineIndexerJob finished");
        }
    }

    private boolean indexFromDb() {
        LinkEngine le = LinkEngineSingletonMapper.getSingleton().get();
        if (le.getRetrievedFromResourceCache()) {
            Log.indexer.error("LinkIndex was retrieved from ResourceCache, resource cache management not fully implemented");
            return true;
            /**
             * Here we should test the dir, if it is, we should copy the whole thing to a temp resource Dir and index from there.
             *
             */
        }
        BaseFileLastTimeMarker ltm = new BaseFileLastTimeMarker(le.getIndexDir(), "post_linkset");
        Date lastTime = ltm.getLastIndexTime();
        LinkSegmentWriter writer = le.getWriter();
        boolean optimize = true;
        try {
            Date toDate = UTCDateUtil.getDateForNMinutesFromNow(60 * 24);
            LinkSegment seg = writer.indexLinkSetFromSQLQuery(lastTime, toDate);
            LinkSegment refSeg = writer.indexReferencesFromSQLQuery(lastTime, toDate);
            if (seg == null && refSeg == null) {
                Log.indexer.info("LinkIndexer....nothing to index");
            } else {
                boolean rollback = true;
                if (seg != null && seg.getSize() > 0) {
                    rollback = false;
                    Log.indexer.info("LinkIndexer Links indexed: %s Postings.", seg.getSize());
                    le.commitWriter(writer, seg);
                }

                if (refSeg != null && refSeg.getSize() > 0) {
                    rollback = false;
                    Log.indexer.info("LinkIndexer References indexed: %s Postings.", refSeg.getSize());
                    le.commitWriter(writer, refSeg);
                }
                if (rollback) {
                    if (seg != null) {
                        le.rollback(seg);
                    }
                    if (refSeg != null) {
                        le.rollback(refSeg);
                    }
                } else {
                    if (optimize == true) {
                        le.optimize();
                    }
                    // do index here.
                    ltm.set();
                }
            }
        } catch (IOException e) {
            Log.indexer.error("Unable to perform link engine index %s %e", e, e);
        }
        return false;
    }

}

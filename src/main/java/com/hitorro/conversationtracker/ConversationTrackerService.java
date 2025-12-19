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
package com.hitorro.conversationtracker;

import com.hitorro.analysis.linkengine.LinkEngineService;
import com.hitorro.base.objects.ScheduledJob;
import com.hitorro.basedms.RichDMSService;
import com.hitorro.basedms.job.JobService;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.conversationtracker.feeds.RssFeedJob;
import com.hitorro.conversationtracker.feeds.RssFeedJobParameters;
import com.hitorro.conversationtracker.feeds.ScrapeFeedsJob;
import com.hitorro.conversationtracker.feeds.ScrapeFeedsJobParameters;
import com.hitorro.conversationtracker.feeds.WriteRssFeedsJob;
import com.hitorro.basetext.commands.FetchPageCommand;
import com.hitorro.basetext.commands.SimilarityCommand;
import com.hitorro.basetext.dfindex.DFIndexSingletonMapper;
import com.hitorro.basetext.dfindex.DumpDF;
import com.hitorro.conversationtracker.feeds.*;
import com.hitorro.conversationtracker.rss.RssFeedGenerator;
import com.hitorro.network.monitor.MonitorAlertUtil;
import com.hitorro.network.rpc.cluster.ClusterService;
import com.hitorro.network.servlet.ServletService;
import com.hitorro.obj.core.InvertUrlContent;
import com.hitorro.sampleapp.ConversationCalculationJob;
import com.hitorro.sampleapp.ConversationCalculationParameters;
import com.hitorro.util.startupframework.phases.ServiceDefinition;
import com.hitorro.util.typesystem.BaseSession;

/**
 * Service at the top level of the ConversationTracker server.
 */
@ServiceDefinition(dependentService = {RichDMSService.class, LinkEngineService.class},
        shortName = "conversation",
        description = "Conversation tracker service",
        debugCommands = {InvertUrlContent.class, SimilarityCommand.class, SimilarityCommand.class, DumpDF.class, FetchPageCommand.class},
        typeManagedClasses = {Conversation.class},
        uiDirectories = {},
        scheduledJobPath = {"conversationtracker"})
public class ConversationTrackerService {
    public static final String ConversationTrackerKey = "conversationtracker";

    public String deInit() {
        return null;
    }


    public String init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        DFIndexSingletonMapper.getSingleton();
        // register admin tasks


        JobService js = JobService.getService();
        // feed & post jobs
        js.registerAppJob(RssFeedJob.class, "Rss Feed Reader", RssFeedJobParameters.class);
        js.registerAppJob(ScrapeFeedsJob.class, "Rss Feed Scraper", ScrapeFeedsJobParameters.class);
        js.registerAppJob(WriteRssFeedsJob.class, "Write Feeds to CSV file", null);
        js.registerAppJob(ConversationCalculationJob.class, "Conversation calculation", ConversationCalculationParameters.class);

        ClusterService.getThisInstanceDefinition().addInstanceCapability(ConversationTrackerKey, "", "", "", true);
        MonitorAlertUtil.addLowDiskAlert();

        ServletService ssrv = ServletService.getService();
        ssrv.addExternalServlet(new RssFeedGenerator(), "/rssquery/*");
        return null;
    }

    public String start(boolean dbInit) {
        // set up the front page
        // create the rss feed scheduled job and make it active
        if (dbInit) {
            BaseSession session = DMSSessionFactory.getFactory().getSession();
            try {
                ScheduledJob feedJob = new ScheduledJob();
                feedJob.setName("Rss feed reader");

                feedJob.setJobName(new RssFeedJob().getName());
                feedJob.setSecondsSchedule(5 * 60);             // every 5 minutes
                //feedJob.setSecondsSchedule(60);             // every 1 minute

                session.persist(feedJob);
                session.commit();
                feedJob.start();

            } finally {
                DMSSessionFactory.closeSession(session);
            }
        }

        return null;

    }

}

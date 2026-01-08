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

import com.hitorro.analysis.linkengine.compute.ConversationRankerContext;
import com.hitorro.analysis.linkengine.compute.RankByParticipantSpread;
import com.hitorro.analysis.linkengine.compute.SpamRemovalRanker;
import com.hitorro.basedms.PostFromUrlFetcherAppJob;
import com.hitorro.basedms.PostFromUrlParameters;
import com.hitorro.basedms.RssItemCacheProxy;
import com.hitorro.basedms.job.JobService;
import com.hitorro.basetext.commands.SimilarityCommand;
import com.hitorro.obj.core.TextService;
import com.hitorro.util.startupframework.phases.ServiceDefinition;



@ServiceDefinition(dependentService = {TextService.class},
        shortName = "linkengine",
        description = "Link Engineservices",
        debugCommands = {SimilarityCommand.class},
        typeManagedClasses = {IndexLink.class, RssItemCacheProxy.class},
        scheduledJobPath = {"linkengine"}
)
public class LinkEngineService {
    public static boolean s_initialized = false;


    public String init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        JobService.getService().registerAppJob(PostFromUrlFetcherAppJob.class, "UrlToPost Fetcher App", PostFromUrlParameters.class);

        ConversationRankerContext.getContext().addConversationRanker(new RankByParticipantSpread(), true);
        ConversationRankerContext.getContext().setSpamRanker(new SpamRemovalRanker());
        return null;
    }

    public String run() {
        return null;
    }

    public String deInit() {
        s_initialized = false;
        return null;
    }

    public String start(boolean dbInit) {
        return null;
    }
}



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
package com.hitorro.sampleapp;

import com.hitorro.analysis.linkengine.compute.ComputeConversation;
import com.hitorro.basetext.indexer.Log;
import com.hitorro.util.core.UTCDateUtil;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.job.Job;
import com.hitorro.util.job.JobExecutionResult;
import com.hitorro.util.job.JobParameters;
import com.hitorro.util.job.PropertiesJobParameters;
import org.apache.log4j.Level;
import org.quartz.JobExecutionException;

import java.io.IOException;
import java.util.Date;

/**
 * Calculates conversations.
 */
public class ConversationCalculationJob extends Job {
    public static final String JobName = "memecalculationjob";

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
        // don't need a session because ComputeConversation does its own session
        return false;
    }

    public JobExecutionResult doAction(JobParameters parameters)
            throws JobExecutionException {
        Log.indexer.info("ConversationComputation");
        try {
            String areaName = null;
            if (!(parameters instanceof ConversationCalculationParameters)) {
                if (parameters instanceof PropertiesJobParameters) {
                    Object o = ((PropertiesJobParameters) parameters).getProperty("areaname");

                    if (o != null) {
                        areaName = o.toString();
                    }
                }
                if (StringUtil.nullOrEmptyString(areaName)) {
                    return new JobExecutionResult(Level.ERROR, "ConversationCalculationParameters were not provided, got %s",
                            parameters.getClass().getCanonicalName());
                }
            } else {
                ConversationCalculationParameters params = (ConversationCalculationParameters) parameters;
                areaName = params.getAreaName();
            }

            ComputeConversation cc = new ComputeConversation();


            Date endDate = new Date();
            Date startDate = UTCDateUtil.changeDays(endDate, -7);
            int minPosters = 4;
            int maxQueue = 100;

            try {
                cc.computeFromSubjectAreaCreatePersistedConversation(areaName, startDate, endDate, minPosters, maxQueue);
            } catch (IOException exc) {
                throw new JobExecutionException(exc);
            }

            return JobExecutionResult.Executed;
        } finally {
            Log.indexer.info("ConversationComputation finished");
        }
    }
}

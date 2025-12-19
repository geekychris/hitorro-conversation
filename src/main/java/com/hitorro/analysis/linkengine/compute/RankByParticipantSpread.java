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
package com.hitorro.analysis.linkengine.compute;

import com.hitorro.conversationtracker.Conversation;
import com.hitorro.util.urlparser.UrlCollectionStats;

import java.util.Iterator;
import java.util.List;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved.
 * <p/>
 * User: chris
 */
public class RankByParticipantSpread implements ConversationRanker {
    public static final String Key = "ParticipantSpread";

    public String getName() {
        return Key;
    }

    public String getDescription() {
        return "Ratio of postings to participant spread";
    }

    public void rank(List<Conversation> list) {
        Iterator<Conversation> iter = list.iterator();
        while (iter.hasNext()) {
            Conversation c = iter.next();
            c.setConversationRank(computeRank(c));
        }
    }

    private double computeRank(Conversation conv) {
        UrlCollectionStats stats = conv.getUrlCollectionStats();
        double tldCount = stats.getTLDCount();
        double count = stats.getSize();
        if (count == 0.0) {
            return 0.0;
        }

        double rank = (tldCount / count);
        return rank;
    }
}

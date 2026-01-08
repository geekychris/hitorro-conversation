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
import com.hitorro.util.core.Console;

import java.util.*;

/**

 * Context that keeps track of all the available ranker functions for conversations.
 */
public class ConversationRankerContext {
    private static ConversationRankerContext s_context = new ConversationRankerContext();

    private ConversationRanker defaultRanker;
    private ConversationRanker spamRanker;
    private Map<String, ConversationRanker> rankers = new HashMap<String, ConversationRanker>();

    public static ConversationRankerContext getContext() {
        return s_context;
    }

    public void setSpamRanker(ConversationRanker spamRanker) {
        this.spamRanker = spamRanker;
    }

    public String getDefaultName() {
        if (defaultRanker != null) {
            return defaultRanker.getName();
        }
        return null;
    }

    public List<String> getRankerNames() {
        List<String> list = new ArrayList<String>();

        for (ConversationRanker ranker : rankers.values()) {
            list.add(ranker.getName());
        }
        return list;
    }

    public void setDefault(String name) {
        defaultRanker = rankers.get(name.toLowerCase());
    }

    public void addConversationRanker(ConversationRanker ranker, boolean isDefault) {
        rankers.put(ranker.getName().toLowerCase(), ranker);
        if (isDefault) {
            this.setDefault(ranker.getName());
        }
    }

    public List<Conversation> rank(List<Conversation> list) {
        return rank(list, defaultRanker);
    }

    public List<Conversation> rank(List<Conversation> list, String rankerName) {
        return rank(list, rankers.get(rankerName.toLowerCase()));
    }

    public List<Conversation> rank(List<Conversation> list, ConversationRanker ranker) {
        if (spamRanker != null) {
            spamRanker.rank(list);
        }
        if (ranker != null) {
            ranker.rank(list);

        }
        Collections.sort(list, new ConversationRankComparator());
        for (Conversation c : list) {
            Console.println("rank: %s", c.getConversationRank());
        }
        return list;
    }

}

class ConversationRankComparator implements Comparator<Conversation> {

    public int compare(Conversation c1, Conversation c2) {
        double c1Rank = c1.getConversationRank();

        double c2Rank = c2.getConversationRank();
        if (c1Rank < c2Rank) {
            return 1;
        }
        if (c1Rank > c2Rank) {
            return -1;
        }
        return 0;
    }
}

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
package com.hitorro.conversationtracker.rss;

import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.basedms.xml.RssItemInterface;
import com.hitorro.util.core.Constants;
import com.hitorro.util.core.HTAssert;
import com.hitorro.util.core.opers.HTPredicate;

import java.util.Date;

/**
 * <p/>
 */
public class MinDateSize implements HTPredicate<RssItemInterface> {
    private Date nowDate;
    private long now;
    private long nowMinus;

    public MinDateSize(int days) {
        nowDate = new Date();
        now = nowDate.getTime();
        nowMinus = now - (Constants.MillisInDay * days);
    }

    public boolean initFromMap(final JsonNode map) {
        // MUST IMPLEMENT
        HTAssert.assertThat(false, "MinDateSize.initFromMap not implemented");
        return false;
    }

    public void initForPass() {

    }

    public boolean test(RssItemInterface rssItemInterface) {
        Date postDate = rssItemInterface.getPostDate();
        if (postDate == null) {
            return false;
        }
        long pd = postDate.getTime();
        return pd > nowMinus;
    }
}

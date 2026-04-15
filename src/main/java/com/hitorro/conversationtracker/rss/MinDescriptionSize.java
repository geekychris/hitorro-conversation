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
import com.hitorro.conversationtracker.Log;
import com.hitorro.util.core.opers.HTPredicate;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.json.keys.IntegerProperty;

/**
 * <p/>
 */
public class MinDescriptionSize implements HTPredicate<RssItemInterface> {
    public static IntegerProperty MinKey = new IntegerProperty("minlength", "", 10);
    private int minDescriptionTextSize = 200;

    public void initForPass() {

    }

    public boolean initFromMap(final JsonNode map) {
        minDescriptionTextSize = MinKey.apply(map);
        return true;
    }

    public boolean test(RssItemInterface rssItemInterface) {
        String txt = rssItemInterface.getDescription();
        if (StringUtil.nullOrEmptyString(txt)) {
            Log.conversationtracker.debug("Failed Min Size: (%s)", txt);
            return false;
        }
        if (txt.length() < minDescriptionTextSize) {
            Log.conversationtracker.debug("Failed Min Size: (%s)", txt);
            return false;
        }
        Log.conversationtracker.debug("Passed Min Size: (%s)", txt);
        return true;
    }
}

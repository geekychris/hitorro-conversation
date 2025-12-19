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

import com.hitorro.obj.core.LuceneResultIteratorAdapter;
import com.hitorro.util.core.string.StringUtil;
import org.apache.lucene.document.Document;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved.
 * <p/>
 * User: chris
 */
public class PostHashToForumNameLuceneAdapter implements LuceneResultIteratorAdapter<PostHashToForumName> {
    public PostHashToForumName map(float score, Document doc) {
        String guid = doc.get("guid");
        String hash = doc.get("identityHash");
        long hashL = 0;
        if (!StringUtil.nullOrEmptyString(hash)) {
            hashL = Long.parseLong(hash);
        }
        String link = doc.get("moreLink");
        return new PostHashToForumName(hashL, link, "");
    }
}

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

import com.hitorro.basedms.PostUtil;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.html.LinkSet;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.io.largedata.iterator.HTSerializableIteratorSink;
import com.hitorro.util.typesystem.BaseSession;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * <p/>
 */
public class LinkEngineIndexerUtil {
    public static String QueryContentForNesLinksSinkHql = "select cc.guid from Post post join post.contents cc where post.degree <= :a and post.modifiedDate > :b and post.modifiedDate < :c and cc.id in " +
            "(select c1.id from Content as c1 left join c1.categories as cat where cat.domain='docparts' and cat.value='reflinksnew')";

    public static String QueryReferenceHql = "select r from Reference r where refType= :a and date > :b and date < :c";

    /**
     * @param query
     * @param date
     * @param dir
     * @param fileExtension
     * @return
     * @throws IOException
     */
    public static final File retrieveLinks(String query, String date, File dir, String fileExtension) throws IOException {
        BaseSession q = DMSSessionFactory.getFactory().getSession();
        BaseSession f = DMSSessionFactory.getFactory().getSession();
        Object args[] = new String[1];
        args[0] = new Integer(PostUtil.MaxDepth.apply());
        args[1] = date;
        //JDBCIterator<LinkSet> iter = new JDBCIterator(query, new HTSerializableFromContentAdapter(f), q.getJDBCConnection(), args);
        int i = 0;
        GetContent adapter = new GetContent(q);
        Iterator<LinkSet> iter = q.getIteratorFromQueryArgs(query, adapter, args);


        FileUtil.ensureDirectoryExists(dir);

        HTSerializableIteratorSink sink = new HTSerializableIteratorSink();
        sink.setIterator(iter);
        File outputFile = FileUtil.getTempFileWithFromDirectoryExtension(dir, fileExtension);
        OutputStream os = FileUtil.getBufferedFileOutputStream(outputFile);
        sink.setOutput(os, true);
        int count = sink.sink();
        return outputFile;
    }


}



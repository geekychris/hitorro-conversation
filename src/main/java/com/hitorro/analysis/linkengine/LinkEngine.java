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

import com.hitorro.analysis.linkengine.tuple.LinkTupleReader;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.core.string.Fmt;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p/>
 * Engine consists of Link Segments that make up an logical index
 */
public class LinkEngine {
    public static final String ResourceName = "linkengine";
    public static final String VersionQuery = "1.0+";

    private BaseFile indexDir;
    private BaseFile segmentFile = null;
    private BaseFile docIdMapFile = null;

    private LinkQueryContext queryContext = null;

    private List<LinkSegment> linkSegments = null;
    private List<String> segmentNames = null;
    private boolean retrievedFromResourceCache = false;
    private ReadWriteLock readerWriterLock = new ReentrantReadWriteLock();

    public LinkEngine(BaseFile dir) throws IOException {
        indexDir = dir;
        init();
    }

    public boolean getRetrievedFromResourceCache() {
        return retrievedFromResourceCache;
    }

    public void setRetrievedFromResourceCache(boolean flag) {
        retrievedFromResourceCache = flag;
    }

    public BaseFile getIndexDir() {
        return indexDir;
    }

    /**
     * Note that this is not transactional at the moment....there is no rollback if there is an io error XXX That needs
     * to be fixed (along with the commit guy that does the same).
     */
    public void optimize() throws IOException {
        SegmentMerger merger = new SegmentMerger(this);
        for (LinkSegment segment : linkSegments) {
            merger.add(segment);
        }
        LinkSegment segment = merger.merge();
        readerWriterLock.writeLock().lock();
        List<LinkSegment> newSeg = new ArrayList<LinkSegment>();
        List<LinkSegment> oldSeg = this.linkSegments;
        linkSegments = new ArrayList<LinkSegment>();
        try {
            segment.load();
            newSeg.add(segment);
            segmentNames.clear();
            segmentNames.add(segment.getName());
            linkSegments.add(segment);
            writeSegmentNames(segmentNames);
            for (LinkSegment old : oldSeg) {
                old.delete();
            }
        } finally {
            readerWriterLock.writeLock().unlock();
        }
    }

    /**
     * get a single reader or a select tree of readers that represent a logic view if the index is more than one
     * segment.  Dont forget to use the free function on the query compContext afterwards to pool the objects.
     * <p/>
     * Note that life is much more efficient if we only have one segment.  If your going to be doing a LOT of queries
     * (such as newman community analsis, I recommend optimizing the index to one tier).
     *
     * @param context
     * @param docId
     * @return
     * @throws IOException
     */
    LinkTupleReader getReader(LinkQueryContext context, long docId) throws IOException {
        if (linkSegments.size() == 1) {
            return linkSegments.get(0).getReaderForDocId(context, docId);
        }
        for (LinkSegment segment : linkSegments) {
            LinkTupleReader reader = segment.getReaderForDocId(context, docId);
            if (reader != null) {
                context.enQueue(reader);
            }
        }
        return context.popQueue();
    }

    public synchronized LinkQueryContext getQueryContext() {
        if (queryContext == null) {
            return new LinkQueryContext(this, readerWriterLock.readLock());
        }
        LinkQueryContext returnMe = queryContext;
        queryContext = queryContext.m_next;
        returnMe.lock();
        return returnMe;
    }

    public synchronized void release(LinkQueryContext context) {
        //we would deal with a pool here.
        context.release();

        context.m_next = queryContext;
        queryContext = context;
    }

    public LinkSegmentWriter getWriter() {
        String name = Fmt.S("%s", System.currentTimeMillis());
        LinkSegment segment = new LinkSegment(indexDir, name);
        return new LinkSegmentWriter(indexDir, segment);
    }

    public void rollback(LinkSegment segment) throws IOException {
        segment.delete();
    }

    /**
     * Once the writer has writen out the postings and the mapping file for that segment, we can update the index.  Note
     * that we wait for all searchers to exit their read phase before we can write since we dont want readers to work
     * against inconsistent data.
     *
     * @param writer
     */
    public void commitWriter(LinkSegmentWriter writer, LinkSegment segment)
            throws IOException {
        if (segment == null) {
            // lowever level probably returned null segment as there was nothing to write.
            return;
        }
        if (segment.getSize() == 0) {
            // nothing really to index.
            segment.delete();
            return;
        }
        readerWriterLock.writeLock().lock();

        try {
            segment.load();
            segmentNames.add(segment.getName());
            writeSegmentNames(segmentNames);
            linkSegments.add(segment);
        } finally {
            readerWriterLock.writeLock().unlock();
        }
    }

    private void init() throws IOException {
        segmentFile = indexDir.getChild("segments");
        docIdMapFile = indexDir.getChild("idmap");

        indexDir.mkdir();
        segmentNames = readSegmentNames();
        linkSegments = readLinkSegmentsFromList(segmentNames);
    }

    private List<String> readSegmentNames() throws IOException {
        if (segmentFile.exists()) {
            return segmentFile.getLines();
        }
        return new ArrayList<String>();
    }

    private List<LinkSegment> readLinkSegmentsFromList(List<String> names)
            throws IOException {
        List<LinkSegment> l = new ArrayList<LinkSegment>();
        for (String name : names) {
            l.add(getLinkSegmentByName(name));
        }
        return l;
    }

    private LinkSegment getLinkSegmentByName(String name)
            throws IOException {
        LinkSegment ls = new LinkSegment(indexDir, name);
        ls.load();
        return ls;
    }

    /**
     * Always the last step in an index update is the writing of the segment table
     *
     * @param names
     * @throws FileNotFoundException
     */
    private void writeSegmentNames(List<String> names) throws IOException {
        BaseFile temp = indexDir.getChild("segments.temp");
        if (temp.exists()) {
            temp.delete();
        }
        temp.writeStringListToFile(names);
        if (segmentFile.exists()) {
            segmentFile.swap(temp);
            temp.delete();
        } else {
            temp.renameTo(segmentFile);
        }
    }
}

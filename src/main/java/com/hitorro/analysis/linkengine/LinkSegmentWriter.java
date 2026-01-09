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
import com.hitorro.analysis.linkengine.tuple.RLELinkTupleWriter;
import com.hitorro.base.BaseBaseFileUtil;
import com.hitorro.base.objects.Content;
import com.hitorro.base.objects.Reference;
import com.hitorro.basedms.PostUtil;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.basedms.session.HibernateQueryResultObjectAdapter;
import com.hitorro.util.basefile.filters.FileExtension;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.core.hash.FPHash64;
import com.hitorro.util.html.Link;
import com.hitorro.util.html.LinkSet;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.io.largedata.BaseFileAccessingObjectFactory;
import com.hitorro.util.io.largedata.buckets.HTSerializableFileBucketWriter;
import com.hitorro.util.io.largedata.compressedstreams.FSOutputStream;
import com.hitorro.util.io.largedata.iterator.BaseFileSelectTreeController;
import com.hitorro.util.io.largedata.iterator.HTSelectTreeController;
import com.hitorro.util.io.largedata.iterator.HTSerializableInputIterator;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.HTSerializableUtil;
import org.hibernate.type.Type;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;

/**
 * Create a segment from a stream of edges.  In the process we will be adding new document ids to the system, thus
 * causing the following components to go through a revision:
 * <p/>
 * HashToIdAllocatingMap new segment new element in the segment table.
 */
public class LinkSegmentWriter {
    private static Comparator<IndexLink> docIdBasedComparator = new IndexLinkByDocIdComparator();
    BaseFileAccessingObjectFactory factory;
    private BaseFile m_dir;
    private RLELinkTupleWriter rleWriter;
    private FSOutputStream fsos;
    private long m_docId = 0;
    private LinkSegment m_segment;

    public LinkSegmentWriter(BaseFile dir, LinkSegment segment) {

        m_dir = dir;
        m_segment = segment;

        BaseFileAccessingObjectFactory fact =
                new BaseFileAccessingObjectFactory();
        fact.setNewFunction(LinkSegmentWriter::getNewValue)
                .setBaseFileToChainingMapper(BaseBaseFileUtil.bf2htser)
                .setBaseFileSinkMapper(HTSerializableUtil.bf2htser)
                .setNewArrayClass(HTSerializable.class);
    }

    public static Link getNewValue(Object o) {
        return new Link();
    }

    public LinkSegment indexFromLinkFile(BaseFile linkFile) throws IOException {
        Iterator<Link> iter = new HTSerializableInputIterator(linkFile, null);
        return indexFromLinkIterator(iter);
    }

    public LinkSegment indexFromLinkSetIterator(Iterator<LinkSet> linkSetIter) {
        return indexFromLinkIterator(new LinkSetToLinkIterator(linkSetIter));
    }

    public LinkSegment indexLinkSetFromSQLQuery(Date start, Date end) {
        BaseSession q = DMSSessionFactory.getFactory().getSession();
        BaseSession f = DMSSessionFactory.getFactory().getSession();
        try {
            Object args[] = new Object[3];
            args[0] = new Integer(PostUtil.MaxDepth.apply());
            args[1] = start;
            args[2] = end;
            //JDBCIterator<LinkSet> iter = new JDBCIterator(query, new HTSerializableFromContentAdapter(f), q.getJDBCConnection(), args);
            int i = 0;
            GetContent adapter = new GetContent(f);
            Iterator<LinkSet> iter = ((DMSSession) q).getIteratorFromQuery(adapter, LinkEngineIndexerUtil.QueryContentForNesLinksSinkHql, args);
            LinkSegment ls = indexFromLinkSetIterator(iter);
            Log.linkengine.info("LinkSegmentWriter retrieved %s rows for dates %s to %s", adapter.getRowsRetrieved(), start.toString(), end.toString());
            return ls;
        } finally {
            DMSSessionFactory.getFactory().rollbackClose(q);
            DMSSessionFactory.getFactory().rollbackClose(f);
        }
    }

    public LinkSegment indexReferencesFromSQLQuery(Date start, Date end) {
        BaseSession q = DMSSessionFactory.getFactory().getSession();
        BaseSession f = DMSSessionFactory.getFactory().getSession();
        try {
            Object args[] = new Object[3];
            args[0] = Reference.ShareReference;
            args[1] = start;
            args[2] = end;
            int i = 0;
            GetContent adapter = new GetContent(f);
            Iterator<Reference> iter = q.getIteratorFromQueryArgs(LinkEngineIndexerUtil.QueryReferenceHql, args);
            LinkSegment ls = indexFromReferences(iter);
            Log.linkengine.info("LinkSegmentWriter retrieved %s REFERENCE rows for dates %s to %s", adapter.getRowsRetrieved(), start.toString(), end.toString());
            return ls;
        } finally {
            DMSSessionFactory.getFactory().rollbackClose(q);
            DMSSessionFactory.getFactory().rollbackClose(f);
        }


    }

    public LinkSegment indexFromReferences(Iterator<Reference> iter) {
        IndexRow r = new IndexReferenceRow(LinkTupleReader.TypeRssShare,
                (byte) (LinkTupleReader.TypeRssShare | LinkTupleReader.TypeBit_Inbound));
        return indexIteratorIterator(iter, r, factory);
    }

    public LinkSegment indexFromLinkIterator(Iterator<Link> iter) {
        IndexRow r = new IndexLinkRow();
        return indexIteratorIterator(iter, r, factory);
    }


    public LinkSegment indexIteratorIterator(Iterator iter, IndexRow r, BaseFileAccessingObjectFactory factory) {
        //int maxLength, boolean removeDupes, Comparator<T> comp, File directory, String extension
        if (!iter.hasNext()) {
            // nothing todo.
            return null;
        }
        Comparator<IndexLink> comp = docIdBasedComparator;
        String fileExtension = "indexlinks";
        HTSerializableFileBucketWriter writer =
                new HTSerializableFileBucketWriter(20000, m_dir, fileExtension, factory);

        try {
            r.indexRows(iter, writer);
            writer.close();
            BaseFile files[] = m_dir.listFiles(new FileExtension(fileExtension, true));

            BaseFileSelectTreeController stc = new HTSelectTreeController(m_dir, files, 20, factory, true, fileExtension, true);
            BaseFile f = stc.merge();
            // now create a segment.
            if (!f.exists()) {
                LinkSegment ls = writeIndexLinkToSegment(f);
                // remove merged file.
                f.delete();
                return ls;
            }
        } catch (Exception e) {
            Log.linkengine.error("%s %e", e, e);
        }
        Log.linkengine.error("Merge produced no output file?");
        return null;
    }


    /**
     * Given a IndexLink file, that is ordered by fromDocId, toDocid, type we create a postings file and positionals
     * file
     *
     * @param linkFile
     * @return
     * @throws IOException
     */
    public LinkSegment writeIndexLinkToSegment(BaseFile linkFile) throws IOException {
        Iterator<IndexLink> ilIter = new HTSerializableInputIterator(linkFile, null);
        open();
        while (ilIter.hasNext()) {
            IndexLink il = ilIter.next();
            write(il.m_fromDocId, il.toDocId, il.m_type, il.m_metric, il.m_date);
        }
        return close();
    }

    /**
     * raw writer of an edge.  Should call open() first and make n calls to this function, followed by a close().  At
     * that point it will close the segment out too.
     *
     * @param fromId
     * @param toId
     * @param type
     * @param metric
     * @param date
     * @return
     * @throws IOException
     */
    public boolean write(long fromId, long toId, byte type, byte metric, short date) throws IOException {
        if (fromId > m_docId) {
            if (m_docId != 0) {
                // new entry  always starts with the docid
                rleWriter.close();
            }
            // from id only used during in memory work of initial write.  Not encoded in the disk form as the from ID
            // is determined from 
            fsos.writeLong(fromId);
            m_segment.setOffset(fromId, fsos.getFilePointer());
            m_docId = fromId;
        }

        rleWriter.writeTuple(toId, type, metric, date);
        return true;
    }

    public void open() throws IOException {
        rleWriter = new RLELinkTupleWriter();
        fsos = m_segment.getOutputStream();
        rleWriter.setOutputStream(fsos);
        m_docId = 0;
    }

    public LinkSegment close() throws IOException {
        rleWriter.close();
        fsos.close();
        // write the positionals hash table out.
        // note, this need not be a hashtable, it could be an ordered docid, offset pair....or alternatively
        // it could be derived from the postings file itself.
        m_segment.writePositional();

        return m_segment;
    }


}


class GetContent<E extends HTSerializable> implements HibernateQueryResultObjectAdapter<E> {
    private BaseSession m_session;

    private int m_count = 0;


    public GetContent(BaseSession session) {
        m_session = session;
    }

    public int getRowsRetrieved() {
        return m_count;
    }

    public E map(Iterator iter, Type[] types, String[] aliases) {
        m_session.rollback();
        if (!iter.hasNext()) {
            return null;
        }
        String guid = null;

        guid = (String) iter.next();

        Content cont = (Content) m_session.getHTSerializableFromGUID(guid);
        if (cont == null) {
            return null;
        }
        try {
            InputStream is = cont.getContent();
            if (is == null) {
                return null;
            }
            E e = (E) HTSerializableUtil.readHTSerializableFromBuffer(is, null);
            m_count++;
            is.close();
            return e;
        } catch (StoreException e) {
            Log.util.error("%s %e", e, e);
        } catch (FileNotFoundException e) {
            Log.util.error("%s %e", e, e);
        } catch (IOException e) {
            Log.util.error("%s %e", e, e);
        }

        return null;
    }


}

abstract class IndexRow {
    public abstract void indexRows(Iterator iter, HTSerializableFileBucketWriter writer) throws IOException;
}

class IndexLinkRow extends IndexRow {
    public IndexLinkRow() {

    }

    public void indexRows(Iterator iter, HTSerializableFileBucketWriter writer) throws IOException {
        byte inboundLink = (byte) (LinkTupleReader.TypeHRef | LinkTupleReader.TypeBit_Inbound);
        while (iter.hasNext()) {
            Link link = (Link) iter.next();

            IndexLink forward = new IndexLink();
            forward.setDate(link.getPublishedDate());
            forward.setHashes(link.getSourceHash(), link.getUrlHash());
            forward.setType(LinkTupleReader.TypeHRef);
            writer.add(forward);
            IndexLink backwards = new IndexLink();
            backwards.setDate(link.getPublishedDate());
            backwards.setHashes(link.getUrlHash(), link.getSourceHash());
            backwards.setType(inboundLink);
            writer.add(backwards);
        }
    }
}

class IndexReferenceRow extends IndexRow {
    private byte m_type;
    private byte inboundType;

    public IndexReferenceRow(byte type, byte inboundType) {
        m_type = type;
        this.inboundType = inboundType;

    }

    public void indexRows(Iterator iter, HTSerializableFileBucketWriter writer) throws IOException {
        while (iter.hasNext()) {
            Reference link = (Reference) iter.next();

            IndexLink forward = new IndexLink();
            long time = link.getDate().getTime();
            forward.setDate(time);
            long fromH = link.getFromHash();
            long toH = link.getToHash();
            if (fromH == 0) {
                fromH = FPHash64.getFP(link.getFromGuid());
            }

            if (toH == 0) {
                toH = FPHash64.getFP(link.getToGuid());
            }
            forward.setHashes(fromH, toH);
            forward.setType(m_type);
            writer.add(forward);
            IndexLink backwards = new IndexLink();
            backwards.setDate(time);
            backwards.setHashes(toH, fromH);
            backwards.setType(inboundType);
            writer.add(backwards);
        }
    }
}
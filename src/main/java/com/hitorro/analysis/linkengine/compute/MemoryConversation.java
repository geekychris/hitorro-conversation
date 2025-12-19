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

import com.hitorro.analysis.linkengine.Log;
import com.hitorro.base.objects.Post;
import com.hitorro.base.objects.User;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.conversationtracker.Conversation;
import com.hitorro.util.core.Console;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;
import org.hibernate.query.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved.
 * <p/>
 * User: chris
 */

@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.MemoryConversation,
        isView = false,
        isPersisted = false,
        schemaVersion = MemoryConversation.SerialVersion)

public class MemoryConversation implements HTSerializable {
    public static final int SerialVersion = 1;
    private static final String Q = "select post.guid, user.guid from Post as post left join post.containers as containers, Forum as forum, RssFeedIn as feed left join feed.forum as feedForum, User as user left join user.rssFeedIn as feedsIn where post.identityHash = :hash and  containers.id = forum.id and feedForum.id = forum.id and feedsIn.id = feed.id";
    /*public boolean createPersistedConv (Date fromDate, Date toDate)
    {
        DMSSession session = DMSSessionFactory.getFactory().getSession();
        DMSSession fetch = DMSSessionFactory.getFactory().getSession();
        Conversation conv = new Conversation();
        conv.setFromDate(fromDate);
        conv.setToDate(toDate);


        ensureFIeldsNotNull();

        boolean notFirst = false;

        mapMCAttributesToConversation(conv);
        // XXX TODO is this correct here?
        for (ReferringPost rp : this.m_referringPosts)
        {
            notFirst = getFill(fetch, session, rp.m_hash, conv);
        }
        if (conv.getPersonalities().size() > 0)
        {
            Iterator<User> iter = conv.getPersonalities().iterator();
            if (iter.hasNext())
            {
                conv.setPrimaryPersonality(iter.next());
            }
        }
        else
        {
            Log.conversation.error("Unable to set primary personality (2)");
        }

        session.persist(conv);
        DMSSessionFactory.getFactory().commitAndClose(session);
        DMSSessionFactory.getFactory().rollbackClose(fetch);

        return true;
    } */
    private static SpamRemovalRanker ranker = new SpamRemovalRanker();
    private String m_guid;
    private long m_hash;
    private String m_title;
    private String m_url;
    private String m_synopsis;
    private String m_primaryPersonality;
    private String m_primaryPersonalityGuid;
    private List<ReferringPost> m_referringPosts = new ArrayList<ReferringPost>();

    public void dump() {
        Console.println("guid: %s, hash: %s, title: %s, url: %s, user: %s, userGuid: %s", m_guid, m_hash, m_title, m_url, m_primaryPersonality, m_primaryPersonalityGuid);
        Console.println("Synopsis: %s", m_synopsis);
        for (ReferringPost post : m_referringPosts) {
            post.dump();
        }
    }

    public String getGuid() {
        return m_guid;
    }

    public long getHash() {
        return m_hash;
    }

    public void setHash(long hash) {
        m_hash = hash;
    }

    public String getTitle() {
        return m_title;
    }

    public String getUrl() {
        return m_url;
    }

    public String getSynopsis() {
        return m_synopsis;
    }

    public String getPrimaryPersonality() {
        return m_primaryPersonality;
    }

    public String getPrimaryPersonalityGuid() {
        return m_primaryPersonalityGuid;
    }

    public List<ReferringPost> getReferringPosts() {
        return m_referringPosts;
    }

    public void setPostDetails(Post post) {
        m_guid = post.getGuid();
        m_url = post.getMoreLink();
        m_hash = post.getIdentityHash();
        m_title = post.getTitle();
        String body = post.getBodyText();
        m_synopsis = StringUtil.truncateDecodedTextToLengthWithPostText(body, Post.MaxTitleSize, "...", "Unknown synopsis");
    }

    public void addReferrer(ReferringPost rp) {
        m_referringPosts.add(rp);
    }

    /**
     * Fill in the blanks for the case we are going to use the memory conversation.
     *
     * @return
     */
    public boolean fillInBlanks() {
        BaseSession fetch = DMSSessionFactory.getFactory().getSession();

        ensureFIeldsNotNull();

        boolean notFirst = false;

        for (ReferringPost rp : this.m_referringPosts) {
            notFirst = fillInRowBlanks(fetch, rp);
        }

        boolean foundPrimary = false;
        for (ReferringPost rp : this.m_referringPosts) {
            if (!StringUtil.nullOrEmptyString(rp.m_userGuid)) {
                foundPrimary = true;
                this.m_primaryPersonality = rp.m_user;
                this.m_primaryPersonalityGuid = rp.m_userGuid;
            }
        }
        if (foundPrimary == false) {
            Log.conversation.error("Unable to set primary personality (1)");
        }
        DMSSessionFactory.getFactory().rollbackClose(fetch);

        return true;
    }

    private boolean fillInRowBlanks(BaseSession fetchSession,
                                    ReferringPost post) {
        Query qq = ((DMSSession) fetchSession).createQuery(Q);
        Object row[] = null;
        qq.setParameter("hash", post.m_hash);

        List result = qq.list();
        if (result != null && result.size() > 0) {
            row = (Object[]) result.get(0);
            String p = (String) row[0];
            String u = (String) row[1];
            if (!StringUtil.nullOrEmptyString(p)) {
                return setOnRow(post, p, u, fetchSession);
            }

        }
        Post p = Post.getPostByMoreLinkHash(fetchSession, post.m_hash);
        if (p != null) {
            return setOnRow(post, p.getGuid(), null, fetchSession);
        }
        Log.conversation.warn("Unable to get Post for %s", post.m_hash);
        return false;
    }

    public boolean createOrUpdatePersistedConv(Date fromDate, Date toDate) {
        BaseSession session = DMSSessionFactory.getFactory().getSession();
        BaseSession fetch = DMSSessionFactory.getFactory().getSession();
        boolean newItem = false;
        Conversation conv = Conversation.getConversationByMoreLink(session, m_url);

        if (conv == null) {
            newItem = true;
            conv = new Conversation();
        } else {
            // for now lets get rid of the existing post references (later we may want to identify new ones.
            conv.getPosts().clear();
            conv.getPersonalities().clear();
        }
        conv.setFromDate(fromDate);
        conv.setToDate(toDate);


        ensureFIeldsNotNull();

        boolean notFirst = false;

        mapMCAttributesToConversation(conv);
        // XXX TODO is this correct here?
        for (ReferringPost rp : this.m_referringPosts) {
            notFirst = getFill(fetch, session, rp.m_hash, conv);
        }
        if (conv.getPersonalities().size() > 0) {
            Iterator<User> iter = conv.getPersonalities().iterator();
            if (iter.hasNext()) {
                conv.setPrimaryPersonality(iter.next());
            }
        } else {
            Log.conversation.error("Unable to set primary personality (2)");
        }

        if (newItem) {
            session.persist(conv);
        }
        DMSSessionFactory.getFactory().commitAndClose(session);
        DMSSessionFactory.getFactory().rollbackClose(fetch);

        return true;
    }

    private void mapMCAttributesToConversation(Conversation conv) {
        conv.setTopicUrl(m_url);
        conv.setSynopsis(m_synopsis);
        conv.setDetailsGuid("xxx");
        conv.setIdentityHash(m_hash);
        conv.setTitle(m_title);
    }

    private void ensureFIeldsNotNull() {
        if (StringUtil.nullOrEmptyString(m_url)) {
            m_url = "Unknown URL";
        }

        if (StringUtil.nullOrEmptyString(m_synopsis)) {
            m_synopsis = "Unknown";
        }

        if (StringUtil.nullOrEmptyString(m_title)) {
            m_title = "Unknown";
        }


        if (StringUtil.nullOrEmptyString(m_title)) {
            m_title = "Unknown";
        }
    }


    private boolean getFill(BaseSession fetchSession,
                            BaseSession session,
                            long hash,
                            Conversation conv) {
        Query qq = ((DMSSession) fetchSession).createQuery(Q);
        Object row[] = null;
        qq.setParameter("hash", hash);

        List result = qq.list();
        if (result != null && result.size() > 0) {
            row = (Object[]) result.get(0);
            String p = (String) row[0];
            String u = (String) row[1];
            if (!StringUtil.nullOrEmptyString(p)) {
                return add(conv, p, u, session);
            }

        }
        Post p = Post.getPostByMoreLinkHash(fetchSession, hash);
        if (p != null) {
            return add(conv, p.getGuid(), null, session);
        }
        Log.conversation.warn("Unable to get Post for %s", hash);
        return false;
    }

    private boolean add(Conversation conv, String postGuid, String userGuid, BaseSession session) {
        Post p = (Post) session.getHTSerializableFromGUID(postGuid);
        if (p != null) {
            conv.getPosts().add(p);

            // set dates on conversation
            Date firstPost = conv.getFirstPostDate();
            Date lastPost = conv.getLastPostDate();
            Date postDate = p.getAuthoredDate();
            if (postDate != null) {
                if (firstPost == null || firstPost.after(postDate)) {
                    conv.setFirstPostDate(postDate);
                }
                if (lastPost == null || lastPost.before(postDate)) {
                    conv.setLastPostDate(postDate);
                }
            }
        } else {
            return false;
        }
        if (!StringUtil.nullOrEmptyString(userGuid)) {
            User u = (User) session.getHTSerializableFromGUID(userGuid);
            if (u != null) {
                conv.getPersonalities().add(u);
            }
        }
        return true;

    }

    private boolean setOnRow(ReferringPost row, String postGuid, String userGuid, BaseSession session) {
        Post p = (Post) session.getHTSerializableFromGUID(postGuid);
        if (p != null) {
            row.m_guid = postGuid;
            row.m_moreLink = p.getMoreLink();
        } else {
            return false;
        }
        if (!StringUtil.nullOrEmptyString(userGuid)) {
            User u = (User) session.getHTSerializableFromGUID(userGuid);
            if (u != null) {
                row.m_user = u.getName();
                row.m_userGuid = userGuid;
            }
        }
        return true;

    }

    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        os.writeString(m_guid);
        os.writeString(m_title);
        os.writeString(m_url);
        os.writeString(m_synopsis);
        os.writeLong(m_hash);
        os.writeString(m_primaryPersonality);
        os.writeString(m_primaryPersonalityGuid);
        os.writeListOfHTSerializable(m_referringPosts);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        m_guid = os.readString();
        m_title = os.readString();
        m_url = os.readString();
        m_synopsis = os.readString();
        m_hash = os.readLong();
        m_primaryPersonality = os.readString();
        m_primaryPersonalityGuid = os.readString();
        os.readListOfHTSerializable(m_referringPosts);
    }

    public int getSerializationVersion() {
        return SerialVersion;
    }

    public boolean isPersisted() {
        return false;
    }

    public boolean hasGuid() {
        return false;
    }

    public boolean hasSoftGuid() {
        return false;
    }
}
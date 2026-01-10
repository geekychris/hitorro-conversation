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
package com.hitorro.conversationtracker;

import com.hitorro.analysis.linkengine.Log;
import com.hitorro.base.objects.Document;
import com.hitorro.base.objects.Post;
import com.hitorro.base.objects.User;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.base.typesystem.accessors.GuidAccessor;
import com.hitorro.base.typesystem.btadapter.rssadapters.VersionableObjectRssItem;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.basetext.inverter.TFIDFTermMeasureFunction;
import com.hitorro.basetext.inverter.TermTupleSet;
import com.hitorro.basetext.inverter.TermTupleSetGroup;
import com.hitorro.language.Iso639Table;
import com.hitorro.language.IsoLanguage;
import com.hitorro.util.core.GenericKeyValue;
import com.hitorro.util.core.hash.FPHash64;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.urlparser.URLUtil;
import com.hitorro.util.urlparser.UrlCollectionStats;
import org.hibernate.query.Query;

import jakarta.persistence.*;

import java.io.IOException;
import java.util.*;

/**
 * <p/>
 * What we do need for a Conversation is: -The domain of the Conversation (Tech, Poetry, Sewing) - this could be a
 * reference to an RssFeedGroup for now. What is the topic (url for external post?  listFiles of terms? reference to a Post
 * that we have?)
 * <p/>
 * - What is the user-visible topic - this is a String - What is the user-visible synopsis - this is a longer string -
 * Who is involved in the Conversation? - listFiles of Personalities and listFiles of feeds - Who is the primary personality, if
 * any - a Personality reference  (this would be the person who started the conversation, or had something critical to
 * say, or whatever) - The "date" of the conversation.  Maybe a span of dates of the posts?  Maybe the date of
 * calculation?  Probably both. - A ranking of the conversation, relative to other conversations. A listFiles of reference
 * URLs, each with a title.  This is to enable "for more information" kind of links to encyclopedia articles, press
 * releases from our customers, and so on.
 */
@Entity
@Table(name = "Conversation")
@PrimaryKeyJoinColumn(name = "system_id")
@com.hitorro.util.typesystem.annotation.TypeClassMetaInfo(shortTypeName = com.hitorro.util.typesystem.annotation.TypeClassMetaInfo.Conversation,
        adapters = {@com.hitorro.util.typesystem.annotation.AdapterClassMeta(className = ConversationtRssItem.class, adapterGroup = VersionableObjectRssItem.AdapterGroup)},
        isView = false,
        isPersisted = true,
        schemaVersion = Conversation.SerializationVersion,
        guidAccessor = GuidAccessor.class)
@com.hitorro.util.typesystem.annotation.UiTypeProperties(name = "Conversation", views = {@com.hitorro.util.typesystem.annotation.ViewClassReference(name = com.hitorro.util.typesystem.annotation.ViewClassReference.ListView, viewClass = Conversation.ConversationListView.class),
        @com.hitorro.util.typesystem.annotation.ViewClassReference(name = com.hitorro.util.typesystem.annotation.ViewClassReference.EditView, viewClass = Conversation.ConversationEditView.class)})
public class Conversation extends Document {
    public static final int SerializationVersion = 1;
    /**
     * Fetch the post by its hash of the more link.
     *
     * @param session  database session
     * @param forumid  Id of the containing forum
     * @param moreLink link we're looking for
     * @return the post, or null if not found
     */

    private static final String ConversationByIdentityHashForum = "select bentry from " + Conversation.class.getName() + " as bentry " +
            "where bentry.identityHash = :hash";
    
    // transient things
    @Transient
    private transient boolean isValid = true;
    
    @Transient
    private UrlCollectionStats urlCollectionStats = null;
    
    //persisted things
    @Column(name = "topicUrl", nullable = false, length = 1024)
    private String m_topicUrl;
    
    @Column(name = "synopsis", nullable = false, length = 1024)
    private String m_synopsis;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "conversation_user",
        joinColumns = @JoinColumn(name = "conversation_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> m_personalities;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "primaryPersonality")
    private User m_primaryPersonality;
    
    @Column(name = "firstPostDate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date m_firstPostDate;
    
    @Column(name = "lastPostDate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date m_lastPostDate;
    
    @Column(name = "conversationRank", nullable = false)
    private double conversationRank;
    
    @Column(name = "detailsGuid", nullable = false, length = 20)
    private String detailsGuid;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "conversation_post",
        joinColumns = @JoinColumn(name = "conversation_id"),
        inverseJoinColumns = @JoinColumn(name = "post_id")
    )
    private Set<Post> m_posts;
    
    @Column(name = "fromDate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fromDate;
    
    @Column(name = "toDate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date toDate;
    
    @Column(name = "originalAuthor")
    private String originalAuthor;
    // no persist
    private transient List<GenericKeyValue<String, Double>> m_rankedPostsList = null;

    public Conversation() {
        m_posts = new HashSet<Post>();
        m_personalities = new HashSet<User>();
    }

    public static Conversation getConversationByMoreLink(BaseSession session, String moreLink) {
        Query qq = ((DMSSession) session).createQuery(ConversationByIdentityHashForum);
        Conversation bl = null;
        moreLink = URLUtil.cleanupUrl(moreLink);
        long fp = calculateIdentityHash(moreLink);
        qq.setParameter("hash", fp);
        List result = qq.list();
        if (result != null && result.size() > 0) {
            bl = (Conversation) result.get(0);
        }

        return bl;
    }

    public static long calculateIdentityHash(String link) {
        return FPHash64.getFP(link);
    }

    public static Conversation getConversationForTopic(DMSSession session, String topicUrl) {
        return (Conversation) session.getObject(Conversation.class, "where topicUrl= :a", topicUrl);
    }

    public String getOriginalAuthor() {
        return originalAuthor;
    }

    public void setoriginalAuthor(String name) {
        originalAuthor = name;
    }

    public User getPrimaryPersonalityWithFailover() {
        User personality = getPrimaryPersonality();
        if (personality == null) {
            // try to use any old person as the primary
            Set<User> ps = getPersonalities();
            if (ps != null && ps.size() > 0) {
                for (User usr : ps) {
                    personality = usr;
                    break;
                }
            }
        }
        if (personality == null) {
            return null;
        }
        return personality;
    }

    public List<GenericKeyValue<String, Double>> getPostSimilarityRank(BaseSession session) {
        m_rankedPostsList = new ArrayList<GenericKeyValue<String, Double>>();
        Post convPost = Post.getPostByMoreLink(session, getTopicUrl());
        if (convPost != null) {
            TermTupleSetGroup convGroup = getGroup(convPost);
            TermTupleSetGroup targetGroup;
            TermTupleSet sourceBody = convGroup.getByName("body");
            for (Post post : getPosts()) {
                targetGroup = getGroup(post);
                MatchVisitor bodyVisitor = new MatchVisitor();

                bodyVisitor.reset();
                TermTupleSet targetBody = targetGroup.getByName("body");

                sourceBody.matchByHash(bodyVisitor, targetBody);
                m_rankedPostsList.add(new GenericKeyValue<String, Double>(post.getGuid(), new Double(bodyVisitor.getVal())));
            }
        }
        Collections.sort(m_rankedPostsList, new RankComparitor());
        return m_rankedPostsList;
    }

    private TermTupleSetGroup getGroup(Post post) {
        // XXX todo get language out of post
        IsoLanguage language = Iso639Table.english;
        TermTupleSetGroup group = new TermTupleSetGroup("body", null,
                TermTupleSet.s_MeasureDescendComparitor,
                new TFIDFTermMeasureFunction());
        group.setId(post.getGuid());
        try {
            group.add("body", post.getBodyText(), language);
        } catch (IOException e) {
            Log.util.error("%s %e", e, e);
        }
        return group;
    }

    public void copy(VersionableObject orig) {
        super.copy(orig);
        Conversation other = (Conversation) orig;

        m_topicUrl = other.m_topicUrl;
        m_synopsis = other.m_synopsis;

        HashSet<User> temp = new HashSet<User>();
        // have to copy this way because of hibernate
        for (User c : other.m_personalities) {
            temp.add(c);
        }
        m_personalities = temp;

        HashSet<Post> temp2 = new HashSet<Post>();
        // have to copy this way because of hibernate
        for (Post c : other.m_posts) {
            temp2.add(c);
        }
        m_posts = temp2;

        m_primaryPersonality = other.m_primaryPersonality;
        m_firstPostDate = other.m_firstPostDate;
        m_lastPostDate = other.m_lastPostDate;
        conversationRank = other.conversationRank;
        detailsGuid = other.detailsGuid;
    }

    public void serialize(HTObjectOutputStream os)
            throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeString(m_topicUrl);
        os.writeString(m_synopsis);
        os.writeSetOfBaseType(m_personalities);
        os.writeVersionedObject(m_primaryPersonality);
        os.writeDate(m_firstPostDate);
        os.writeDate(m_lastPostDate);
        os.writeDouble(conversationRank);
        os.writeString(detailsGuid);
        os.writeSetOfBaseType(m_posts);

    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);

        switch (version) {
            case 1:
                m_topicUrl = os.readString();
                m_synopsis = os.readString();
                os.readSetOfHTSerializable(m_personalities);
                m_primaryPersonality = (User) os.readVersionedObject();
                m_firstPostDate = os.readDate();
                m_lastPostDate = os.readDate();
                conversationRank = os.readDouble();
                detailsGuid = os.readString();
                os.readSetOfHTSerializable(m_posts);
        }
    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }

    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Topic", displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay, order = 40)
    public String getTopicUrl() {
        return m_topicUrl;
    }

    public void setTopicUrl(String m_topicUrl) {
        this.m_topicUrl = m_topicUrl;
    }

    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Synopsis", displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay, order = 50)
    public String getSynopsis() {
        return m_synopsis;
    }

    public void setSynopsis(String m_synopsis) {
        this.m_synopsis = m_synopsis;
    }

    public Set<User> getPersonalities() {
        return m_personalities;
    }

    public void setPersonalities(Set<User> m_personalities) {
        this.m_personalities = m_personalities;
    }

    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Primary Personality", displayType = com.hitorro.util.typesystem.annotation.UiProperties.VersionableObjectDisplay, order = 30)
    public User getPrimaryPersonality() {
        return m_primaryPersonality;
    }

    public void setPrimaryPersonality(User m_primaryPersonality) {
        this.m_primaryPersonality = m_primaryPersonality;
    }

    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "First Post Date", displayType = com.hitorro.util.typesystem.annotation.UiProperties.DateDisplay, order = 70)
    public Date getFirstPostDate() {
        return m_firstPostDate;
    }

    public void setFirstPostDate(Date m_firstPostDate) {
        this.m_firstPostDate = m_firstPostDate;
    }

    public Date getLastPostDate() {
        return m_lastPostDate;
    }

    public void setLastPostDate(Date m_lastPostDate) {
        this.m_lastPostDate = m_lastPostDate;
    }

    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Rank", displayType = com.hitorro.util.typesystem.annotation.UiProperties.DoubleFieldDisplay, order = 60)
    public double getConversationRank() {
        return conversationRank;
    }

    public void setConversationRank(double conversationRank) {
        this.conversationRank = conversationRank;
    }

    public String getDetailsGuid() {
        return detailsGuid;
    }

    public void setDetailsGuid(String val) {
        detailsGuid = val;
    }

    public Set<Post> getPosts() {
        return m_posts;
    }

    public void setPosts(Set<Post> val) {
        m_posts = val;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public UrlCollectionStats getUrlCollectionStats() {
        if (urlCollectionStats == null) {
            urlCollectionStats = new UrlCollectionStats();
            for (Post p : this.getPosts()) {
                urlCollectionStats.add(p.getMoreLink());

            }
        }

        return urlCollectionStats;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    /**
     * View class enumerating which fields to show when listing.
     */
    @com.hitorro.util.typesystem.annotation.TypeClassMetaInfo(shortTypeName = "ConversationListView",
            isView = true,
            isPersisted = false,
            schemaVersion = Conversation.SerializationVersion)
    public abstract static class ConversationListView {
        public abstract String getTopicUrl();

        public abstract String getSynopsis();

        public abstract String getTitle();
    }

    /**
     * View class enumerating which fields to show when editing.
     */
    @com.hitorro.util.typesystem.annotation.TypeClassMetaInfo(shortTypeName = "ConversationEditView",
            isView = true,
            isPersisted = false,
            schemaVersion = Conversation.SerializationVersion)
    public abstract static class ConversationEditView {
        public abstract String getTopicUrl();

        public abstract String getSynopsis();

        public abstract String getTitle();

        public abstract double getConversationRank();

        public abstract Date getFirstPostDate();

        public abstract User getPrimaryPersonality();
    }
}


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
package com.hitorro.basedms;

import com.hitorro.base.objects.ExternalContent;
import com.hitorro.basedms.xml.RssItemInterface;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Oct 3, 2005 Time: 8:29:33 PM
 */

@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.RssItemCacheProxy,
        isView = false,
        isPersisted = false,
        schemaVersion = RssItemCacheProxy.SerialVersion)
public class RssItemCacheProxy implements RssItemInterface, HTSerializable {
    public static final int SerialVersion = 1;
    private String guid;
    private String description;
    private String referringAuthDispName;
    private String authDisplayName;
    private String feedContactAuthorDisplayName;
    private String comments;

    private String link;
    private String imageUrl;
    private String title;

    private Date postDate;


    private boolean hasAd;
    private String adUrl;
    private String adText;
    private String sponsored;
    private boolean hasExternalUrl;

    private String clickThroughText;
    private String clickThroughUrl;
    private boolean hasEnclosure;
    private int degree;
    private List<String> topWords;
    private List<ExternalContent> enclosures;
    private List<String> categoriesList;

    private String postId;
    private String type;
    private String tld;
    private String show;
    private String alternateQuery;

    public String getPostId() {
        return postId;
    }

    public void set(RssItemInterface r) {
        guid = r.getGuid();
        description = r.getDescription();
        referringAuthDispName = r.getReferringAuthorDisplayName();
        authDisplayName = r.getAuthorDisplayName();
        feedContactAuthorDisplayName = r.getFeedContactAuthorDisplayName();
        comments = r.getComments();
        postDate = r.getPostDate();
        link = r.getLink();
        imageUrl = r.getImageUrl();
        title = r.getTitle();
        hasAd = r.hasAd();
        adUrl = r.getAdUrl();
        adText = r.getAdText();
        sponsored = r.getSponsored();
        hasExternalUrl = r.hasExternalUrl();

        clickThroughText = r.getClickThroughText();
        clickThroughUrl = r.getClickThroughUrl();
        hasEnclosure = r.hasEnclosure();
        degree = r.getDegree();
        topWords = r.getTopWords();
        enclosures = r.getEnclosures();
        categoriesList = r.getCategoriesList();
        type = r.getType();
        tld = r.getTLD();
        show = r.getShow();
        alternateQuery = r.getAlternateQuery();
        postId = r.getPostId();
    }

    public String getType() {
        return type;
    }

    public String getTLD() {
        return tld;
    }

    public String getShow() {
        return show;
    }

    public String getAlternateQuery() {
        return alternateQuery;
    }

    public String getGuid() {
        return guid;
    }

    public String getDescription() {
        return description;
    }

    public String getReferringAuthorDisplayName() {
        return referringAuthDispName;
    }

    public String getAuthorDisplayName() {
        return authDisplayName;
    }

    public String getFeedContactAuthorDisplayName() {
        return feedContactAuthorDisplayName;
    }

    public String getComments() {
        return comments;
    }

    public Date getPostDate() {
        return postDate;
    }

    public String getLink() {
        return link;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getCategoriesList() {
        return categoriesList;
    }


    // Advertising
    public boolean hasAd() {
        return hasAd;
    }

    public String getAdUrl() {
        return adUrl;
    }

    public String getAdText() {
        return adText;
    }

    public String getSponsored() {
        return sponsored;
    }


    public boolean hasExternalUrl() {
        return hasExternalUrl;
    }

    // click through
    public String getClickThroughText() {
        return clickThroughText;
    }

    public String getClickThroughUrl() {
        return clickThroughUrl;
    }


    public boolean hasEnclosure() {
        return hasEnclosure;
    }

    public int getDegree() {
        return degree;
    }


    public List<String> getTopWords() {
        return topWords;
    }

    public void setTopWords(List<String> words) {
        topWords = words;
    }

    // enclosure

    public List<ExternalContent> getEnclosures() {
        return enclosures;
    }

    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        os.writeString(guid);
        os.writeString(description);
        os.writeString(referringAuthDispName);
        os.writeString(authDisplayName);
        os.writeString(feedContactAuthorDisplayName);
        os.writeString(comments);

        os.writeString(link);
        os.writeString(imageUrl);
        os.writeString(title);

        os.writeDate(postDate);

        os.writeBoolean(hasAd);
        os.writeString(adUrl);
        os.writeString(adText);
        os.writeString(sponsored);
        os.writeBoolean(hasExternalUrl);

        os.writeString(clickThroughText);
        os.writeString(clickThroughUrl);
        os.writeBoolean(hasEnclosure);
        os.writeInt(degree);
        os.writeListOfString(topWords);
        os.writeListOfHTSerializable(enclosures);
        os.writeListOfString(categoriesList);
        os.writeString(postId);
    }

    public void deserialize(HTObjectInputStream os) throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        guid = os.readString();
        description = os.readString();
        referringAuthDispName = os.readString();
        authDisplayName = os.readString();
        feedContactAuthorDisplayName = os.readString();
        comments = os.readString();

        link = os.readString();
        imageUrl = os.readString();
        title = os.readString();

        postDate = os.readDate();

        hasAd = os.readBoolean();
        adUrl = os.readString();
        adText = os.readString();
        sponsored = os.readString();
        hasExternalUrl = os.readBoolean();

        clickThroughText = os.readString();
        clickThroughUrl = os.readString();
        hasEnclosure = os.readBoolean();
        degree = os.readInt();
        topWords = os.readListOfStrings();

        enclosures = os.readListOfHTSerializable();
        categoriesList = os.readListOfStrings();
        postId = os.readString();
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
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
package com.hitorro.conversationtracker.feeds;

import com.hitorro.base.objects.SubjectArea;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.job.JobParameters;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;
import com.hitorro.util.typesystem.annotation.UiProperties;
import com.hitorro.util.typesystem.annotation.UiTypeProperties;
import com.hitorro.util.typesystem.annotation.ViewClassReference;

import java.io.IOException;

/**
 * Parameters for the ScrapeFeedsJob job
 */
@TypeClassMetaInfo(shortTypeName = "ScrapeFeedsJobParameters",
        isView = false,
        isPersisted = true,
        schemaVersion = 1)
@UiTypeProperties(name = "Feed Scraper Parameters", views = {@ViewClassReference(name = ViewClassReference.ListView, viewClass = ScrapeFeedsJobParameters.ScrapeFeedsParametersListView.class),
        @ViewClassReference(name = ViewClassReference.EditView, viewClass = ScrapeFeedsJobParameters.ScrapeFeedsParametersEditView.class)})
public class ScrapeFeedsJobParameters extends JobParameters {
    private static final int SerializationVersion = 1;
    private String _readUrl;
    private String _areaName;

    public ScrapeFeedsJobParameters() {
        _readUrl = "";
        _areaName = "Unsorted";
    }


    public String getJobName() {
        return ScrapeFeedsJob.ScrapeFeeds;
    }

    @UiProperties(displayName = "Read URL", displayType = UiProperties.TextFieldDisplay)
    public String getReadUrl() {
        return _readUrl;
    }

    public void setReadUrl(String url) {
        _readUrl = url;
    }

    /*
    @UiProperties(displayName = "Feed Group Name", displayType = UiProperties.TextFieldDisplay)
    public String getFeedGroupName ()
    {
        return _areaName;
    }

    public void setFeedGroupName (String nm)
    {
        _areaName = nm;
    }
    */

    @UiProperties(displayName = "Area", displayType = UiProperties.VersionableObjectDisplay)
    public SubjectArea getArea() {
        BaseSession session = getEditingSession();
        return SubjectArea.getSubjectAreaForName(session, _areaName);
    }

    public void setArea(SubjectArea val) {
        _areaName = val.getName();
    }

    // ----------------- HTSerializable

    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeString(_readUrl);
        os.writeString(_areaName);
    }

    public void deserialize(HTObjectInputStream os) throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);
        switch (version) {
            case 1:
                _readUrl = os.readString();
                _areaName = os.readString();
                if (_areaName.equals("Tech")) {
                    _areaName = "Technology";
                }
        }
    }

    public int getSerializationVersion() {
        return SerializationVersion;
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

    /**
     * View class enumerating which fields to show when listing.
     */
    @TypeClassMetaInfo(shortTypeName = "ScrapeFeedsParametersListView",
            isView = true,
            isPersisted = false,
            schemaVersion = SerializationVersion)
    public abstract static class ScrapeFeedsParametersListView {
        public abstract String getReadUrl();
    }

    /**
     * View class enumerating which fields to show when editing.
     */
    @TypeClassMetaInfo(shortTypeName = "ScrapeFeedsParametersEditView",
            isView = true,
            isPersisted = false,
            schemaVersion = SerializationVersion)
    public abstract static class ScrapeFeedsParametersEditView {
        public abstract String getReadUrl();

        public abstract SubjectArea getArea();
    }
}

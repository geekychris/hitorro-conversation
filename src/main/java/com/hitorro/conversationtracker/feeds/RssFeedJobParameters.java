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
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;
import com.hitorro.util.typesystem.annotation.UiProperties;
import com.hitorro.util.typesystem.annotation.UiTypeProperties;
import com.hitorro.util.typesystem.annotation.ViewClassReference;

import java.io.IOException;

/**
 * Parameters for the RssFeedJob.
 */
@TypeClassMetaInfo(shortTypeName = "RssFeedJobParameters",
        isView = false,
        isPersisted = true,
        schemaVersion = 1)
@UiTypeProperties(name = "Feed Reader Parameters", views = {@ViewClassReference(name = ViewClassReference.ListView, viewClass = RssFeedJobParameters.RssFeedJobParametersListView.class),
        @ViewClassReference(name = ViewClassReference.EditView, viewClass = RssFeedJobParameters.RssFeedJobParametersEditView.class)})
public class RssFeedJobParameters extends JobParameters {
    private static final int SerializationVersion = 1;
    private String _areaName;

    public RssFeedJobParameters() {
        _areaName = "Technology";
    }

    public String getJobName() {
        return RssFeedJob.RssFeedJob;
    }

    @UiProperties(displayName = "Area", displayType = UiProperties.VersionableObjectDisplay)
    public SubjectArea getSubjectArea() {
        return SubjectArea.getSubjectAreaForName(getEditingSession(), _areaName);
    }

    public void setSubjectArea(SubjectArea val) {
        _areaName = (val != null) ? val.getName() : null;
    }

    public void setAreaName(String val) {
        _areaName = val;
    }

    // ----------------- HTSerializable

    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeString(_areaName);
    }

    public void deserialize(HTObjectInputStream os) throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);
        switch (version) {
            case 1:
                _areaName = os.readString();
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
    @TypeClassMetaInfo(shortTypeName = "RssFeedJobParametersListView",
            isView = true,
            isPersisted = false,
            schemaVersion = RssFeedJobParameters.SerializationVersion)
    public abstract static class RssFeedJobParametersListView {
        public abstract String getSubjectArea();
    }

    /**
     * View class enumerating which fields to show when editing.
     */
    @TypeClassMetaInfo(shortTypeName = "RssFeedJobParametersEditView",
            isView = true,
            isPersisted = false,
            schemaVersion = RssFeedJobParameters.SerializationVersion)
    public abstract static class RssFeedJobParametersEditView {
        public abstract String getSubjectArea();
    }
}

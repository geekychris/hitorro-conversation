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

import com.hitorro.basedms.session.DMSSession;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.basedms.session.HibernateQueryResultObjectAdapter;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.util.commandandcontrol.Command;
import com.hitorro.util.commandandcontrol.CommandSession;
import com.hitorro.util.commandandcontrol.Response;
import com.hitorro.util.commandandcontrol.ResponseShape;
import com.hitorro.util.commandandcontrol.ano.CommandArgument;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.commandandcontrol.ano.RespColumn;
import com.hitorro.util.commandandcontrol.ano.ResponseDefinition;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.json.keys.StringProperty;
import com.hitorro.util.typesystem.BaseSession;
import org.hibernate.type.Type;

import java.util.Iterator;

/**
 * http://localhost:8072/external/action?verb=conversationquery&personality=robert Copyright (c) 2007 HiTorro All rights
 * reserved.
 * <p/>
 * User: chris
 */
@CommandDef(command = "conversation.query", description = "query conversations", isInternal = false)
public class ConversationQuery extends Command {
    @CommandArgument(required = true)
    public static StringProperty PersonalityProperty = new StringProperty("personality", "personality", "");

    @ResponseDefinition(command = "conversations",
            rowname = "conversation",
            columns = {@RespColumn(name = "Topic Url", lName = "topicurl"),
                    @RespColumn(name = "Title", lName = "title"),
                    @RespColumn(name = "Guid", lName = "guid")})
    private ResponseShape shape = new ResponseShape();

    public boolean execute(String rawValue, JVS args, Response response, CommandSession cmdSession) {
        String personality = PersonalityProperty.apply(args);
        BaseSession session = DMSSessionFactory.getFactory().getSession();

        try {

            MapIt mapIt = new MapIt();
            Iterator<ConversationResult> iter = mapIt.get(session, personality);
            response.setResponseShape(shape);
            while (iter.hasNext()) {
                mapIt.addRow(response, iter.next());
            }
            response.end();

        } finally {

        }
        return false;
    }
}

class MapIt implements HibernateQueryResultObjectAdapter {
    public Iterator<ConversationResult> get(BaseSession session, String personality) {
        String query = Fmt.S("select topicUrl, title, guid from Conversation where primaryPersonality.name='%s'", personality);
        return ((DMSSession) session).getIteratorFromQuery(this, query);
    }

    public ConversationResult map(Iterator iter, Type[] types, String[] aliases) {
        if (iter.hasNext()) {
            Object args[] = (Object[]) iter.next();

            ConversationResult result = new ConversationResult(args);
            return result;
        }
        return null;
    }


    public void addRow(Response response, ConversationResult cr) {
        response.addRow(cr.getTopicUrl(), cr.getTitle(), cr.getGuid());
    }
}

class ConversationResult {
    private String topicUrl;
    private String title;
    private String guid;

    ConversationResult(Object args[]) {
        topicUrl = args[0].toString();
        title = args[1].toString();
        guid = args[2].toString();
    }

    public String getTopicUrl() {
        return topicUrl;
    }

    public void setTopicUrl(String topicUrl) {
        this.topicUrl = topicUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }
}
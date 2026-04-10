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
package com.hitorro.basetext.commands;

import com.hitorro.analysis.linkengine.compute.ComputeConversation;
import com.hitorro.analysis.linkengine.compute.MemoryConversation;
import com.hitorro.base.objects.Post;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.basetext.termmatch.Ranker;
import com.hitorro.basetext.termmatch.RankerResult;
import com.hitorro.basetext.termmatch.RankerResultRow;
import com.hitorro.basetext.termmatch.UrlTermMatchQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.util.commandandcontrol.ano.CommandArgument;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.commandandcontrol.ano.RespColumn;
import com.hitorro.util.commandandcontrol.ano.ResponseDefinition;
import com.hitorro.util.core.Console;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.json.keys.StringProperty;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTSerializable;

import java.io.IOException;
import java.util.List;

/**
 */
@CommandDef(command = "text.similarity", description = "Test similarity of documents to a fetched page")
public class SimilarityCommand extends com.hitorro.util.commandandcontrol.Command {
    @CommandArgument(required = true)
    public static final StringProperty Url = new StringProperty("url", "url to fetch", null);

    @ResponseDefinition(command = "dumpversiontree",
            rowname = "document",
            columns = {@RespColumn(name = "Guid", lName = "guid"),
                    @RespColumn(name = "Score", lName = "score"),
                    @RespColumn(name = "Hash", lName = "hash"),
                    @RespColumn(name = "Url", lName = "url")})
    private com.hitorro.util.commandandcontrol.ResponseShape header = new com.hitorro.util.commandandcontrol.ResponseShape();

    public boolean execute(String rawValue, JsonNode args, com.hitorro.util.commandandcontrol.Response response, com.hitorro.util.commandandcontrol.CommandSession session) {
        String url = Url.apply(args);
        /*testConversation(url);
        if (1 == 1)
        {
            return true;
        }*/

        UrlTermMatchQuery query = new UrlTermMatchQuery();
        //GuidTermMatchQuery query = new GuidTermMatchQuery();
        query.setQuery(url);
        Ranker ranker = new Ranker();
        RankerResult result = ranker.rank(query, 20, (float) 0.3, 5);
        if (result == null) {
            writeSimpleError(response, "No result return");
            return false;
        }
        if (!StringUtil.nullOrEmptyString(result.getError())) {
            writeSimpleError(response, result.getError());
            return false;
        }
        List<RankerResultRow> rows = result.getRows();
        response.setResponseShape(header);
        BaseSession sess = DMSSessionFactory.getFactory().getSession();
        try {
            for (RankerResultRow row : rows) {
                response.addRow(row.getGuid(), row.getScore(), row.getHash(), row.getUrl());
                //dumpPost(row, sess);

            }
        } finally {
            DMSSessionFactory.getFactory().rollbackCloseSession(sess);
        }
        response.addInfo(com.hitorro.util.commandandcontrol.InfoLevel.Info, Fmt.S("Terms Used: %s", StringUtil.mergeWithJoinToken(result.getTermsUsed(), ",")));
        return true;
    }

    private void dumpPost(RankerResultRow row, DMSSession sess) {
        HTSerializable pts = sess.getHTSerializableFromGUID(row.getGuid());
        if (pts instanceof Post) {
            Post item = (Post) pts;
            Console.println("%s : %s, text: %s", row.getGuid(), row.getScore(), item.getContentText());
            Console.println();
            Console.println("============================================================");

        } else {
            Console.println("Not a feed item %s", pts.getClass().getCanonicalName());
        }
    }

    public void testConversation(String url) throws IOException {
        UrlTermMatchQuery query = new UrlTermMatchQuery();
        query.setQuery(url);
        ComputeConversation cc = new ComputeConversation();
        List<MemoryConversation> list = cc.getConversationsSimilarTo(query, 30, 3000, 4, 50, (float) 0.3, "Technology");

        for (MemoryConversation mc : list) {
            mc.fillInBlanks();
            mc.dump();
            Console.println("====================================================================");
        }
    }

}

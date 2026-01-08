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

import com.hitorro.basetext.inverter.TermTuple;
import com.hitorro.basetext.inverter.TermTupleSetVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 */
class MatchVisitor implements TermTupleSetVisitor {
    private double m_val;
    private List<String> m_words = new ArrayList<String>();

    public void reset() {
        m_val = 0;
    }

    public double getVal() {
        return m_val;
    }

    public boolean visit(TermTuple t, TermTuple otherT, Mode mode) {
        // for now we assume that the left tuple is what we are simply doing a tf/idf sum.
        // this of course is not really sufficient as we are not considering the frequency of occurences on the
        // right
        if (t.isGood()) {
            m_val += t.getMeasure();
            m_words.add(t.getTerm());
        }
        return true;
    }
}

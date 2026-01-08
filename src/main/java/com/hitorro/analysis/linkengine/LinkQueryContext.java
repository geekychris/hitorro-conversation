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
import com.hitorro.util.core.hash.FPHash64;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

/**
 */
public class LinkQueryContext extends TuplePool {
    LinkQueryContext m_next;
    private LinkEngine m_engine;

    private Lock m_lock;

    public LinkQueryContext(LinkEngine engine, Lock lock) {
        m_engine = engine;
        m_lock = lock;
        lock();
    }

    public LinkTupleReader getReaderForStringToken(String token) throws IOException {
        return getReaderForHash(FPHash64.getFP(token));
    }

    public LinkTupleReader getReaderForDocId(long docId) throws IOException {
        return m_engine.getReader(this, docId);
    }

    public LinkTupleReader getReaderForHash(long docId) throws IOException {

        if (docId == 0) {
            return null;
        }
        return m_engine.getReader(this, docId);
    }

    void lock() {
        m_lock.lock();
    }

    void release() {
        m_lock.unlock();
    }
}

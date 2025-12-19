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

import com.hitorro.util.core.GenericKeyValue;

import java.util.Comparator;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Mar 29, 2005 Time: 10:03:02 AM
 */
class RankComparitor implements Comparator<GenericKeyValue<String, Double>> {
    public int compare(GenericKeyValue<String, Double> genericKeyValue, GenericKeyValue<String, Double> genericKeyValue1) {
        double d1 = genericKeyValue.getValue().doubleValue();
        double d2 = genericKeyValue1.getValue().doubleValue();
        if (d1 > d2) {
            return -1;
        }
        if (d1 < d2) {
            return 1;
        }
        return 0;
    }
}

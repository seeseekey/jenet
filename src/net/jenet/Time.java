/*
 * Copyright (c) 2005 Dizan Vasquez
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.jenet;

/**
 * @author Dizan Vasquez
 */
class Time {

        public final static int OVERFLOW = 86400000;

        public static boolean less( long a, long b ) {
                return a - b >= OVERFLOW || a - b < 0;
        }

        public static boolean greater( long a, long b ) {
                return b - a >= OVERFLOW || b - a < 0;
        }

        public static boolean lessEqual( long a, long b ) {
                return !greater( a, b );
        }

        public static boolean greaterEqual( long a, long b ) {
                return !less( a, b );
        }

        public static int difference( int a, int b ) {
                if ( a - b >= OVERFLOW )
                        return b - a;
                else
                        return a - b;
        }

}
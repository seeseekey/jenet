/*
 * Copyright (c) 2005 Dizan Vasquez.
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

import java.nio.ByteBuffer;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Wraps information to be sent through JeNet.
 * @author Dizan Vasquez
 */
public class Packet implements IBufferable {

        /**
         * Indicates that this packet is reliable
         */
        public static final int FLAG_RELIABLE = 1;

        /**
         * Indicates that the packet should be processed no 
         * matter its order relative to other packets.
         */
        public static final int FLAG_UNSEQUENCED = 2;

        protected int referenceCount;

        protected int flags;

        protected ByteBuffer data;

        protected int dataLength;

        private Packet() {
                super();
        }

        /**
         * Creates a new Packet.
         * The constructor allocates a new packet and allocates a
         * buffer of <code>dataLength</code> bytes for it.
         * 
         * @param dataLength
         *                      The size in bytes of this packet.
         * @param flags
         *                      An byte inidicating the how to handle this packet.
         */
        public Packet( int dataLength, int flags ) {
                data = ByteBuffer.allocateDirect( dataLength );
                this.dataLength = dataLength;
                this.flags = flags;
        }
        
        /**
         * Copies this packet's data into the given buffer.
         * @param buffer
         *              Destination buffer
         */
        public void toBuffer( ByteBuffer buffer ) {
                data.flip();
                for ( int i = 0; i < dataLength; i++ ) {
                        buffer.put( data.get() );
                }
        }

        /**
         * Copies part of this packet's data into the given buffer.
         * @param buffer
         *              Destination buffer
         * @param offset
         *              Initial position of the destination buffer
         * @param length
         *              Total number of bytes to copy
         */
        public void toBuffer( ByteBuffer buffer, int offset, int length ) {
                int position = data.position();
                int limit = data.limit();
                data.flip();
                data.position( offset );
                for ( int i = 0; i < length; i++ ) {
                        buffer.put( data.get() );
                }
                data.position( position );
                data.limit( limit );
        }

        /**
         * Copies the given buffer into this packet's data.
         * @ param buffer
         *              Buffer to copy from
         */
        public void fromBuffer( ByteBuffer buffer ) {
                data.clear();
                for ( int i = 0; i < dataLength; i++ ) {
                        data.put( buffer.get() );
                }
        }
        
        /**
         * Copies part of the given buffer into this packet's data.
         * @param buffer
         *              Buffer to copy from
         * @param fragmentOffset
         *              Position of the first byte to copy
         * @param length
         *              Total number of bytes to copy
         */
        public void fromBuffer( ByteBuffer buffer, int fragmentOffset, int length ) {
                data.position( fragmentOffset );
                for ( int i = 0; i < length; i++ ) {
                        data.put( buffer.get() );
                }
                data.position( dataLength );
                data.limit( dataLength );
        }

        /**
         * Returs size of this packet.
         * @return Size in bytes of this packet
         */
        public int byteSize() {
                return dataLength;
        }

        /**
         * Returns the data contained in this packet
         * @return Returns the data.
         */
        public ByteBuffer getData() {
                return data;
        }

        
        /**
         * Returns the size in bytes of this packet's data
         * @return Returns the dataLength.
         */
        public int getDataLength() {
                return dataLength;
        }

        /**
         * Returns this packet's flags.
         * @return Returns the flags.
         */
        public int getFlags() {
                return flags;
        }

        /**
         * @return Returns the referenceCount.
         */
        int getReferenceCount() {
                return referenceCount;
        }

        /**
         * Sets the flags for this packet.
         * The parameter is an or of the flags <code>FLAG_RELIABLE</code> and <code>FLAG_UNSEQUENCED</code>
         * a value of zero indicates an unreliable, sequenced (last one is kept) packet.
         * @param flags
         *            The flags to set.
         */
        public void setFlags( int flags ) {
                this.flags = flags;
        }

        /**
         * @param referenceCount
         *            The referenceCount to set.
         */
        void setReferenceCount( int referenceCount ) {
                this.referenceCount = referenceCount;
        }

        public String toString() {
                return ToStringBuilder.reflectionToString( this, ToStringStyle.MULTI_LINE_STYLE );
        }

}
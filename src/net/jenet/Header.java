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
 * @author Dizan Vasquez
 */
class Header implements IBufferable {

        public static final byte FLAG_ACKNOWLEDGE = 1;

        public static final byte FLAG_UNSEQUENCED = 2;
        
        public static final int BYTE_SIZE = 12;

        protected int challenge;

        protected byte commandCount;

        protected byte flags;

        protected short peerID;

        protected int sentTime;

        public Header() {
                super();
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Bufferable#fromBuffer(java.nio.ByteBuffer)
         */
        public void fromBuffer( ByteBuffer buffer ) {
                peerID = buffer.getShort();
                flags = buffer.get();
                commandCount = buffer.get();
                sentTime = buffer.getInt();
                challenge = buffer.getInt();
        }

        /**
         * @return Returns the challenge.
         */
        public int getChallenge() {
                return challenge;
        }

        /**
         * @return Returns the commandCount.
         */
        public byte getCommandCount() {
                return commandCount;
        }

        /**
         * @return Returns the flags.
         */
        public byte getFlags() {
                return flags;
        }

        /**
         * @return Returns the peerID.
         */
        public short getPeerID() {
                return peerID;
        }

        /**
         * @return Returns the sentTime.
         */
        public int getSentTime() {
                return sentTime;
        }

        /**
         * @param challenge
         *            The challenge to set.
         */
        public void setChallenge( int challenge ) {
                this.challenge = challenge;
        }

        /**
         * @param commandCount
         *            The commandCount to set.
         */
        public void setCommandCount( byte commandCount ) {
                this.commandCount = commandCount;
        }

        /**
         * @param flags
         *            The flags to set.
         */
        public void setFlags( byte flags ) {
                this.flags = flags;
        }

        /**
         * @param peerID
         *            The peerID to set.
         */
        public void setPeerID( short peerID ) {
                this.peerID = peerID;
        }

        /**
         * @param sentTime
         *            The sentTime to set.
         */
        public void setSentTime( int sentTime ) {
                this.sentTime = sentTime;
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Bufferable#toBuffer(java.nio.ByteBuffer)
         */
        public void toBuffer( ByteBuffer buffer ) {
                buffer.putShort( peerID );
                buffer.put( flags );
                buffer.put( commandCount );
                buffer.putInt( sentTime );
                buffer.putInt( challenge );
        }

        public String toString() {
                return ToStringBuilder.reflectionToString( this, ToStringStyle.MULTI_LINE_STYLE );
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.IByteSize#byteSize()
         */
        public int byteSize() {
                return BYTE_SIZE;
        }
}
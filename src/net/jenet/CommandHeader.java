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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author Dizan Vasquez
 */
class CommandHeader implements IBufferable {
        
        public static final int BYTE_SIZE = 12;

        protected byte channelID;

        protected byte command;

        protected int commandLength;

        protected byte flags;

        protected byte reserved;

        protected int reliableSequenceNumber;

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Bufferable#fromBuffer(java.nio.ByteBuffer)
         */
        public void fromBuffer( ByteBuffer buffer ) {
                command = buffer.get();
                channelID = buffer.get();
                flags = buffer.get();
                reserved = buffer.get();
                commandLength = buffer.getInt();
                reliableSequenceNumber = buffer.getInt();
        }

        /**
         * @return Returns the channelID.
         */
        public byte getChannelID() {
                return channelID;
        }

        /**
         * @return Returns the command.
         */
        public byte getCommand() {
                return command;
        }

        /**
         * @return Returns the commandLength.
         */
        public int getCommandLength() {
                return commandLength;
        }

        /**
         * @return Returns the flags.
         */
        public byte getFlags() {
                return flags;
        }

        /**
         * @return Returns the reserved.
         */
        public byte getReserved() {
                return reserved;
        }

        /**
         * @param channelID
         *            The channelID to set.
         */
        public void setChannelID( byte channelID ) {
                this.channelID = channelID;
        }

        /**
         * @param command
         *            The command to set.
         */
        public void setCommand( byte command ) {
                this.command = command;
        }

        /**
         * @param commandLength
         *            The commandLength to set.
         */
        public void setCommandLength( int commandLength ) {
                this.commandLength = commandLength;
        }

        /**
         * @param flags
         *            The flags to set.
         */
        public void setFlags( byte flags ) {
                this.flags = flags;
        }

        /**
         * @param reserved
         *            The reserved to set.
         */
        public void setReserved( byte reserved ) {
                this.reserved = reserved;
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Bufferable#toBuffer()
         */
        public void toBuffer( ByteBuffer buffer ) {
                buffer.put( command );
                buffer.put( channelID );
                buffer.put( flags );
                buffer.put( reserved );
                buffer.putInt( commandLength );
                buffer.putInt( reliableSequenceNumber );
        }

        /**
         * @return Returns the reliableSequenceNumber.
         */
        public int getReliableSequenceNumber() {
                return reliableSequenceNumber;
        }

        /**
         * @param reliableSequenceNumber
         *            The reliableSequenceNumber to set.
         */
        public void setReliableSequenceNumber( int reliableSequenceNumber ) {
                this.reliableSequenceNumber = reliableSequenceNumber;
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.IByteSize#byteSize()
         */
        public int byteSize() {
                return BYTE_SIZE;
        }

        public String toString() {
                return ToStringBuilder.reflectionToString( this, ToStringStyle.DEFAULT_STYLE );
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
                return HashCodeBuilder.reflectionHashCode( this );
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
                return EqualsBuilder.reflectionEquals( this, obj );
        }

}
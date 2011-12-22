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
abstract class Command implements IBufferable { 

        public static final int BYTE_SIZE = Header.BYTE_SIZE;

        public static byte commandCode( Command command ) {
                if ( command instanceof None )
                        return 0;
                else if ( command instanceof Acknowledge )
                        return 1;
                else if ( command instanceof Connect )
                        return 2;
                else if ( command instanceof VerifyConnect )
                        return 3;
                else if ( command instanceof Disconnect )
                        return 4;
                else if ( command instanceof Ping )
                        return 5;
                else if ( command instanceof SendReliable )
                        return 6;
                else if ( command instanceof SendUnreliable )
                        return 7;
                else if ( command instanceof SendFragment )
                        return 8;
                else if ( command instanceof BandwidthLimit )
                        return 9;
                else if ( command instanceof ThrottleConfigure )
                        return 10;
                else
                        return 11;
        }

        public static Command readCommand( ByteBuffer buffer ) {
                int position = buffer.position();
                Command command = null;

                CommandHeader header = new CommandHeader();
                header.fromBuffer( buffer );
                buffer.position( position );

                switch ( header.getCommand() ) {
                case 0:
                        command = new None();
                        break;
                case 1:
                        command = new Acknowledge();
                        break;
                case 2:
                        command = new Connect();
                        break;
                case 3:
                        command = new VerifyConnect();
                        break;
                case 4:
                        command = new Disconnect();
                        break;
                case 5:
                        command = new Ping();
                        break;
                case 6:
                        command = new SendReliable();
                        break;
                case 7:
                        command = new SendUnreliable();
                        break;
                case 8:
                        command = new SendFragment();
                        break;
                case 9:
                        command = new BandwidthLimit();
                        break;
                case 10:
                        command = new ThrottleConfigure();
                        break;
                case 11:
                        command = new SendUnsequenced();
                        break;

                default:
                        command = null;
                        return command;
                }

                command.fromBuffer( buffer );

                return command;
        }

        protected CommandHeader header = new CommandHeader();

        public Command() {
                super();
                header.setCommand( commandCode( this ) );
                header.setCommandLength( this.byteSize() );
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.IByteSize#byteSize()
         */
        public int byteSize() {
                return BYTE_SIZE;
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

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Bufferable#fromBuffer(java.nio.ByteBuffer)
         */
        public void fromBuffer( ByteBuffer buffer ) {
                header.fromBuffer( buffer );
        }

        /**
         * @return Returns the header.
         */
        public CommandHeader getHeader() {
                return header;
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

        /**
         * @param header
         *            The header to set.
         */
        public void setHeader( CommandHeader header ) {
                this.header = header;
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Bufferable#toBuffer(java.nio.ByteBuffer)
         */
        public void toBuffer( ByteBuffer buffer ) {
                header.toBuffer( buffer );
        }

        public String toString() {
                return ToStringBuilder.reflectionToString( this, ToStringStyle.MULTI_LINE_STYLE );
        }

        /**
         * @param peer
         */
        public abstract Event execute( Host host, Peer peer, Header header );
}
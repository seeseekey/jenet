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

/**
 * @author Dizan Vasquez
 */
class SendUnreliable extends Command {

        protected int unreliableSequenceNumber;

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.protocol.Command#execute(org.Dizan
         *      Vasquez.jnet.Peer, net.jenet.protocol.Header)
         */
        @Override
        public Event execute( Host host, Peer peer, Header header ) {
                Event result = new Event();
                Channel channel = peer.selectChannel( getHeader().getChannelID() );
                Packet packet;
                if ( channel == null || !peer.isConnected() )
                        return result;

                packet = new Packet( getHeader().getCommandLength() - byteSize(), 0 );

                packet.fromBuffer( host.getReceivedData() );

                peer.queueIncomingCommand( this, packet, 0 );
                return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Command#fromBuffer(java.nio.ByteBuffer)
         */
        @Override
        public void fromBuffer( ByteBuffer buffer ) {
                super.fromBuffer( buffer );
                unreliableSequenceNumber = buffer.getInt();
        }

        /**
         * @return Returns the unreliableSequenceNumber.
         */
        public int getUnreliableSequenceNumber() {
                return unreliableSequenceNumber;
        }

        /**
         * @param unreliableSequenceNumber
         *            The unreliableSequenceNumber to set.
         */
        public void setUnreliableSequenceNumber( int unreliableSequenceNumber ) {
                this.unreliableSequenceNumber = unreliableSequenceNumber;
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Command#toBuffer(java.nio.ByteBuffer)
         */
        @Override
        public void toBuffer( ByteBuffer buffer ) {
                super.toBuffer( buffer );
                buffer.putInt( unreliableSequenceNumber );
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Command#byteSize()
         */
        @Override
        public int byteSize() {
                return super.byteSize() + 4;
        }

}
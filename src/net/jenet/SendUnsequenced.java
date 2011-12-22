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
import java.util.Arrays;

/**
 * @author Dizan Vasquez
 */
class SendUnsequenced extends Command {

        protected int unsequencedGroup;

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.protocol.Command#execute(org.Dizan
         *      Vasquez.jnet.Peer, net.jenet.protocol.Header)
         */
        @Override
        public Event execute( Host host, Peer peer, Header header ) {
                Event result = new Event();
                Packet packet;
                int index;
                Channel channel = peer.selectChannel( getHeader().getChannelID() );

                if ( channel == null || !peer.isConnected() )
                        return result;

                int windowSize = peer.getConfiguration().getInt( "ENET_PEER_UNSEQUENCED_WINDOW_SIZE" );
                index = unsequencedGroup % windowSize;
                if ( unsequencedGroup >= peer.getIncomingUnsequencedGroup() + windowSize ) {
                        peer.setIncomingUnsequencedGroup( unsequencedGroup - index );
                        Arrays.fill( peer.getUnsequencedWindow(), 0 );
                } else if ( unsequencedGroup < peer.getIncomingUnsequencedGroup()
                                || ( peer.getUnsequencedWindow()[index / 32] & ( 1 << ( index % 32 ) ) ) != 0 )
                        return result;

                peer.getUnsequencedWindow()[index / 32] |= 1 << ( index % 32 );

                packet = new Packet( getHeader().getCommandLength() - this.byteSize(), Packet.FLAG_UNSEQUENCED );
                packet.fromBuffer( host.getReceivedData() );

                peer.queueIncomingCommand( this, packet, 0 );

                return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Command#toBuffer(java.nio.ByteBuffer)
         */
        @Override
        public void toBuffer( ByteBuffer buffer ) {
                super.toBuffer( buffer );
                buffer.putInt( unsequencedGroup );
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Command#fromBuffer(java.nio.ByteBuffer)
         */
        @Override
        public void fromBuffer( ByteBuffer buffer ) {
                super.fromBuffer( buffer );
                unsequencedGroup = buffer.getInt();
        }

        /**
         * @return Returns the unsequencedGroup.
         */
        public int getUnsequencedGroup() {
                return unsequencedGroup;
        }

        /**
         * @param unsequencedGroup
         *            The unsequencedGroup to set.
         */
        public void setUnsequencedGroup( int unsequencedGroup ) {
                this.unsequencedGroup = unsequencedGroup;
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
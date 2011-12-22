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

import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * @author Dizan Vasquez
 */
class BandwidthLimit extends Command {

        protected int incomingBandwidth;

        protected int outgoingBandwidth;

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.protocol.Command#execute(org.Dizan
         *      Vasquez.jnet.Peer, net.jenet.protocol.Header)
         */
        @Override
        public Event execute( Host host, Peer peer, Header header ) {
                Event result = new Event();
                PropertiesConfiguration configuration = peer.getConfiguration();
                peer.setIncomingBandwidth( incomingBandwidth );
                peer.setOutgoingBandwidth( outgoingBandwidth );

                int windowSize = 0;

                if ( incomingBandwidth == 0 && host.getOutgoingBandwidth() == 0 )
                        windowSize = configuration.getInt( "ENET_PROTOCOL_MAXIMUM_WINDOW_SIZE" );
                else {
                        windowSize = incomingBandwidth < host.getOutgoingBandwidth() ? incomingBandwidth : host
                                        .getOutgoingBandwidth();
                        windowSize = windowSize / configuration.getInt( "ENET_PEER_WINDOW_SIZE_SCALE" );
                        windowSize *= configuration.getInt( "ENET_PROTOCOL_MINIMUM_WINDOW_SIZE" );
                }

                if ( windowSize < configuration.getInt( "ENET_PROTOCOL_MINIMUM_WINDOW_SIZE" ) )
                        windowSize = configuration.getInt( "ENET_PROTOCOL_MINIMUM_WINDOW_SIZE" );
                else if ( windowSize > configuration.getInt( "ENET_PROTOCOL_MAXIMUM_WINDOW_SIZE" ) )
                        windowSize = configuration.getInt( "ENET_PROTOCOL_MAXIMUM_WINDOW_SIZE" );

                peer.setWindowSize( windowSize );
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
                incomingBandwidth = buffer.getInt();
                outgoingBandwidth = buffer.getInt();
        }

        /**
         * @return Returns the incomingBandwidth.
         */
        public int getIncomingBandwidth() {
                return incomingBandwidth;
        }

        /**
         * @return Returns the outgoingBandwidth.
         */
        public int getOutgoingBandwidth() {
                return outgoingBandwidth;
        }

        /**
         * @param incomingBandwidth
         *            The incomingBandwidth to set.
         */
        public void setIncomingBandwidth( int incomingBandwidth ) {
                this.incomingBandwidth = incomingBandwidth;
        }

        /**
         * @param outgoingBandwidth
         *            The outgoingBandwidth to set.
         */
        public void setOutgoingBandwidth( int outgoingBandwidth ) {
                this.outgoingBandwidth = outgoingBandwidth;
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Command#toBuffer(java.nio.ByteBuffer)
         */
        @Override
        public void toBuffer( ByteBuffer buffer ) {
                super.toBuffer( buffer );
                buffer.putInt( incomingBandwidth );
                buffer.putInt( outgoingBandwidth );
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Command#byteSize()
         */
        @Override
        public int byteSize() {
                return super.byteSize() + 8;
        }
}

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
import java.util.HashMap;

import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * @author Dizan Vasquez
 */
class Connect extends Command {

        protected int channelCount;

        protected int incomingBandwidth;

        protected short mtu;

        protected int outgoingBandwidth;

        protected short outgoingPeerID;

        protected int packetThrottleAcceleration;

        protected int packetThrottleDeceleration;

        protected int packetThrottleInterval;

        protected int windowSize;

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.protocol.Command#execute(org.Dizan
         *      Vasquez.jnet.Peer, net.jenet.protocol.Header)
         */
        @Override
        public Event execute( Host host, Peer peer, Header header ) {
                Event result = new Event();
                VerifyConnect verifyConnect = new VerifyConnect();
                PropertiesConfiguration configuration = host.getConfiguration();

                if ( channelCount < configuration.getInt( "ENET_PROTOCOL_MINIMUM_CHANNEL_COUNT" )
                                || channelCount > configuration.getInt( "ENET_PROTOCOL_MAXIMUM_CHANNEL_COUNT" ) )
                        return result;

                for ( Peer currentPeer : host.getPeers().values() )
                        if ( !currentPeer.isDisconnected() && currentPeer.getAddress() == host.getReceivedAddress()
                                        && currentPeer.getChallenge() == header.getChallenge() )
                                return result;

                Peer currentPeer = null;

                if ( host.getPeers().values().size() < host.getMaxConnections() ) {
                        currentPeer = new Peer( host, host.getReceivedAddress(), channelCount );
                        host.assignPeerID( currentPeer );
                }

                if ( currentPeer == null )
                        return result;

                currentPeer.setState( Peer.STATE.ACKNOWLEDGING_CONNECT );
                currentPeer.setChallenge( header.getChallenge() );
                currentPeer.setAddress( host.getReceivedAddress() );
                currentPeer.setOutgoingPeerID( outgoingPeerID );
                currentPeer.setIncomingBandwidth( incomingBandwidth );
                currentPeer.setOutgoingBandwidth( outgoingBandwidth );
                currentPeer.setPacketThrottleInterval( packetThrottleInterval );
                currentPeer.setPacketThrottleAcceleration( packetThrottleAcceleration );
                currentPeer.setPacketThrottleDeceleration( packetThrottleDeceleration );
                currentPeer.setChannels( new HashMap<Byte, Channel>() );
                currentPeer.setChannelCount( channelCount );

                if ( mtu < configuration.getShort( "ENET_PROTOCOL_MINIMUM_MTU" ) )
                        mtu = configuration.getShort( "ENET_PROTOCOL_MINIMUM_MTU" );
                else if ( mtu > configuration.getShort( "ENET_PROTOCOL_MAXIMUM_MTU" ) )
                        mtu = configuration.getShort( "ENET_PROTOCOL_MAXIMUM_MTU" );

                currentPeer.setMtu( mtu );

                int peerWindowSize;
                if ( incomingBandwidth == 0 && host.getOutgoingBandwidth() == 0 )
                        peerWindowSize = configuration.getInt( "ENET_PROTOCOL_MAXIMUM_WINDOW_SIZE" );
                else {
                        peerWindowSize = incomingBandwidth < host.getOutgoingBandwidth() ? incomingBandwidth : host
                                        .getOutgoingBandwidth();
                        peerWindowSize = peerWindowSize / configuration.getInt( "ENET_PEER_WINDOW_SIZE_SCALE" );
                        peerWindowSize *= configuration.getInt( "ENET_PROTOCOL_MINIMUM_WINDOW_SIZE" );
                }

                if ( peerWindowSize < configuration.getInt( "ENET_PROTOCOL_MINIMUM_WINDOW_SIZE" ) )
                        peerWindowSize = configuration.getInt( "ENET_PROTOCOL_MINIMUM_WINDOW_SIZE" );
                else if ( peerWindowSize > configuration.getInt( "ENET_PROTOCOL_MAXIMUM_WINDOW_SIZE" ) )
                        peerWindowSize = configuration.getInt( "ENET_PROTOCOL_MAXIMUM_WINDOW_SIZE" );

                currentPeer.setWindowSize( peerWindowSize );

                if ( host.getIncomingBandwidth() == 0 )
                        windowSize = configuration.getInt( "ENET_PROTOCOL_MAXIMUM_WINDOW_SIZE" );
                else {
                        windowSize = host.getIncomingBandwidth() / configuration.getInt( "ENET_PEER_WINDOW_SIZE_SCALE" );
                        windowSize = windowSize * configuration.getInt( "ENET_PROTOCOL_MINIMUM_WINDOW_SIZE" );
                }

                if ( windowSize < configuration.getInt( "ENET_PROTOCOL_MINIMUM_WINDOW_SIZE" ) )
                        windowSize = configuration.getInt( "ENET_PROTOCOL_MINIMUM_WINDOW_SIZE" );
                else if ( windowSize > configuration.getInt( "ENET_PROTOCOL_MAXIMUM_WINDOW_SIZE" ) )
                        windowSize = configuration.getInt( "ENET_PROTOCOL_MAXIMUM_WINDOW_SIZE" );

                verifyConnect.getHeader().setChannelID( (byte) 0xFF );
                verifyConnect.getHeader().setFlags( Header.FLAG_ACKNOWLEDGE );
                verifyConnect.setOutgoingPeerID( currentPeer.getIncomingPeerID() );
                verifyConnect.setMtu( mtu );
                verifyConnect.setWindowSize( windowSize );
                verifyConnect.setChannelCount( channelCount );
                verifyConnect.setIncomingBandwidth( incomingBandwidth );
                verifyConnect.setOutgoingBandwidth( outgoingBandwidth );
                verifyConnect.setPacketThrottleInterval( packetThrottleInterval );
                verifyConnect.setPacketThrottleAcceleration( packetThrottleAcceleration );
                verifyConnect.setPacketThrottleDeceleration( packetThrottleDeceleration );
                currentPeer.queueOutgoingCommand( verifyConnect, null, 0, (short) 0 );

                result.setPeer( currentPeer );
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
                outgoingPeerID = buffer.getShort();
                mtu = buffer.getShort();
                windowSize = buffer.getInt();
                channelCount = buffer.getInt();
                incomingBandwidth = buffer.getInt();
                outgoingBandwidth = buffer.getInt();
                packetThrottleInterval = buffer.getInt();
                packetThrottleAcceleration = buffer.getInt();
                packetThrottleDeceleration = buffer.getInt();
        }

        /**
         * @return Returns the channelCount.
         */
        public int getChannelCount() {
                return channelCount;
        }

        /**
         * @return Returns the incomingBandwidth.
         */
        public int getIncomingBandwidth() {
                return incomingBandwidth;
        }

        /**
         * @return Returns the mtu.
         */
        public short getMtu() {
                return mtu;
        }

        /**
         * @return Returns the outgoingBandwidth.
         */
        public int getOutgoingBandwidth() {
                return outgoingBandwidth;
        }

        /**
         * @return Returns the outgoingPeerID.
         */
        public short getOutgoingPeerID() {
                return outgoingPeerID;
        }

        /**
         * @return Returns the packetThrottleAcceleration.
         */
        public int getPacketThrottleAcceleration() {
                return packetThrottleAcceleration;
        }

        /**
         * @return Returns the packetThrottleDeceleration.
         */
        public int getPacketThrottleDeceleration() {
                return packetThrottleDeceleration;
        }

        /**
         * @return Returns the packetThrottleInterval.
         */
        public int getPacketThrottleInterval() {
                return packetThrottleInterval;
        }

        /**
         * @return Returns the windowSize.
         */
        public int getWindowSize() {
                return windowSize;
        }

        /**
         * @param channelCount
         *            The channelCount to set.
         */
        public void setChannelCount( int channelCount ) {
                this.channelCount = channelCount;
        }

        /**
         * @param incomingBandwidth
         *            The incomingBandwidth to set.
         */
        public void setIncomingBandwidth( int incomingBandwidth ) {
                this.incomingBandwidth = incomingBandwidth;
        }

        /**
         * @param mtu
         *            The mtu to set.
         */
        public void setMtu( short mtu ) {
                this.mtu = mtu;
        }

        /**
         * @param outgoingBandwidth
         *            The outgoingBandwidth to set.
         */
        public void setOutgoingBandwidth( int outgoingBandwidth ) {
                this.outgoingBandwidth = outgoingBandwidth;
        }

        /**
         * @param outgoingPeerID
         *            The outgoingPeerID to set.
         */
        public void setOutgoingPeerID( short outgoingPeerID ) {
                this.outgoingPeerID = outgoingPeerID;
        }

        /**
         * @param packetThrottleAcceleration
         *            The packetThrottleAcceleration to set.
         */
        public void setPacketThrottleAcceleration( int packetThrottleAcceleration ) {
                this.packetThrottleAcceleration = packetThrottleAcceleration;
        }

        /**
         * @param packetThrottleDeceleration
         *            The packetThrottleDeceleration to set.
         */
        public void setPacketThrottleDeceleration( int packetThrottleDeceleration ) {
                this.packetThrottleDeceleration = packetThrottleDeceleration;
        }

        /**
         * @param packetThrottleInterval
         *            The packetThrottleInterval to set.
         */
        public void setPacketThrottleInterval( int packetThrottleInterval ) {
                this.packetThrottleInterval = packetThrottleInterval;
        }

        /**
         * @param windowSize
         *            The windowSize to set.
         */
        public void setWindowSize( int windowSize ) {
                this.windowSize = windowSize;
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Command#toBuffer(java.nio.ByteBuffer)
         */
        @Override
        public void toBuffer( ByteBuffer buffer ) {
                super.toBuffer( buffer );
                buffer.putShort( outgoingPeerID );
                buffer.putShort( mtu );
                buffer.putInt( windowSize );
                buffer.putInt( channelCount );
                buffer.putInt( incomingBandwidth );
                buffer.putInt( outgoingBandwidth );
                buffer.putInt( packetThrottleInterval );
                buffer.putInt( packetThrottleAcceleration );
                buffer.putInt( packetThrottleDeceleration );
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Command#byteSize()
         */
        @Override
        public int byteSize() {
                return super.byteSize() + 32;
        }
}

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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the base class used for UDP packet communications.
 * 
 * @author Dizan Vasquez
 */
public class Host {

        private static Log LOG = LogFactory.getLog( Host.class );

        protected static final short WAIT_RECEIVE = 1;

        protected static final short WAIT_SEND = 2;

        protected static final short WAIT_NONE = 4;

        protected static final short WAIT_ERROR = 8;

        protected InetSocketAddress address;

        protected int bandwidthThrottleEpoch;

        protected int bufferCount;

        protected ByteBuffer buffers;

        protected ConcurrentLinkedQueue<Command> commands = new ConcurrentLinkedQueue<Command>();

        protected DatagramChannel communicationChannel;

        protected Selector communicationSelector;

        protected PropertiesConfiguration configuration;

        protected int incomingBandwidth;

        protected Peer lastServicedPeer;

        protected int maxConnections;

        protected short mtu;

        protected int outgoingBandwidth;

        protected HashMap<Short, Peer> peers = new HashMap<Short, Peer>();

        protected boolean recalculateBandwithLimits;

        protected InetSocketAddress receivedAddress;

        protected ByteBuffer receivedData;

        protected long timeBase;

        protected int timeCurrent;

        /**
         * Creates a new <code>Host</code> object. If the incoming/outgoing
         * bandwidths are not bounded, the host will drop unreliable packets in
         * order to keep them below the fixed limits. This parameter also influences
         * the number and size of the reliable packets that may be handled at once.
         * 
         * @param address
         *            The to bind this host to (<emph>ie</emph> the address at
         *            which other peers may connect to this one) or
         *            <code>0</code> to get a system-assigned address.
         * @param maxConnections
         *            The maximum number of peers/connections that this host will be
         *            able to connect to.
         * @param incomingBandwith
         *            The maximum incoming bandwidth in bytes/second (0 =
         *            unbounded).
         * @param outgoingBandwith
         *            The maximum outgoing bandwidth in bytes/second (0 =
         *            unbounded).
         * @throws IOException
         *             if it can not bind the port.
         * @throws ConfigurationException
         *             if the file enet.properties is not in the path
         */
        public Host( InetSocketAddress address, int maxConnections, int incomingBandwith, int outgoingBandwith )
                        throws IOException, ConfigurationException {
                super();
                communicationChannel = DatagramChannel.open();
                communicationChannel.configureBlocking( false );
                communicationChannel.socket().bind( address );
                communicationSelector = Selector.open();
                communicationChannel.register( communicationSelector, SelectionKey.OP_READ );
                LOG.debug( "Host bound to address: " + address );
                this.address = (InetSocketAddress) communicationChannel.socket().getLocalSocketAddress();
                initHost( maxConnections, incomingBandwith, outgoingBandwith );
        }

        short assignPeerID( Peer peer ) {
                for ( short peerID = 0; peerID < maxConnections; peerID++ )
                        if ( !peers.containsKey( peerID ) ) {
                                peer.setIncomingPeerID( peerID );
                                peers.put( peerID, peer );
                                return peerID;
                        }
                return -1;
        }

        /**
         * Adjusts the incoming/outgoing bandwidths for this host.
         * 
         * @see #Host
         * @param incomingBandwidth
         *            The maximum incoming bandwidth in bytes/secod (0 = unbounded).
         * @param outgoingBandwidth
         *            The maximum outgoing bandwidth in bytes/secod (0 = unbounded).
         */
        public void bandwidthLimit( int incomingBandwidth, int outgoingBandwidth ) {
                this.incomingBandwidth = incomingBandwidth;
                this.outgoingBandwidth = outgoingBandwidth;
                recalculateBandwithLimits = true;
        }

        void bandwidthThrottle() {
                int timeCurrent = getTime();
                int elapsedTime = timeCurrent - bandwidthThrottleEpoch;
                int peersTotal = 0;
                int dataTotal = 0;
                int peersRemaining = 0;
                int bandwidth = 0;
                int throttle = 0;
                int bandwidthLimit = 0;
                boolean needsAdjustment = false;
                BandwidthLimit command = new BandwidthLimit();

                if ( elapsedTime < configuration.getInt( "ENET_HOST_BANDWIDTH_THROTTLE_INTERVAL" ) )
                        return;

                for ( Peer peer : peers.values() )
                        if ( peer.isConnected() ) {
                                ++peersTotal;
                                dataTotal += peer.getOutgoingDataTotal();
                        }

                if ( peersTotal == 0 )
                        return;

                peersRemaining = peersTotal;
                needsAdjustment = true;

                if ( outgoingBandwidth == 0 )
                        bandwidth = 0;
                else
                        bandwidth = outgoingBandwidth * elapsedTime / 1000;

                while ( peersRemaining > 0 && needsAdjustment ) {
                        needsAdjustment = false;
                        if ( dataTotal < bandwidth )
                                throttle = bandwidth * configuration.getInt( "ENET_PEER_PACKET_THROTTLE_SCALE" ) / dataTotal;

                        for ( Peer peer : peers.values() ) {
                                int peerBandwidth;

                                if ( !peer.isConnected() || peer.getIncomingBandwidth() == 0
                                                || peer.getOutgoingBandwidthThrottleEpoch() == timeCurrent )
                                        continue;

                                peerBandwidth = peer.getIncomingBandwidth() * elapsedTime / 1000;

                                if ( throttle * peer.getOutgoingDataTotal()
                                                / configuration.getInt( "ENET_PEER_PACKET_THROTTLE_SCALE" ) >= peerBandwidth )
                                        continue;

                                peer.setPacketThrottleLimit( peerBandwidth
                                                * configuration.getInt( "ENET_PEER_PACKET_THROTTLE_SCALE" )
                                                / peer.getOutgoingDataTotal() );

                                if ( peer.getPacketThrottleLimit() == 0 )
                                        peer.setPacketThrottleLimit( 1 );

                                if ( peer.getPacketThrottle() > peer.getPacketThrottleLimit() )
                                        peer.setPacketThrottle( peer.getPacketThrottleLimit() );

                                peer.setOutgoingBandwidthThrottleEpoch( timeCurrent );

                                needsAdjustment = true;

                                --peersRemaining;

                                bandwidth -= peerBandwidth;
                                dataTotal -= peerBandwidth;
                        }
                }

                if ( peersRemaining > 0 ) {
                        for ( Peer peer : peers.values() ) {
                                if ( !peer.isConnected() || peer.getOutgoingBandwidthThrottleEpoch() == timeCurrent )
                                        continue;

                                peer.setPacketThrottleLimit( throttle );

                                if ( peer.getPacketThrottle() > peer.getPacketThrottleLimit() )
                                        peer.setPacketThrottle( peer.getPacketThrottleLimit() );
                        }
                }

                if ( recalculateBandwithLimits ) {
                        recalculateBandwithLimits = false;

                        peersRemaining = peersTotal;

                        bandwidth = incomingBandwidth;
                        needsAdjustment = true;

                        if ( bandwidth == 0 )
                                bandwidthLimit = 0;
                        else
                                while ( peersRemaining > 0 && needsAdjustment ) {
                                        for ( Peer peer : peers.values() ) {
                                                if ( !peer.isConnected() || peer.getIncomingBandwidthThrottleEpoch() == timeCurrent )
                                                        continue;

                                                if ( peer.getOutgoingBandwidth() > 0
                                                                && bandwidthLimit > peer.getIncomingBandwidthThrottleEpoch() )
                                                        continue;

                                                peer.setIncomingBandwidthThrottleEpoch( timeCurrent );

                                                needsAdjustment = true;
                                                --peersRemaining;
                                                bandwidth -= peer.getOutgoingBandwidth();
                                        }
                                }

                        for ( Peer peer : peers.values() ) {
                                if ( !peer.isConnected() )
                                        continue;

                                command.getHeader().setChannelID( (byte) 0xFF );
                                command.getHeader().setFlags( Header.FLAG_ACKNOWLEDGE );
                                command.setOutgoingBandwidth( outgoingBandwidth );

                                if ( peer.getIncomingBandwidthThrottleEpoch() == timeCurrent )
                                        command.setIncomingBandwidth( peer.getOutgoingBandwidth() );
                                else
                                        command.setIncomingBandwidth( bandwidthLimit );

                                peer.queueOutgoingCommand( command, null, 0, (short) 0 );
                        }
                }

                bandwidthThrottleEpoch = timeCurrent;

                for ( Peer peer : peers.values() ) {
                        peer.setIncomingDataTotal( 0 );
                        peer.setOutgoingDataTotal( 0 );
                }
        }

        /**
         * Broadcasts a packet to all the connected peers.
         * 
         * @param channelID
         *            The channel on which to broadcast.
         * @param packet
         *            The packet to broadcast.
         * @throws IOException
         *             on failure
         */
        public void broadcast( byte channelID, Packet packet ) throws IOException {
                for ( Peer currentPeer : peers.values() )
                        if ( currentPeer.isConnected() )
                                currentPeer.send( channelID, packet );
        }

        void buffer( IBufferable object ) {
                LOG.debug( address + ": Buffering object: \n" + object );
                object.toBuffer( buffers );
                bufferCount++;
        }

        void buffer( OutgoingCommand command ) {
                command.getCommand().getHeader().setCommandLength( command.byteSize() );
                buffer( command.getCommand() );
                if ( command.getPacket() != null ) {
                        LOG.debug( address + ": Buffering object: \n" + command.getPacket() );
                        command.getPacket().toBuffer( buffers, command.getFragmentOffset(), command.getFragmentLength() );
                        bufferCount++;
                }
        }

        /**
         * Initiates a connection to a foreign host. Subsequent calls to
         * {@link #service} will take care of any further handshaking.
         * 
         * @param address
         *            Address of the host to connect to.
         * @param channelCount
         *            Maximum number of communication channels.
         * @return A {@link net.jenet.Peer} object representing the foreign host.
         */
        public Peer connect( InetSocketAddress address, int channelCount ) {
                if ( channelCount < configuration.getInt( "ENET_PROTOCOL_MINIMUM_CHANNEL_COUNT" ) )
                        channelCount = configuration.getInt( "ENET_PROTOCOL_MINIMUM_CHANNEL_COUNT" );
                else if ( channelCount > configuration.getInt( "ENET_PROTOCOL_MAXIMUM_CHANNEL_COUNT" ) )
                        channelCount = configuration.getInt( "ENET_PROTOCOL_MAXIMUM_CHANNEL_COUNT" );

                if ( peers.size() >= maxConnections )
                        return null;

                Peer currentPeer = new Peer( this, address, channelCount );
                assignPeerID( currentPeer );
                currentPeer.setChannelCount( channelCount );
                Connect connect = new Connect();
                connect.getHeader().setChannelID( (byte) 0xFF );
                connect.getHeader().setFlags( Header.FLAG_ACKNOWLEDGE );
                connect.setOutgoingPeerID( currentPeer.getIncomingPeerID() );
                connect.setMtu( currentPeer.getMtu() );
                connect.setWindowSize( currentPeer.getWindowSize() );
                connect.setChannelCount( channelCount );
                connect.setIncomingBandwidth( incomingBandwidth );
                connect.setOutgoingBandwidth( outgoingBandwidth );
                connect.setPacketThrottleInterval( currentPeer.getPacketThrottleInterval() );
                connect.setPacketThrottleAcceleration( currentPeer.getPacketThrottleAcceleration() );
                connect.setPacketThrottleDeceleration( currentPeer.getPacketThrottleDeceleration() );
                currentPeer.queueOutgoingCommand( connect, null, 0, (short) 0 );
                return currentPeer;
        }

        /**
         * Destroys this host object.
         * 
         * @throws IOException
         */
        public void destroy() throws IOException {
                communicationChannel.close();
        }

        Event dispatchIncomingCommands() {
                Event result = new Event();
                Peer currentPeer;

                LinkedList<Peer> peersList = new LinkedList<Peer>( peers.values() );

                if ( peers.size() == 0 )
                        return result;

                /* 
                 * Simply calling containsKey( lastServicedPeer.getIncomingPeerId() ) will 
                 * not be sufficient because the peerID of lastServicedPeer may have been 
                 * reassigned.  The get operation is quicker than containsValue because 
                 * it does not have to search through all the peers. 
                 * 
                 * lastServicedPeer.isDisconnected() may be sufficient, but this feels more robust.
                 */
                if ( lastServicedPeer == null || peers.get( lastServicedPeer.getIncomingPeerID() ) != lastServicedPeer )
                        lastServicedPeer = peersList.getFirst();
                else
                        while ( peersList.getLast() != lastServicedPeer )
                                peersList.addLast( peersList.removeFirst() );

                do {
                        currentPeer = peersList.removeFirst();
                        peersList.addLast( currentPeer );

                        if ( currentPeer.isZombie() ) {
                                recalculateBandwithLimits = true;
                                currentPeer.reset();
                                result.setType( Event.TYPE.DISCONNECTED );
                                result.setPeer( currentPeer );
                                lastServicedPeer = currentPeer;
                                return result;
                        }

                        if ( !currentPeer.isConnected() )
                                continue;

                        for ( byte channelID : currentPeer.getChannels().keySet() ) {
                                Channel channel = currentPeer.getChannels().get( channelID );
                                if ( channel.getIncomingReliableCommands().isEmpty()
                                                && channel.getIncomingUnreliableCommands().isEmpty() )
                                        continue;
                                Packet packet = currentPeer.receive( channelID );
                                result.setPacket( packet );
                                if ( packet == null )
                                        continue;
                                result.setType( Event.TYPE.RECEIVED );
                                result.setPeer( currentPeer );
                                result.setChannelID( channelID );
                                result.setPacket( packet );
                                lastServicedPeer = currentPeer;

                                return result;
                        }
                } while ( currentPeer != lastServicedPeer );

                return result;
        }

        /**
         * Sends all pending messages to all the peers.
         */
        public void flush() {
                timeCurrent = getTime();
                sendOutgoingCommands( false );
        }

        /**
         * Get the address to which this Host is connected.
         * 
         * @return Returns the address.
         */
        public InetSocketAddress getAddress() {
                return address;
        }

        /**
         * @return Returns the buffers.
         */
        int getBufferCount() {
                return bufferCount;
        }

        /**
         * @return Returns the buffers.
         */
        int getCommandCount() {
                return commands.size();
        }

        ConcurrentLinkedQueue<Command> getCommands() {
                return commands;
        }

        /**
         * @return Returns the configuration.
         */
        PropertiesConfiguration getConfiguration() {
                return configuration;
        }

        /**
         * The total incomingBandwidth for this Host.
         * 
         * @return Returns the incomingBandwidth.
         */
        public int getIncomingBandwidth() {
                return incomingBandwidth;
        }

        /**
         * The number of simultaneous connections that this host can handle.
         * 
         * @return Returns the maxConnections.
         */
        public int getMaxConnections() {
                return maxConnections;
        }

        /**
         * @return Returns the mtu.
         */
        short getMtu() {
                return mtu;
        }

        /**
         * The total outgoingBandwidth for this host
         * 
         * @return Returns the outgoingBandwidth.
         */
        public int getOutgoingBandwidth() {
                return outgoingBandwidth;
        }

        /**
         * @return Returns the packetSize.
         */
        int getPacketSize() {
                int size = buffers.position();
                return size;
        }

        /**
         * The peers to which this host is connected.
         * 
         * @return Returns the peers.
         */
        public HashMap<Short, Peer> getPeers() {
                return peers;
        }

        /**
         * @return Returns the receivedAddress.
         */
        InetSocketAddress getReceivedAddress() {
                return receivedAddress;
        }

        /**
         * @return Returns the receivedData.
         */
        ByteBuffer getReceivedData() {
                return receivedData;
        }

        /**
         * @return The time elapsed in milliseconds.
         */

        /**
         * @return The system's time
         */
        int getTime() {
                return (int) ( System.currentTimeMillis() - timeBase );
        }

        /**
         * @return Returns the timeBase.
         */
        long getTimeBase() {
                return timeBase;
        }

        /**
         * Returns the timestamp of last network's operation.
         * 
         * @return Returns the timeCurrent.
         */
        public int getTimeCurrent() {
                return timeCurrent;
        }

        /**
         * Executes received command
         * @return
         */
        private Event handleIncomingCommands() {
                Event result = new Event();
                Command command;
                Header header = new Header();
                Peer peer;

                if ( receivedData.limit() < header.byteSize() )
                        return result;

                header.fromBuffer( receivedData );

                LOG.debug( address + ": Parsed header: \n" + header );

                if ( header.getPeerID() == (short) 0xFFFF )
                        peer = null;
                else if ( header.getPeerID() >= maxConnections )
                        return result;
                else {
                        peer = peers.get( header.getPeerID() );
                        if ( peer == null || peer.isDisconnected() || peer.isZombie() || !receivedAddress.equals( peer.getAddress() )
                                        || peer.getChallenge() != header.getChallenge() )
                                return result;
                        else
                                peer.setAddress( receivedAddress );
                }

                if ( peer != null )
                        peer.setIncomingDataTotal( peer.getIncomingDataTotal() + receivedData.limit() );

                int commandCount = header.getCommandCount();

                while ( commandCount > 0 && receivedData.position() < receivedData.limit() ) {
                        command = Command.readCommand( receivedData );
                        LOG.debug( address + ": Parsed Command: \n" + command );

                        if ( command == null )
                                return result;

                        commandCount--;

                        if ( peer == null )
                                if ( !( command instanceof Connect ) )
                                        return result;

                        LOG.debug( address + " executing command " + command.getClass() );
                        Event event = command.execute( this, peer, header );
                        LOG.debug( address + " command execution ended " + command.getClass() );
                        
                        if ( event.type != Event.TYPE.NONE )
                                result = event;
                        
                        if ( command instanceof Connect )
                                peer = event.getPeer();

                        if ( peer != null && ( command.getHeader().getFlags() & Header.FLAG_ACKNOWLEDGE ) != 0 ) {
                                switch ( peer.getState() ) {
                                case DISCONNECTING:
                                        break;
                                case ACKNOWLEDGING_DISCONNECT:
                                        if ( !( command instanceof Disconnect ) )
                                                break;

                                default:
                                        peer.queueAcknowledgement( command, header.getSentTime() );
                                        break;
                                }
                        }
                }

                return result;
        }

        void initHost( int maxConnections, int incomingBandwith, int outgoingBandwith )
                        throws ConfigurationException {
                this.maxConnections = maxConnections;
                this.incomingBandwidth = incomingBandwith;
                this.outgoingBandwidth = outgoingBandwith;

                lastServicedPeer = null;

                configuration = new PropertiesConfiguration( "enet.properties" );

                peers = new HashMap<Short, Peer>();

                bandwidthThrottleEpoch = 0;
                recalculateBandwithLimits = false;
                mtu = configuration.getShort( "ENET_HOST_DEFAULT_MTU" );
                receivedAddress = new InetSocketAddress( (InetAddress) null, 0 );
                buffers = ByteBuffer.allocateDirect( mtu );
                buffers.clear();
                bufferCount = 0;
        }

        /**
         * @param buffer
         * @return
         */
        int receive( ByteBuffer buffer ) {
                try {
                        buffer.clear();
                        receivedAddress = (InetSocketAddress) communicationChannel.receive( buffer );
                        buffer.flip();
                        if ( receivedAddress != null )
                                LOG.debug( "Host.receive:" + address + ". Received " + buffer.limit() + " bytes  from "
                                                + receivedAddress );
                        return buffer.limit();
                } catch ( Exception e ) {
                        LOG.error( "Host.receive: Error reading buffers.", e );
                        return -1;
                }
        }

        /**
         * Reads data from socket and calls handleIncomingCommands
         */
        Event receiveIncomingCommands() {
                Event result = new Event();
                for ( ;; ) {
                        receivedData = ByteBuffer.allocateDirect( mtu );

                        int receivedLength = receive( receivedData );

                        if ( receivedLength < 0 ) {
                                result.setType( Event.TYPE.ERROR );
                                return result;
                        }

                        if ( receivedLength == 0 ) {
                                result.setType( Event.TYPE.NONE );
                                return result;
                        }

                        result = handleIncomingCommands();

                        if ( result.getType() != Event.TYPE.NONE )
                                return result;
                }
        }

        // enet_socket_send
        int send( Peer peer ) {
                buffers.flip();
                LOG.debug( address + ": Sending " + buffers.limit() + " bytes to " + peer.getAddress() );
                try {
                        return (int) communicationChannel.send( buffers, peer.getAddress() );
                } catch ( Exception e ) {
                        LOG.error( "Host.send: Error writing buffers.", e );
                        return -1;
                }
        }

        Event sendOutgoingCommands( boolean checkForTimeouts ) {
                Event result = new Event();
                result.setType( Event.TYPE.NONE );
                Header header = new Header();
                int packetsSent = 1;
                int sentLength;

                while ( packetsSent > 0 ) {
                        packetsSent = 0;
                        for ( Peer currentPeer : peers.values() ) {
                                if ( currentPeer.isDisconnected() || currentPeer.isZombie() )
                                        continue;

                                commands.clear();
                                buffers.clear();
                                bufferCount = 0;
                                header.toBuffer( buffers );

                                if ( !currentPeer.getAcknowledgements().isEmpty() )
                                        currentPeer.sendAcknowledgements();

                                if ( commands.size() < configuration.getInt( "ENET_PROTOCOL_MAXIMUM_PACKET_COMMANDS" ) ) {

                                        if ( checkForTimeouts && !currentPeer.getSentReliableCommands().isEmpty()
                                                        && Time.greaterEqual( timeCurrent, currentPeer.getNextTimeout() ) ) {
                                                result = currentPeer.checkTimeouts();
                                                if ( result.getType() == Event.TYPE.DISCONNECTED )
                                                        return result;
                                        }

                                        Ping ping = new Ping();

                                        //Send any reliable commands
                                        if ( !currentPeer.getOutgoingReliableCommands().isEmpty() )
                                                currentPeer.sendReliableOutgoingCommands();

                                        else if ( currentPeer.getSentReliableCommands().isEmpty()
                                                        && Time.difference( timeCurrent, currentPeer.getLastReceiveTime() ) >= configuration
                                                                        .getInt( "ENET_PEER_PING_INTERVAL" )
                                                        && currentPeer.getMtu() - getPacketSize() >= ping.byteSize() ) {
                                                currentPeer.ping(); //Add the ping the peer's reliable command list
                                                currentPeer.sendReliableOutgoingCommands(); //Send the ping
                                        }

                                        //Send unreliable commands if there is space
                                        if ( commands.size() < configuration.getInt( "ENET_PROTOCOL_MAXIMUM_PACKET_COMMANDS" )
                                                        && !currentPeer.getOutgoingUnreliableCommands().isEmpty() ) {
                                                currentPeer.sendUnreliableOutgoingCommands();
                                        }

                                        if ( commands.size() == 0 )
                                                continue;

                                        if ( currentPeer.getPacketLossEpoch() == 0 )
                                                currentPeer.setPacketLossEpoch( timeCurrent );
                                        else if ( Time.difference( timeCurrent, currentPeer.getPacketLossEpoch() ) >= configuration
                                                        .getInt( "ENET_PEER_PACKET_LOSS_INTERVAL" )
                                                        && currentPeer.getPacketsSent() > 0 ) {
                                                currentPeer.updatePacketLossVariance( timeCurrent );
                                        }

                                        header.setPeerID( currentPeer.getOutgoingPeerID() );
                                        header.setFlags( (byte) 0 );
                                        header.setCommandCount( (byte) commands.size() );
                                        header.setSentTime( timeCurrent );
                                        header.setChallenge( currentPeer.getChallenge() );

                                        int position = buffers.position();
                                        int limit = buffers.limit();
                                        buffers.clear();
                                        header.toBuffer( buffers );
                                        buffers.position( position );
                                        buffers.limit( limit );

                                        LOG.debug( "Host.sendOutgoingCommands:" + address + ". Buffering header: \n" + header );

                                        currentPeer.setLastSendTime( timeCurrent );

                                        ++packetsSent;

                                        sentLength = send( currentPeer );
                                        currentPeer.getSentUnreliableCommands().clear();

                                        if ( sentLength < 0 ) {
                                                result.setType( Event.TYPE.ERROR );
                                                return result;
                                        }
                                }

                        }
                }
                return result;
        }

        /**
         * Poll for new events and handle packet transmission and reception. This
         * method polls the network until a new {@link net.jenet.Event} is received
         * or the number of milliseconds in <code>timeout</code> has elapsed.
         * 
         * @see net.jenet.Event
         * @param timeout
         *            The time to poll the network in milliseconds
         * @return An {@link net.jenet.Event} object.
         */
        synchronized public Event service( int timeout ) {
                LOG.debug( "Servicing host:\n" + this );
                Event event = dispatchIncomingCommands();
                short waitCondition;

                if ( event.getType() == Event.TYPE.DISCONNECTED || event.getType() == Event.TYPE.RECEIVED )
                        return event;

                timeCurrent = getTime();
                timeout += timeCurrent;

                do {

                        if ( Time.difference( timeCurrent, bandwidthThrottleEpoch ) >= configuration
                                        .getInt( "ENET_HOST_BANDWIDTH_THROTTLE_INTERVAL" ) )
                                bandwidthThrottle();

                        event = sendOutgoingCommands( true );
                        
                        if ( event.getType() == Event.TYPE.ERROR )
                                LOG.error( "Error" );

                        if ( event.getType() == Event.TYPE.DISCONNECTED || event.getType() == Event.TYPE.ERROR )
                                return event;

                        event = receiveIncomingCommands();

                        if ( event.getType() != Event.TYPE.NONE )
                                return event;

                        event = sendOutgoingCommands( true );

                        if ( event.getType() == Event.TYPE.DISCONNECTED || event.getType() == Event.TYPE.ERROR )
                                return event;

                        event = dispatchIncomingCommands();

                        if ( event.getType() == Event.TYPE.DISCONNECTED || event.getType() == Event.TYPE.RECEIVED )
                                return event;

                        timeCurrent = getTime();

                        if ( Time.greaterEqual( timeCurrent, timeout ) ) {
                                LOG.debug( "Host.service: " + address + " service timed out" );
                                return event;
                        }

                        waitCondition = socketWait( WAIT_RECEIVE, Time.difference( timeout, timeCurrent ) );

                        if ( waitCondition == WAIT_ERROR ) {
                                event.setType( Event.TYPE.ERROR );
                                return event;
                        }

                        timeCurrent = getTime();

                } while ( waitCondition == WAIT_RECEIVE );

                return event;
        }

        /**
         * @param recalculateBandwithLimits
         *            The recalculateBandwithLimits to set.
         */
        void setRecalculateBandwithLimits( boolean recalculateBandwithLimits ) {
                this.recalculateBandwithLimits = recalculateBandwithLimits;
        }

        short socketWait( short keyType, int timeOut ) {
                try {
                        communicationSelector.select( timeOut );
                } catch ( Exception e ) {
                        LOG.error( "Error waiting network events", e );
                        return WAIT_ERROR;
                }

                for ( SelectionKey key : communicationSelector.selectedKeys() ) {
                        if ( key.isReadable() && keyType == WAIT_RECEIVE )
                                return WAIT_RECEIVE;
                        else if ( key.isWritable() && keyType == WAIT_SEND )
                                return WAIT_SEND;

                }
                return WAIT_NONE;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
                return new ToStringBuilder( this, ToStringStyle.MULTI_LINE_STYLE ).append( "address", address )
                                .append( "peers", peers ).toString();
        }
}
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class representing a remote host. Elements of this class can not be created
 * directly, a call to {@link net.jenet.Host#connect Host.connect} should be
 * used.
 * <p>
 * Once connected, messages are sent to this peer using the {@link #send send}
 * method.
 * 
 * @author Dizan Vasquez
 */
public class Peer {

        enum STATE {
                DISCONNECTED, CONNECTING, ACKNOWLEDGING_CONNECT, CONNECTED, DISCONNECTING, ACKNOWLEDGING_DISCONNECT, ZOMBIE
        }

        private class Acknowledgement {

                protected Command command;

                protected int sentTime;

                public Command getCommand() {
                        return command;
                }

                public int getSentTime() {
                        return sentTime;
                }
        }

        private static Log LOG = LogFactory.getLog( Peer.class );

        protected ConcurrentLinkedQueue<Acknowledgement> acknowledgements = new ConcurrentLinkedQueue<Acknowledgement>();

        protected InetSocketAddress address;

        protected int challenge;

        protected int channelCount;

        protected Map<Byte, Channel> channels;

        protected PropertiesConfiguration configuration;

        protected int highestRoundTripTimeVariance;

        protected Host host;

        protected int incomingBandwidth;

        protected int incomingBandwidthThrottleEpoch;

        protected int incomingDataTotal;

        protected short incomingPeerID;

        protected int incomingUnsequencedGroup;

        protected int lastReceiveTime;

        protected int lastRoundTripTime;

        protected int lastRoundTripTimeVariance;

        protected int lastSendTime;

        protected int lowestRoundTripTime;

        protected short mtu;

        protected int nextTimeout;

        protected int outgoingBandwidth;

        protected int outgoingBandwidthThrottleEpoch;

        protected int outgoingDataTotal;

        protected short outgoingPeerID;

        protected ConcurrentLinkedQueue<OutgoingCommand> outgoingReliableCommands = new ConcurrentLinkedQueue<OutgoingCommand>();

        protected int outgoingReliableSequenceNumber;

        protected ConcurrentLinkedQueue<OutgoingCommand> outgoingUnreliableCommands = new ConcurrentLinkedQueue<OutgoingCommand>();

        protected int outgoingUnsequencedGroup;

        protected int packetLoss;

        protected int packetLossEpoch;

        protected int packetLossVariance;

        protected int packetsLost;

        protected int packetsSent;

        protected int packetThrottle;

        protected int packetThrottleAcceleration;

        protected int packetThrottleCounter;

        protected int packetThrottleDeceleration;

        protected int packetThrottleEpoch;

        protected int packetThrottleInterval;

        protected int packetThrottleLimit;

        protected int reliableDataInTransit;

        protected int roundTripTime;

        /**
         * < mean round trip time (RTT), in milliseconds, between sending a reliable
         * packet and receiving its acknowledgement
         */
        protected int roundTripTimeVariance;

        protected ConcurrentLinkedQueue<OutgoingCommand> sentReliableCommands = new ConcurrentLinkedQueue<OutgoingCommand>();

        protected ConcurrentLinkedQueue<OutgoingCommand> sentUnreliableCommands = new ConcurrentLinkedQueue<OutgoingCommand>();

        protected STATE state;

        protected int[] unsequencedWindow;

        protected int windowSize;

        /**
         * @param address
         * @param channelCount
         */
        Peer( Host host, InetSocketAddress address, int channelCount ) {
                super();
                init( host );
                this.address = address;
                state = STATE.CONNECTING;
                channels = new HashMap<Byte, Channel>();
                challenge = (int) ( Math.random() * Integer.MAX_VALUE );
                unsequencedWindow = new int[configuration.getInt( "ENET_PEER_UNSEQUENCED_WINDOW_SIZE" ) / 32];

                if ( host.getOutgoingBandwidth() == 0 )
                        windowSize = configuration.getInt( "ENET_PROTOCOL_MAXIMUM_WINDOW_SIZE" );
                else
                        windowSize = host.getOutgoingBandwidth() / configuration.getInt( "ENET_PEER_WINDOW_SIZE_SCALE" )
                                        * configuration.getInt( "ENET_PROTOCOL_MINIMUM_WINDOW_SIZE" );

                if ( windowSize < configuration.getInt( "ENET_PROTOCOL_MINIMUM_WINDOW_SIZE" ) )
                        windowSize = configuration.getInt( "ENET_PROTOCOL_MINIMUM_WINDOW_SIZE" );
                else if ( windowSize > configuration.getInt( "ENET_PROTOCOL_MAXIMUM_WINDOW_SIZE" ) )
                        windowSize = configuration.getInt( "ENET_PROTOCOL_MAXIMUM_WINDOW_SIZE" );
        }

        Event checkTimeouts() {
                Event result = new Event();
                OutgoingCommand outgoingCommand = null;
                for ( Iterator<OutgoingCommand> currentCommand = sentReliableCommands.iterator(); currentCommand
                                .hasNext(); ) {
                        outgoingCommand = currentCommand.next();

                        if ( Time.difference( host.getTimeCurrent(), outgoingCommand.getSentTime() ) < outgoingCommand
                                        .getRoundTripTimeout() )
                                continue;

                        if ( outgoingCommand.getRoundTripTimeout() >= outgoingCommand.getRoundTripTimeoutLimit() ) {
                                reset();
                                result.setType( Event.TYPE.DISCONNECTED );
                                result.setPeer( this );
                                return result;
                        }

                        if ( outgoingCommand.getPacket() != null )
                                reliableDataInTransit -= outgoingCommand.getFragmentLength();

                        packetsLost++;

                        outgoingCommand.setRoundTripTimeout( outgoingCommand.getRoundTripTimeout() * 2 );
                        outgoingReliableCommands.add( outgoingCommand );
                        currentCommand.remove();

                        if ( sentReliableCommands.size() == 1 )
                                nextTimeout = outgoingCommand.getSentTime() + outgoingCommand.getRoundTripTimeout();
                }
                return result;
        }

        /**
         * Requests a disconnection from this peer. When the disconnection is
         * completed, {@link Host#service Host.service} will return an
         * {@link Event Event} object having its type set to
         * <code>Event.TYPE.DISCONNECTED</code>
         */
        public void disconnect() {
                Disconnect command = new Disconnect();

                if ( isDisconnected() || isDisconnecting() || isZombie() )
                        return;

                resetQueues();

                command.getHeader().setChannelID( (byte) 0xFF );
                command.getHeader().setFlags( Header.FLAG_UNSEQUENCED );

                if ( isConnected() )
                        command.getHeader().setFlags( Header.FLAG_ACKNOWLEDGE );

                queueOutgoingCommand( command, null, 0, (short) 0 );

                if ( isConnected() )
                        state = STATE.DISCONNECTING;
                else {
                        host.flush();
                        reset();
                }
        }

        /**
         * Immediately disconnects this peer. Performs instantaneous disconnection
         * of this peer and sends it a disconnection notification which is not
         * guaranteed to arrive. It will not produce an Event in
         * {@link Host#service Host.service}.
         */
        public void disconnectNow() {
                if ( isDisconnected() )
                        return;

                if ( !isZombie() && !isDisconnecting() ) {
                        Disconnect command = new Disconnect();
                        command.getHeader().setChannelID( (byte) 0xFF );
                        command.getHeader().setFlags( Header.FLAG_UNSEQUENCED );
                        queueOutgoingCommand( command, null, 0, (short) 0 );
                        host.flush();
                }

                reset();
        }

        boolean fitsInPacket( IByteSize object ) {
                boolean fits = host.getCommandCount() < configuration
                                .getInt( "ENET_PROTOCOL_MAXIMUM_PACKET_COMMANDS" );
                fits &= host.getBufferCount() < configuration.getInt( "ENET_BUFFER_MAXIMUM" );
                fits &= mtu - host.getPacketSize() >= object.byteSize();
                return fits;
        }

        /**
         * @return Returns the acknowledgements.
         */
        ConcurrentLinkedQueue<Acknowledgement> getAcknowledgements() {
                return acknowledgements;
        }

        /**
         * Returns this peer's address.
         * 
         * @return Returns the address.
         */
        public InetSocketAddress getAddress() {
                return address;
        }

        /**
         * @return Returns the challenge.
         */
        int getChallenge() {
                return challenge;
        }

        /**
         * Returns the number of channels for this peer.
         * 
         * @return number of channels.
         */
        public int getChannelCount() {
                return channelCount;
        }

        /**
         * @return Returns the channels.
         */
        Map<Byte, Channel> getChannels() {
                return channels;
        }

        /**
         * @return Returns the configuration.
         */
        PropertiesConfiguration getConfiguration() {
                return configuration;
        }

        /**
         * @return Returns the highestRoundTripTimeVariance.
         */
        int getHighestRoundTripTimeVariance() {
                return highestRoundTripTimeVariance;
        }

        /**
         * Returns the host this peer is connected to.
         * 
         * @return The host.
         */
        public Host getHost() {
                return host;
        }

        /**
         * Returns the actual incoming bandwidth used to communicate with this peer.
         * 
         * @return The incomingBandwidth.
         */
        public int getIncomingBandwidth() {
                return incomingBandwidth;
        }

        /**
         * @return Returns the incomingBandwidthThrottleEpoch.
         */
        int getIncomingBandwidthThrottleEpoch() {
                return incomingBandwidthThrottleEpoch;
        }

        /**
         * @return Returns the incomingDataTotal.
         */
        int getIncomingDataTotal() {
                return incomingDataTotal;
        }

        /**
         * @return Returns the incomingPeerID.
         */
        short getIncomingPeerID() {
                return incomingPeerID;
        }

        /**
         * @return Returns the incomingUnsequencedGroup.
         */
        int getIncomingUnsequencedGroup() {
                return incomingUnsequencedGroup;
        }

        /**
         * @return Returns the lastReceiveTime.
         */
        int getLastReceiveTime() {
                return lastReceiveTime;
        }

        /**
         * Returns the last round trip time to communicate with this peer. The round
         * trip time is the time elapsed between sending a reliable packet and the
         * reception of the corresponding acknowledge.
         * 
         * @return The time in milliseconds.
         */
        public int getLastRoundTripTime() {
                return lastRoundTripTime;
        }

        /**
         * @return Returns the lastRoundTripTimeVariance.
         */
        int getLastRoundTripTimeVariance() {
                return lastRoundTripTimeVariance;
        }

        /**
         * @return Returns the lastSendTime.
         */
        int getLastSendTime() {
                return lastSendTime;
        }

        /**
         * @return Returns the lowestRoundTripTime.
         */
        int getLowestRoundTripTime() {
                return lowestRoundTripTime;
        }

        /**
         * @return Returns the mtu.
         */
        short getMtu() {
                return mtu;
        }

        /**
         * @return Returns the nextTimeout.
         */
        int getNextTimeout() {
                return nextTimeout;
        }

        /**
         * Returns the actual outgoing bandwidth used to communicate with this peer.
         * 
         * @return The outgoingBandwidth.
         */
        int getOutgoingBandwidth() {
                return outgoingBandwidth;
        }

        /**
         * @return Returns the outgoingBandwidthThrottleEpoch.
         */
        int getOutgoingBandwidthThrottleEpoch() {
                return outgoingBandwidthThrottleEpoch;
        }

        /**
         * @return Returns the outgoingDataTotal.
         */
        int getOutgoingDataTotal() {
                return outgoingDataTotal;
        }

        /**
         * @return Returns the outgoingPeerID.
         */
        short getOutgoingPeerID() {
                return outgoingPeerID;
        }

        /**
         * @return Returns the outgoingReliableCommands.
         */
        ConcurrentLinkedQueue<OutgoingCommand> getOutgoingReliableCommands() {
                return outgoingReliableCommands;
        }

        /**
         * @return Returns the outgoingReliableSequenceNumber.
         */
        int getOutgoingReliableSequenceNumber() {
                return outgoingReliableSequenceNumber;
        }

        /**
         * @return Returns the outgoingUnreliableCommands.
         */
        ConcurrentLinkedQueue<OutgoingCommand> getOutgoingUnreliableCommands() {
                return outgoingUnreliableCommands;
        }

        /**
         * @return Returns the outgoingUnsequencedGroup.
         */
        int getOutgoingUnsequencedGroup() {
                return outgoingUnsequencedGroup;
        }

        /**
         * @return Returns the packetLoss.
         */
        int getPacketLoss() {
                return packetLoss;
        }

        /**
         * @return Returns the packetLossEpoch.
         */
        int getPacketLossEpoch() {
                return packetLossEpoch;
        }

        /**
         * @return Returns the packetLossVariance.
         */
        int getPacketLossVariance() {
                return packetLossVariance;
        }

        /**
         * The total number of packets lost when commincating with this peer.
         * 
         * @return Returns the packetsLost.
         */
        public int getPacketsLost() {
                return packetsLost;
        }

        /**
         * The total number of sent packets.
         * 
         * @return Returns the packetsSent.
         */
        public int getPacketsSent() {
                return packetsSent;
        }

        /**
         * @return Returns the packetThrottle.
         */
        int getPacketThrottle() {
                return packetThrottle;
        }

        /**
         * @return Returns the packetThrottleAcceleration.
         */
        int getPacketThrottleAcceleration() {
                return packetThrottleAcceleration;
        }

        /**
         * @return Returns the packetThrottleCounter.
         */
        int getPacketThrottleCounter() {
                return packetThrottleCounter;
        }

        /**
         * @return Returns the packetThrottleDeceleration.
         */
        int getPacketThrottleDeceleration() {
                return packetThrottleDeceleration;
        }

        /**
         * @return Returns the packetThrottleEpoch.
         */
        int getPacketThrottleEpoch() {
                return packetThrottleEpoch;
        }

        /**
         * @return Returns the packetThrottleInterval.
         */
        int getPacketThrottleInterval() {
                return packetThrottleInterval;
        }

        /**
         * @return Returns the packetThrottleLimit.
         */
        int getPacketThrottleLimit() {
                return packetThrottleLimit;
        }

        /**
         * @return Returns the reliableDataInTransit.
         */
        int getReliableDataInTransit() {
                return reliableDataInTransit;
        }

        /**
         * Returns mean round trip time for reliable packages.
         * 
         * @see #lastRoundTripTime
         * @return the time in milliseconds.
         */
        public int getRoundTripTime() {
                return roundTripTime;
        }

        /**
         * @return Returns the roundTripTimeVariance.
         */
        int getRoundTripTimeVariance() {
                return roundTripTimeVariance;
        }

        /**
         * @return Returns the sentReliableCommands.
         */
        ConcurrentLinkedQueue<OutgoingCommand> getSentReliableCommands() {
                return sentReliableCommands;
        }

        /**
         * @return Returns the sentUnreliableCommands.
         */
        ConcurrentLinkedQueue<OutgoingCommand> getSentUnreliableCommands() {
                return sentUnreliableCommands;
        }

        /**
         * @return Returns the state.
         */
        STATE getState() {
                return state;
        }

        /**
         * @return Returns the unsequencedWindow.
         */
        int[] getUnsequencedWindow() {
                return unsequencedWindow;
        }

        /**
         * @return Returns the windowSize.
         */
        int getWindowSize() {
                return windowSize;
        }

        void init( Host host ) {
                this.host = host;
                this.configuration = host.getConfiguration();
                outgoingPeerID = (short) 0xFFFF;
                challenge = 0;
                address = new InetSocketAddress( (InetAddress) null, 0 );

                state = STATE.DISCONNECTED;

                incomingBandwidth = 0;
                outgoingBandwidth = 0;
                incomingBandwidthThrottleEpoch = 0;
                outgoingBandwidthThrottleEpoch = 0;
                incomingDataTotal = 0;
                outgoingDataTotal = 0;
                lastSendTime = 0;
                lastReceiveTime = 0;
                nextTimeout = 0;
                packetLossEpoch = 0;
                packetsSent = 0;
                packetsLost = 0;
                packetLoss = 0;
                packetLossVariance = 0;
                packetThrottle = configuration.getInt( "ENET_PEER_DEFAULT_PACKET_THROTTLE" );
                packetThrottleLimit = configuration.getInt( "ENET_PEER_PACKET_THROTTLE_SCALE" );
                packetThrottleCounter = 0;
                packetThrottleEpoch = 0;
                packetThrottleAcceleration = configuration.getInt( "ENET_PEER_PACKET_THROTTLE_ACCELERATION" );
                packetThrottleDeceleration = configuration.getInt( "ENET_PEER_PACKET_THROTTLE_DECELERATION" );
                packetThrottleInterval = configuration.getInt( "ENET_PEER_PACKET_THROTTLE_INTERVAL" );
                lastRoundTripTime = configuration.getInt( "ENET_PEER_DEFAULT_ROUND_TRIP_TIME" );
                lowestRoundTripTime = configuration.getInt( "ENET_PEER_DEFAULT_ROUND_TRIP_TIME" );
                lastRoundTripTimeVariance = 0;
                highestRoundTripTimeVariance = 0;
                roundTripTime = configuration.getInt( "ENET_PEER_DEFAULT_ROUND_TRIP_TIME" );
                roundTripTimeVariance = 0;
                mtu = host.getMtu();
                reliableDataInTransit = 0;
                outgoingReliableSequenceNumber = 0;
                windowSize = configuration.getInt( "ENET_PROTOCOL_MAXIMUM_WINDOW_SIZE" );
                incomingUnsequencedGroup = 0;
                outgoingUnsequencedGroup = 0;
        }

        /**
         * Returns true if this peer is connected.
         * 
         * @return the connection status
         */
        public boolean isConnected() {
                return state == STATE.CONNECTED;
        }

        /**
         * Returns true if a connection request has been sent to this peer.
         * 
         * @return the connection status
         */
        public boolean isConnecting() {
                return state == STATE.CONNECTING;
        }

        /**
         * Returns true if this peer has been disconnected. If the peer is
         * disconnected, it will not get in its connected state again.
         * 
         * @return the connection status
         */
        public boolean isDisconnected() {
                return state == STATE.DISCONNECTED;
        }

        /**
         * Returns true if a disconnection request has been sent to this peer.
         * 
         * @return the connection status
         */
        public boolean isDisconnecting() {
                return state == STATE.DISCONNECTING;
        }

        /**
         * Returns true if this peer is disconnecting due to a problem.
         * 
         * @return the connection state
         */
        public boolean isZombie() {
                return state == STATE.ZOMBIE;
        }

        /**
         * Sends a ping to a a peer. This is automatically done by JeNet, so the
         * only reason to call this method is to increase ping frequency.
         */
        public void ping() {
                Ping command = new Ping();
                if ( !isConnected() )
                        return;
                command.getHeader().setChannelID( (byte) 0xFF );
                command.getHeader().setFlags( Header.FLAG_ACKNOWLEDGE );
                queueOutgoingCommand( command, null, 0, (short) 0 );
        }

        /**
         * @param command
         * @param sentTime
         */
        void queueAcknowledgement( Command command, int sentTime ) {
                Acknowledgement acknowledgement = new Acknowledgement();
                outgoingDataTotal += command.byteSize();
                acknowledgement.sentTime = sentTime;
                acknowledgement.command = command;
                acknowledgements.add( acknowledgement );
        }

        /**
         * @param hostCommand
         * @param packet
         * @param fragmentCount
         */
        IncomingCommand queueIncomingCommand( Command command, Packet packet, int fragmentCount ) {
                Channel channel = selectChannel( command.getHeader().getChannelID() );
                LinkedList<IncomingCommand> commandList = null;
                int unreliableSequenceNumber = 0;
                boolean freePacket = false;
                if ( command instanceof SendFragment || command instanceof SendReliable ) {
                        commandList = channel.getIncomingReliableCommands();
                        for ( IncomingCommand incomingCommand : channel.getIncomingReliableCommands() )
                                if ( incomingCommand.getReliableSequenceNumber() <= command.getHeader() 
                                                .getReliableSequenceNumber() ) {
                                        if ( incomingCommand.getReliableSequenceNumber() < command.getHeader()
                                                        .getReliableSequenceNumber() )
                                                freePacket = false;
                                        else
                                                freePacket = true;
                                        break;
                                }
                } else if ( command instanceof SendUnreliable ) {
                        commandList = channel.getIncomingUnreliableCommands();
                        unreliableSequenceNumber = ( (SendUnreliable) command ).getUnreliableSequenceNumber();
                        if ( command.getHeader().getReliableSequenceNumber() < channel
                                        .getIncomingReliableSequenceNumber() )
                                freePacket = true;
                        else if ( unreliableSequenceNumber <= channel.getIncomingUnreliableSequenceNumber() )
                                freePacket = true;
                        else
                                for ( IncomingCommand incomingCommand : channel.getIncomingUnreliableCommands() )
                                        if ( incomingCommand.getUnreliableSequenceNumber() <= unreliableSequenceNumber ) {
                                                if ( incomingCommand.getUnreliableSequenceNumber() < unreliableSequenceNumber )
                                                        freePacket = false;
                                                else
                                                        freePacket = true;
                                                break;
                                        }
                } else if ( command instanceof SendUnsequenced ) {
                        commandList = channel.getIncomingUnreliableCommands();
                } else {
                        freePacket = true;
                }

                if ( !freePacket ) {
                        IncomingCommand incomingCommand = new IncomingCommand();
                        incomingCommand.setReliableSequenceNumber( command.getHeader().getReliableSequenceNumber() );
                        incomingCommand.setUnreliableSequenceNumber( unreliableSequenceNumber );
                        incomingCommand.setCommand( command );
                        incomingCommand.setFragmentCount( fragmentCount );
                        incomingCommand.setFragmentsRemaining( fragmentCount );
                        incomingCommand.setPacket( packet );
                        incomingCommand.setFragments( null );

                        if ( fragmentCount > 0 )
                                incomingCommand.setFragments( new int[fragmentCount + 31 / 32] );

                        commandList.add( incomingCommand );
                        return incomingCommand;
                }
                return null;
        }

        OutgoingCommand queueOutgoingCommand( Command command, Packet packet, int offset, short length ) {
                Channel channel = selectChannel( command.getHeader().getChannelID() );
                OutgoingCommand outgoingCommand = new OutgoingCommand();
                outgoingDataTotal += command.getHeader().getCommandLength() + length;

                if ( command.getHeader().getChannelID() == (byte) ( 0xFF ) ) {
                        ++outgoingReliableSequenceNumber;
                        outgoingCommand.setReliableSequenceNumber( outgoingReliableSequenceNumber );
                        outgoingCommand.setUnreliableSequenceNumber( 0 );
                } else if ( ( command.getHeader().getFlags() & Header.FLAG_ACKNOWLEDGE ) != 0 ) {
                        channel.setOutgoingReliableSequenceNumber( channel.getOutgoingReliableSequenceNumber() + 1 );
                        outgoingCommand.setReliableSequenceNumber( channel.getOutgoingReliableSequenceNumber() );
                        outgoingCommand.setUnreliableSequenceNumber( 0 );
                } else if ( ( command.getHeader().getFlags() & Header.FLAG_UNSEQUENCED ) != 0 ) {
                        ++outgoingUnsequencedGroup;
                        outgoingCommand.setReliableSequenceNumber( 0 );
                        outgoingCommand.setUnreliableSequenceNumber( 0 );
                } else {
                        channel.setOutgoingUnreliableSequenceNumber( channel.getOutgoingUnreliableSequenceNumber() + 1 );
                        outgoingCommand.setReliableSequenceNumber( channel.getOutgoingReliableSequenceNumber() );
                        outgoingCommand.setUnreliableSequenceNumber( channel.getOutgoingUnreliableSequenceNumber() );
                }

                outgoingCommand.setSentTime( 0 );
                outgoingCommand.setRoundTripTimeout( 0 );
                outgoingCommand.setRoundTripTimeoutLimit( 0 );
                outgoingCommand.setFragmentOffset( offset );
                outgoingCommand.setFragmentLength( length );
                outgoingCommand.setPacket( packet );
                outgoingCommand.setCommand( command );
                outgoingCommand.getCommand().getHeader().setReliableSequenceNumber(
                                outgoingCommand.getReliableSequenceNumber() );

                if ( packet != null )
                        packet.setReferenceCount( packet.getReferenceCount() + 1 );

                LOG.debug( "Sent " + outgoingCommand.command );
                if ( ( command.getHeader().getFlags() & Header.FLAG_ACKNOWLEDGE ) != 0 )
                        outgoingReliableCommands.add( outgoingCommand );
                else 
                        outgoingUnreliableCommands.add( outgoingCommand );

                return outgoingCommand;
        }

        Packet receive( byte channelID ) {
                Channel channel = selectChannel( channelID );
                IncomingCommand incomingCommand = null;
                Packet packet;
                LinkedList<IncomingCommand> listRemove = null;

                if ( !channel.getIncomingUnreliableCommands().isEmpty() ) {
                        listRemove = channel.getIncomingUnreliableCommands();
                        incomingCommand = channel.getIncomingUnreliableCommands().getFirst();

                        if ( incomingCommand.getUnreliableSequenceNumber() > 0 )
                                if ( incomingCommand.getReliableSequenceNumber() > channel
                                                .getIncomingReliableSequenceNumber() )
                                        incomingCommand = null;
                                else
                                        channel.setIncomingUnreliableSequenceNumber( incomingCommand
                                                        .getUnreliableSequenceNumber() );
                }

                if ( incomingCommand == null && !channel.getIncomingReliableCommands().isEmpty() ) {
                        listRemove = channel.getIncomingReliableCommands();
                        do {
                                incomingCommand = channel.getIncomingReliableCommands().getFirst();
                                if ( incomingCommand.getFragmentsRemaining() > 0
                                                || incomingCommand.getReliableSequenceNumber() > channel
                                                                .getIncomingReliableSequenceNumber() + 1 )
                                        return null;

                                if ( incomingCommand.getReliableSequenceNumber() <= channel
                                                .getIncomingReliableSequenceNumber() ) {
                                        channel.getIncomingReliableCommands().remove( incomingCommand );
                                        incomingCommand = null;
                                }
                        } while ( incomingCommand == null && !channel.getIncomingReliableCommands().isEmpty() );

                        if ( incomingCommand == null )
                                return null;

                        channel.setIncomingReliableSequenceNumber( incomingCommand.getReliableSequenceNumber() );

                        if ( incomingCommand.getFragmentCount() > 0 )
                                channel.setIncomingReliableSequenceNumber( incomingCommand.getReliableSequenceNumber()
                                                + incomingCommand.getFragmentCount() - 1 );

                }

                if ( incomingCommand == null )
                        return null;

                listRemove.remove( incomingCommand );

                packet = incomingCommand.getPacket();
                return packet;
        }

        /**
         * @param receivedReliableSequenceNumber
         * @param channelID
         * @return
         */
        Command removeSentReliableCommand( int reliableSequenceNumber, byte channelID ) {
                OutgoingCommand outgoingCommand = null;

                for ( OutgoingCommand currentCommand : sentReliableCommands )
                        if ( currentCommand.getReliableSequenceNumber() == reliableSequenceNumber
                                        && currentCommand.getCommand().getHeader().getChannelID() == channelID ) {
                                outgoingCommand = currentCommand;
                                break;
                        }

                if ( outgoingCommand == null )
                        return null;

                Command commandNumber = outgoingCommand.getCommand();
                sentReliableCommands.remove( outgoingCommand );
                if ( outgoingCommand.getPacket() != null )
                        reliableDataInTransit -= outgoingCommand.getFragmentLength();

                if ( sentReliableCommands.isEmpty() )
                        return commandNumber;

                outgoingCommand = sentReliableCommands.element();
                nextTimeout = outgoingCommand.getSentTime() + outgoingCommand.getRoundTripTimeout();

                return commandNumber;
        }

        /**
         * Forcefully disconnects this peer. The foreign host represented by this
         * peer is not notified of the disconnection and its connection with the
         * local host will time out.
         */
        public void reset() {
                Object obj = host.getPeers().remove( getIncomingPeerID() );
                state = STATE.DISCONNECTED;
                LOG.debug("removing from peers: "+obj);
        }

        void resetQueues() {
                acknowledgements.clear();
                sentReliableCommands.clear();
                sentUnreliableCommands.clear();
                outgoingReliableCommands.clear();
                outgoingUnreliableCommands.clear();
                channels.clear();
        }

        Channel selectChannel( byte channelID ) {
                if ( channelID != (byte) 0xFF && channelID >= channelCount )
                        return null;
                Channel result = channels.get( channelID );
                if ( result == null ) {
                        result = new Channel();
                        channels.put( channelID, result );
                }

                return result;
        }

        /**
         * Queues a packet to be sent to this peer.
         * 
         * @param channelID
         *            the channel on which to send
         * @param packet
         *            the packet to send
         * @throws IOException
         *             on failure
         */
        public void send( byte channelID, Packet packet ) throws IOException {
                LOG.debug( host.getAddress() + ": sending packet to " + address + " on channel " + channelID );
                Channel channel = selectChannel( channelID );

                Header header = new Header();
                Command command = new SendFragment();
                int fragmentLength;

                if ( !isConnected() || channel == null )
                        throw new IOException();

                fragmentLength = mtu - header.byteSize() - command.byteSize();
                if ( packet.getDataLength() > fragmentLength ) {
                        int fragmentCount = ( packet.getDataLength() + fragmentLength - 1 ) / fragmentLength;
                        int startSequenceNumber = channel.getOutgoingReliableSequenceNumber() + 1;
                        int fragmentNumber;
                        int fragmentOffset;

                        packet.setFlags( Packet.FLAG_RELIABLE );

                        for ( fragmentNumber = 0, fragmentOffset = 0; fragmentOffset < packet.getDataLength(); ++fragmentNumber, fragmentOffset += fragmentLength ) {
                                SendFragment sf = new SendFragment();
                                command = sf;
                                sf.getHeader().setChannelID( channelID );
                                sf.getHeader().setFlags( Header.FLAG_ACKNOWLEDGE );
                                sf.setFragmentNumber( fragmentNumber );
                                sf.setStartSequenceNumber( startSequenceNumber );
                                sf.setFragmentCount( fragmentCount );
                                sf.setTotalLength( packet.getDataLength() );
                                sf.setFragmentOffset( fragmentOffset );

                                if ( packet.getDataLength() - fragmentOffset < fragmentLength )
                                        fragmentLength = packet.getDataLength() - fragmentOffset;

                                queueOutgoingCommand( command, packet, fragmentOffset, (short) fragmentLength );
                        }
                        return;
                }

                if ( ( packet.getFlags() & Packet.FLAG_RELIABLE ) != 0 ) {
                        command = new SendReliable();
                        command.getHeader().setChannelID( channelID ); 
                        command.getHeader().setFlags( Header.FLAG_ACKNOWLEDGE );
                } else if ( ( packet.getFlags() & Packet.FLAG_UNSEQUENCED ) != 0 ) {
                        SendUnsequenced su = new SendUnsequenced();
                        command = su;
                        su.getHeader().setChannelID( channelID ); 
                        su.getHeader().setFlags( Header.FLAG_UNSEQUENCED );
                        su.setUnsequencedGroup( outgoingUnsequencedGroup + 1 );
                } else {
                        SendUnreliable su = new SendUnreliable();
                        command = su;
                        su.getHeader().setChannelID( channelID ); 
                        su.getHeader().setFlags( (byte) 0 );
                        su.setUnreliableSequenceNumber( channel.getOutgoingUnreliableSequenceNumber() + 1 );
                }

                queueOutgoingCommand( command, packet, 0, (short) packet.getDataLength() );
        }

        void sendAcknowledgements() {
                for ( Acknowledgement acknowledgement : acknowledgements ) {
                        Acknowledge command = new Acknowledge();

                        if ( !fitsInPacket( command ) )
                                break;

                        command.getHeader().setChannelID( acknowledgement.getCommand().getHeader().getChannelID() );
                        command.getHeader().setFlags( (byte) 0 );

                        command.setReceivedReliableSequenceNumber( acknowledgement.getCommand().getHeader()
                                        .getReliableSequenceNumber() );
                        command.setReceivedSentTime( acknowledgement.getSentTime() );

                        host.getCommands().add( command );

                        host.buffer( command );

                        if ( acknowledgement.getCommand() instanceof Disconnect )
                                state = STATE.ZOMBIE;

                }
                acknowledgements.clear();
        }

        void sendReliableOutgoingCommands() {
                for ( Iterator<OutgoingCommand> currentCommand = outgoingReliableCommands.iterator(); currentCommand
                                .hasNext(); ) {
                        OutgoingCommand outgoingCommand = currentCommand.next();

                        if ( !fitsInPacket( outgoingCommand ) )
                                break;

                        if ( outgoingCommand.getPacket() != null
                                        && reliableDataInTransit + outgoingCommand.getFragmentLength() > windowSize )
                                break;

                        if ( outgoingCommand.getRoundTripTimeout() == 0 ) {
                                outgoingCommand.setRoundTripTimeout( roundTripTime + 4 * roundTripTimeVariance );
                                outgoingCommand.setRoundTripTimeoutLimit( outgoingCommand.getRoundTripTimeout()
                                                * configuration.getInt( "ENET_PEER_TIMEOUT_LIMIT" ) );
                        }

                        if ( sentReliableCommands.isEmpty() )
                                nextTimeout = host.getTimeCurrent() + outgoingCommand.getRoundTripTimeout();

                        outgoingCommand.sentTime = host.getTimeCurrent();
                        host.getCommands().add( outgoingCommand.getCommand() );

                        if ( outgoingCommand.getPacket() != null ) {
                                int length = outgoingCommand.getCommand().getHeader().getCommandLength();
                                length += outgoingCommand.getFragmentLength();
                                outgoingCommand.getCommand().getHeader().setCommandLength( length );
                                // TODO:Check updating host packetSize
                                reliableDataInTransit += outgoingCommand.getFragmentLength();
                        }

                        host.buffer( outgoingCommand );
                        currentCommand.remove();

                        sentReliableCommands.add( outgoingCommand );

                        packetsSent++;
                }
        }

        void sendUnreliableOutgoingCommands() {
                // TODO: Verify Unreliable sending
                for ( Iterator<OutgoingCommand> currentCommand = outgoingUnreliableCommands.iterator(); currentCommand
                                .hasNext(); ) {
                        OutgoingCommand outgoingCommand = currentCommand.next();

                        if ( !fitsInPacket( outgoingCommand ) )
                                break;

                        if ( outgoingCommand.getPacket() != null ) {
                                packetThrottleCounter += configuration.getInt( "ENET_PEER_PACKET_THROTTLE_COUNTER" );
                                packetThrottleCounter %= configuration.getInt( "ENET_PEER_PACKET_THROTTLE_SCALE" );

                                if ( packetThrottleCounter > packetThrottle ) {
                                        currentCommand.remove();
                                        continue;
                                }
                        }

                        host.buffer( outgoingCommand );
                        currentCommand.remove();
                        host.getCommands().add( outgoingCommand.getCommand() );

                        if ( outgoingCommand.getPacket() != null )
                                sentUnreliableCommands.add( outgoingCommand );
                }
        }

        /**
         * @param address
         *            The address to set.
         */
        void setAddress( InetSocketAddress address ) {
                this.address = address;
        }

        /**
         * @param challenge
         *            The challenge to set.
         */
        void setChallenge( int challenge ) {
                this.challenge = challenge;
        }

        void setChannelCount( int channelCount ) {
                this.channelCount = channelCount;
        }

        /**
         * @param channels
         *            The channels to set.
         */
        void setChannels( Map<Byte, Channel> channels ) {
                this.channels = channels;
        }

        /**
         * @param incomingBandwidth
         *            The incomingBandwidth to set.
         */
        void setIncomingBandwidth( int incomingBandwidth ) {
                this.incomingBandwidth = incomingBandwidth;
        }

        /**
         * @param incomingBandwidthThrottleEpoch
         *            The incomingBandwidthThrottleEpoch to set.
         */
        void setIncomingBandwidthThrottleEpoch( int incomingBandwidthThrottleEpoch ) {
                this.incomingBandwidthThrottleEpoch = incomingBandwidthThrottleEpoch;
        }

        /**
         * @param incomingDataTotal
         *            The incomingDataTotal to set.
         */
        void setIncomingDataTotal( int incomingDataTotal ) {
                this.incomingDataTotal = incomingDataTotal;
        }

        /**
         * @param incomingPeerID
         *            The incomingPeerID to set.
         */
        void setIncomingPeerID( short incomingPeerID ) {
                this.incomingPeerID = incomingPeerID;
        }

        /**
         * @param incomingUnsequencedGroup
         *            The incomingUnsequencedGroup to set.
         */
        void setIncomingUnsequencedGroup( int incomingUnsequencedGroup ) {
                this.incomingUnsequencedGroup = incomingUnsequencedGroup;
        }

        /**
         * @param lastReceiveTime
         *            The lastReceiveTime to set.
         */
        void setLastReceiveTime( int lastReceiveTime ) {
                this.lastReceiveTime = lastReceiveTime;
        }

        /**
         * @param lastSendTime
         *            The lastSendTime to set.
         */
        void setLastSendTime( int lastSendTime ) {
                this.lastSendTime = lastSendTime;
        }

        /**
         * @param mtu
         *            The mtu to set.
         */
        void setMtu( short mtu ) {
                this.mtu = mtu;
        }

        /**
         * @param outgoingBandwidth
         *            The outgoingBandwidth to set.
         */
        void setOutgoingBandwidth( int outgoingBandwidth ) {
                this.outgoingBandwidth = outgoingBandwidth;
        }

        /**
         * @param outgoingBandwidthThrottleEpoch
         *            The outgoingBandwidthThrottleEpoch to set.
         */
        void setOutgoingBandwidthThrottleEpoch( int outgoingBandwidthThrottleEpoch ) {
                this.outgoingBandwidthThrottleEpoch = outgoingBandwidthThrottleEpoch;
        }

        /**
         * @param outgoingDataTotal
         *            The outgoingDataTotal to set.
         */
        void setOutgoingDataTotal( int outgoingDataTotal ) {
                this.outgoingDataTotal = outgoingDataTotal;
        }

        /**
         * @param outgoingPeerID
         *            The outgoingPeerID to set.
         */
        void setOutgoingPeerID( short outgoingPeerID ) {
                this.outgoingPeerID = outgoingPeerID;
        }

        /**
         * @param packetLossEpoch
         *            The packetLossEpoch to set.
         */
        void setPacketLossEpoch( int packetLossEpoch ) {
                this.packetLossEpoch = packetLossEpoch;
        }

        /**
         * @param packetThrottle
         *            The packetThrottle to set.
         */
        void setPacketThrottle( int packetThrottle ) {
                this.packetThrottle = packetThrottle;
        }

        /**
         * @param packetThrottleAcceleration
         *            The packetThrottleAcceleration to set.
         */
        void setPacketThrottleAcceleration( int packetThrottleAcceleration ) {
                this.packetThrottleAcceleration = packetThrottleAcceleration;
        }

        /**
         * @param packetThrottleDeceleration
         *            The packetThrottleDeceleration to set.
         */
        void setPacketThrottleDeceleration( int packetThrottleDeceleration ) {
                this.packetThrottleDeceleration = packetThrottleDeceleration;
        }

        /**
         * @param packetThrottleInterval
         *            The packetThrottleInterval to set.
         */
        void setPacketThrottleInterval( int packetThrottleInterval ) {
                this.packetThrottleInterval = packetThrottleInterval;
        }

        /**
         * @param packetThrottleLimit
         *            The packetThrottleLimit to set.
         */
        void setPacketThrottleLimit( int packetThrottleLimit ) {
                this.packetThrottleLimit = packetThrottleLimit;
        }

        /**
         * @param state
         *            The state to set.
         */
        void setState( STATE state ) {
                LOG.debug( host.getAddress() + ": peer " + address + " changed state to " + state );
                this.state = state;
        }

        /**
         * @param windowSize
         *            The windowSize to set.
         */
        void setWindowSize( int windowSize ) {
                this.windowSize = windowSize;
        }

        /**
         * @param roundTripTime2
         */
        void throttle( int rtt ) {
                if ( lastRoundTripTime <= lastRoundTripTimeVariance )
                        packetThrottle = packetThrottleLimit;
                else if ( rtt < lastRoundTripTime ) {
                        packetThrottle += packetThrottleAcceleration;
                        if ( packetThrottle > packetThrottleLimit )
                                packetThrottle = packetThrottleLimit;
                } else if ( rtt > lastRoundTripTime * 2 * lastRoundTripTimeVariance )
                        if ( packetThrottle > packetThrottleDeceleration )
                                packetThrottle -= packetThrottleDeceleration;
                        else
                                packetThrottle = 0;
        }

        /**
         * @param interval
         *                      The time in milliseconds to calculate the mean RTT
         * @param acceleration
         *                      The rate to increase the throttle as mean RTT increases
         * @param deceleration
         *                      The rate to decrease the throttle as mean RTT decreases
         */
        public void throttleConfigure( int interval, int acceleration, int deceleration ) {
                ThrottleConfigure command = new ThrottleConfigure();
                packetThrottleInterval = interval;
                packetThrottleAcceleration = acceleration;
                packetThrottleDeceleration = deceleration;

                command.setPacketThrottleInterval( interval );
                command.setPacketThrottleAcceleration( acceleration );
                command.setPacketThrottleDeceleration( deceleration );

                command.getHeader().setChannelID( (byte) 0xFF );
                command.getHeader().setFlags( Header.FLAG_ACKNOWLEDGE );
                queueOutgoingCommand( command, null, 0, (short) 0 );
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
                return new ToStringBuilder( this ).append( "address", address ).append( "state", state ).toString();
        }

        void updatePacketLossVariance( int currentTime ) {
                int lossDifference = packetsLost * configuration.getInt( "ENET_PEER_PACKET_LOSS_SCALE" )
                                / packetsSent - packetLoss;
                packetLossVariance -= packetLossVariance / 4;

                if ( lossDifference >= 0 ) {
                        packetLoss += lossDifference / 8;
                        packetLossVariance += lossDifference / 4;
                } else {
                        packetLoss += lossDifference / 8;
                        packetLossVariance -= lossDifference / 4;
                }

                packetLossEpoch = currentTime;
                packetsSent = 0;
                packetsLost = 0;
        }

        void updateRoundTripTimeVariance( int time ) {
                roundTripTimeVariance -= roundTripTimeVariance / 4;
                if ( time >= roundTripTime ) {
                        roundTripTime += ( time - roundTripTime ) / 8;
                        roundTripTimeVariance += ( time - roundTripTime ) / 4;
                } else {
                        roundTripTime += ( time - roundTripTime ) / 8;
                        roundTripTimeVariance -= ( time - roundTripTime ) / 4;
                }

                if ( roundTripTime < lowestRoundTripTime )
                        lowestRoundTripTime = roundTripTime;

                if ( roundTripTimeVariance > highestRoundTripTimeVariance )
                        highestRoundTripTimeVariance = roundTripTimeVariance;

                if ( packetThrottleEpoch == 0
                                || Time.difference( host.getTimeCurrent(), packetThrottleEpoch ) >= packetThrottleInterval ) {
                        lastRoundTripTime = lowestRoundTripTime;
                        lastRoundTripTimeVariance = highestRoundTripTimeVariance;
                        lowestRoundTripTime = roundTripTime;
                        highestRoundTripTimeVariance = roundTripTimeVariance;
                        packetThrottleEpoch = host.getTimeCurrent();
                }
        }

}
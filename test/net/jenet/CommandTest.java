/**
 * Copyright 2005 Dizan Vasquez All Rights Reserved
 */
package net.jenet;

import java.nio.ByteBuffer;

import net.jenet.Acknowledge;
import net.jenet.BandwidthLimit;
import net.jenet.Command;
import net.jenet.Connect;
import net.jenet.Disconnect;
import net.jenet.None;
import net.jenet.Ping;
import net.jenet.SendFragment;
import net.jenet.SendReliable;
import net.jenet.SendUnreliable;
import net.jenet.SendUnsequenced;
import net.jenet.ThrottleConfigure;
import net.jenet.VerifyConnect;

import junit.framework.TestCase;

/**
 * @author Dizan Vasquez
 */
public class CommandTest extends TestCase {

        public void testReadCommand() {
                None none = new None();
                none.getHeader().setChannelID( (byte) 1 );
                none.getHeader().setFlags( (byte) 2 );
                none.getHeader().setReliableSequenceNumber( 3 );
                none.getHeader().setReserved( (byte) 4 );

                Acknowledge acknowledge = new Acknowledge();
                acknowledge.getHeader().setChannelID( (byte) 1 );
                acknowledge.getHeader().setFlags( (byte) 2 );
                acknowledge.getHeader().setReliableSequenceNumber( 3 );
                acknowledge.getHeader().setReserved( (byte) 4 );

                Connect connect = new Connect();
                connect.getHeader().setChannelID( (byte) 1 );
                connect.getHeader().setFlags( (byte) 2 );
                connect.getHeader().setReliableSequenceNumber( 3 );
                connect.getHeader().setReserved( (byte) 4 );
                connect.setChannelCount( 5 );
                connect.setIncomingBandwidth( 6 );
                connect.setMtu( (short) 7 );
                connect.setOutgoingBandwidth( 8 );
                connect.setOutgoingPeerID( (short) 9 );
                connect.setPacketThrottleAcceleration( 10 );
                connect.setPacketThrottleDeceleration( 11 );
                connect.setPacketThrottleInterval( 12 );
                connect.setWindowSize( 13 );

                VerifyConnect verifyConnect = new VerifyConnect();
                verifyConnect.getHeader().setChannelID( (byte) 1 );
                verifyConnect.getHeader().setFlags( (byte) 2 );
                verifyConnect.getHeader().setReliableSequenceNumber( 3 );
                verifyConnect.getHeader().setReserved( (byte) 4 );
                verifyConnect.setChannelCount( 14 );
                verifyConnect.setIncomingBandwidth( 15 );
                verifyConnect.setMtu( (short) 16 );
                verifyConnect.setOutgoingBandwidth( 17 );
                verifyConnect.setOutgoingPeerID( (short) 18 );
                verifyConnect.setPacketThrottleAcceleration( 19 );
                verifyConnect.setPacketThrottleDeceleration( 20 );
                verifyConnect.setPacketThrottleInterval( 21 );
                verifyConnect.setWindowSize( 22 );

                Disconnect disconnect = new Disconnect();
                disconnect.getHeader().setChannelID( (byte) 1 );
                disconnect.getHeader().setFlags( (byte) 2 );
                disconnect.getHeader().setReliableSequenceNumber( 3 );
                disconnect.getHeader().setReserved( (byte) 4 );

                Ping ping = new Ping();
                ping.getHeader().setChannelID( (byte) 1 );
                ping.getHeader().setFlags( (byte) 2 );
                ping.getHeader().setReliableSequenceNumber( 3 );
                ping.getHeader().setReserved( (byte) 4 );

                SendReliable sendReliable = new SendReliable();
                sendReliable.getHeader().setChannelID( (byte) 1 );
                sendReliable.getHeader().setFlags( (byte) 2 );
                sendReliable.getHeader().setReliableSequenceNumber( 3 );
                sendReliable.getHeader().setReserved( (byte) 4 );

                SendUnreliable sendUnreliable = new SendUnreliable();
                sendUnreliable.getHeader().setChannelID( (byte) 1 );
                sendUnreliable.getHeader().setFlags( (byte) 2 );
                sendUnreliable.getHeader().setReliableSequenceNumber( 3 );
                sendUnreliable.getHeader().setReserved( (byte) 4 );
                sendUnreliable.setUnreliableSequenceNumber( 23 );

                SendFragment sendFragment = new SendFragment();
                sendFragment.getHeader().setChannelID( (byte) 1 );
                sendFragment.getHeader().setFlags( (byte) 2 );
                sendFragment.getHeader().setReliableSequenceNumber( 3 );
                sendFragment.getHeader().setReserved( (byte) 4 );
                sendFragment.setFragmentCount( 24 );
                sendFragment.setFragmentNumber( 25 );
                sendFragment.setFragmentOffset( 26 );
                sendFragment.setStartSequenceNumber( 27 );
                sendFragment.setTotalLength( 28 );

                BandwidthLimit bandwidthLimit = new BandwidthLimit();
                bandwidthLimit.getHeader().setChannelID( (byte) 1 );
                bandwidthLimit.getHeader().setFlags( (byte) 2 );
                bandwidthLimit.getHeader().setReliableSequenceNumber( 3 );
                bandwidthLimit.getHeader().setReserved( (byte) 4 );
                bandwidthLimit.setIncomingBandwidth( 29 );
                bandwidthLimit.setOutgoingBandwidth( 30 );

                ThrottleConfigure throttleConfigure = new ThrottleConfigure();
                throttleConfigure.getHeader().setChannelID( (byte) 1 );
                throttleConfigure.getHeader().setFlags( (byte) 2 );
                throttleConfigure.getHeader().setReliableSequenceNumber( 3 );
                throttleConfigure.getHeader().setReserved( (byte) 4 );
                throttleConfigure.setPacketThrottleAcceleration( 31 );
                throttleConfigure.setPacketThrottleDeceleration( 32 );
                throttleConfigure.setPacketThrottleInterval( 33 );

                SendUnsequenced sendUnsequenced = new SendUnsequenced();
                sendUnsequenced.getHeader().setChannelID( (byte) 1 );
                sendUnsequenced.getHeader().setFlags( (byte) 2 );
                sendUnsequenced.getHeader().setReliableSequenceNumber( 3 );
                sendUnsequenced.getHeader().setReserved( (byte) 4 );
                sendUnsequenced.setUnsequencedGroup( 34 );

                ByteBuffer buffer = ByteBuffer.allocateDirect( none.byteSize() + acknowledge.byteSize()
                                + connect.byteSize() + verifyConnect.byteSize() + disconnect.byteSize() + ping.byteSize()
                                + sendReliable.byteSize() + sendUnreliable.byteSize() + sendFragment.byteSize()
                                + bandwidthLimit.byteSize() + throttleConfigure.byteSize() + sendUnsequenced.byteSize() );

                none.toBuffer( buffer );
                acknowledge.toBuffer( buffer );
                connect.toBuffer( buffer );
                verifyConnect.toBuffer( buffer );
                disconnect.toBuffer( buffer );
                ping.toBuffer( buffer );
                sendReliable.toBuffer( buffer );
                sendUnreliable.toBuffer( buffer );
                sendFragment.toBuffer( buffer );
                bandwidthLimit.toBuffer( buffer );
                throttleConfigure.toBuffer( buffer );
                sendUnsequenced.toBuffer( buffer );

                buffer.flip();

                assertEquals( none, Command.readCommand( buffer ) );
                assertEquals( acknowledge, Command.readCommand( buffer ) );
                assertEquals( connect, Command.readCommand( buffer ) );
                assertEquals( verifyConnect, Command.readCommand( buffer ) );
                assertEquals( disconnect, Command.readCommand( buffer ) );
                assertEquals( ping, Command.readCommand( buffer ) );
                assertEquals( sendReliable, Command.readCommand( buffer ) );
                assertEquals( sendUnreliable, Command.readCommand( buffer ) );
                assertEquals( sendFragment, Command.readCommand( buffer ) );
                assertEquals( bandwidthLimit, Command.readCommand( buffer ) );
                assertEquals( throttleConfigure, Command.readCommand( buffer ) );
                assertEquals( sendUnsequenced, Command.readCommand( buffer ) );
        }

        public void testCommandCode() {
        }

        public void testFromBuffer() {
        }

        public void testToBuffer() {
        }

        public void testByteSize() {
        }

}

/**
 * Copyright 2005 Dizan Vasquez All Rights Reserved
 */

package net.jenet;

import java.net.InetSocketAddress;
import java.nio.*;
import java.nio.charset.*;

import junit.framework.TestCase;

import org.apache.commons.logging.*;


/**
 * @author Dizan Vasquez
 */
public class HostTest extends TestCase {

        private static final Log LOG = LogFactory.getLog(HostTest.class);

        static Host host1, host2;
        static InetSocketAddress address1 = new InetSocketAddress("localhost", 10012);
        static InetSocketAddress address2 = new InetSocketAddress("localhost", 10013);

        Peer peer1, peer2;

        int time = 0;

        final int maxCount = 15;

        static {
                try {
                        host1 = new Host(address1, 10, 100, 100);
                        host2 = new Host(address2, 10, 0, 0);
                }
                catch (Exception e) {
                        e.printStackTrace();
                        fail(e.toString());
                }
        }

        public void sendPacket(String sent, int flags, Peer peer, Host host1, Host host2) {
                int maxCount = 100;
                int count = 0;
                int time = 0;
                Charset ascii = Charset.forName("US-ASCII");
                CharsetDecoder decoder = ascii.newDecoder();
                CharsetEncoder encoder = ascii.newEncoder();

                try {
                        CharBuffer charBuffer = CharBuffer.wrap(sent);
                        ByteBuffer data = encoder.encode(charBuffer);
                        Packet packet = new Packet(data.capacity(), flags);
                        packet.fromBuffer(data);
                        String received = "";
                        peer.send((byte)0, packet);

                        while (received.equals("") && count < maxCount) {
                                count++;
                                host1.service(time);
                                Event event = host2.service(time);
                                if (event.getType() == Event.TYPE.RECEIVED) {
                                        packet = event.getPacket();
                                        packet.getData().flip();
                                        charBuffer = decoder.decode(packet.getData());
                                        received = charBuffer.toString();
                                }
                        }
                        assertTrue(count < maxCount);
                        assertEquals(received, sent);
                        LOG.info("Finished in " + count + " operations");
                }
                catch (Exception e) {
                        e.printStackTrace();
                        fail(e.toString());
                }
        }

        public void testCommunication() {
                connect();
                //Test SendFragment
                String sent = "1";
                for (int i = 0; i < 18; i++) {
                        sent += sent;
                }

                LOG.info("Sent:" + sent.length() + " bytes");
                sendPacket(sent, 0, peer1, host1, host2);
                LOG.info("Received:" + sent.length() + " bytes");

                sent = "RELIABLE ";
                for (int i = 0; i < 10; i++)
                        sent += "" + i;
                LOG.info("Sent:" + sent);
                sendPacket(sent, Packet.FLAG_RELIABLE, peer1, host1, host2);
                LOG.info("Received:" + sent);

                sent = "UNSEQUENCED ";
                for (int i = 0; i < 100; i++)
                        sent += "" + i;
                LOG.info("Sent:" + sent);
                sendPacket(sent, Packet.FLAG_UNSEQUENCED, peer1, host1, host2);
                LOG.info("Received:" + sent);

                sent = "UNRELIABLE ";
                for (int i = 0; i < 10; i++)
                        sent += "" + i;
                LOG.info("Sent:" + sent);
                sendPacket(sent, 0, peer1, host1, host2);
                LOG.info("Received:" + sent);

                assertTrue(peer1.isConnected());
                assertTrue(peer2.isConnected());
                disconnect();
        }

        public void testConnectTimeout() {
                int timeout = 35000;
                long startTime = System.currentTimeMillis();
                long endTime = startTime + timeout;
                peer1 = host1.connect(new InetSocketAddress("www.google.com", 10011), 2);
                while (System.currentTimeMillis() < endTime && !peer1.isDisconnected()) {
                        Event event = host1.service(50);
                        if (event.type != Event.TYPE.NONE) {
                                assertEquals(event.getType(), Event.TYPE.DISCONNECTED);
                        }
                }
                assertTrue(peer1.isDisconnected());
                assertEquals(host1.getPeers().size(), 0);
                LOG.info("Connect sucessfully timed out in " + (System.currentTimeMillis() - startTime) + " ms");
        }

        public void testTimeout() {
                connect();
                LOG.info("Testing timout...");
                peer1.reset();
                assertTrue(peer1.isDisconnected());
                assertEquals(host1.getPeers().size(), 0);
                int timeout = 35000;
                long startTime = System.currentTimeMillis();
                long endTime = startTime + timeout;
                while (System.currentTimeMillis() < endTime && !peer2.isDisconnected()) {
                        Event event2 = host2.service(50);
                        if (event2.type != Event.TYPE.NONE) {
                                assertEquals(event2.getType(), Event.TYPE.DISCONNECTED);
                        }
                }
                assertTrue(peer2.isDisconnected());
                assertEquals(host2.getPeers().size(), 0);
                LOG.info("Sucessfully disconnected due to timeout after " + (System.currentTimeMillis() - startTime) + " ms");
        }

        public void connect() {
                LOG.info("Connecting...");
                peer1 = host1.connect(address2, 2);
                peer2 = null;
                int count = 0;
                boolean host1Connected = false;
                boolean host2Connected = false;
                while (host2.getPeers().size() == 0 || !host2.getPeers().values().iterator().next().isConnected() && count < maxCount) {
                        Event event1 = host1.service(time);
                        if (event1.type == Event.TYPE.CONNECTED) {
                                host1Connected = true;
                                assertEquals(peer1, event1.getPeer());
                                assertTrue(peer1.isConnected());
                        }
                        else {
                                assertEquals(event1.type, Event.TYPE.NONE);
                        }
                        assertEquals(event1.getPacket(), null);

                        Event event2 = host2.service(time);
                        if (event2.type == Event.TYPE.CONNECTED) {
                                host2Connected = true;
                                peer2 = event2.getPeer();
                                assertTrue(peer2.isConnected());
                        }
                        else {
                                assertEquals(event2.type, Event.TYPE.NONE);
                        }
                        assertEquals(event2.getPacket(), null);
                        count++;
                }
                assertTrue(host1Connected);
                assertTrue(host2Connected);
                assertEquals(host1.getPeers().size(), 1);
                assertEquals(host2.getPeers().size(), 1);
                assertTrue(count < maxCount);
                LOG.info("Finished connecting in " + count + " operations");
        }

        public void disconnect() {
                peer2.disconnect();
                int count = 0;
                boolean host1Disonnected = false;
                boolean host2Disconnected = false;
                LOG.info("Disconnecting...");
                while ((host2.getPeers().size() > 0 || host1.getPeers().size() > 0) && count < maxCount) {
                        Event event1 = host1.service(time);
                        if (event1.type == Event.TYPE.DISCONNECTED)
                                host1Disonnected = true;
                        else
                                assertEquals(event1.type, Event.TYPE.NONE);
                        Event event2 = host2.service(time);
                        if (event2.type == Event.TYPE.DISCONNECTED)
                                host2Disconnected = true;
                        else
                                assertEquals(event2.type, Event.TYPE.NONE);
                        count++;
                }
                assertTrue(host1Disonnected);
                assertTrue(host2Disconnected);
                assertEquals(host1.getPeers().size(), 0);
                assertEquals(host2.getPeers().size(), 0);
                assertTrue(count < maxCount);
                LOG.info("Disconnected in " + count + " operations");

                assertTrue(peer1.isDisconnected());
                assertTrue(peer2.isDisconnected());
        }
}

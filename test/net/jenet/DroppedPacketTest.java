/*
 * Created on Jul 24, 2005
 */


package net.jenet;

import java.net.InetSocketAddress;
import java.nio.*;
import java.nio.charset.*;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.commons.logging.*;

public class DroppedPacketTest extends TestCase {

        private static final Log LOG = LogFactory.getLog(DroppedPacketTest.class);
        
        static Host host1, host2;
        Peer peer1, peer2;
        
        int time = 1;
        int maxCount = Integer.MAX_VALUE;
        
        static final int minLatency = 200;
        static final int maxLatency = 400;
        static final double dropRate = 0.1;
        
        static {
                try {
                        host1 = new MockHost(new InetSocketAddress("localhost", 10010), 10, 0, 0, minLatency, maxLatency, dropRate);
                        host2 = new MockHost(new InetSocketAddress("localhost", 10011), 10, 0, 0, minLatency, maxLatency, dropRate);
                }
                catch (Exception e) {
                        e.printStackTrace();
                        fail(e.toString());
                }
        }
        
        public void sendPacket(String sent, int flags, Peer peer, Host host1, Host host2) {
                int count = 0;
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

        public void testDroppedPackets() {
                connect();
                
                //for (int n = 0; n < 10; n++) {
                Random random = new Random();
                StringBuffer sentbuf = new StringBuffer();
                for (int i = 0; i < 100*1024; i++)
                        sentbuf.append((char)(random.nextInt(26)+'A'));
                String sent = sentbuf.toString();
                
                LOG.info("Sent:" + sent.length() + " bytes");
                sendPacket(sent, Packet.FLAG_RELIABLE, peer1, host1, host2);
                LOG.info("Received:" + sent.length() + " bytes");
                //}
                disconnect();
        }
        
        public void connect() {
                LOG.info("Connecting...");
                peer1 = host1.connect(new InetSocketAddress("localhost", 10011), 2);
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
                        else
                                assertEquals(event1.type, Event.TYPE.NONE);
                        assertEquals(event1.getPacket(), null);

                        Event event2 = host2.service(time);
                        if (event2.type == Event.TYPE.CONNECTED) {
                                host2Connected = true;
                                peer2 = event2.getPeer();
                                assertTrue(peer2.isConnected());
                        }
                        else
                                assertEquals(event2.type, Event.TYPE.NONE);
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
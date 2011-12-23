/*
 * Created on Jul 24, 2005
 */

package net.jenet;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.PriorityQueue;

import org.apache.commons.logging.*;


public class MockHost extends Host {

        int minLatency;
        int maxLatency;
        double dropRate;

        DelayThread thread = new DelayThread();

        private static Log LOG = LogFactory.getLog(MockHost.class);

        PriorityQueue<Packet> queue = new PriorityQueue<Packet>();

        public MockHost(InetSocketAddress address, int maxConnections, int incomingBandwith, int outgoingBandwith, int minLatency, int maxLatency,
                        double dropRate) throws Exception {
                super(address, maxConnections, incomingBandwith, outgoingBandwith);
                this.minLatency = minLatency;
                this.maxLatency = maxLatency;
                this.dropRate = dropRate;
                thread.start();
        }

        @Override
        public int send(Peer peer) {
                buffers.flip();
                int remaining = buffers.remaining();

                if (Math.random() < dropRate) {
                        ByteBuffer test = buffers.duplicate();
                        Header header = new Header();
                        header.fromBuffer(test);
                        for (int i = header.commandCount; i > 0; i--) {
                                Command command = Command.readCommand(test);
                                if (command instanceof SendFragment) {
                                        SendFragment frag = (SendFragment)command;
                                        LOG.info("dropped #" + frag.fragmentNumber);
                                }
                        }
                        return remaining;
                }

                int delay = (int)(Math.random() * (maxLatency - minLatency)) + minLatency;
                synchronized (queue) {
                        queue.add(new Packet(buffers, peer.getAddress(), System.currentTimeMillis() + delay));
                        queue.notify();
                }

                return remaining;
        }

        @SuppressWarnings("deprecation")
        public void destroy() {
                thread.stop();
        }

        class Packet implements Comparable {

                ByteBuffer buf;
                InetSocketAddress adddress;
                long sendTime;

                Packet(ByteBuffer buf, InetSocketAddress address, long sendTime) {
                        this.buf = ByteBuffer.allocate(buf.remaining());
                        this.buf.put(buf);
                        this.buf.flip();
                        this.adddress = address;
                        this.sendTime = sendTime;
                }

                public int compareTo(Object obj) {
                        Packet packet = (Packet)obj;
                        return (int)(sendTime - packet.sendTime);
                }
        }

        class DelayThread extends Thread {
                public void run() {
                        try {
                                while (true) {
                                        synchronized (queue) {
                                                while (queue.isEmpty() || queue.peek().sendTime > System.currentTimeMillis())
                                                        if (queue.isEmpty())
                                                                queue.wait();
                                                        else
                                                                queue.wait(queue.peek().sendTime - System.currentTimeMillis());

                                                Packet packet = queue.remove();
                                                ByteBuffer buffers = packet.buf;
                                                InetSocketAddress address = packet.adddress;
                                                ByteBuffer test = buffers.duplicate();
                                                Header header = new Header();
                                                header.fromBuffer(test);
                                                for (int i = header.commandCount; i > 0; i--) {
                                                        Command command = Command.readCommand(test);
                                                        if (command instanceof SendFragment) {
                                                                SendFragment frag = (SendFragment)command;
                                                                LOG.info("sent #" + frag.fragmentNumber);
                                                        }
                                                }
                                                try {
                                                        communicationChannel.send(buffers, address);
                                                }
                                                catch (Exception e) {
                                                        LOG.error("MockHost.send: Error writing buffers.", e);
                                                }
                                        }
                                }
                        }
                        catch (Exception err) {
                                err.printStackTrace();
                        }
                }
        }
}
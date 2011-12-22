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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Holds information about JeNet events.
 * Events are returned by the {@link net.jenet.Host#service(int) Host.service} method
 * to indicate that something happened.
 * @author Dizan Vasquez
 */
public class Event {

        public enum TYPE {
                /**
                 * A peer has connected to this host.
                 */
                CONNECTED,
                
                /**
                 * A peer has disconnected from this host.
                 */
                DISCONNECTED,
                
                /**
                 * A packet has been received.
                 */
                RECEIVED,
                
                /**
                 * Nothing happened.
                 */
                NONE, 
                
                /**
                 * An communication error ocurred.
                 */
                ERROR
        }

        protected int channelID;

        protected Packet packet;

        protected Peer peer;

        protected TYPE type;

        Event() {
                type = TYPE.NONE;
        }

        /**
         * Returns the ID of the channel related to this event.
         * @return channelID.
         */
        public int getChannelID() {
                return channelID;
        }

        /**
         * Returns the packet related to this event.
         * This is different from null only for <code>RECEIVED</code>
         * events.
         * @return Returns the packet.
         */
        public Packet getPacket() {
                return packet;
        }

        /**
         * Returns the peer which originated this event.
         * @return Returns the peer.
         */
        public Peer getPeer() {
                return peer;
        }

        /**
         * Return the event's type
         * @return Returns the type.
         */
        public TYPE getType() {
                return type;
        }

        /**
         * @param channelID
         *            The channelID to set.
         */
        void setChannelID( int channelID ) {
                this.channelID = channelID;
        }

        /**
         * @param packet
         *            The packet to set.
         */
        void setPacket( Packet packet ) {
                this.packet = packet;
        }

        /**
         * @param peer
         *            The peer to set.
         */
        void setPeer( Peer peer ) {
                this.peer = peer;
        }

        /**
         * @param type
         *            The type to set.
         */
        void setType( TYPE type ) {
                this.type = type;
        }

        public String toString() {
                return ToStringBuilder.reflectionToString( this, ToStringStyle.MULTI_LINE_STYLE );
        }
}
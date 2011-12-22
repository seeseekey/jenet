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

import java.util.List;

/**
 * @author Dizan Vasquez
 */
class OutgoingCommand implements IByteSize {

        protected List outgoingCommandList;

        protected int reliableSequenceNumber;

        protected int unreliableSequenceNumber;

        protected int sentTime;

        protected int roundTripTimeout;

        protected int roundTripTimeoutLimit;

        protected int fragmentOffset;

        protected short fragmentLength;

        protected Command command;

        protected Packet packet;

        /**
         * @return Returns the command.
         */
        public Command getCommand() {
                return command;
        }

        /**
         * @param command
         *            The command to set.
         */
        public void setCommand( Command command ) {
                this.command = command;
        }

        /**
         * @return Returns the fragmentLength.
         */
        public short getFragmentLength() {
                return fragmentLength;
        }

        /**
         * @param fragmentLength
         *            The fragmentLength to set.
         */
        public void setFragmentLength( short fragmentLength ) {
                this.fragmentLength = fragmentLength;
        }

        /**
         * @return Returns the fragmentOffset.
         */
        public int getFragmentOffset() {
                return fragmentOffset;
        }

        /**
         * @param fragmentOffset
         *            The fragmentOffset to set.
         */
        public void setFragmentOffset( int fragmentOffset ) {
                this.fragmentOffset = fragmentOffset;
        }

        /**
         * @return Returns the outgoingCommandList.
         */
        public List getOutgoingCommandList() {
                return outgoingCommandList;
        }

        /**
         * @param outgoingCommandList
         *            The outgoingCommandList to set.
         */
        public void setOutgoingCommandList( List outgoingCommandList ) {
                this.outgoingCommandList = outgoingCommandList;
        }

        /**
         * @return Returns the packet.
         */
        public Packet getPacket() {
                return packet;
        }

        /**
         * @param packet
         *            The packet to set.
         */
        public void setPacket( Packet packet ) {
                this.packet = packet;
        }

        /**
         * @return Returns the reliableSequenceNumber.
         */
        public int getReliableSequenceNumber() {
                return reliableSequenceNumber;
        }

        /**
         * @param reliableSequenceNumber
         *            The reliableSequenceNumber to set.
         */
        public void setReliableSequenceNumber( int reliableSequenceNumber ) {
                this.reliableSequenceNumber = reliableSequenceNumber;
        }

        /**
         * @return Returns the roundTripTimeout.
         */
        public int getRoundTripTimeout() {
                return roundTripTimeout;
        }

        /**
         * @param roundTripTimeout
         *            The roundTripTimeout to set.
         */
        public void setRoundTripTimeout( int roundTripTimeout ) {
                this.roundTripTimeout = roundTripTimeout;
        }

        /**
         * @return Returns the roundTripTimeoutLimit.
         */
        public int getRoundTripTimeoutLimit() {
                return roundTripTimeoutLimit;
        }

        /**
         * @param roundTripTimeoutLimit
         *            The roundTripTimeoutLimit to set.
         */
        public void setRoundTripTimeoutLimit( int roundTripTimeoutLimit ) {
                this.roundTripTimeoutLimit = roundTripTimeoutLimit;
        }

        /**
         * @return Returns the sentTime.
         */
        public int getSentTime() {
                return sentTime;
        }

        /**
         * @param sentTime
         *            The sentTime to set.
         */
        public void setSentTime( int sentTime ) {
                this.sentTime = sentTime;
        }

        /**
         * @return Returns the unreliableSequenceNumber.
         */
        public int getUnreliableSequenceNumber() {
                return unreliableSequenceNumber;
        }

        /**
         * @param unreliableSequenceNumber
         *            The unreliableSequenceNumber to set.
         */
        public void setUnreliableSequenceNumber( int unreliableSequenceNumber ) {
                this.unreliableSequenceNumber = unreliableSequenceNumber;
        }

        /**
         * @return
         */
        public int byteSize() {
                if ( packet == null )
                        return command.byteSize();
                else
                        return fragmentLength + command.byteSize();
        }

}
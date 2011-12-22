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

/**
 * @author Dizan Vasquez
 */
class Acknowledge extends Command {

        protected int receivedReliableSequenceNumber;

        protected int receivedSentTime;

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Command#toBuffer(java.nio.ByteBuffer)
         */
        @Override
        public void toBuffer( ByteBuffer buffer ) {
                super.toBuffer( buffer );
                buffer.putInt( receivedReliableSequenceNumber );
                buffer.putInt( receivedSentTime );
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Command#fromBuffer(java.nio.ByteBuffer)
         */
        @Override
        public void fromBuffer( ByteBuffer buffer ) {
                super.fromBuffer( buffer );
                receivedReliableSequenceNumber = buffer.getInt();
                receivedSentTime = buffer.getInt();
        }

        /**
         * @return Returns the receivedReliableSequenceNumber.
         */
        public int getReceivedReliableSequenceNumber() {
                return receivedReliableSequenceNumber;
        }

        /**
         * @param receivedReliableSequenceNumber
         *            The receivedReliableSequenceNumber to set.
         */
        public void setReceivedReliableSequenceNumber( int receivedReliableSequenceNumber ) {
                this.receivedReliableSequenceNumber = receivedReliableSequenceNumber;
        }

        /**
         * @return Returns the receivedSentTime.
         */
        public int getReceivedSentTime() {
                return receivedSentTime;
        }

        /**
         * @param receivedSentTime
         *            The receivedSentTime to set.
         */
        public void setReceivedSentTime( int receivedSentTime ) {
                this.receivedSentTime = receivedSentTime;
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

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.protocol.Command#execute(org.Dizan
         *      Vasquez.jnet.Peer, net.jenet.protocol.Header)
         */
        @Override
        public Event execute( Host host, Peer peer, Header header ) {
                Event result = new Event();
                int timeCurrent = host.getTimeCurrent();
                int roundTripTime;

                if ( Time.less( timeCurrent, receivedSentTime ) )
                        return result;

                peer.setLastReceiveTime( timeCurrent );

                roundTripTime = Time.difference( timeCurrent, receivedSentTime );

                peer.throttle( roundTripTime );

                peer.updateRoundTripTimeVariance( roundTripTime );

                Command command = peer.removeSentReliableCommand( receivedReliableSequenceNumber,
                                this.header.channelID );

                switch ( peer.getState() ) {
                case ACKNOWLEDGING_CONNECT:
                        if ( !( command instanceof VerifyConnect ) )
                                return result;
                        host.setRecalculateBandwithLimits( true );
                        peer.setState( Peer.STATE.CONNECTED );
                        result.setType( Event.TYPE.CONNECTED );
                        result.setPeer( peer );
                        return result;

                case DISCONNECTING:
                        if ( !( command instanceof Disconnect ) )
                                return result;
                        host.setRecalculateBandwithLimits( true );
                        peer.reset();
                        result.setType( Event.TYPE.DISCONNECTED );
                        result.setPeer( peer );
                        return result;

                default:
                        break;
                }

                return result;
        }
}
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
class SendFragment extends Command {
        
        public static final int BYTE_SIZE = Command.BYTE_SIZE + 20;

        protected int fragmentCount;

        protected int fragmentNumber;

        protected int fragmentOffset;

        protected int startSequenceNumber;

        protected int totalLength;

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.protocol.Command#execute(org.Dizan
         *      Vasquez.jnet.Peer, net.jenet.protocol.Header)
         */
        @Override
        public Event execute( Host host, Peer peer, Header header ) {
                Event result = new Event();
                int fragmentLength = getHeader().commandLength - byteSize();
                Channel channel = peer.selectChannel( getHeader().getChannelID() );

                if ( channel == null || !peer.isConnected() )
                        return result;

                if ( fragmentOffset >= totalLength || fragmentOffset + fragmentLength > totalLength
                                || fragmentNumber >= fragmentCount )
                        return result;

                if ( startSequenceNumber <= channel.getIncomingReliableSequenceNumber() )
                        return result;

                IncomingCommand startCommand = null;

                for ( IncomingCommand command : channel.getIncomingReliableCommands() )
                        if ( command.getCommand() instanceof SendFragment
                                        && ( (SendFragment) command.getCommand() ).getStartSequenceNumber() == startSequenceNumber ) {
                                startCommand = command;
                                break;
                        }

                if ( startCommand == null ) {
                        SendFragment hostCommand = new SendFragment();
                        hostCommand.header.reliableSequenceNumber = startSequenceNumber;
                        hostCommand.startSequenceNumber = startSequenceNumber;
                        hostCommand.fragmentNumber = fragmentNumber;
                        hostCommand.fragmentCount = fragmentCount;
                        hostCommand.fragmentOffset = fragmentOffset;
                        hostCommand.totalLength = totalLength;

                        startCommand = peer.queueIncomingCommand( hostCommand, new Packet( totalLength, Packet.FLAG_RELIABLE ),
                                        fragmentCount );
                        
                } else if ( totalLength != startCommand.getPacket().getDataLength()
                                || fragmentCount != startCommand.getFragmentCount() )
                        return result;
                
                int fragment = startCommand.getFragments()[fragmentNumber / 32];
                if ( ( fragment & ( 1 << fragmentNumber % 32 ) ) == 0 ) {
                        startCommand.setFragmentsRemaining( startCommand.getFragmentsRemaining() - 1 );
                        startCommand.getFragments()[fragmentNumber / 32] |= ( 1 << ( fragmentNumber % 32 ) );

                        if ( fragmentOffset + fragmentLength > startCommand.getPacket().getDataLength() )
                                fragmentLength = startCommand.getPacket().getDataLength() - fragmentOffset;

                        startCommand.getPacket().fromBuffer( host.getReceivedData(), fragmentOffset, fragmentLength );
                }

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
                startSequenceNumber = buffer.getInt();
                fragmentCount = buffer.getInt();
                fragmentNumber = buffer.getInt();
                totalLength = buffer.getInt();
                fragmentOffset = buffer.getInt();
        }

        /**
         * @return Returns the fragmentCount.
         */
        public int getFragmentCount() {
                return fragmentCount;
        }

        /**
         * @return Returns the fragmentNumber.
         */
        public int getFragmentNumber() {
                return fragmentNumber;
        }

        /**
         * @return Returns the fragmentOffset.
         */
        public int getFragmentOffset() {
                return fragmentOffset;
        }

        /**
         * @return Returns the startSequenceNumber.
         */
        public int getStartSequenceNumber() {
                return startSequenceNumber;
        }

        /**
         * @return Returns the totalLength.
         */
        public int getTotalLength() {
                return totalLength;
        }

        /**
         * @param fragmentCount
         *            The fragmentCount to set.
         */
        public void setFragmentCount( int fragmentCount ) {
                this.fragmentCount = fragmentCount;
        }

        /**
         * @param fragmentNumber
         *            The fragmentNumber to set.
         */
        public void setFragmentNumber( int fragmentNumber ) {
                this.fragmentNumber = fragmentNumber;
        }

        /**
         * @param fragmentOffset
         *            The fragmentOffset to set.
         */
        public void setFragmentOffset( int fragmentOffset ) {
                this.fragmentOffset = fragmentOffset;
        }

        /**
         * @param startSequenceNumber
         *            The startSequenceNumber to set.
         */
        public void setStartSequenceNumber( int startSequenceNumber ) {
                this.startSequenceNumber = startSequenceNumber;
        }

        /**
         * @param totalLength
         *            The totalLength to set.
         */
        public void setTotalLength( int totalLength ) {
                this.totalLength = totalLength;
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Command#toBuffer(java.nio.ByteBuffer)
         */
        @Override
        public void toBuffer( ByteBuffer buffer ) {
                super.toBuffer( buffer );
                buffer.putInt( startSequenceNumber );
                buffer.putInt( fragmentCount );
                buffer.putInt( fragmentNumber );
                buffer.putInt( totalLength );
                buffer.putInt( fragmentOffset );
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.jenet.Command#byteSize()
         */
        @Override
        public int byteSize() {
                return BYTE_SIZE;
                
        }

}
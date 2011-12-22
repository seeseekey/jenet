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

/**
 * @author Dizan Vasquez
 */
class IncomingCommand {

        protected int reliableSequenceNumber;

        protected int unreliableSequenceNumber;

        protected Command command;

        protected int fragmentCount;

        protected int fragmentsRemaining;

        protected int[] fragments;

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
         * @return Returns the fragmentCount.
         */
        public int getFragmentCount() {
                return fragmentCount;
        }

        /**
         * @param fragmentCount
         *            The fragmentCount to set.
         */
        public void setFragmentCount( int fragmentCount ) {
                this.fragmentCount = fragmentCount;
        }

        /**
         * @return Returns the fragments.
         */
        public int[] getFragments() {
                return fragments;
        }

        /**
         * @param fragments
         *            The fragments to set.
         */
        public void setFragments( int[] fragments ) {
                this.fragments = fragments;
        }

        /**
         * @return Returns the fragmentsRemaining.
         */
        public int getFragmentsRemaining() {
                return fragmentsRemaining;
        }

        /**
         * @param fragmentsRemaining
         *            The fragmentsRemaining to set.
         */
        public void setFragmentsRemaining( int fragmentsRemaining ) {
                this.fragmentsRemaining = fragmentsRemaining;
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

}
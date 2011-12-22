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

import java.util.LinkedList;

/**
 * @author Dizan Vasquez
 * 
 */
class Channel {

        protected int outgoingReliableSequenceNumber;

        protected int outgoingUnreliableSequenceNumber;

        protected int incomingReliableSequenceNumber;

        protected int incomingUnreliableSequenceNumber;

        protected LinkedList<IncomingCommand> incomingReliableCommands = new LinkedList<IncomingCommand>();

        protected LinkedList<IncomingCommand> incomingUnreliableCommands = new LinkedList<IncomingCommand>();

        /**
         * @return Returns the incomingReliableCommands.
         */
        public LinkedList<IncomingCommand> getIncomingReliableCommands() {
                return incomingReliableCommands;
        }

        /**
         * @param incomingReliableCommands
         *            The incomingReliableCommands to set.
         */
        public void setIncomingReliableCommands( LinkedList<IncomingCommand> incomingReliableCommands ) {
                this.incomingReliableCommands = incomingReliableCommands;
        }

        /**
         * @return Returns the incomingReliableSequenceNumber.
         */
        public int getIncomingReliableSequenceNumber() {
                return incomingReliableSequenceNumber;
        }

        /**
         * @param incomingReliableSequenceNumber
         *            The incomingReliableSequenceNumber to set.
         */
        public void setIncomingReliableSequenceNumber( int incomingReliableSequenceNumber ) {
                this.incomingReliableSequenceNumber = incomingReliableSequenceNumber;
        }

        /**
         * @return Returns the incomingUnreliableCommands.
         */
        public LinkedList<IncomingCommand> getIncomingUnreliableCommands() {
                return incomingUnreliableCommands;
        }

        /**
         * @param incomingUnreliableCommands
         *            The incomingUnreliableCommands to set.
         */
        public void setIncomingUnreliableCommands( LinkedList<IncomingCommand> incomingUnreliableCommands ) {
                this.incomingUnreliableCommands = incomingUnreliableCommands;
        }

        /**
         * @return Returns the incomingUnreliableSequenceNumber.
         */
        public int getIncomingUnreliableSequenceNumber() {
                return incomingUnreliableSequenceNumber;
        }

        /**
         * @param incomingUnreliableSequenceNumber
         *            The incomingUnreliableSequenceNumber to set.
         */
        public void setIncomingUnreliableSequenceNumber( int incomingUnreliableSequenceNumber ) {
                this.incomingUnreliableSequenceNumber = incomingUnreliableSequenceNumber;
        }

        /**
         * @return Returns the outgoingReliableSequenceNumber.
         */
        public int getOutgoingReliableSequenceNumber() {
                return outgoingReliableSequenceNumber;
        }

        /**
         * @param outgoingReliableSequenceNumber
         *            The outgoingReliableSequenceNumber to set.
         */
        public void setOutgoingReliableSequenceNumber( int outgoingReliableSequenceNumber ) {
                this.outgoingReliableSequenceNumber = outgoingReliableSequenceNumber;
        }

        /**
         * @return Returns the outgoingUnreliableSequenceNumber.
         */
        public int getOutgoingUnreliableSequenceNumber() {
                return outgoingUnreliableSequenceNumber;
        }

        /**
         * @param outgoingUnreliableSequenceNumber
         *            The outgoingUnreliableSequenceNumber to set.
         */
        public void setOutgoingUnreliableSequenceNumber( int outgoingUnreliableSequenceNumber ) {
                this.outgoingUnreliableSequenceNumber = outgoingUnreliableSequenceNumber;
        }

}
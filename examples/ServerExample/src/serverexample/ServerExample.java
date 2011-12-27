/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package serverexample;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import net.jenet.Event;
import net.jenet.Host;
import net.jenet.Packet;
import org.apache.commons.configuration.ConfigurationException;

/**
 *
 * @author bottke
 */
public class ServerExample {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, ConfigurationException {
        System.out.println("Server example with jeNet");
        String SHUTDOWN_MSG = "SHUTDOWN";
        
        InetSocketAddress adress=new InetSocketAddress("localhost", 9601);
        Host host=new Host(adress, 10, 0, 0);
        
        int connectCount=0;
        boolean run=true;
        boolean shutdownRecieved=false;
        
        while(run)
        {
            //Wait one second for an event
            Event event=host.service(1000);
            
            if(event.type==Event.TYPE.CONNECTED)
            {
                System.out.println(event.getPeer().getAddress() + " CONNECT");
                connectCount++;
            }
            else if(event.type==Event.TYPE.DISCONNECTED)
            {
                System.out.println(event.getPeer().getAddress() + " DISCONNECT");
                connectCount--;
            }
            else if(event.type==Event.TYPE.RECEIVED)
            {
                ByteBuffer msg=event.getPacket().getData();
                System.out.println(event.getPeer().getAddress() + " IN " + msg);
                
                //Packet packet=new Packet(connectCount, connectCount)
                Packet packet = new Packet(msg.capacity(), 0);
                packet.fromBuffer(msg);
                
                //send echo packet
                event.getPeer().send((byte)0, packet);                
            }
        }
    }
}

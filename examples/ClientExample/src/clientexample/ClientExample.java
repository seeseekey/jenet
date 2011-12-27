/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package clientexample;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import net.jenet.Event;
import net.jenet.Host;
import net.jenet.Packet;
import net.jenet.Peer;
import org.apache.commons.configuration.ConfigurationException;

/**
 *
 * @author bottke
 */
public class ClientExample {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, ConfigurationException {
        System.out.println("Client example with jeNet");
        String SHUTDOWN_MSG = "SHUTDOWN";
        int MSG_NUMBER=10;
        
        InetSocketAddress adress=new InetSocketAddress("localhost", 9601);
        
        Host host=new Host(null, 1, 0, 0);
        Peer peer=host.connect(adress, 1);
        
        int counter=0;
        boolean run=true;
        
        while(run)
        {
            Event event=host.service(1000);
            
            if(event.type==Event.TYPE.CONNECTED)
            {
                System.out.println(event.getPeer().getAddress() + " CONNECT");
            }
            else if(event.type==Event.TYPE.DISCONNECTED)
            {
                System.out.println(event.getPeer().getAddress() + " DISCONNECT");
                run=false;
            }
            else if(event.type==Event.TYPE.RECEIVED)
            {
                ByteBuffer msg=event.getPacket().getData();
                System.out.println(event.getPeer().getAddress() + " IN " + msg);
                continue;               
            }
            
            int msgSize=40;
            ByteBuffer msg=ByteBuffer.allocate(msgSize);
            
            for(int i=0;i<msgSize;i++)
            {
                byte randomNumber=(byte)(Math.random()*254);
                msg.put(randomNumber);
            }
            
            Packet packet = new Packet(msg.capacity(), 0);
            packet.fromBuffer(msg);
            
            peer.send((byte)0, packet);
            
            counter++;
            
            System.out.println(event.getPeer().getAddress() + " OUT " + msg);
            
            if(counter>=MSG_NUMBER)
            {
                ByteBuffer msgShutDown=ByteBuffer.allocate(SHUTDOWN_MSG.length());
                msgShutDown.put(msgShutDown);
                
                packet = new Packet(msgShutDown.capacity(), 0);
                packet.fromBuffer(msgShutDown);
            
                peer.send((byte)0, packet);
                
                host.service(0);
                peer.disconnect();
            }     
        }
    }
}

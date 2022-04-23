package registry;

import java.io.*;
import java.net.*;

//Collaborate with other peers
public class CollaboratePeer implements Runnable {

    //Default constructor
    public CollaboratePeer() {

    }

    @Override
    public void run() {
        //While stop has not been received
        while (!Peer.STOP) {
            try {
                //Send peer info to other peers at regular intervals
                sendPeerInfo();
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendPeerInfo() {
        //Get random peer from unique peer list
        System.out.println("Sending peer info");
        int pos = (int) (Math.random() * Peer.uniquePeerList.size());
        for (Peer p : Peer.uniquePeerList) {
            try {
                //Create peer message
                InetAddress ip;
                String peerString = "peer" + Peer.uniquePeerList.get(pos).getAddress() + ":" + Peer.uniquePeerList.get(pos).getPort();

                if(HelperFunctions.getIP().equals(p.getAddress())) {
                    ip = InetAddress.getByName("localhost");
                }
                else {
                    ip = InetAddress.getByName(p.getAddress());
                }
                byte[] buf = peerString.getBytes(); 

                //Create datagram packet and send it to other peers
                DatagramPacket pack = new DatagramPacket(buf, buf.length, ip, p.getPort());
                Peer.udp_socket.send(pack);
                //Add to sent peer list
                Peer.addToSentPeerList(p);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
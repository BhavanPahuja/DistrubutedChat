package registry;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

//Gets message from user and sends it to other peers
public class HandleSnip implements Runnable {

    //Default constructor
    public HandleSnip() {

    }

    @Override
    public void run() {
        Scanner kb = new Scanner(System.in);
        //While system is still running
        while (!Peer.STOP) {
            //Get input from user
            String input = kb.nextLine().trim();

            if (input.equalsIgnoreCase("snip")) {
                System.out.println("Enter a snip: ");
                String content = kb.nextLine().trim();

                //Increase timestamp to match lamports happen before
                Peer.snippetTimestampInc();

                //Create snip message
                String message = "snip" + Peer.getSnippetTimestamp() + " " + content;
                System.out.println(message);

                byte[] buf = message.getBytes();

                //Send to all known peers
                for (Peer p : Peer.uniquePeerList) {
                    try {
                        InetAddress ip;
                        if (HelperFunctions.getIP().equals(p.getAddress())) {
                            ip = InetAddress.getByName("localhost");
                        } else {
                            ip = InetAddress.getByName(p.getAddress());
                        }
                        //Create datagram packet and send
                        DatagramPacket pack = new DatagramPacket(buf, buf.length, ip, p.getPort());
                        Peer.udp_socket.send(pack);

                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

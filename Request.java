package registry;

import java.io.*;
import java.net.*;

//Create and handle requests
public class Request implements Runnable {

    private String type;
    private String text;
    private String address;
    private int port;

    //Constructor
    public Request(String type, String address, int port) {
        this.type = type;
        this.text = "";
        this.address = address;
        this.port = port;
    }

    //Constructor
    public Request(String type, String text, String address, int port) {
        this.type = type;
        this.text = text;
        this.address = address;
        this.port = port;
    }

    @Override
    public void run() {
        //Check the type of request and call the corresponding method
        switch (this.type) {
            case "snip":
                Request.HandleSnip(this.text, this.address, this.port);
                break;
            case "peer":
                Request.HandlePeer(this.text, this.address, this.port);
                break;
            case "stop":
                System.out.println("Request is a stop");
                break;
        }
    }

    //Handle snippet messages
    public static void HandleSnip(String text, String address, int port) {
        String[] snip = text.split(" ");

        int timestamp = Peer.getSnippetTimestamp();

        //If timestamp of snip is greater than current, replace
        if (Integer.parseInt(snip[0]) > timestamp) {
            Peer.setSnippetTimestamp(Integer.parseInt(snip[0]));
        }

        Peer np = new Peer(address, port, address + ":" + port, HelperFunctions.getFormattedDate());
        Peer.addToPeers(np);

        //Create a new snippet and add it to list of snips.
        Snippet newSnip = new Snippet(text.substring(snip[0].length()).trim(), address, port,
                Integer.parseInt(snip[0]));
        System.out.println(newSnip.toString());
        Peer.addToSnips(newSnip);
    }

    //Handle stop request
    public static void HandleStop(String address, int port) {
        //Create ack string and set peer ack status to true
        String ackstring = "ack" + Peer.teamName;
        Peer.acked = true;

        //Create and send datagram packet for ack stinrg
        try {
            DatagramPacket pack = new DatagramPacket(ackstring.getBytes(), ackstring.getBytes().length, InetAddress.getByName(address), port);
            Peer.udp_socket.send(pack);
        } catch (UnknownHostException uh) {
            uh.printStackTrace();
        } catch (IOException io) {
            io.printStackTrace();
        }
        //Stop the program
        Peer.STOP = true;
    }

    //Handle peer requests
    public static void HandlePeer(String text, String address, int port) {
        try {
            String[] newPeer = text.trim().split(":");
            // Create new peer and add to peer list
            Peer np = new Peer(newPeer[0], Integer.parseInt(newPeer[1]), address + ":" + port,
                    HelperFunctions.getFormattedDate());

            Peer.addToPeers(np);
        } catch (ArrayIndexOutOfBoundsException ai) {
            ai.printStackTrace();
        }
    }
}
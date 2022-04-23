package registry;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Peer {

    //Variables
    String address;
    int port;
    static String teamName;
    String source_address;
    String timestamp;

    static boolean acked = false;
    static Peer[] peersSent;

    //static final String host = "136.159.5.27";
    static final String host = "localhost";
    static final int host_port = 55921;

    static String initial_timestamp;

    //ArrayLists to keep track of received and sent peers
    static ArrayList<Peer> initialPeerList = new ArrayList<Peer>();
    static CopyOnWriteArrayList<Peer> uniquePeerList = new CopyOnWriteArrayList<Peer>();
    static CopyOnWriteArrayList<Peer> receivedPeerList = new CopyOnWriteArrayList<Peer>();
    static CopyOnWriteArrayList<Peer> sentPeerList = new CopyOnWriteArrayList<Peer>();

    static int snippet_timestamp = 0;
    //Array List for snips
    static CopyOnWriteArrayList<Snippet> snips = new CopyOnWriteArrayList<Snippet>();


    //Multithreading variables
    public static final int THREAD_POOL_SIZE = 10;
    public static ExecutorService executor;

    //UDP related variables
    static int UDP_PORT;
    public static DatagramSocket udp_socket;
    public static boolean STOP;

    //Default constructor
    public Peer() {
    }

    //Constructor
    public Peer(String addr, int port, String source, String time) {
        this.address = addr;
        this.setPort(port);
        this.source_address = source;
        this.timestamp = time;
    }

    //Start method that takes upd port and name as arguments
    public static void start(int udp_port, String name) throws SocketException {

        teamName = name;
        UDP_PORT = udp_port;
        executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try {
            //Create new udp socket
            Peer.udp_socket = new DatagramSocket(UDP_PORT);

            //Start a TCP connection to handle initial communication with registry.
            HandleTCP begin = new HandleTCP(host, host_port, teamName);
            begin.start();

            //Start a thread for handling incoming udp packets
            executor.execute(new HandleUDP());
            //Start a thread for handling snippets/messages from user
            executor.execute(new HandleSnip());
            //Start a thread for collaborating with other peers
            executor.execute(new CollaboratePeer());

            //Wait until the threads finish
            while (!STOP) {
                System.out.print("");
            }

            //Shutdown
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //Start a TCP handler for handling reporting to the registry
            HandleTCP reportGenerator = new HandleTCP(host, host_port, teamName);
            reportGenerator.start();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Get the ip address of peer
    public String getAddress() {
        return this.address;
    }

    //Get the ip address of peer source
    public String getSourceAddress() {
        return this.source_address;
    }

    //Get the timestamp of peer
    public String getTimestamp() {
        return this.timestamp;
    }

    //Get the teamname of peer
    String key() {
        return teamName;
    }

    //Convert to string
    public String toString() {
        return key() + " " + address + ":" + getPort();
    }

    //Get the port of peer
    int getPort() {
        return this.port;
    }

    //Set the ip address of peer
    void setPort(int port) {
        if (port > 0) {
            this.port = port;
        } else {
            port = 55555;
        }
    }

    //Get the udp port of peer
    public static int getUdpPort() {
        return Peer.UDP_PORT;
    }

    //Add to list of peers
    public static void addToPeers(Peer peer) {
        //Add to received peer list
        receivedPeerList.add(peer);
        //If already in unique peer list, replace
        if (checkIfInPeers(peer)) {
            replacePeer(peer);
        //Otherwise add to list of unique peers
        } else {
            uniquePeerList.add(peer);
        }
    }

    //Checks if already in unique peer list
    private static boolean checkIfInPeers(Peer p) {
        for (Peer q : uniquePeerList) {
            if (q.getAddress().equals(p.getAddress()) && q.getPort() == p.getPort()) {
                return true;
            }
        }
        return false;
    }

    //Replace peer
    public static void replacePeer(Peer p) {
        for (Peer q : uniquePeerList) {
            if (q.getAddress().equals(p.getAddress()) && q.getPort() == p.getPort()) {
                uniquePeerList.remove(q);
                uniquePeerList.add(p);
                break;
            }
        }
    }

    //Add to sent peer list
    public static void addToSentPeerList(Peer p) {
    Peer.sentPeerList.add(p);
    Peer.peersSent = Peer.sentPeerList.toArray(new Peer[Peer.sentPeerList.size()]);
    }

    //Add snippet to list of snippets
    public static void addToSnips(Snippet s) {
        snips.add(s);
    }

    //Get snippet timestamp
    public static int getSnippetTimestamp() {
        return Peer.snippet_timestamp;
    }

    public static void setSnippetTimestamp(int times) {
        Peer.snippet_timestamp = times;
    }

    //Increment snippet timestamp by 1
    public static void snippetTimestampInc() {
        snippet_timestamp++;
    }
}

package registry;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import registry.Peer;

//Handles TCP connection with registry
public class HandleTCP {

    public static String server_address;
    public static int server_port;
    public static String teamName;

    //Default constructor
    public HandleTCP() {
        HandleTCP.server_address = "localhost";
        HandleTCP.server_port = 55921;
        HandleTCP.teamName = "BP";
    }

    //Constructor
    public HandleTCP(String address, int port, String teamName) {
        HandleTCP.server_address = address;
        HandleTCP.server_port = port;
        HandleTCP.teamName = teamName;
    }

    public void start() {
        try {
            //Establish TCP Connection
            Socket sock = new Socket(server_address, server_port);

            //Create reader and writer for communication
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            String response = "";
            String request = in.readLine();

            //While the request is not a close request
            while (!request.equals("close")) {
                System.out.println(request);
                if (request.equals("get team name")) {
                    response = sendTeamName();
                    out.write(response);
                    out.flush();
                } else if (request.equals("get location")) {
                    response = sendLocation();
                    out.write(response);
                    out.flush();
                } else if (request.equals("get code")) {
                    response = sendCode();
                    out.write(response);
                    out.flush();
                } else if (request.equals("get report")) {
                    System.out.println("Sending report");
                    response = sendReport();
                    System.out.println(response);
                    out.write(response);
                    out.flush();

                //Otherwise peers are being sent by the registry
                } else{
                    int numOfPeers = Integer.parseInt(in.readLine().trim());
                    Peer.initial_timestamp = HelperFunctions.getFormattedDate();
                    //Parse the peers and add to peer list
                    for (int i = 0; i < numOfPeers; i++) {
                        String receivedPeer = in.readLine();
                        System.out.println("Received peer is: " + receivedPeer);
                        String[] peerInfo = receivedPeer.trim().split(":");
                        Peer peer = new Peer(peerInfo[0], Integer.parseInt(peerInfo[1]), server_address + ":" + server_port, HelperFunctions.getFormattedDate());
                        Peer.initialPeerList.add(peer);
                        Peer.addToPeers(peer);
                    }
                }
                request = in.readLine();
            }
            //Cleanup
            out.close();
            in.close();
            sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Create teamName string
    public static String sendTeamName() {
        return teamName + "\n";
    }

    //Create location string
    public static String sendLocation() {
        try {
            return InetAddress.getLocalHost().getHostAddress() + ":" + Peer.UDP_PORT + "\n";
        } catch(UnknownHostException e) {
            e.printStackTrace();
            return "";
        }
    }

    /*Create location string
    public static String sendLocation() {    
        return HelperFunction.getIP() + ":" + Peer.UDP_PORT + "\n";
    }*/

    //Create code string
    public static String sendCode() {
        System.out.println("Sending code");
        return "Java\n"
                + printAllFiles()
                + "\n...\n";
    }

    //Converts java files to string
    public static String printAllFiles() {
        try {
            //Get all files in current directory
            String[] files = listFiles();
            String content = "";
            for (String file : files) {
                File f = new File(file);
                if(f.isDirectory()) {
                    content += content + "";
                }
                //Read each code lien
                else {
                    Scanner myReader = new Scanner(f);
                    if (f.getName().endsWith(".java")) {
                        while(myReader.hasNextLine()) {
                            String codeLine = myReader.nextLine();
                            content += codeLine + "\n";
                        }
                    }
                }
            }
            return content;
        } catch(Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    //List all files in a directory
    private static String[] listFiles() {
        String[] files;
        String currentDir = System.getProperty("user.dir");
        File d = new File(currentDir);
        files = d.list();
        return files;
    }

    //Create send report string
    public static String sendReport() {
        int numOfPeers = Peer.uniquePeerList.size();
        if (numOfPeers == 0)
            return "0\n0\n";
        return numOfPeers
                + "\n"
                + getUniquePeerList()
                + "1\n"
                + Peer.host
                + ":"
                + Peer.host_port
                + "\n"
                + Peer.initial_timestamp
                + "\n"
                + Peer.initialPeerList.size()
                + "\n"
                + getInitialPeerList()
                + Peer.receivedPeerList.size()
                + "\n"
                + getReceievedPeerList()
                + Peer.sentPeerList.size()
                + "\n"
                + getSentPeerList()
                + Peer.snips.size()
                + "\n"
                + getSnips();
    }

    //Get unique peer list as a string
    public static String getUniquePeerList() {
        String response = "";
        for (Peer p : Peer.uniquePeerList) {
            response += p.getAddress() + ":" + p.getPort() + "\n";
        }
        if (response.equals(""))
            return "\n";
        else
            return response;
    }

    //Get initial peer list as a string
    public static String getInitialPeerList() {
        String response = "";
        for (Peer p : Peer.initialPeerList) {
            response += p.getAddress() + ":" + p.getPort() + "\n";
        }
        if (response.equals(""))
            return "\n";
        else
            return response;
    }

    //Get received peer list as a string
    public static String getReceievedPeerList() {
        String response = "";
        for (Peer p : Peer.receivedPeerList) {
            response += p.getAddress() + ":" + p.getPort() + "\n";
        }
        if (response.equals(""))
            return "\n";
        else
            return response;
    }

    //Get sent peer list as a string
    public static String getSentPeerList() {
        String response = "";
        for (Peer p : Peer.sentPeerList) {
            response += p.getAddress() + ":" + p.getPort() + "\n";
        }
        return response;
    }

    //Get snippet list as a string
    public static String getSnips() {
        String response = "";
        for (int i = 0; i < Peer.snips.size(); i++) {
            if (!Peer.snips.get(i).getSourceAddress().equals("127.0.0.1")) {
                response += Peer.snips.get(i).toString() + "\n";
            } else {
                response += Peer.snips.get(i).getTimestamp()
                        + " "
                        + Peer.snips.get(i).getContent()
                        + " "
                        + HelperFunctions.getIP()
                        + ":"
                        + Peer.UDP_PORT
                        + "\n";
            }
        }
        if (response.equals(""))
            return "\n";
        else
            return response;
    }
}

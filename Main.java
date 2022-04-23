package registry;

import java.net.*;

public class Main {

    public static int UDP_PORT;
    public static String TEAMNAME;

    public static void main(String[] args) {
        //Get the teamname and udp from console
        parseArguments(args);
        try {
            System.out.println("Starting program on: " + UDP_PORT + " with teamname: " + TEAMNAME);
            Peer.start(UDP_PORT, TEAMNAME);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    //Parse arguments for teamname and udp port
    public static void parseArguments(String[] arguments) {
        if (arguments.length > 0) {
            TEAMNAME = arguments[1];
            UDP_PORT = Integer.parseInt(arguments[0]);

        } else {
            UDP_PORT = 36636;
            TEAMNAME = "Default";
        }
    }

}
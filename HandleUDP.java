package registry;

import java.io.*;
import java.net.*;

//Handles incoming messages over UDP
public class HandleUDP implements Runnable {
	
	//Variables to keep track of IP address and port where stop request was made from
	private String stopIP = "";
	private int stopPort = -1;
	
	//Default constructor
	public HandleUDP() throws SocketException {
			
	}
	

	public void run() {
		try {
			byte[] buff = new byte[1024];
			//While progrma is running
			while(!Peer.STOP) {
				//Receive packets
				DatagramPacket pack = new DatagramPacket(buff, buff.length);
				Peer.udp_socket.receive(pack);

				String request = HelperFunctions.data(buff);
				String [] requestSplit = parsePacket(request);

				//If it is a stop request, get the IP and port of where stop request was made from
				if(request.startsWith("stop")) {
					System.out.println("Received a stop packet");
					this.stopIP = pack.getAddress().toString().split("/")[1];
					this.stopPort = pack.getPort();
					break;
				}
				//Otherwise, convert it to a request and execute it
				Request req = new Request(requestSplit[0], requestSplit[1], pack.getAddress().toString().split("/")[1], pack.getPort());
				Peer.executor.execute(req);
				buff = new byte[1024];				
			}
		} catch(SocketException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}
		//Stop the program
		Request.HandleStop(stopIP, stopPort);
	}
	
	//Parse received packet
	private String[] parsePacket(String pck) {
	    String[] parsed = new String[2];
	    if (pck.startsWith("snip")) {
	      parsed[0] = "snip";
	      parsed[1] = pck.substring(4);
	    } else if (pck.startsWith("peer")) {
	      parsed[0] = "peer";
	      parsed[1] = pck.substring(4);
	    } else {
	      parsed[0] = "stop";
	      parsed[1] = "";
	    }
	    return parsed;
	}
}

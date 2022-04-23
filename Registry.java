package registry;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.*;

/**
 * Registry process that allows peers in the system to register as peers and ask for
 * the location of other peers in the system.
 * <p>
 * Communication with individual peers is managed using RequestProcessor.
 * @author Nathaly Verwaal
 *
 */
public class Registry {
	/** Number of threads allowed in the system.  Each thread manages communication with a peer */
	public static final int THREAD_POOL_SIZE = 10;
	/** Maximum number of peer addresses that will be send when communicating with a peer */
	public static final int MAX_NUM_OF_PEERS = 4;
	/** If no port number is provided when running the registry, this port number will be used. */
	public static final int DEFAULT_PORT_NUMBER = 55921;

	/** Length of time to run the system and accept new peer connections before shutting
	 * the system down and wait for reports from peers in the system.  Note that the time
	 * we spend waiting for reports will reduce the time we run the system.
	 */
	public static final int MINUTES_TO_RUN_SYSTEM = 10;
	
	/** Length of time to wait for connection from peers after shut down message was multicast.
	 * This connection is to communicate with peers and get their reports.
	 */
	public static final int MINUTES_TO_WAIT_FOR_REPORT = 3;
	
	/** Length of time we'll wait for ack of stop message before resending the message. */
	public static final int SECONDS_TO_WAIT_FOR_ACK	 = 10;
	
	/** number of times we'll resend 'stop' requests if no ack received. */
	public static final int NUM_OF_TIMES_SEND_STOP = 3;
	
	private final static Logger LOGGER = Logger.getLogger(Registry.class.getName());
	
	/** Contains the peers we know about: no duplicates allowed */
	private ConcurrentHashMap<String, Peer> peers = new ConcurrentHashMap<String, Peer>();
	
	/** Port number used by this registry */
	private int portNumber;

	/** Indicates if we are done and in shut-down mode */
	boolean done = false;

	/**
	 * Create registry to run at specified port number	
	 * @param aPortNumber port number to attempt running this registry at.
	 */
	public Registry(int aPortNumber) {
		portNumber = aPortNumber;
		LOGGER.setLevel(Level.INFO);
		System.setProperty("java.util.logging.SimpleFormatter.format",
				"%5$s %1$tF %1$tT%n");
		try {
			FileHandler fh = new FileHandler("RegistryLog.log", true);
			fh.setFormatter(new SimpleFormatter());
			LOGGER.addHandler(fh);
		} catch (SecurityException | IOException e) {
			System.out.println("Problem creating logging file for Registry");
		}

	}
	
	/**
	 * Starts this registry and accepts connection requests from peers.  For each
	 * connection request, a RequestProcessor object is created and provided to the
	 * thread pool.
	 * @throws IOException if there are problems starting this registry server or if there
	 * are problems communication along a connection with a peer.
	 */
	public void start() throws IOException {
		ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		
		// This thread will look for command line input: right now the only command is 'done'
		createCommandLineThread();
		// Shuts-down and restarts the system on a timer.
		createTimerThread();
		try {
			ServerSocket server = new ServerSocket(portNumber);
			LOGGER.log(Level.INFO, "Server started at " + 
					server.getInetAddress().getLocalHost().getHostAddress() +
					":" + portNumber);
			// We're ending and restarting this registry continually.  For each restart
			// we'll continue to use the same server socket.
			//while (!done) {
			while (true) {
				Socket sock = server.accept();
				LOGGER.log(Level.INFO, "Connection accepted with " + sock.getRemoteSocketAddress());
				executor.execute(new RequestProcessor(sock, this));				
			}
			//server.close();
		} catch (BindException be) {
			LOGGER.log(Level.SEVERE, "Unable to start registry at port " + portNumber);
		}
		executor.shutdown();
	}
	
	/*------------------------- updated code ----------------------------------------------*/
	
	/**
	 * Creates a thread that monitors for input from the command line.  Right now, we are only
	 * interested in the command 'done' that indicates we should start the process of shutting the
	 * entire system down.
	 * <p>
	 * It will only let all peers in the system know to shut down.  The actually shutting down of this 
	 * server will still have to be done manually.
	 */
	private void createCommandLineThread() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				Scanner keyboard = new Scanner(System.in);
				while (!done) {
					String command = keyboard.nextLine();
					if (command.equalsIgnoreCase("done")) {
						done();
					}
				}
				System.out.println("Thread to read input from command line is done.");
			}
			
		});
		t.start();
	}
	
	/**
	 * Keeps track of timers to shut system down and then restart after sufficient time has
	 * passed to get all reports from peers.  The amount of time the system will run and that the 
	 * system will wait for reports is in the constants MINUTES_TO_RUN_SYSTEM and MINUTES_TO_WAIT_FOR_REPORT.
	 */
	private void createTimerThread() {
		//Timer object allows us to set a timer.  Timers are in milliseconds.
		Timer timer = new Timer();
		// This outer time runs for the length of time that the system should run.
		timer.schedule(new TimerTask() {
			public void run() {
				LOGGER.log(Level.INFO, "closing system");
				done();
				// This inner timer allows us to wait for peer reports before restarting.
				timer.schedule(new TimerTask() {
					public void run() {
						LOGGER.log(Level.INFO, "restarting system");
						RequestProcessor.nextRun();
						peers.clear();
						done = false;
						/*String[] argsForPeer = {"localhost","" + portNumber, "test"};
						try {
							peer.Iteration3Solution.main(argsForPeer);
						} catch (Exception e) {
							System.out.println("Problem running test peers");
						}*/
					}
				}, MINUTES_TO_WAIT_FOR_REPORT*60*1000);
			}
		}, MINUTES_TO_RUN_SYSTEM*60*1000, MINUTES_TO_RUN_SYSTEM*60*1000);
	}
	
	/**
	 * Reveive messages coming in along this UDP socket.  The only messages we're interested
	 * in are ack message which are expected to be in the format:
	 * ack<team name>
	 * <p>
	 * All other messages are ignored.  This method will continue reading until the socket 
	 * is closed.
	 * 
	 * @param udpSocket socket for receiving udp messages.
	 */
	private void receiveAcks(DatagramSocket udpSocket) {
		boolean socketOpen = true;
		while (socketOpen) {
			byte[] message = new byte[1024];
			DatagramPacket packet = new DatagramPacket(message,1024);
			try {
				udpSocket.receive(packet);
				String ackMessage = new String(message);
				if (ackMessage.substring(0,3).equalsIgnoreCase("ack")) {
					String teamName = ackMessage.substring(3).trim();
					System.out.println("Received ack from " + teamName);
					Peer p = peers.get(teamName);
					if (p != null) {
						p.acked = true;
						LOGGER.log(Level.INFO, "Received ack from " + teamName);
					} else {
						LOGGER.log(Level.INFO, "Can't store ack, unknown teamname: " + teamName);
					}
				}
			} catch (IOException e) {
				// do nothing.  When socket closes we can end this method.
				socketOpen = false;
			}
		}
	}
	
	/**
	 * Flag that we are in the shutdown phase.  We'll send a UDP message to each peer to let them know
	 * to end their work and re-connect with the registry to submit a final report.
	 */
	private void done() {
		done = true;
		// Let all processes we know about that we're done and that they should shut down.
		// TODO: create multiple threads so we can communicate with multiple peers simultaneously
		try {
			DatagramSocket udpServer = new DatagramSocket();
			Thread ackReceiver = new Thread( () -> receiveAcks(udpServer));
			ackReceiver.start();
			
			byte[] msg = "stop".getBytes();

			for (int counter = 0; counter < 3; counter++) {
				LOGGER.log(Level.INFO, "Round " + (counter + 1) + " of sending 'stop' to peers'");
				Collection<Peer> knownPeers = peers.values();
				for (Peer p : knownPeers) {
					if (!p.acked) { 
						try {
							System.out.println("About to send 'stop' to " + p);
							DatagramPacket packet = new DatagramPacket(msg, msg.length, InetAddress.getByName(p.address), p.getPort());
							udpServer.send(packet);
							LOGGER.log(Level.INFO, "Sent 'stop' to " + p);
						} catch (IOException e) {
							LOGGER.log(Level.INFO, " Problem sending 'stop' to " + p.teamName);
						}
					}
				}
				
				try {
					Thread.sleep(4*1000);  // wait 4 seconds and resend to any that haven't acked yet
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// closing this will let the thread reading acks know that we are done.  (Not the best way to do this!)
			udpServer.close();
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//System.exit(0);
	}
	
	/*----------------------------------- end of updated code --------------------------------------*/

	/** 
	 * Place the specified in the list of know peers.  If this peer already exists, the 
	 * existing peer will be replaced ensuring that the list of peers send to this new/updated
	 * peer is maintained.
	 * @param p Peer object to add to the list of know peers.
	 */
	public void addPeer(Peer p) {
		Peer replaced = peers.put(p.key(), p);
		if (replaced != null) {
			p.peersSent = replaced.peersSent;
			p.acked = replaced.acked;
		}
	}
	
	/**
	 * Gets MAX_NUM_OF_PEERS sized selection of randomly chosen known peers.
	 * @return the list of randomly chosen peers.
	 */
	public Peer[] getRandomPeerList() {
		int numOfPeers = peers.size();
		Peer[] peerValues = peers.values().toArray(new Peer[numOfPeers]);
		// If total number of known peers is less than the maximum, add all known peers 
		// to the list. Otherwise, we will take the first MAX_NUM_OF_PEERS from the 
		// list after doing a random shuffle.
		if (numOfPeers > MAX_NUM_OF_PEERS) {
			// shuffle the list to help randomly choose peers to send
			Random rand = new Random();
			for (int index = peerValues.length - 2; index >= 0; index--) {
				int randomIndex = rand.nextInt(peerValues.length - index) + index;
				Peer newValue = peerValues[randomIndex];
				peerValues[randomIndex] = peerValues[index];
				peerValues[index] = newValue;
			}
			
			// copy required number of peers from randomly shuffled array
			Peer[] reduced = new Peer[MAX_NUM_OF_PEERS];
			for (int index = 0; index < MAX_NUM_OF_PEERS; index++) {
				reduced[index] = peerValues[index];
			}
			peerValues = reduced;
		}
		return peerValues;
	}
	
	/**
	 * Starts the registry server. If a port number is provided as a runtime argument, 
	 * it will be used to start the registry.
	 * Otherwise, the port number provided as an argument will be used. 
	 * <p>
	 * If we can't start the registry server, the stack trace for the exception will
	 * be printed and the program ended.
	 * 
	 * @param args optional port number as a first argument.
	 */
	public static void main(String[] args)  {
		int portNumber = DEFAULT_PORT_NUMBER;
		if (args.length > 0) {
			try {
				portNumber = Integer.parseInt(args[0]);
			} catch (NumberFormatException nfe) {
				System.out.println("Expected first argument to be a port number.  Argument ignored.");
			}
		}
		Registry r = new Registry(portNumber);
		Thread t = new Thread() { 
			public void run() {
				try {
					r.start();
				} catch (IOException e) {
					// Show that an error occurred with exception info
					e.printStackTrace();
					// end program with a error code
					System.exit(1);
				}
			} 
		};
		t.start();
	}
}

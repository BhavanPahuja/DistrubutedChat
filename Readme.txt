This is a twitter like Peer-to-Peer console based chat system. The registry can either be run locally or remotely.

To run -

- Download and compile all files using javac *.java
- Run the registry using java registry.Registry
- You can start a Peer by running java registry.Peer udp username 
 (two arguments, udp gives the port number for the udp socket, teamName gives the username for the peer)
- You can shutdown the system by typing done in the registry console.
 
 Some details -
 
- The peers will establish a connection with the registry and get a list of all connected peers.
- Using this list, the peers will discover each other and start communicating directly. 
- Each peer sends a ping to every other peer in it's list to check if they are alive.
- Peers can send each other messages/snippets that are all formatted by lamport's timestamp algorithm.
- The registry will send a stop message after some time or if the user types "done" in the console. This will prompt the peers to shutdown communication and send their report to the registry.

Demo video -






This version works with the local registry. To make it work with a remote server, replace the sendLocation() function in HandleTCP.java at line 93 with the commented out function at line 103.

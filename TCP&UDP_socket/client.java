import java.io.*;
import java.net.*;

class client {
	private static int r_port;	

// my solution is made "modular".
	private static void negotiation(String host, int n_port) throws Exception {
// negotiation stage		
		// create client socket connect to server with given host and n_port
		Socket clientSocket1 = new Socket(host, n_port);
		// create output stream attached to socket
		DataOutputStream outToServer = new DataOutputStream(clientSocket1.getOutputStream());
		// create input stream attached to socket
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket1.getInputStream()));
		// client send an integer 13 to initiate negotiation
		outToServer.writeByte(13);
		// generate a random port number.
		r_port = Integer.parseInt(inFromServer.readLine());
		clientSocket1.close();
	}
	
	
	private static void transaction(String host, String msg) throws Exception {
// transaction stage		
		// create client socket for UDP client
		DatagramSocket clientSocket2 = new DatagramSocket();
		// translate hostname to IP address using DNS
		InetAddress IPAddress = InetAddress.getByName(host);
		// allocate array size to store msg
		byte[] receiveData = new byte[1024];
		byte[] sendData = msg.getBytes();
		// create datagram with data-to-send, length, IP address, port
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, r_port);
		// send datagram to server
		clientSocket2.send(sendPacket);
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		// read datagram from server
		clientSocket2.receive(receivePacket);
		String finalmsg = new String(receivePacket.getData());
		// print out the random port number and the msg has been reversed from server.
		System.out.println("r_port:" + r_port);
		System.out.println("FROM SERVER:" + finalmsg);
		clientSocket2.close();		
	}
		
	public static void main(String[] args) throws Exception {
		// check whether the args is valid
		if (args.length == 3 && !args[0].isEmpty() && !args[1].isEmpty()) {
		// initiate arguments
		String host = args[0];
		int n_port = Integer.parseInt(args[1]);
		String msg = args[2];
		// call the help function negotiation and transaction
		negotiation(host, n_port);
		transaction(host, msg);
		
		}
		else {
			// handle exception error check
			System.out.println("Invalid input");
			return;
		}
	}

}

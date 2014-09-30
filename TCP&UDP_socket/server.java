import java.io.*;
import java.net.*;
import java.util.Random;

class server {
	private static int r_port;
	public static void main(String[] args) throws Exception {
		int n_port = 3001;
		byte[] receiveData = new byte[1024];
		byte[] sendData = new byte[1024];
		// print out n_port
		System.out.printf("The current n_port number is %d\n", n_port);
		
		// negotiation stage
		Random random_num = new Random();
		// create welcoming socket at fixed port 3001
		ServerSocket welcomeSocket = new ServerSocket(n_port);
		// wait, on welcome socket for contact by client
		Socket connectionSocket = welcomeSocket.accept();
		// create output stream, attached to socket
		DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
		// generate a random port between 1025 ant 2024
		r_port = random_num.nextInt(1000) + 1024;
		// write out r_port to socket
		outToClient.writeBytes(String.valueOf(r_port) + '\n');
		welcomeSocket.close();
			
		// transaction stage
		// create datagram socket at the random port we generate at negotiation stage
		DatagramSocket serverSocket = new DatagramSocket(r_port);
		// create space for received datagram
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		// received datagram
		serverSocket.receive(receivePacket);
		String msg = new String(receivePacket.getData());
		// get IP address, port # of sender
		InetAddress IPAddress = receivePacket.getAddress();
		int port = receivePacket.getPort();
		String finalmsg = new StringBuffer(msg).reverse().toString();
		sendData = finalmsg.getBytes();
		// create datagram to send to client
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
		// write out datagram to socket
		serverSocket.send(sendPacket);
		serverSocket.close();
	}

}
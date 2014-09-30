import java.util.*;
import java.lang.*;
import java.net.*;
import java.io.*;




public class receiver {
	private static int ESeqNum;
	private static int Packet_received;
	private static FileWriter f1_log;
	private static FileWriter f2;
	private static BufferedWriter buffer1;
	private static BufferedWriter buffer2;

	private static boolean Flag_EOT_Receive;
	private static DatagramSocket sender_socket;
	private static InetAddress IP_Address;
	private static int Emulator_Receieve_Port;
	private static int Receiver_Receieve_Port;
	private static String Emulator_host;
	private static String file_name;
	private static Get_Packet Listener;
	
	
	public receiver() throws Exception {
		sender_socket = new DatagramSocket();
		ESeqNum = 0;
		Packet_received = 0;
		f1_log = new FileWriter("arrival.log");
		buffer1 = new BufferedWriter(f1_log);
		f2 = new FileWriter(file_name);
		buffer2 = new BufferedWriter(f2);
		Flag_EOT_Receive = false;
		Listener = new Get_Packet();
		Listener.start();
	}
	
	
	public void Send_Ack(packet p) throws IOException {
		byte[] Data_to_send = p.getUDPdata();
		IP_Address = InetAddress.getByName(Emulator_host);
		DatagramPacket Packet_to_send 
		= new DatagramPacket(Data_to_send, Data_to_send.length, IP_Address, Emulator_Receieve_Port);
		sender_socket.send(Packet_to_send);
		//System.out.println("Ready to send ACK: " + p.getSeqNum());   // test
	}
	
	
	class Get_Packet extends Thread {
		public void run() {
			try {
				DatagramSocket receiveSocket = new DatagramSocket(Receiver_Receieve_Port);
				while (!Flag_EOT_Receive) {
						byte[] R = new byte[512];
						DatagramPacket Packet_to_receive = new DatagramPacket(R, R.length);
						receiveSocket.receive(Packet_to_receive);
						byte[] Data_to_receive = Packet_to_receive.getData();
						packet receive_Packet = packet.parseUDPdata(Data_to_receive);
					
					if (receive_Packet.getType() == 2) {
						//System.out.println("Received EOT: " + receive_Packet.getSeqNum());   // test
						Flag_EOT_Receive = true;
						Send_Ack(packet.createEOT(receive_Packet.getSeqNum()));
					}
					else if (receive_Packet.getType() == 1) {
						//System.out.println("Ready to get packet: " + receive_Packet.getSeqNum());   // test
						buffer1.write(Integer.toString(receive_Packet.getSeqNum()));
						buffer1.newLine();
						if (ESeqNum % 32 == receive_Packet.getSeqNum()) {
							Packet_received = receive_Packet.getSeqNum();
							ESeqNum = receive_Packet.getSeqNum() + 1;
							buffer2.write(new String(receive_Packet.getData()));
							Send_Ack(packet.createACK(receive_Packet.getSeqNum()));   /// change
						}
						else {
							if (ESeqNum == 0) continue;
							Send_Ack(packet.createACK(Packet_received));
						}
					}
				}
				
				buffer1.close();
				buffer2.close();
				f1_log.close();
				f2.close();
				
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length == 4) { 
			Emulator_host = args[0];
			Emulator_Receieve_Port = Integer.parseInt(args[1]);
			Receiver_Receieve_Port = Integer.parseInt(args[2]);
			file_name = args[3];
			receiver r = new receiver();
		}
		else
			System.out.println("error arguments");

	}
	
}


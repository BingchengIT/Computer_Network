import java.util.*;
import java.lang.*;
import java.net.*;
import java.io.*;


public class sender {
	private final static int window_size = 10;
	private static volatile int Base;
	private static volatile int NextSeq;
	private static ArrayList<packet> Packet_storage;
	private static FileWriter Seqnum_log;
	private static FileWriter Ack_log;
	private static BufferedWriter Seqnum_buffer;
	private static BufferedWriter Ack_buffer;
	private static boolean Flag_SendFileEnd;
	private static boolean Flag_EOT_Receive;
	private static DatagramSocket sender_socket;
	private static InetAddress IP_Address;
	private static int Emulator_Receieve_Port;
	private static int Sender_Receieve_Port;
	private static String Emulator_host;
	private static String file_name;
	private static scheduler time;
	private static receiver Listener;
	private static int CheckValue;
	
	
	// constructor for sender initial variable
	public sender() throws IOException {
		Base = 0;
		NextSeq = 0;
		Packet_storage = new ArrayList<packet>();
		Seqnum_log = new FileWriter("seqnum.log");
		Ack_log = new FileWriter("ack.log");
		Seqnum_buffer = new BufferedWriter(Seqnum_log);
		Ack_buffer = new BufferedWriter(Ack_log);
		Flag_SendFileEnd = false;
		Flag_EOT_Receive = false;
		sender_socket = new DatagramSocket();
		time = new scheduler();
		Listener = new receiver();
		Listener.start();
		CheckValue = 0;
		
	}


	class receiver extends Thread {
		public void run() {
			try {
				DatagramSocket receiveSocket = new DatagramSocket(Sender_Receieve_Port);
				
				while (!Flag_EOT_Receive) {
					// receive acknowledge and pack them up.
					byte[] R = new byte[512];
					DatagramPacket Packet_to_receive = new DatagramPacket(R, R.length);
					receiveSocket.receive(Packet_to_receive);
					byte[] Data_to_receive = Packet_to_receive.getData();

					packet receive_Packet = packet.parseUDPdata(Data_to_receive);
					
					if (receive_Packet.getType() == 2) {
						Flag_EOT_Receive = true;
					}
					else if (receive_Packet.getType() == 0) {
							Ack_buffer.write(Integer.toString(receive_Packet.getSeqNum()));
							Ack_buffer.newLine();
						
							if ((receive_Packet.getSeqNum() + CheckValue*32) == Packet_storage.size() -1) {
								time.interrupt();
								Flag_SendFileEnd = true;
							}
							else if (CheckValue > 0) {
									if (receive_Packet.getSeqNum() >= 22 && Base % 32 <= receive_Packet.getSeqNum() && Base % 32 > 22 ){ 
										time.interrupt();
										Base = receive_Packet.getSeqNum() + 1;
										time = new scheduler();
										time.start();
											
									}
									else if (Base <= CheckValue * 32 + receive_Packet.getSeqNum() && receive_Packet.getSeqNum() < 11) {
										time.interrupt();
										Base = CheckValue * 32 + receive_Packet.getSeqNum() + 1;
										time = new scheduler();
										time.start();
									}	
							}
							else if (receive_Packet.getSeqNum() >= Base % 32) {
									time.interrupt();
									Base = receive_Packet.getSeqNum() + 1;
									time = new scheduler();
									time.start();
							}
					}
				}
			Ack_buffer.close();
			Ack_log.close();
			receiveSocket.close();

			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	
	//  create a timer thread for resending packet and set a fixed timeout.
	class scheduler extends Thread {
		public void run() {
			try {
				Thread.sleep(2000);
				Packet_resend();	
			}
			catch (Exception e) {	
			}
		}
	}

	
	public void Send_Packet(packet p) throws IOException {
		byte[] Data_to_send = p.getUDPdata();
		IP_Address = InetAddress.getByName(Emulator_host);
		DatagramPacket Packet_to_send 
		= new DatagramPacket(Data_to_send, Data_to_send.length, IP_Address, Emulator_Receieve_Port);
		sender_socket.send(Packet_to_send);
	}
	
	public void Packet_resend() throws IOException {
		time.interrupt();
		time = new scheduler();
		time.start();
		
		 int index = Base;
		 while (index < NextSeq) {
		 	Send_Packet(Packet_storage.get(index));
		 	//System.out.println("Resend packet: " + index%32);   // test
		 	Seqnum_buffer.write(Integer.toString(index % 32));
		 	Seqnum_buffer.newLine();
		 	index = index + 1;
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		if (args.length == 4) {

			Emulator_host = args[0];
			Emulator_Receieve_Port = Integer.parseInt(args[1]);
			Sender_Receieve_Port = Integer.parseInt(args[2]);
			file_name = args[3];
			sender s = new sender();

			FileReader Input_file = new FileReader(file_name);
			BufferedReader In_buffer = new BufferedReader(Input_file);
			char[] characters = new char[500];
			while(true) {
				int NumOfChar = In_buffer.read(characters, 0, 500);
				if (NumOfChar != -1) {
					packet Input_packet = packet.createPacket(NextSeq, String.valueOf(characters, 0, NumOfChar));
					Packet_storage.add(Input_packet);
				}
				else
					break;
				NextSeq = (NextSeq + 1) % 32;
			}
			NextSeq = 0;
			In_buffer.close();
			Input_file.close();


			while (Packet_storage.size() > NextSeq) {
				while (NextSeq >= Base + window_size)
					Thread.yield();
				//System.out.println(NextSeq); // test
				s.Send_Packet(Packet_storage.get(NextSeq));
				Seqnum_buffer.write(Integer.toString(NextSeq % 32));
				Seqnum_buffer.newLine();
				CheckValue = NextSeq / 32;
				if (Base == NextSeq) {
					time.start();
				}
				NextSeq++;
			}	
			while (!Flag_SendFileEnd) {
				Thread.yield();
			}
			
			packet EOT_Packet = packet.createEOT(NextSeq % 32);
			s.Send_Packet(EOT_Packet);
			//System.out.println("hello world1");
			Seqnum_buffer.close();
			//System.out.println("hello world");
			Seqnum_log.close();
			//System.out.println("hello world2");
			sender_socket.close();
			//System.out.println("hello world3");
		}
			
		else 
			System.out.println("error arguments");
		}
		//System.exit(0);

}



import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;


class router {
	private FileWriter Router_logWriter;
	private BufferedWriter Router_logBuffer;
	private DatagramSocket Router_Socket;
	
	private int Router_ID;
	private InetAddress NSE_Address;
	private int NSE_Port;
	private int Router_Port;
	
	public ArrayList<RIB> RIBTable;
	public ArrayList<component> componentTable;
	public ArrayList<neighborInfo> neighborTable;
	
	
	class RIB {
		private int Destination;
		private int Path;
		private int Cost;
		
		public RIB(int Dest, int Path, int Cost) {
			this.Destination = Dest;
			this.Path = Path;
			this.Cost = Cost;
		}
	}


	class link_cost {
		private int Link;
		private int Cost;
		
		public link_cost(int Link, int Cost) {
			this.Link = Link;
			this.Cost = Cost;
		}
	}
	
	class component {
		private int router_id;
		private int link_owner;
		public ArrayList<link_cost> linkTable;
		
		public component(int router_id, int link_owner) {
			this.router_id = router_id;
			this.link_owner = link_owner;
			linkTable = new ArrayList<router.link_cost>();
		}
		
		public void appendlink(link_cost link) {
			this.linkTable.add(link);
		}
		
		public link_cost findlink(int link_id) {
			for (link_cost L : linkTable) {
				if (L.Link == link_id)
					return L;
			}
			return null;
		}
	}
	
	class neighborInfo {
		private int id;
		private int link_id;
		private int cost;
		
		public neighborInfo(int link_id, int cost) {
			this.id = 0;
			this.link_id = link_id;
			this.cost = cost;
		}
		public void update(int id) {
			this.id = id;
		}
	}
	
	public router() throws SocketException {
		Router_Socket = new DatagramSocket(Router_Port);
		RIBTable = new ArrayList<RIB>();
		componentTable = new ArrayList<component>();
		neighborTable = new ArrayList<neighborInfo>();
	}
	
	public void sendInitPacket() throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(4);   // can be 512?
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(Router_ID);
		byte[] sendData = buffer.array();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, NSE_Address, NSE_Port);
		Router_Socket.send(sendPacket);
		Router_logBuffer.write("R"+ Router_ID + " send INIT to NSE");
		Router_logBuffer.newLine();
		Router_logBuffer.flush();
		Router_logWriter.flush();
	}
	
	public void receiveCircuitDatabase() throws IOException {
		byte[] receiveData = new byte[512];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		Router_Socket.receive(receivePacket);
		Router_logBuffer.write("R" + Router_ID + " receipt of INIT circuit DB");
		Router_logBuffer.newLine();
		Router_logBuffer.flush();
		Router_logWriter.flush();
		
		ByteBuffer buffer = ByteBuffer.wrap(receivePacket.getData());
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		RIBTable.add(new RIB(Router_ID,Router_ID, 0));
		component C = new component(Router_ID, Router_ID);
		
		int nbr_link = buffer.getInt();
		for (int i = 0; i < nbr_link; i++) {
			int link_id = buffer.getInt();
			int link_cost = buffer.getInt();
			neighborInfo neighbor = new neighborInfo(link_id, link_cost);
			neighborTable.add(neighbor);
			C.appendlink(new link_cost(link_id, link_cost));
			Router_logBuffer.write("R" + Router_ID + " new component(topology):");
			Router_logBuffer.newLine();
			Router_logBuffer.write("R" + Router_ID + " -> "+ "R"+ C.link_owner+" link "+link_id+" cost "+
					link_cost);
			Router_logBuffer.newLine();
			Router_logBuffer.flush();
			Router_logWriter.flush();
		}
		componentTable.add(C);
		sendHelloPacket(C);
	}
	
	public void sendHelloPacket(component C) throws IOException {
		for (int i = 0; i < C.linkTable.size(); i++) {
			link_cost link = C.linkTable.get(i);
			ByteBuffer buffer = ByteBuffer.allocate(8);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			buffer.putInt(Router_ID);
			buffer.putInt(link.Link);
			
			byte[] sendData = buffer.array();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, NSE_Address, NSE_Port);
			Router_Socket.send(sendPacket);
			Router_logBuffer.write("R"+ Router_ID + " send Hello parket: Router "+ Router_ID+ ", link_id " 
					+ link.Link);
			Router_logBuffer.newLine();
			Router_logBuffer.flush();
			Router_logWriter.flush();
		}
	}
	
	public neighborInfo findNeighbor(int link_id) {
		for (neighborInfo neighbor : neighborTable) {
			if (neighbor.link_id == link_id) {
				return neighbor;
			}
		}
		return null;
	}
	
	public component findcomponent(int R_id) {
		for (component C : componentTable) {
			if (C.link_owner == R_id) {
				return C;
			}
		}
		return null;
	}
	
	public RIB findRIB(int Desti) {
		for (RIB rib : RIBTable) {
			if (rib.Destination == Desti) {
				return rib;
			}
		}
		return null;
	}
	
	public void findShortestPath() throws IOException {
		ArrayList<Integer> N = new ArrayList<Integer>();
		N.add(Router_ID);
		
		for (int i=1; i < componentTable.size(); i++) {
			component C = componentTable.get(i);
			RIB rib = findRIB(C.link_owner);
			if (rib == null) {
				rib = new RIB(C.link_owner, Integer.MIN_VALUE, (int)Double.POSITIVE_INFINITY);
				RIBTable.add(rib);
			}
			for (neighborInfo neighbor : neighborTable) {
				for (link_cost link : C.linkTable) {
						if (link.Link == neighbor.link_id) {
							rib.Path = neighbor.id;
							rib.Cost = neighbor.cost;
						}
					}
				}
			}
		for(;;) {
			int w = 0;
			int MIN = (int)Double.POSITIVE_INFINITY;
			for (RIB rib : RIBTable) {
				if (rib.Cost < MIN && !N.contains(rib.Destination)) {
					MIN = rib.Cost;
					w = rib.Destination;
				}
			}
			if (w == 0)
				break;
			N.add(w);
			component C_sub = findcomponent(w);
			for (component W : componentTable) {
				if (N.contains(W.link_owner)) continue;
				for (link_cost linkW : W.linkTable) {
					for (link_cost linksub : C_sub.linkTable) {
						if (linkW.Link == linksub.Link) {
							RIB ribW = findRIB(W.link_owner);
							ribW.Cost = Math.min(ribW.Cost, MIN+linksub.Cost);
							ribW.Path = ribW.Cost < MIN + linksub.Cost ? ribW.Path : findRIB(w).Path;
						}
					}
				}
			}
		}
		Router_logBuffer.write("R"+Router_ID+" current RIB Info:");
        Router_logBuffer.newLine();
        RIB first = RIBTable.get(0);
        Router_logBuffer.write("R"+Router_ID+" -> "+"R"+first.Destination+" -> local, "+first.Cost);
        Router_logBuffer.newLine();
        for (int i = 1; i < RIBTable.size(); i++) {
        	RIB rib = RIBTable.get(i);
        	Router_logBuffer.write("R" + Router_ID + " -> "+ "R" +rib.Destination+ " -> R" + rib.Path+ ", "+
        	rib.Cost);;
        	Router_logBuffer.newLine();
        }
        Router_logBuffer.flush();
        Router_logWriter.flush();
	}
	
	public void sendLSPDU(int sender, int rid, int lid, int cost, int via) throws Exception {
        ByteBuffer buff = ByteBuffer.allocate(20);
        buff.order(ByteOrder.LITTLE_ENDIAN);
        buff.putInt(sender);
        buff.putInt(rid);
        buff.putInt(lid);
        buff.putInt(cost);
        buff.putInt(via);
        byte[] sendData = buff.array();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, NSE_Address, NSE_Port);
        Router_Socket.send(sendPacket);
        Router_logBuffer.write("R"+Router_ID+" sends a LSPDU packet: sender "+sender+", router_id "+rid+", link_id "+lid
        +", cost "+cost+", via "+via);
        Router_logBuffer.newLine();
        Router_logBuffer.flush();
        Router_logWriter.flush();
    }

	public void listener() throws Exception {
        while (true) {
            boolean hasNewLink = false;
            byte[] receiveData = new byte[512];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            Router_Socket.receive(receivePacket);
            byte[] UDPData = receivePacket.getData();
            ByteBuffer buffer = ByteBuffer.wrap(UDPData);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            /* Hello packet has 8 bytes */
            if (receivePacket.getLength() == 8) {
                int rid = buffer.getInt();
                int lid = buffer.getInt();
                Router_logBuffer.write("R"+Router_ID+" receives a Hello packet: router_id "+rid+", link_id "+lid);
                Router_logBuffer.newLine();
                Router_logBuffer.flush();
                Router_logWriter.flush();
                findNeighbor(lid).update(rid);
                /* Send all LSPDU information of current router to the router who sent the hello packet */
                for (neighborInfo neighbor : neighborTable) {
                    sendLSPDU(Router_ID, Router_ID, neighbor.link_id, neighbor.cost, lid);
                }
            }
            /* LSPDU packet has 20 bytes */
            else if (receivePacket.getLength() == 20) {
                int sender = buffer.getInt();
                int rid = buffer.getInt();
                int lid = buffer.getInt();
                int cost = buffer.getInt();
                int via = buffer.getInt();

                /* If the LSPDU's owner is not in the topology then create a new one */
                if (findcomponent(rid) == null) {
                    hasNewLink = true;
                    component topology = new component(Router_ID, rid);
                    topology.appendlink(new link_cost(lid, cost));
                    Router_logBuffer.write("R"+Router_ID+" new topology Info:");
                    Router_logBuffer.newLine();
                    Router_logBuffer.write("R"+Router_ID+" -> "+"R"+topology.link_owner+" link "+lid+" cost "+cost);
                    Router_logBuffer.newLine();
                    Router_logBuffer.flush();
                    Router_logWriter.flush();
                    componentTable.add(topology);
                }
                /* I the LSPDU's owner is in the topology then check whether the link is in the link list or not */
                else {
                    component topology = findcomponent(rid);
                    /* Add this new link to the topology link list */
                    if (topology.findlink(lid) == null) {
                        hasNewLink = true;
                        topology.appendlink(new link_cost(lid, cost));
                        Router_logBuffer.write("R"+Router_ID+" new topology Info:");
                        Router_logBuffer.newLine();
                        Router_logBuffer.write("R"+Router_ID+" -> "+"R"+topology.link_owner+" link "+lid+" cost "+cost);
                        Router_logBuffer.newLine();
                        Router_logBuffer.flush();
                        Router_logWriter.flush();
                    }
                }

                /* If we received a new LSPDU packet then calculate the shortest path and send this LSPDU to all
                   neighbor who already helloed current router and is not the one who sent this LSPDU
                */
                if (hasNewLink) {
                    findShortestPath();
                    for (neighborInfo neighbor : neighborTable) {
                        if (neighbor.id != 0 && neighbor.id != sender) {
                            sendLSPDU(Router_ID, rid, lid, cost, neighbor.link_id);
                        }
                    }
                }
            }

        }
    }

	
	




	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		if (args.length < 4) {
			System.out.println("Invalid number of input");
		}
		router r = new router();
		r.Router_ID = Integer.parseInt(args[0]);
		r.NSE_Address = InetAddress.getByName(args[1]);
		r.NSE_Port = Integer.parseInt(args[2]);
		r.Router_Port = Integer.parseInt(args[3]);
		
		StringBuilder buf = new StringBuilder("router");
		buf.append(r.Router_ID);
		buf.append(".log");
		try {
			r.Router_logWriter = new FileWriter(buf.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		r.Router_logBuffer = new BufferedWriter(r.Router_logWriter);
		r.sendInitPacket();
		r.receiveCircuitDatabase();
		r.listener();
		
	}

}
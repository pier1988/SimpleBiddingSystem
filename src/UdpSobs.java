/*This program implements a simple online bidding system using the UDP protocol.
It can run on either Server or Client mode. 
The users of this system include sellers and customers. 
Sellers can post item entries on the Server and all customers can view the item list. 
If a customer is interested in one of the items, he can directly contact the seller to request more information or to place a bid. */

//Message Flag: 1.register 2.deregister 3.client table update 4.sell an item 5.get items' information 6.place a bid  7.sold item 8.purchase item 9.purchase ack 10.sold ack 11.direct buy from buyer 12.direct sell(ack) 13.direct buy from seller 14.directbuy sold ack 15.direct buy ack to buyer
//Message Flag(cont.):0.error arguments -1.item-num should be positive. -2.Error: itemnum not found -3.Error: owner -4.Error: duplicate bid -5.Error: negative bid

import java.io.*;
import java.net.*;
import java.util.ArrayList;



class UdpSobs {
	public static void main(String args[]) throws Exception	{
		BufferedReader InputFromUser = new BufferedReader(new InputStreamReader(System.in));	//read command
		String str = InputFromUser.readLine();
		String[] UserComm = str.split(" ");
		if (UserComm[0].equals("$")) {
			if (UserComm[1].equals("sobs")) {
				if (UserComm[2].equals("-c")) {
					
					//Client mode
					int ServerPort = Integer.parseInt(UserComm[4]);
					if (ServerPort>=1024 && ServerPort<=65535) {	
						DatagramSocket clientSocket = new DatagramSocket();
						try  {
							InetAddress IPAddress = InetAddress.getByName(UserComm[3]);	
							Client Client1 = new Client();
							String ClientName = null;
							//boolean IsSignIn = false;
							ArrayList<ClientTableC> ClientListC = new ArrayList<ClientTableC>();
							new Update(ClientListC, clientSocket, Client1, IPAddress, ServerPort);
							
							
							//listen keyboard input
							while (true) {
								System.out.print("sobs>");
								InputFromUser = new BufferedReader(new InputStreamReader(System.in));	//read command
								str = InputFromUser.readLine();
								UserComm = str.split(" ");
								
								//register
								if (UserComm[0].equals("register") && !Client1.IsSignIn) {
									Client1.register(str, clientSocket, IPAddress, ServerPort);
									ClientName = UserComm[1];
									Client1.SetClientName(ClientName);
									//IsSignIn = true;
								}	//end if
								else if (UserComm[0].equals("register") && Client1.IsSignIn){
									System.out.println("You have registered.");
								}
								else if (Client1.IsSignIn) {
									//deregister
									if (UserComm[0].equals("deregister")) {
										Client1.deregister(str, ClientName, clientSocket, IPAddress, ServerPort);
										//IsSignIn = false;
									}
									
									
									//sell item
									else if (UserComm[0].equals("sell")) {
										Client1.Sell(str, ClientName, clientSocket, IPAddress, ServerPort);
									}
									
									//get item's information
									else if (UserComm[0].equals("info")) {
										if (UserComm.length == 1) {
											Client1.GetInfo("0", clientSocket, IPAddress, ServerPort);
										}	//end if
										else
											Client1.GetInfo(UserComm[1], clientSocket, IPAddress, ServerPort);
									}
									
									//place a bit
									else if (UserComm[0].equals("bid")) {
										Client1.Bid(UserComm[1], UserComm[2], ClientName, clientSocket, IPAddress, ServerPort);
									}
									
									//direct buy
									else if (UserComm[0].equals("buy")) {
										Client1.DirectBuy(ClientListC, UserComm[1], UserComm[2], ClientName, clientSocket, IPAddress, ServerPort);
									}
									
									else {
										System.out.println("error!");
									}
								}	//end if
								else {
									System.out.println("You have not registered.");
								}	//end else							
											
							} //end while
						} //end try 
						catch(Exception e) {	//invalid IPAddress
							System.out.println("Invalid IPAddress.");
							return;
						} //end catch
					} else System.out.println("Port number is out of range(64 ~ 65535)");
				} 
				
				//Server mode
				else if (UserComm[2].equals("-s")){
					int SocketNum = Integer.parseInt(UserComm[3]);
					if (SocketNum>=64 && SocketNum<=65535) {		
						int MAX_INT = 100;
						DatagramSocket serverSocket = new DatagramSocket(SocketNum);
						System.out.println("Socket " + SocketNum + " has been created.");
						Server Server1 = new Server();
						ArrayList<ClientTableS> ClientList1 = new ArrayList<ClientTableS>();
						ArrayList<OfflineMessage> OfflineMessage1 = new ArrayList<OfflineMessage>();
						ItemTable[] ItemTable1 = new ItemTable[MAX_INT];
						while(true) {
							serverSocket.setSoTimeout(0);
							byte[] receiveData = new byte[64];
							
							DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
							serverSocket.receive(receivePacket);
							
							//remove useless spaces
							byte[] buffer = new byte[receivePacket.getLength()];	//remove useless spaces
							System.arraycopy(receivePacket.getData(), 0, buffer, 0, receivePacket.getLength());
							String sentence = new String(buffer);
							
							InetAddress IPAddress = receivePacket.getAddress();
							int port = receivePacket.getPort();		
							String[] ClientComm= sentence.split(" ");
							int flag = Integer.parseInt(ClientComm[0]);
							
							//register to ClientTable1
							if (flag == 1) {	//register to ClientTable1
								Server1.register(serverSocket, ClientList1, ClientComm[2], IPAddress, port);
								Server1.OffMessageSend(serverSocket, OfflineMessage1, ClientComm[2], IPAddress, port);
							}
							
							//deregister
							else if (flag == 2) {	//register to ClientTable1
								Server1.deregister(serverSocket, ClientList1, ClientComm[2], IPAddress, port);								
							}
							
							//client sell an item
							else if (flag == 4) {
								String desc = ClientComm[6];
								for (int i=7; i<ClientComm.length-1;i++)
									desc = desc + " " + ClientComm[i];
								Server1.ClientSell(serverSocket, ClientComm[ClientComm.length-1], ItemTable1,ClientComm[2], Integer.parseInt(ClientComm[3]), Integer.parseInt(ClientComm[4]),Integer.parseInt(ClientComm[5]), desc, IPAddress, port);
							}
							
							//get items' information
							else if (flag == 5) {
								Server1.GetInfo(serverSocket, ItemTable1, ClientComm[1], IPAddress, port);
							}
							
							//place a bid
							else if (flag == 6) {
								Server1.Bid(serverSocket, ClientList1, OfflineMessage1, ItemTable1, ClientComm[1], ClientComm[2], ClientComm[3], IPAddress, port);
							}
							
							//direct buy from buyer
							else if (flag == 11) {
								Server1.DirectBuyBuyer(serverSocket, ClientList1, OfflineMessage1, ItemTable1, ClientComm[1], ClientComm[2], IPAddress, port);
							}
							
							//direct buy from seller
							else if (flag == 13) {
								Server1.DirectBuySeller(serverSocket, ClientList1, OfflineMessage1, ItemTable1, ClientComm[1], ClientComm[2], IPAddress, port);
							}
				
						}	//end while
					} else System.out.println("Port number is out of range(1024 ~ 65535)");
				} else System.out.println("error command.");
			} else System.out.println("error command.");
		} else System.out.println("error command.");
	} //end main
	
} //end class

//Thread to listen packet
class Update extends Thread{
	private DatagramSocket socket;
	private Client Client1;
	private InetAddress IP;
	private int Port;
	private ArrayList<ClientTableC> ClientList;
	
	
	Update(ArrayList<ClientTableC> ClientList1, DatagramSocket sk, Client client, InetAddress ip, int port) {
		socket = sk;
		Client1 = client;
		IP = ip;
		Port = port;
		start();
		ClientList = ClientList1;
	}	//end method;
	
	public void run() {
		while(true) {
			byte[] receiveData = new byte[64];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			try {
				socket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
			String ReceivedInfo = DataToString(receivePacket);
			String[] RemoteComm = ReceivedInfo.split(" ");
			int flag = Integer.parseInt(RemoteComm[0]);
			
			//register ack
			if (flag == 1) {
				Client1.SetIsAcked();
				Client1.IsSignIn = true;
				System.out.println("Welcome " + RemoteComm[1] + ", you have successfully signed in.");
			} 	//end if
			
			//deregister ack
			else if (flag == 2) {
				Client1.SetIsAcked();
				Client1.IsSignIn = false;
				System.out.println("You have successfully signed out. Bye!.");
			}	//end if
			
			//update client table
			else  if(flag == 3) {
				Client1.SetIsAcked();
				ClearClientTable(ClientList);
				for (int i=1; i<RemoteComm.length; i=i+3) {
					RemoteComm[i+1] = RemoteComm[i+1].substring(1);
					try {
						UpdateClientTable(ClientList, RemoteComm[i], InetAddress.getByName(RemoteComm[i+1]), Integer.parseInt(RemoteComm[i+2]));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}	//end for
				System.out.print("\nClient table updated.\n");
			} 	//end if
			
			//sell item
			else if (flag == 4) {
				Client1.SetIsAcked();
				System.out.println(RemoteComm[1] + " added with number " + RemoteComm[2]);
			}	//end if
			
			//get info
			else if (flag == 5) {
				Client1.SetIsAcked();
				String info = RemoteComm[1];
				for (int i=2; i<RemoteComm.length;i++)
					info = info + " " + RemoteComm[i];
				String[] iteminfo = info.split("/");
				if (iteminfo[0].equals("Error:") && iteminfo[1].equals("empty")) {
					System.out.println(iteminfo[0] + " " + iteminfo[1]);
				}
				else if (iteminfo[0].equals("Error:") && !iteminfo[1].equals("empty")) 
					System.out.println(iteminfo[0] + " " + iteminfo[1] + " " + iteminfo[2] + " " + iteminfo[3]);
				else if (iteminfo[0].equals("item-num")) 
					System.out.println("item-num should be non-negative.");
				else {
					for (int i=1; i<iteminfo.length; i=i+6) {
						System.out.println(iteminfo[i] + " " + iteminfo[i+1] + " " + iteminfo[i+2] + " " + iteminfo[i+3] + " " + iteminfo[i+4] + " " + iteminfo[i+5]);
					}	//end for
				}
			}	//end else if
			
			//simple bid
			else if (flag == 6) {
				Client1.SetIsAcked();
				System.out.println(RemoteComm[1] + " " + RemoteComm[2] + " " + RemoteComm[3]);
			}
			
			//sold
			else if (flag == 7) {
				Client1.SetIsAcked();
				System.out.print("\nsold " + RemoteComm[1] + " " + RemoteComm[2] + " " + RemoteComm[3] + "\nsobs>");
				try {
					Client1.Ack("10", socket, IP, Port);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			//purchase
			else if (flag == 8) {
				Client1.SetIsAcked();
				System.out.println("purchased " + RemoteComm[1] + " " + RemoteComm[2] + " " + RemoteComm[3]);
				try {
					Client1.Ack("9", socket, IP, Port);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}	//end if
			
			//direct sell
			else if (flag == 11) {
				System.out.print(RemoteComm[2] + " want to buy your " + RemoteComm[1] + "\nsobs>");
				try {
					Client1.DirectSell(ClientList, "12", RemoteComm[1], RemoteComm[2], socket, IP, Port);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}	//end if
			
			//direct buy ack from seller
			else if (flag == 12) {
				Client1.SetIsAcked();
				System.out.print(RemoteComm[1] + " has received your request.\nsobs>");
			}
			
			//directbuy sold info
			else if (flag == 14) {
				Client1.SetIsAcked();
				System.out.print("\nsold " + RemoteComm[1] + " " + RemoteComm[2] + " " + RemoteComm[3] + "\nsobs>");
			}
			
			//directbuy ack to buyer
			else if (flag == 15) {
				Client1.SetIsAcked();
				System.out.print(RemoteComm[1] + " currently is offline. Request forwarded to the server.\n");
			}
			
			//below is all error messages
			else if (flag == 0) {	
				Client1.SetIsAcked();
				System.out.println("Error:arguments");
			}
			
			else if (flag == -1) {
				Client1.SetIsAcked();
				System.out.println("itemnum should be positive.");
			}
			
			else if (flag == -2) {
				Client1.SetIsAcked();
				System.out.println("Error: " + RemoteComm[1] + " not found");
			}
			
			else if (flag == -3) {
				Client1.SetIsAcked();
				System.out.println("Error: owner");
			}
			
			else if (flag == -4) {
				Client1.SetIsAcked();
				System.out.println("Error: duplicate bid");
			}
			
			else if (flag == -5) {
				Client1.SetIsAcked();
				System.out.println("Error: negative bid");
			}
			
			
			else System.out.println("error");
		}	//end while
	}	//end method
	
	void UpdateClientTable(ArrayList<ClientTableC> list, String name, InetAddress ip, int port) {
		ClientTableC c = new ClientTableC(name, ip, port);
		list.add(c);
	}	//end method
	
	
	String DataToString(DatagramPacket packet) {	//remove useless space
		byte[] buffer = new byte[packet.getLength()];
		System.arraycopy(packet.getData(), 0, buffer, 0, packet.getLength());
		String ReceivedInfo = new String(buffer);	
		return ReceivedInfo;
	}	//end method
	
	void ClearClientTable(ArrayList<ClientTableC> list) {
		list.clear();
	}	//end method
	
	
}	//end class

class Client {
	private boolean IsAcked;
	private int Retry;
	public boolean IsSignIn = false;
	String ClientName;
	
	public void SetIsAcked() {
		IsAcked = true;
	}
	
	public void SetClientName(String name) {
		ClientName = name;
	}
	
	public String GetClientName() {
		return ClientName;
	}
	
	
	public void register(String str, DatagramSocket socket, InetAddress ip, int port) throws IOException {
		IsAcked = false;
		Retry = 5;
		while (Retry >= 0) {
			str = Integer.toString(1) + " " + str;
			byte[] sendData = new byte[64];
			sendData = str.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
			socket.send(sendPacket);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (IsAcked)
				break;
			else
				Retry--;			
		}
		if (Retry<0)
			System.out.print("Server no response.\n");
	}	//end method

	
	public void deregister(String str, String name, DatagramSocket socket, InetAddress ip, int port) throws IOException {
		IsAcked = false;
		Retry = 5;
		while (Retry >= 0) {
			byte[] sendData = new byte[64];
			String SendInfo = Integer.toString(2) + " " + str + " " + name;
			sendData = SendInfo.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
			socket.send(sendPacket);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (IsAcked)
				break;
			else
				Retry--;	
		}	//end while
		if (Retry<0)
			System.out.print("Server no response.\n");
	}	//end method
	
	public void Sell(String str, String name, DatagramSocket socket, InetAddress ip, int port) throws IOException {
		IsAcked = false;
		Retry = 5;
		while (Retry >= 0) {
			str = Integer.toString(4) + " " + str + " " + name;
			byte[] sendData = new byte[64];
			sendData = str.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
			socket.send(sendPacket);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (IsAcked)
				break;
			else
				Retry--;	
		}	//end while
		if (Retry<0)
			System.out.print("Server no response.\n");
	}	//end method
	
	public void GetInfo(String str, DatagramSocket socket, InetAddress ip, int port) throws IOException {
		IsAcked = false;
		Retry = 5;
		while (Retry >= 0) {
			byte[] sendData = new byte[64];
			String SendInfo = Integer.toString(5) + " " + str;
			sendData = SendInfo.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
			socket.send(sendPacket);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (IsAcked)
				break;
			else
				Retry--;	
		}	//end while
		if (Retry<0)
			System.out.print("Server no response.\n");
	}	//end method
	
	public void Bid(String itemnum, String amount, String clientname, DatagramSocket socket, InetAddress ip, int port) throws IOException {
		IsAcked = false;
		Retry = 5;
		while (Retry >= 0) {
			byte[] sendData = new byte[64];
			String SendInfo = Integer.toString(6) + " " + itemnum + " " + amount + " " + clientname;
			sendData = SendInfo.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
			socket.send(sendPacket);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (IsAcked)
				break;
			else
				Retry--;	
		}	//end while
		if (Retry<0)
			System.out.print("Server no response.\n");
	}	//end method
	
	public void Ack(String str, DatagramSocket socket, InetAddress ip, int port) throws IOException {
		byte[] sendData = new byte[64];
		String SendInfo = str;
		sendData = SendInfo.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
		socket.send(sendPacket);
	}	//end method
	
	public void DirectBuy(ArrayList<ClientTableC> list, String sellername, String itemnum, String clientname, DatagramSocket socket, InetAddress ip, int port) throws IOException {
		boolean IsSellerOnline =  false;
		boolean IsSellerAck = false;
		for (int i=0; i<list.size(); i++) {
			if (list.get(i).GetName().equals(sellername)) {
				IsAcked = false;
				Retry = 5;
				IsSellerOnline =  true;
				while (Retry >= 0) {
					byte[] sendData = new byte[64];
					String SendInfo = "11 " + itemnum + " " +  clientname;
					sendData = SendInfo.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, list.get(i).GetIP(), list.get(i).GetPort());
					socket.send(sendPacket);
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (IsAcked) {
						IsSellerAck = true;
						break;
					}
					else
						Retry--;	
				}	//end while
				if (Retry<0)
					IsSellerAck = false;
			}	//end if
		}	//end for
		if (!IsSellerOnline || !IsSellerAck) {
			IsAcked = false;
			Retry = 5;
			while (Retry >= 0) {
				byte[] sendData = new byte[64];
				String SendInfo = "11 " + itemnum + " " +  clientname;
				sendData = SendInfo.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
				socket.send(sendPacket);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (IsAcked)
					break;
				else
					Retry--;	
			}	//end while
			if (Retry<0)
				System.out.print("Server no response.\n");
		}	//end if 
	}	//end method
	
	public void DirectSell(ArrayList<ClientTableC> list, String str, String itemnum, String clientname, DatagramSocket socket, InetAddress ip, int port) throws IOException {
		for (int i=0; i<list.size(); i++) {
			if (list.get(i).GetName().equals(clientname)) {
				byte[] sendData = new byte[64];
				String SendInfo = str + " " + ClientName;
				sendData = SendInfo.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, list.get(i).GetIP(), list.get(i).GetPort());
				socket.send(sendPacket);
			}
		}
	
		byte[] sendDataS = new byte[64];
		String SendInfoS = "13 " + itemnum + " " +  clientname;
		sendDataS = SendInfoS.getBytes();
		DatagramPacket sendPacketS = new DatagramPacket(sendDataS, sendDataS.length, ip, port);
		socket.send(sendPacketS);
	}	//end method
	
	String DataToString(DatagramPacket packet) {	//remove useless space
		byte[] buffer = new byte[packet.getLength()];
		System.arraycopy(packet.getData(), 0, buffer, 0, packet.getLength());
		String ReceivedInfo = new String(buffer);	
		return ReceivedInfo;
	}	//end method
	
} //end class


class ClientTableC {
	private String Name;
	private InetAddress IP;
	private int Port;
	
	public ClientTableC(String name, InetAddress ip, int port) {
		Name = name;
		IP = ip;
		Port = port;
	}	//end contructor
	
	public String GetName() {
		return Name;
	}	//end method
	
	public InetAddress GetIP() {
		return IP;
	}	//end method
	
	public int GetPort() {
		return Port;
	}	//end method
	
	public void SetIP(InetAddress ip) {
		IP = ip;
	}	//end method
	
	public void SetPort(int port) {
		Port = port;
	}	//end method
}	//end class

class Server {
	private int currID=0;
	private int Retry;
	
	void register(DatagramSocket socket, ArrayList<ClientTableS> list, String name, InetAddress ip, int port) throws IOException {
		ClientTableS c = new ClientTableS(name, ip, port);
		for (int i=0; i<list.size(); i++) {	//check if username exists
			if (list.get(i).GetName().equals(name)) {
				list.get(i).SetIP(ip);
				list.get(i).SetPort(port);
				byte[] sendData = new byte[64];
				String SendInfo = Integer.toString(1) + " " + name;
				sendData = SendInfo.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
				socket.send(sendPacket);
				
				BroadcastClientTable("3", list, socket);
				return;
			}	//end if
		}	//end for
		list.add(c);
		byte[] sendData = new byte[64];
		String SendInfo = Integer.toString(1) + " " + name;
		sendData = SendInfo.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
		socket.send(sendPacket);
		
		BroadcastClientTable("3", list, socket);
	}	//end method
	
	void OffMessageSend(DatagramSocket socket, ArrayList<OfflineMessage> offline, String name, InetAddress ip, int port) throws IOException {
		for (int i=0; i<offline.size(); i++) {	//check if username exists
			if (offline.get(i).GetName().equals(name)) {
				byte[] sendData = new byte[64];
				String SendInfo = offline.get(i).GetData();
				sendData = SendInfo.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
				socket.send(sendPacket);
				if (offline.size()>1) {	//list will not be empty
					offline.remove(i);
				}
				else {
					offline.clear();
				}
			}	//end if
		}	//end for
	}	//end method
	
	void deregister(DatagramSocket socket, ArrayList<ClientTableS> list, String name, InetAddress ip, int port) throws IOException {
		for (int i=0; i<list.size(); i++) {	//check if username exists
			if (list.get(i).GetName().equals(name)) {
				if (list.size()>1) {	//list will not be empty
					list.remove(i);
					break;
				}
				else {
					list.clear();
					break;
				}
			}	//end if
		}	//end for
		byte[] sendData = new byte[64];
		String SendInfo = Integer.toString(2) + " deregister";
		sendData = SendInfo.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
		socket.send(sendPacket);
		
		BroadcastClientTable("3", list, socket);

		return;
	}	//end method
	
	void ClientSell(DatagramSocket socket, String clientname, ItemTable[] ItemTable1, String itemname, int limit, int bid, int buynow, String desc, InetAddress ip, int port) throws IOException {
		String SendInfo;
		if (limit < 1 || bid <= 0 || (buynow <=0 || buynow <= bid)) {	//error arguments
			SendInfo = Integer.toString(0);
		}
		else {
			ItemTable1[currID] = new ItemTable(itemname, clientname, limit, bid, buynow, desc);
			System.out.println("add " + clientname + " "+limit+" "+bid+" "+buynow+" "+desc);
			currID++;
			SendInfo = Integer.toString(4) + " " + itemname + " "  + Integer.toString(currID);
		}
		byte[] sendData = new byte[64];
		sendData = SendInfo.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
		socket.send(sendPacket);		
	}	//end method
	
	void GetInfo(DatagramSocket socket, ItemTable[] ItemTable1, String id, InetAddress ip, int port) throws IOException {
		String SendInfo = new String();
		int searchid = Integer.parseInt(id);
		boolean IsEmpty = true;
		for (int i=0; i<currID; i++) {
			if (!ItemTable1[i].IsSoldOut) {
				IsEmpty = false;
			}
		}
		if (searchid == 0) {
			if (currID == 0 || IsEmpty) {
				SendInfo = "Error:/empty";
			}	//end if
			else {
				for (int i=0; i<currID; i++) {
					if (!ItemTable1[i].IsSoldOut) {
						SendInfo = SendInfo + "/" + Integer.toString(i+1) + "/" + ItemTable1[i].PrintItem();
					}	//end if
				}	//end for
			}	//end else
		}	//end if
		else if (searchid > 0) {
			if (searchid-1 >= currID || ItemTable1[searchid-1].IsSoldOut) {
				SendInfo = "Error:/" + searchid + "/not/found";
			}
			else
				SendInfo = "/" + Integer.toString(searchid) + "/" + ItemTable1[searchid-1].PrintItem();
		}
		else
			SendInfo = "item-num/should/be/non-negative.";
		byte[] sendData = new byte[64];
		SendInfo = Integer.toString(5) + " " + SendInfo;
		sendData = SendInfo.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
		socket.send(sendPacket);
	}	//end method
	
	void Bid(DatagramSocket socket, ArrayList<ClientTableS> list, ArrayList<OfflineMessage> offline, ItemTable[] ItemTable1, String id, String amount, String clientname, InetAddress ip, int port) throws IOException {
	String SendInfo = new String();
	String SendInfoBuyer = new String();
	String SendInfoSeller = new String();
	int searchid = Integer.parseInt(id);
	if (searchid > 0) {
		if (searchid-1 >= currID || ItemTable1[searchid-1].IsSoldOut) {
			SendInfo = "-2 " + id;	//Error: itemnum not found
			byte[] sendData = new byte[64];
			sendData = SendInfo.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
			socket.send(sendPacket);
		}
		else {
			if (clientname.equals(ItemTable1[searchid-1].GetSellerName())) {
				SendInfo = "-3";	//Error: owner
				byte[] sendData = new byte[64];
				sendData = SendInfo.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
				socket.send(sendPacket);
			}
			else if (clientname.equals(ItemTable1[searchid-1].GetHighestBuyer())) {
				SendInfo = "-4";	//Error: duplicate bid
				byte[] sendData = new byte[64];
				sendData = SendInfo.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
				socket.send(sendPacket);
			}
			else if (Integer.parseInt(amount) <= 0) {
				SendInfo = "-5";	//Error: negative bid
				byte[] sendData = new byte[64];
				sendData = SendInfo.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
				socket.send(sendPacket);
			}
			else if (ItemTable1[searchid-1].GetTransLimit()>1) {	//simple bid
				ItemTable1[searchid-1].UpdateCurrBid(Integer.parseInt(amount));
				ItemTable1[searchid-1].UpdateHighestBuyer(clientname);
				ItemTable1[searchid-1].UpdateTransLimit();
				SendInfo = "6 " + id + " " + ItemTable1[searchid-1].GetItemName() + " " + Integer.toString(ItemTable1[searchid-1].GetCurrBid());
				byte[] sendData = new byte[64];
				sendData = SendInfo.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
				socket.send(sendPacket);
			}
			else if (ItemTable1[searchid-1].GetTransLimit() == 1) {	//sold
				ItemTable1[searchid-1].UpdateCurrBid(Integer.parseInt(amount));
				ItemTable1[searchid-1].IsSoldOut = true;
				SendInfoSeller = "7 " + id + " " + ItemTable1[searchid-1].GetItemName() + " " + Integer.toString(ItemTable1[searchid-1].GetCurrBid());
				SendInfoBuyer = "8 " + id + " " + ItemTable1[searchid-1].GetItemName() + " " + Integer.toString(ItemTable1[searchid-1].GetCurrBid());
				
				Retry = 5;
				while (Retry >= 0) {
					byte[] sendDataBuyer = new byte[64];
					sendDataBuyer = SendInfoBuyer.getBytes();
					DatagramPacket sendPacketBuyer = new DatagramPacket(sendDataBuyer, sendDataBuyer.length, ip, port);
					socket.send(sendPacketBuyer);
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					socket.setSoTimeout(500);
					try {
						byte[] receiveData = new byte[64];
						DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
						socket.receive(receivePacket);
						byte[] buffer = new byte[receivePacket.getLength()];	//remove useless spaces
						System.arraycopy(receivePacket.getData(), 0, buffer, 0, receivePacket.getLength());
						String sentence = new String(buffer);
						String[] ClientComm= sentence.split(" ");
						int flag = Integer.parseInt(ClientComm[0]);
						if (flag == 9) {
							break;
						}
					} catch (SocketTimeoutException ste) {
						Retry--;	
					}
				}
				if (Retry<0) {
					System.out.print("Buyer no response.\n");
					OfflineMessage b = new OfflineMessage(clientname, SendInfoBuyer);
					offline.add(b);
				}	//end if
				
				int noresponse=-1;
				Retry = 5;
				while (Retry >= 0) {
					boolean SellerOnline = false;
					for (int i=0; i<list.size(); i++) {	//check if username exists
						if (list.get(i).GetName().equals(ItemTable1[searchid-1].GetSellerName())) {
							byte[] sendDataSeller = new byte[64];
							sendDataSeller = SendInfoSeller.getBytes();
							DatagramPacket sendPacketSeller = new DatagramPacket(sendDataSeller, sendDataSeller.length, list.get(i).GetIP(), list.get(i).GetPort());
							socket.send(sendPacketSeller);
							SellerOnline = true;
							noresponse = i;
						}	//end if
					}
					if (!SellerOnline) {
						System.out.print("Seller is off-line.\n");
						OfflineMessage s = new OfflineMessage(ItemTable1[searchid-1].GetSellerName(), SendInfoSeller);
						offline.add(s);
					}	//end if
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					socket.setSoTimeout(500);
					try {
						byte[] receiveData = new byte[64];
						DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
						socket.receive(receivePacket);
						byte[] buffer = new byte[receivePacket.getLength()];	//remove useless spaces
						System.arraycopy(receivePacket.getData(), 0, buffer, 0, receivePacket.getLength());
						String sentence = new String(buffer);
						String[] ClientComm= sentence.split(" ");
						int flag = Integer.parseInt(ClientComm[0]);
						if (flag == 10)
							break;
					} catch (SocketTimeoutException ste) {
						Retry--;
					}
				}
				if (Retry<0) {
					if (list.size()>1) {	//list will not be empty
						list.remove(noresponse);
					}
					else {
						list.clear();
					}
					System.out.print("Seller no response.\n");
					BroadcastClientTable("3", list, socket);
				}
			}	//end sold
		}	//end else
	}
	else {
		SendInfo = "-1";	//itemnum should be positive.
		byte[] sendData = new byte[64];
		sendData = SendInfo.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
		socket.send(sendPacket);
	}
}	//end method
	
	void DirectBuySeller(DatagramSocket socket, ArrayList<ClientTableS> list, ArrayList<OfflineMessage> offline, ItemTable[] ItemTable1, String id, String clientname, InetAddress ip, int port) throws IOException {
		String SendInfo = new String();
		String SendInfoBuyer = new String();
		String SendInfoSeller = new String();
		int searchid = Integer.parseInt(id);
		if (searchid > 0) {
			if (searchid-1 >= currID || ItemTable1[searchid-1].IsSoldOut) {
				SendInfo = "-2 " + id;	//Error: itemnum not found
				byte[] sendData = new byte[64];
				sendData = SendInfo.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
				socket.send(sendPacket);
			}
			else {
				//sold
				ItemTable1[searchid-1].IsSoldOut = true;
				SendInfoSeller = "14 " + id + " " + ItemTable1[searchid-1].GetItemName() + " " + Integer.toString(ItemTable1[searchid-1].GetBuyNow());
				SendInfoBuyer = "8 " + id + " " + ItemTable1[searchid-1].GetItemName() + " " + Integer.toString(ItemTable1[searchid-1].GetBuyNow());
				
				byte[] sendDataSeller = new byte[64];
				sendDataSeller = SendInfoSeller.getBytes();
				DatagramPacket sendPacketSeller = new DatagramPacket(sendDataSeller, sendDataSeller.length, ip, port);
				socket.send(sendPacketSeller);
										
				int noresponse=-1;
				Retry = 5;
				while (Retry >= 0) {
					boolean BuyerOnline = false;
					for (int i=0; i<list.size(); i++) {	//check if username exists
						if (list.get(i).GetName().equals(clientname)) {
							byte[] sendDataBuyer = new byte[64];
							sendDataBuyer = SendInfoBuyer.getBytes();
							DatagramPacket sendPacketBuyer = new DatagramPacket(sendDataBuyer, sendDataBuyer.length, list.get(i).GetIP(), list.get(i).GetPort());
							socket.send(sendPacketBuyer);
							BuyerOnline = true;
							noresponse = i;
						}	//end if
					}
					if (!BuyerOnline) {
						System.out.print("Buyer is off-line.\n");
						OfflineMessage s = new OfflineMessage(clientname, SendInfoBuyer);
						offline.add(s);
					}	//end if
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					socket.setSoTimeout(500);
					try {
						byte[] receiveData = new byte[64];
						DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
						socket.receive(receivePacket);
						byte[] buffer = new byte[receivePacket.getLength()];	//remove useless spaces
						System.arraycopy(receivePacket.getData(), 0, buffer, 0, receivePacket.getLength());
						String sentence = new String(buffer);
						String[] ClientComm= sentence.split(" ");
						int flag = Integer.parseInt(ClientComm[0]);
						if (flag == 9) {
							break;
						}	
						else {
							continue;
						}						
					} catch (SocketTimeoutException ste) {
						Retry--;
					}
				}
				if (Retry<0) {
					if (list.size()>1) {	//list will not be empty
						list.remove(noresponse);
					}
					else {
						list.clear();
					}
					System.out.print("Buyer no response.\n");
					BroadcastClientTable("3", list, socket);
				} 
			}	//end else
		}	//end if
		else {
			SendInfo = "-1";	//itemnum should be positive.
			byte[] sendData = new byte[64];
			sendData = SendInfo.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
			socket.send(sendPacket);
		}
	}	//end method
	
	void DirectBuyBuyer(DatagramSocket socket, ArrayList<ClientTableS> list, ArrayList<OfflineMessage> offline, ItemTable[] ItemTable1, String id, String clientname, InetAddress ip, int port) throws IOException {
		int searchid = Integer.parseInt(id);
		if (searchid > 0) {
			if (searchid-1 >= currID || ItemTable1[searchid-1].IsSoldOut) {
				String SendInfo = "-2 " + id;	//Error: itemnum not found
				byte[] sendData = new byte[64];
				sendData = SendInfo.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
				socket.send(sendPacket);
			}
			else {
				//sold
				ItemTable1[searchid-1].IsSoldOut = true;
				for (int i=0; i<list.size(); i++) {
					if (list.get(i).GetName().equals(ItemTable1[searchid-1].GetSellerName())) {
						if (list.size()>1) {	//list will not be empty
							list.remove(i);
						}
						else {
							list.clear();
						}
					}	//end if
				}	//end for
				String SendInfo = "15 " + ItemTable1[searchid-1].GetSellerName();
				byte[] sendData = new byte[64];
				sendData = SendInfo.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
				socket.send(sendPacket);
				
				String str = "7 " + id + " " + ItemTable1[searchid-1].GetItemName() + " " + ItemTable1[searchid-1].GetBuyNow();
				OfflineMessage o = new OfflineMessage(ItemTable1[searchid-1].GetSellerName(), str);
				offline.add(o);
			}	//end else
		}	//end if
		else {
			String SendInfo = "-1";	//itemnum should be positive.
			byte[] sendData = new byte[64];
			sendData = SendInfo.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
			socket.send(sendPacket);
		}
	}	//end method
	
	void BroadcastClientTable(String comm, ArrayList<ClientTableS> list, DatagramSocket socket) throws IOException {
		String str = comm;
		for (int j=0; j<list.size(); j++) {	//create client table string
			str = str + " " + list.get(j).GetName() +" " + list.get(j).GetIP().toString() + " " + Integer.toString(list.get(j).GetPort());
		}	//end for
		for (int j=0; j<list.size(); j++) {
			byte[] sendData = new byte[64];
			sendData = str.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, list.get(j).GetIP(), list.get(j).GetPort());
			socket.send(sendPacket);
		}

	}
}	//end class

class ClientTableS {
	private String Name;
	private InetAddress IP;
	private int Port;
	
	public ClientTableS(String name, InetAddress ip, int port) {
		Name = name;
		IP = ip;
		Port = port;
	}	//end contructor
	
	public String GetName() {
		return Name;
	}	//end method
	
	public InetAddress GetIP() {
		return IP;
	}	//end method
	
	public int GetPort() {
		return Port;
	}	//end method
	
	public void SetIP(InetAddress ip) {
		IP = ip;
	}	//end method
	
	public void SetPort(int port) {
		Port = port;
	}	//end method
}	//end class


class ItemTable {
	private String ItemName;
	private String SellerName;
	private int StartBid;
	private int CurrBid;
	private int TransLimit;
	private int BuyNow;
	private String Desc;
	public boolean IsSoldOut;
	private String HighestBuyer;
	
	public ItemTable(String itemname, String sellername, int limit, int bid, int buynow, String desc) {
		ItemName = itemname;
		SellerName = sellername;
		TransLimit = limit;
		StartBid = bid;
		CurrBid = bid;
		BuyNow= buynow;
		Desc = desc;
		IsSoldOut = false;
	}	//end constructor
	
	public String PrintItem() {
		String str = ItemName + "/" + SellerName + "/" + Integer.toString(CurrBid) + "/" + Integer.toString(BuyNow) + "/" + Desc;
		return str;
	}	//end method
	
	public String GetHighestBuyer() {
		return HighestBuyer;
	}	//end method;
	
	public String GetSellerName() {
		return SellerName;
	}	//end method;
	
	public int GetTransLimit()  {
		return TransLimit;
	}	//end method;
	
	public String GetItemName() {
		return ItemName;
	}	//end method
	
	public int GetCurrBid() {
		return CurrBid;
	}	//end method
	
	public int GetBuyNow() {
		return BuyNow;
	}	//end method
	
	public void UpdateCurrBid(int bid) {
		CurrBid = CurrBid + bid;
	}
	
	public void UpdateHighestBuyer(String name) {
		HighestBuyer = name;
	}
	
	public void UpdateTransLimit() {
		TransLimit--;
	}
}	//end class

class OfflineMessage {
	private String UserName;
	private String Message;
	
	OfflineMessage(String name, String str) {
		UserName = name;
		Message = str;
	}
	
	public String GetName() {
		return UserName;
	}
	
	public String GetData() {
		return Message;
	}
	
}	//end class







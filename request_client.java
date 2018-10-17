import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;


public class request_client {
	static int pings = 0;
	static int total_pings = 5;
	final static int MAGIC = 15440;
	static int ping_port;
	public static void main(String[] args) {
		// args[0] --> server IPAddress
		// args[1] --> server port
		// args[2] --> request client IPAddress
		// args[3] --> request client port
		// args[4] --> hash
		try {
			InetAddress server_ip = InetAddress.getByName(args[0]);
			int server_port = Integer.parseInt(args[1]);
			DatagramSocket client_socket = new DatagramSocket();
			InetAddress client_ip = InetAddress.getByName(args[2]);
			
			boolean id_set = false;
			int client_id = 0;
			DatagramPacket receive_packet;
			
			int command = 8;
			while (true) {
				switch (command) {
				case 8:
					if (id_set == false){
						System.out.println("Initial client id: " + client_id);
						// Send join command to server
						send_packet(client_socket, server_ip, server_port, MAGIC +", "+ client_id +", " +Integer.toString(command)+ ", " + args[4]);
						
						DatagramSocket cSocket = client_socket;
						// Receive ACK JOB
						cSocket.setSoTimeout(5000);
						byte[] rAck = new byte[1024];
						byte[] rid = new byte[1024];
						int times_job_sent = 0;
						boolean server_responded = false;
						DatagramPacket receive_id_packet = new DatagramPacket(rid, rid.length);
						DatagramPacket receiveACK = new DatagramPacket(rAck, rAck.length);
						while (!server_responded && times_job_sent < 3) {
							try {
								// Receive server response ID
								cSocket.receive(receive_id_packet);
								String server_allocated_id =  new String(receive_id_packet.getData());
								client_id = Integer.parseInt(server_allocated_id.trim());
								System.out.println("ID given by server: " + client_id);
								// receive ack
								client_socket.receive(receiveACK);
								String server_ack = new String(receiveACK.getData()).trim();
								String[] split_msg = server_ack.split(" ");
								if (split_msg[0].equals("ACK")) {
									ping_port = Integer.parseInt(split_msg[1]);
									System.out.println(split_msg[0]);
								}
								server_responded = true;
							}
							catch (SocketTimeoutException e) {
								times_job_sent++;
								System.out.println("Number of times job sent: " + times_job_sent);
								
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						if (times_job_sent >= 3) {
							System.out.println("Server didn't give ACK: "+times_job_sent);
						}
						if (times_job_sent < 3) {
							byte[] receiveData = new byte[1024];

							Timer timer = new Timer();
							final DatagramSocket csocket = client_socket;
							final InetAddress ip = server_ip;
							final int port = ping_port;
							final int rclientID = client_id;
							final DatagramPacket rp = new DatagramPacket(receiveData, receiveData.length);

							timer.scheduleAtFixedRate(new TimerTask() {
								public void run() {
									send_packet(csocket, ip, port, MAGIC + ", " + rclientID + ", 0"); 
									check_if_server_dead(csocket, rp);
								}
							}, 0, 5000);
						}
						while(true){}
					} else {
						System.out.println("Client already has an id: " + client_id);
					}
					break;
				default:
					System.out.println("Invalid Input");
					break;
				}
					
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void check_if_server_dead(DatagramSocket client_socket, DatagramPacket receivePacket) {
		try {
			client_socket.setSoTimeout(5000);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		pings++;
		//System.out.println(pings);
		try {
			client_socket.receive(receivePacket);
			String new_port = new String(receivePacket.getData());
			//System.out.println("Data received from client: " + new_port);
			
			pings = 0;
		}
		catch (SocketTimeoutException e) {
			// no response received after 1 second. continue sending
			if (pings <= total_pings) {
				if (pings == 1) {
					System.out.println("No response recieved after: " + pings + " ping.");
				} else {
					System.out.println("No response recieved after: " + pings + " pings.");
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (pings == total_pings) {
			System.out.println("Server crashed...");
		}
	}
	
	
	public static void send_packet(DatagramSocket socket, InetAddress IP, int port, String message){
		byte[] send_data = new byte[1024];
		send_data = message.getBytes();
		DatagramPacket packet = 
				new DatagramPacket(send_data, send_data.length, IP, port);
		try {
			socket.send(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static DatagramPacket receive_packet(DatagramSocket socket) {
		byte[] receive_data = new byte[1024];
		DatagramPacket receive_packet = 
				new DatagramPacket(receive_data, receive_data.length);
		try {
			socket.receive(receive_packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return receive_packet;
	}
	
}

import java.io.*;
import java.net.*;

public class worker_client {
	static int pings_port;
	static InetAddress pings_port_ip;
	final static int MAGIC = 15440;
	public static String start_range;
	public static String status = "aynoor";
	static int assigned = 0;
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// args[0] --> server IPAddress
		// args[1] --> server port
		// args[2] --> worker client IPAddress
		// args[3] --> worker client port
		try {
			pings_port_ip = InetAddress.getByName("localhost");
			InetAddress server_ip = InetAddress.getByName(args[0]);
			int server_port = Integer.parseInt(args[1]);
			DatagramSocket client_socket = new DatagramSocket();
			InetAddress client_ip = InetAddress.getByName(args[2]);
			int worker_client_job_port = Integer.parseInt(args[4]);
			InetAddress worker_client_job_IP = InetAddress.getByName("localhost");
			// take input from user
			boolean id_set = false;
			int client_id = 0;
			int command = 1;
			String key_range_start = null;
			String key_range_end = null;
			String password_hash = null;
			DatagramPacket receive_packet;
			int pings = 0;
			while (true) {
				//String cases = user_input.readLine();
				switch (command) {
				case 0:		// send not done to server
					send_packet(client_socket, pings_port_ip, pings_port,  MAGIC +", "+ client_id +", 2, " + status);
					//send_packet(client_socket, worker_client_job_IP, worker_client_job_port,  MAGIC +", "+ client_id +", 0");
					
					break;
				case 1: 	// get id from server
					if (id_set == false){
						System.out.println("Initial client id: " + client_id);
						// Send join command to server
						command = 1;
						send_packet(client_socket, server_ip, server_port,  MAGIC +", "+ client_id +", " +Integer.toString(command) + ", "+ worker_client_job_port);

						// Receive server response
						receive_packet = receive_packet(client_socket);
						String server_meg =  new String(receive_packet.getData()).trim();
						String[] splitted_client_msg = server_meg.split(" ");
						String server_allocated_id = splitted_client_msg[0];
						pings_port = Integer.parseInt(splitted_client_msg[1]);
						client_id = Integer.parseInt(server_allocated_id);
						System.out.println("ID given by server: " + client_id);
						id_set = true;
					} else {
						System.out.println("Client already has an id: " + client_id);
					}
					break;
				case 2:		// receive ob from server
					System.out.println("Job receives: " + key_range_start + " " + key_range_end +  " " + password_hash);
					//send_packet(client_socket, pings_port_ip, pings_port,  MAGIC +", "+ client_id +", 7");
					if (assigned == 1) {
						worker_client_job job_thread = new worker_client_job(key_range_start, key_range_end, password_hash, worker_client_job_port);;
						job_thread.start();
						assigned++;
					}
					/*int worker_client_index = get_worker_client_index(client_id);
					if (server.worker_clients.get(worker_client_index).job_assigned == true) {
						worker_client_job job_thread = new worker_client_job(key_range_start, key_range_end, password_hash, server.worker_clients.get(worker_client_index).woker_client_job_port);;
						job_thread.start();
					}*/
					break;
				default:
					System.out.println("Invalid Input");
					break;
				}
				// Receive data from client
				DatagramPacket server_msg_packet = receive_packet(client_socket);
				String client_msg = new String(server_msg_packet.getData()).trim();;
				String[] splitted_client_msg = client_msg.split("\\s*,\\s*");		// split on comma and space
				System.out.println(client_msg);
				command = Integer.parseInt(splitted_client_msg[2]);
				if (command == 2) {
					key_range_start = splitted_client_msg[3];
					key_range_end = splitted_client_msg[4];
					password_hash = splitted_client_msg[5];
					send_packet(client_socket, pings_port_ip, pings_port,  MAGIC +", "+ client_id +", 5, " + "ACK");
					assigned++;
				}
				System.out.println(command);	
			}
			
		} catch (Exception e) {
			e.printStackTrace();
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
			//String client_msg = new String(receive_packet.getData());
			String server_msg = new String(receive_packet.getData());
			System.out.println("Message received from server: " + server_msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return receive_packet;
	}

	
	public static int get_worker_client_index(int id) {
		for (int i = 0; i < server.worker_clients.size(); i++) {
			if (server.worker_clients.get(i) != null) {
				if (id == server.worker_clients.get(i).id) {
					return i;
				}
			}
		}
		return -1;
	}
	
}

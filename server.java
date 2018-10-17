import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class server {
	
	public static List<worker_client_info> worker_clients = new ArrayList<worker_client_info>();
	public static List<request_client_info> request_clients = new ArrayList<request_client_info>();
	public final static int MAGIC = 15440;
	public final static int PASSWORD_LENGTH = 5;
	
	static InetAddress pings_thread_IP;
	static int pings_thread_port;
	
	public static void main(String[] args) {
		// args[0]   -->   server port
		// args[1]   -->   pings thread port
		int server_port = Integer.parseInt(args[0]);
		pings_thread_port = Integer.parseInt(args[1]);
		try {
			pings_thread_IP = InetAddress.getByName("localhost");
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// Start a thread to listen to pings
		server_handle_pings pings_thread = new server_handle_pings(pings_thread_port);
		pings_thread.start();
		
		try {
			DatagramSocket server_socket = new DatagramSocket(server_port);
			int command; 
			give_job(server_socket);
			while (true) {
				DatagramPacket client_msg_packet = receive_packet(server_socket);
				String client_msg = new String(client_msg_packet.getData()).trim();;
				String[] splitted_client_msg = client_msg.split("\\s*,\\s*");		// split on comma and space
				command = Integer.parseInt(splitted_client_msg[2]);

				switch(command) {
				case 1:
					int client_id = generate_id();
					while (check_if_duplicate_id(client_id)) {
						client_id = generate_id();
					}
					int worker_client_job_port = Integer.parseInt(splitted_client_msg[3]);
					System.out.println("Client id generated: " + client_id);
					// Send message to connected client
					InetAddress connected_client_ip1 = client_msg_packet.getAddress();
					int connected_client_port1 = client_msg_packet.getPort();
					send_packet(server_socket, connected_client_ip1, connected_client_port1, Integer.toString(client_id) + " " + Integer.toString(pings_thread_port));
					server.worker_clients.add(new worker_client_info(client_id, connected_client_ip1, connected_client_port1, worker_client_job_port));
					System.out.println("worker_clients length: " + server.worker_clients.size());
					
					break;
				case 8:
					int r_client_id = generate_id();
					while (check_if_duplicate_id(r_client_id)) {
						r_client_id = generate_id();
					}
					System.out.println("Request client id generated: " + r_client_id);
					// Send message to connected client
					InetAddress connected_client_ip8 = client_msg_packet.getAddress();
					int connected_client_port8 = client_msg_packet.getPort();
					send_packet(server_socket, connected_client_ip8, connected_client_port8, Integer.toString(r_client_id));
					server.request_clients.add(new request_client_info(r_client_id, splitted_client_msg[3], pings_thread_port));
					send_packet(server_socket, connected_client_ip8, connected_client_port8, "ACK " + pings_thread_port);
					break;
				default:
					System.out.println("Invalid input: " + command);
					break;	
				}
			}
			
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
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
			String client_msg = new String(receive_packet.getData());
			//System.out.println("Message received from client: " + client_msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return receive_packet;
	}
	
	public static int generate_id() {
		Random rand = new Random();
		return rand.nextInt(100);
	}
	
	public static boolean check_if_duplicate_id(int id) {
		for (int i = 0; i < server.worker_clients.size(); i++) {
			if (server.worker_clients.get(i) != null) {
				if (id == server.worker_clients.get(i).id) {
					return true;
				}
			}
		}
		for (int i = 0; i < server.request_clients.size(); i++) {
			if (server.request_clients.get(i) != null) {
				if (id == server.request_clients.get(i).id) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static String get_job() {
		for(int i = 0; i < server.request_clients.size(); i++) {
			if (server.request_clients.get(i) != null) {
				if (server.request_clients.get(i).job_done == false) {
					for(int k = 0; k< server.request_clients.get(i).hashRanges.size(); k++) {
						if (server.request_clients.get(i).hashRanges.get(k).assigned == false) {
							return server.request_clients.get(i).hashRanges.get(k).hash_range + " " + i + " " + k;
						}
					}
				}
			}
		}
		return null;
	}
	
	public static void give_job(DatagramSocket server_socket) {
		Timer timer = new Timer();
		final DatagramSocket ssocket = server_socket;
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				assign_job(ssocket);
			}
		}, 0, 5000);
	}
	
	public static void assign_job(final DatagramSocket server_socket) {
		String job_info = get_job();
		if (job_info != null) {
			String[] temp_array = job_info.split(" ");	// temp_array[0] = key_range_start, temp_array[1] = key_range_end, temp_array[2] = request_client_index, temp_array[3] = hash_of_range_index 
			String krs = "";
			String kre = "";
			for (int i = 0; i < server.PASSWORD_LENGTH; i++) {
				krs = krs + temp_array[0];
				kre = kre + temp_array[1];
			}
			final String password_to_crack = server.request_clients.get(Integer.parseInt(temp_array[2])).password_hash;
			
			System.out.println("JOB: " + krs + " " +kre + " " + password_to_crack);
			
			final String key_range_start = krs;
			final String key_range_end = kre;
			final worker_client_info free_worker_client = get_free_worker_client();
			if (free_worker_client != null) {
				final int worker_client_index = get_free_worker_client_index(free_worker_client);
				server.worker_clients.get(worker_client_index).job_assigned = true;
				final int job_index = Integer.parseInt(temp_array[3]);
				final int request_client_index = Integer.parseInt(temp_array[2]);

				server.request_clients.get(Integer.parseInt(temp_array[2])).hashRanges.get(Integer.parseInt(temp_array[3])).assigned = true;
				send_packet(server_socket, pings_thread_IP, pings_thread_port, server.MAGIC + ", " + free_worker_client.id + ", " + 8 + ", " + key_range_start + ", " + key_range_end + ", " +worker_client_index + ", " + job_index + ", " + request_client_index);
				
			}
		}
	}
		
	
	public static worker_client_info get_free_worker_client() {
		for (int i = 0; i < server.worker_clients.size(); i++) {
			if (server.worker_clients.get(i) != null) {
				if (server.worker_clients.get(i).job_assigned == false) {
					return server.worker_clients.get(i);
				}
			}
		}
		return null;
	}
	
	public static int get_free_worker_client_index(worker_client_info w) {
		for (int i = 0; i < server.worker_clients.size(); i++) {
			if (server.worker_clients.get(i) != null) {
				if (w.id == server.worker_clients.get(i).id) {
					return i;
				}
			}
		}
		return -1;	// no worker_client_found
	}
}

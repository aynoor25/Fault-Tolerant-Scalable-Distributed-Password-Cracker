import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;


public class server_handle_pings implements Runnable {
	private Thread _t;
	DatagramSocket socket;
	int port;
	InetAddress socket_ip;
	server_handle_pings(int p) {
		port = p;
		try {
			socket_ip = InetAddress.getByName("localhost");
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void start () {
		if (_t == null) {
			_t = new Thread (this, Integer.toString(socket.getLocalPort()));
			_t.start ();
		}
	}
	// case 5 worker_client_ack
	// case 8 ping worker_client
	// case 0 monitor request client pings
	// case 1 send pings to worker
	
	public void run() {
		int command = 100;		// default coomand
		while (true) {
			DatagramPacket client_msg_packet = receive_packet(socket);
			String client_msg = new String(client_msg_packet.getData()).trim();;
			String[] splitted_client_msg = client_msg.split("\\s*,\\s*");		// split on comma and space
			int client_id = Integer.parseInt(splitted_client_msg[1]);
			command = Integer.parseInt(splitted_client_msg[2]);
			switch(command) {
			case 0:				// case 0 monitor request client pings
				// Send message to connected client
				int requested_client_id = Integer.parseInt(splitted_client_msg[1]);
				InetAddress connected_client_ip0 = client_msg_packet.getAddress();
				int connected_client_port0 = client_msg_packet.getPort();
				send_packet(socket, connected_client_ip0, connected_client_port0, "Alive");
				
				if (if_request_client_alive(requested_client_id)) {
					int requestClientIndex = get_request_client(requested_client_id);
					server.request_clients.get(requestClientIndex).pings_send = true;
					if (server.request_clients.get(requestClientIndex).pings_to_server == 0) {
						server.request_clients.get(requestClientIndex).pings_to_server++;
						final int start_timer_id =  requested_client_id;
						final int index = requestClientIndex;
						server.request_clients.get(index).timer.scheduleAtFixedRate(new TimerTask() {

							public void run() {
								boolean check = get_pings(index);
								if (check == true) {
									System.out.println("Request client is alive: " + start_timer_id);
									update_pings(index);
									check = get_pings(index);
								} else {
									System.out.println("Request client is dead: " +start_timer_id);
									server.request_clients.get(index).timer.cancel();
									server.request_clients.set(index, null);
									//GlobalVariables.request_clients.remove(index);
								}
							}
						}, 0, 15000);

					}
				}
				break;
			case 1:		// send pings to worker client
				final int worker_client_index1 = Integer.parseInt(splitted_client_msg[3]);
				if (server.worker_clients.get(worker_client_index1).if_ACK == true) {
					final Timer t = new Timer();
					t.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							if (server.worker_clients.get(worker_client_index1) !=  null) {
								if (server.worker_clients.get(worker_client_index1).pings >= 3) {
									t.cancel();
									System.out.println("Worker client: " + server.worker_clients.get(worker_client_index1).id + " has crashed");
									server.worker_clients.set(worker_client_index1, null);				// set worker client to null
								} else {
									send_packet(socket, server.worker_clients.get(worker_client_index1).inet_address, 
											server.worker_clients.get(worker_client_index1).port,
											server.MAGIC +", "+ server.worker_clients.get(worker_client_index1).id + ", " +0 );
									server.worker_clients.get(worker_client_index1).pings++;
								}
							}
						}
					}, 0, 3000);
				}
				break;
			case 2:		// check pings reply from worker
				int client_index = get_worker_client_index(client_id);
				String client_job_update = splitted_client_msg[3];
				if (client_job_update.equals("NOT_DONE")) {
					server.worker_clients.get(client_index).pings = 0;
					System.out.println("Job update from client " + server.worker_clients.get(client_index).id + " " + client_job_update);
				}
				break;
			case 5:		// worker client ack
				String job_ack = splitted_client_msg[3];
				if (job_ack.equals("ACK")) {
					set_ack_true(client_id);
				}
				System.out.println("ACK received by client: " + client_id);
				break;
			case 8:		// case 8 send ack to  worker_client
				final String key_range_start = splitted_client_msg[3];
				final String key_range_end = splitted_client_msg[4];
				final int worker_client_index = Integer.parseInt(splitted_client_msg[5]);
				final int job_index  = Integer.parseInt(splitted_client_msg[6]);
				final int request_client_index = Integer.parseInt(splitted_client_msg[7]);
				final worker_client_info free_worker_client = get_free_worker_client(worker_client_index);
				final byte[] rp_array = new byte[1024];
				final DatagramPacket receive_packet = new DatagramPacket(rp_array, rp_array.length);
				final Timer t = new Timer();
				final String password_hash = server.request_clients.get(request_client_index).password_hash;
				//System.out.println("message received: " + client_msg);
				if (free_worker_client != null) {
					final int total_pings = 3;
					t.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							if (server.worker_clients.get(worker_client_index) != null) {
								if (server.worker_clients.get(worker_client_index).pings >= total_pings) {
									t.cancel();
									server.worker_clients.set(worker_client_index, null);	// remove crashes worker client from array
									System.out.println("Worker client: " + server.worker_clients.get(worker_client_index).id + " crashed.");
								}
								if (server.worker_clients.get(worker_client_index).if_ACK == false) {
									send_packet(socket, free_worker_client.inet_address, free_worker_client.port,
											server.MAGIC +", "+ free_worker_client.id + ", " +Integer.toString(2)+ ", " +
													key_range_start + ", " + key_range_end + ", " + password_hash);
									server.worker_clients.get(worker_client_index).pings++;
								} else {
										//	GlobalVariables.MAGIC +", "+ free_worker_client.id + ", 0");
									server.worker_clients.get(worker_client_index).pings = 0;
									System.out.println("Job acknowledged by: " + server.worker_clients.get(worker_client_index).id);
									// add the job info in worker client array
									server.worker_clients.get(worker_client_index).key_range_start = key_range_start;
									server.worker_clients.get(worker_client_index).key_range_end = key_range_end;
									server.worker_clients.get(worker_client_index).password_hash = password_hash;
									send_packet(socket, socket_ip, port, server.MAGIC + ", " + free_worker_client.id + ", " + 1 + ", " +worker_client_index);
									
									t.cancel();
								}
							} else {
								t.cancel();
							}
						}
					}, 0, 3000);
				}
				break;
			default:
				System.out.println("server_handle_pings invalid input: " + command);
				break;
			}
		}
	}

	public static void if_job_ack(DatagramSocket socket, int client_index) {
		try {
			socket.setSoTimeout(3000);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			if (server.worker_clients.get(client_index).if_ACK == true) {
				System.out.println("Job acknowledged by: " + client_index);
			}
		} catch (Exception e) {
			
		}
	}
	
	
	public static void check_if_worker_client_dead(DatagramSocket socket, DatagramPacket receivePacket, int client_index, int job_index, int reques_client_index) {
		int total_pings = 3;
		try {
			socket.setSoTimeout(3000);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		server.worker_clients.get(client_index).pings++;
		//System.out.println(pings);
		try {
			socket.receive(receivePacket);
			String client_msg = new String(receivePacket.getData()).trim();
			//System.out.println("Data received from client: " + new_port);
			String[] splitted_client_msg = client_msg.split("\\s*,\\s*");
			if (splitted_client_msg[2].equals("W")) {
				System.out.println(splitted_client_msg[4]);
				if (splitted_client_msg[4].equals("ACK")) {
					server.worker_clients.get(client_index).if_ACK = true;
				}
			}
			server.worker_clients.get(client_index).pings = 0;
		}
		catch (SocketTimeoutException e) {
			// no response received after 1 second. continue sending
			if (server.worker_clients.get(client_index).pings <= total_pings) {
				if (server.worker_clients.get(client_index).pings == 1) {
					System.out.println("No response from worker client " + server.worker_clients.get(client_index).id + " recieved after: " + server.worker_clients.get(client_index).pings + " ping.");
				} else {
					System.out.println("No response from worker client " + server.worker_clients.get(client_index).id + " recieved after: " + server.worker_clients.get(client_index).pings + " pings.");
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (server.worker_clients.get(client_index).pings == total_pings) {
			System.out.println("Worker client crashed...");
			server.worker_clients.set(client_index, null);
			server.request_clients.get(reques_client_index).hashRanges.get(job_index).assigned = false;
		}
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

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
	// check null?
	public static boolean get_pings(int index) {
		return server.request_clients.get(index).pings_send;
	}
	// check null?
	public static void update_pings(int index) {
		server.request_clients.get(index).pings_send = false;
	}

	public static int get_request_client(int client_id) {
		for(int i = 0; i < server.request_clients.size(); i++) { 
			if (server.request_clients.get(i) != null) {
				if (server.request_clients.get(i).id == client_id) {
					return i;
				}
			}
		}
		return 100;
	}
	
	public static boolean if_request_client_alive(int client_id) {
		for(int i = 0; i < server.request_clients.size(); i++) { 
			if (server.request_clients.get(i) != null) {
				if (server.request_clients.get(i).id == client_id) {
					return true;
				}
			}
		}
		return false;
	}
	public static worker_client_info get_free_worker_client(int worker_client_index) {
		for (int i = 0; i < server.worker_clients.size(); i++) {
			if (server.worker_clients.get(i) != null) {
				if (server.worker_clients.get(worker_client_index).id == server.worker_clients.get(i).id) {
					return server.worker_clients.get(i);
				}
			}
		}
		return null;
	}
	
	public static void set_ack_true(int id) {
		for (int i = 0; i < server.worker_clients.size(); i++) {
			if (server.worker_clients.get(i) != null) {
				if (id == server.worker_clients.get(i).id) {
					server.worker_clients.get(i).if_ACK = true;
				}
			}
		}
	}
	
	public static int get_worker_client_index(int id) {
		for (int i = 0; i < server.worker_clients.size(); i++) {
			if (server.worker_clients.get(i) != null) {
				if (id == server.worker_clients.get(i).id) {
					return i;
				}
			}
		}
		return 500;	// no worker_client_found
	}
	
	public static int get_free_worker_client_index(worker_client_info w) {
		for (int i = 0; i < server.worker_clients.size(); i++) {
			if (server.worker_clients.get(i) != null) {
				if (w.id == server.worker_clients.get(i).id) {
					return i;
				}
			}
		}
		return 500;	// no worker_client_found
	}
}

import java.net.DatagramSocket;
import java.net.InetAddress;


public class worker_client_info {

	int id;
	String key_range_start;
	String key_range_end;
	String password_hash;
	InetAddress inet_address;
	int port;
	boolean job_assigned;
	boolean job_done ;
	boolean if_ACK;
	int pings;
	int woker_client_job_port;
	worker_client_info(int _id, InetAddress ia, int p, int wp) {
		id = _id;
		key_range_start = null;
		key_range_end = null;
		password_hash = null;
		inet_address = ia;
		port = p;
		job_assigned = false;
		job_done = false;
		pings = 0;
		if_ACK = false;
		woker_client_job_port = wp;
	}
	
	public void print_details() {
		System.out.println("Client_ID: " + id + ", Key_Range_Start: " + key_range_start +
				", Key_Range_End: " + key_range_end + ", Password hash: " + password_hash +
				", Port: " + port);
	}
	
	public void set_key_range_start(String key_range) {
		key_range_start = key_range;
	}
	
	public void set_key_range_end(String key_range) {
		key_range_end = key_range;
	}
	
	public void set_password(String password) {
		password_hash = password;
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

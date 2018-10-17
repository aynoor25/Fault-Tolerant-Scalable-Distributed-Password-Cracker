import java.util.ArrayList;
import java.util.List;
import java.util.Timer;


public class request_client_info {

	int id;
	String password_hash;
	boolean job_done;
	boolean assigned;
	int pings_received;
	int pings_to_server;
	boolean pings_send;
	Timer timer;
	List<hash_ranges> hashRanges = new ArrayList<hash_ranges>();
	int ping_port;
	request_client_info(int _id, String hash, int pp) {
		id = _id;
		password_hash = hash;
		job_done = false;
		assigned = false;
		pings_received = 0;
		pings_to_server = 0;
		pings_send = false;
		timer = new Timer();
		hashRanges.add(new hash_ranges("a c"));
		hashRanges.add(new hash_ranges("d f"));
		hashRanges.add(new hash_ranges("g i"));
		hashRanges.add(new hash_ranges("j l"));
		hashRanges.add(new hash_ranges("m o"));
		hashRanges.add(new hash_ranges("p r"));
		hashRanges.add(new hash_ranges("s u"));
		hashRanges.add(new hash_ranges("v x"));
		hashRanges.add(new hash_ranges("y z"));
		ping_port = pp;
	}

	public void print_details() {
		System.out.println("Client_ID: " + id + ", Password hash: " + password_hash);
	}
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

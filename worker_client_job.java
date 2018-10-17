import java.util.Timer;


public class worker_client_job implements Runnable {
	Thread _t;
	String key_range_start;
	String key_range_end;
	String password_hash;
	int port;
	
	worker_client_job(String krs, String kre, String pass, int p) {
		key_range_start = krs;
		key_range_end = kre;
		password_hash = pass;
		port = p;
	}
	
	public void start () {
		if (_t == null) {
			_t = new Thread (this, Integer.toString(port));
			_t.start ();
		}
	}
	
	public void run() {
		worker_client.status = "NOT_DONE";
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

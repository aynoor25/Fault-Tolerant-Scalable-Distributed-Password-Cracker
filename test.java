import java.net.InetAddress;
import java.net.UnknownHostException;


public class test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		InetAddress a = null;
		try {
			a = InetAddress.getByName("localhost");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*GlobalVariables.worker_clients.add(new worker_client_info(1,a, 1));
		GlobalVariables.worker_clients.add(new worker_client_info(2,a, 2));
		GlobalVariables.worker_clients.add(new worker_client_info(3,a, 3));
		
		GlobalVariables.worker_clients.remove(1);
		GlobalVariables.print_worker_client_details();*/

	}
	
	
}

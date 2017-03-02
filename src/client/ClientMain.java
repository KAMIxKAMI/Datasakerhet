package client;

public class ClientMain {

	public static void main(String[] args) {
		Client client = new Client();
		try {
			client.init();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
package depskys.clouds.drivers.localStorageService;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 
 * @author tiago oliveira
 *
 */
public class ServerThread {

	public static void main(String[] args) {
		
		try {
			@SuppressWarnings("resource")
			ServerSocket serverSocket = new ServerSocket(5555);
			Socket socket;
			int cond = 0;
			System.out.println("Waiting requests....");
			while(true){
				socket = serverSocket.accept();
				CloudThread cloud = new CloudThread(socket);
				cloud.start();
				cond++;
				System.out.println("Accept cloud " + cond);
			}			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

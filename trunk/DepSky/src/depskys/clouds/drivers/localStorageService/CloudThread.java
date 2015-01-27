package depskys.clouds.drivers.localStorageService;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;

import message.Message;
import exceptions.StorageCloudException;


public class CloudThread extends Thread{

	private Socket socket;
	private ObjectInputStream in;
	private ObjectOutputStream out;

	public CloudThread(Socket client){

		this.socket = client;

		try {
			this.in = new ObjectInputStream(socket.getInputStream());
			// Enable auto-flush:
			this.out = new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	public void run(){

		String op = "";
		LinkedList<String> args = new LinkedList<String>();
		byte[] arrayW;
		CloudStorage localDriver = null;

		while(true){
			try {
				String cloud = new String();
				Message message = (Message) in.readObject();
				op = (String) message.getOp();
				if(message.getArgs()!=null)
					args = (LinkedList<String>) message.getArgs();
				String res;
				Message msgErro = new Message("Erro na cloud", null, null);
				Message msgOK = new Message("ok", null, null);

				try {
					if(message.getOp().equals("init")){
						cloud = args.get(0);
						localDriver = new CloudStorage(args.get(0));

						out.writeObject(msgOK);

					}else if(message.getOp().equals("uploadData")){
						arrayW = (byte[]) message.getBytes();
						res = localDriver.uploadData(args.get(0), arrayW, args.get(1), null);
						if(res != null){ 
							out.writeObject(msgOK);
						}else{
							out.writeObject(msgErro);
						}

					}else if(message.getOp().equals("downloadData")){
						arrayW = localDriver.downloadData(args.get(0), args.get(1), null);
						if(arrayW != null){
							msgOK = new Message("ok", null, arrayW);
							out.writeObject(msgOK);
						}else{
							out.writeObject(msgErro);
						}	
					}else if(message.getOp().equals("deleteData")){
						boolean bool = localDriver.deleteData(args.get(0), args.get(1), null);
						if(bool){
							out.writeObject(msgOK);
						}else{
							out.writeObject(msgErro);
						}
					}else if(message.getOp().equals("listNames")){
						LinkedList<String> names = localDriver.listNames("", "", null);
						if(names != null){
							Message msgOK1 = new Message("ok", names, null);
							out.writeObject(msgOK1);
						}else{
							out.writeObject(msgErro);
						}

					}else if(message.getOp().equals("deleteContainer")){
						String[] names = new String[args.size()];
						int p = 0;
						for(String str : args){
							names[p] = str;
							p++;
						}
						boolean bool = localDriver.deleteContainer("", names, null);
						if(bool){
							out.writeObject(msgOK);
						}else{
							out.writeObject(msgErro);
						}

					}
					//out.flush();
				} catch (StorageCloudException e) {
					out.writeObject(msgErro);
					//System.out.println("Cloud Error");
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				try {
					socket.close();
					in.close();
					out.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
}

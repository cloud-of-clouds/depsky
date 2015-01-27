package depskys.clouds.drivers;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;

import message.Message;
import depskyDep.IDepSkySDriver;
import exceptions.StorageCloudException;

/**
 *
 * @author koras
 */
public class LocalDiskDriver implements IDepSkySDriver {

	private String mainPath;
	private String staticpath = "ds-local" + File.separator;
	private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private Message msg;
	private LinkedList<String> args;

	public LocalDiskDriver() {
	}

	public LocalDiskDriver(String driverpath) {
		try {
			socket = new Socket("127.0.0.1", 5555);
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());

			args = new LinkedList<String>();
			args.add(driverpath);
			msg = new Message("init", args, null);
			out.writeObject(msg);
			out.reset();
			try {
				msg = (Message) in.readObject();
				if(msg.getOp().equals("ok")){
					//System.out.println("Cloud iniciada!");
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mainPath = driverpath;
	}

	public String uploadData(String bucket, byte[] data, String id, String[] uploadToAnotherAccount) throws StorageCloudException {
		try {
			args.clear();
			args.add(bucket);
			args.add(id);
			Message msg = new Message("uploadData", args, data);
			out.writeObject(msg);
			out.reset();
			try {
				msg = (Message) in.readObject();
				if(msg.getOp().equals("ok")){
					//System.out.println("Upload executado com sucesso");
				}else{
					//System.out.println("Upload com problemas");
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			return id;
		} catch (IOException iOException) {
			throw new StorageCloudException(iOException.getMessage());
		}
	}

	public byte[] downloadData(String bucket, String id, String[] uploadToAnotherAccount) throws StorageCloudException {
		try {

			args.clear();
			args.add(bucket);
			args.add(id);
			Message msg = new Message("downloadData", args, null);
			out.writeObject(msg);
			out.reset();
			try {
				msg = (Message) in.readObject();
				if(msg.getOp().equals("ok") && msg.getBytes() != null){
					//System.out.println("Download executado com sucesso");
					return msg.getBytes();
				}else{
					//System.out.println("download com problemas");
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			return null;

		} catch (Exception e) {
			throw new StorageCloudException(e.getMessage());
		}
	}

	public boolean deleteData(String bucket, String id, String[] uploadToAnotherAccount) throws StorageCloudException {

		try{
			args.clear();
			args.add(bucket);
			args.add(id);
			Message msg = new Message("deleteData", args, null);
			out.writeObject(msg);
			out.reset();
			try {
				msg = (Message) in.readObject();
				if(msg.getOp().equals("ok")){
					//System.out.println("Download executado com sucesso");
					return true;
				}else{
					//System.out.println("download com problemas");
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return false;
		} catch (Exception e) {
			throw new StorageCloudException(e.getMessage());
		}
	}

	public LinkedList<String> listNames(String prefix, String sid, String[] uploadToAnotherAccount) throws StorageCloudException{

		try{
			args.clear();
			args.add("");
			args.add("");
			args.add("");
			Message msg = new Message("listNames", null, null);
			out.writeObject(msg);
			out.reset();
			try {
				msg = (Message) in.readObject();
				if(msg.getOp().equals("ok")){
					//System.out.println("Download executado com sucesso");
					LinkedList<String> allNames = msg.getArgs();
					LinkedList<String> find = new LinkedList<String>();
					for(String str : allNames){
						if(str.contains(prefix)){
							find.add(str);
						}

					}
					return find;
				}else{
					System.out.println("list com problemas");
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			throw new StorageCloudException(e.getMessage());
		}
		return null;
	}


	public boolean deleteContainer(String bucket, String[] allNames, String[] uploadToAnotherAccount) throws StorageCloudException {
		try{
			args.clear();
			//args.add(sid);
			//args.add(cid);
			for(String str : allNames){
				args.add(str);
			}
			Message msg = new Message("deleteContainer", args, null);
			out.writeObject(msg);
			out.reset();
			try {
				msg = (Message) in.readObject();
				if(msg.getOp().equals("ok")){
					//System.out.println("Delete executado com sucesso");
					return true;
				}else{
					//System.out.println("delete com problemas");
					throw new StorageCloudException("Container creation failed... maybe container already exist.");
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return false;
		} catch (Exception e) {
			throw new StorageCloudException(e.getMessage());
		}
	}

	public String initSession() throws StorageCloudException {
		return mainPath;
	}

	public boolean endSession(String sid) throws StorageCloudException {
		return true;
	}

	public String getDriverId() {
		return mainPath;
	}

	public String getSessionKey() {
		return "sid";
	}

	@Override
	public String[] setAcl(String bucket, String[] canonicalId, String permission) throws StorageCloudException {
		// TODO Auto-generated method stub
		return null;
	}


}
package rackazure;

import static com.google.common.io.Closeables.closeQuietly;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Properties;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.io.Payload;

import depskyDep.IDepSkySDriver;
import depskyDep.StorageCloudException;

/**
 * Class that interact directly with windows azure api 
 * @author tiago oliveira
 *
 */
public class WindowsAzureDriver implements IDepSkySDriver{

	private String driverId;
	private String accessKey;
	private String secretKey;
	private String session_key;
	private String bucketname = "depskys";
	
	private BlobStore storage;


	public WindowsAzureDriver(String driverId, String accessKey, String secretKey){
		this.driverId = driverId;
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		try {
			getBucketName();
		} catch (FileNotFoundException e) {
			System.out.println("Problem with bucket_name.properties file!");
			//e.printStackTrace();
		}
	}

	public String initSession() throws StorageCloudException {
		try{
		BlobStoreContext context = (BlobStoreContext) ContextBuilder.newBuilder("azureblob")
				.credentials(accessKey, secretKey)
				.buildView(BlobStoreContext.class);
		storage = context.getBlobStore();
		storage.createContainerInLocation(null, bucketname);
		}catch(Exception e){
			System.out.println("Cannot connect with Windows Azure.");
			//e.printStackTrace();
			throw new StorageCloudException(StorageCloudException.INVALID_SESSION);
		}
		session_key = "sid";		
		return "sid";
	}

	public String uploadData(String sid, String cid, byte[] data, String id) {
		
		Blob blob = storage.blobBuilder(id).payload(data).build();
		blob.setPayload(data);
		storage.putBlob(bucketname, blob);		
		return id;

	}

	public byte[] downloadData(String sid, String cid, String id)  throws StorageCloudException {
		Blob bl = storage.getBlob(bucketname, id);
		Payload b = bl.getPayload();
		InputStream in = b.getInput();
		byte[] array = null;
		
		try {
			array = getBytesFromInputStream(in);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(array == null){
			throw new StorageCloudException("AWSS3Exception:: download data");
		}
		return array;
	}

	@Override
	public boolean deleteData(String sid, String cid, String id) {
		storage.removeBlob(bucketname, id);
		return true;
	}

	//TODO: implement this method (will be used in the lock operation)
	public LinkedList<String> listNames(String prefix) throws StorageCloudException{
		return null;
	}
	
	public boolean deleteContainer(String sid, String[] allNames) {

		for(String str : allNames){
			storage.removeBlob(bucketname, str);
		}
		return true;
	}

	public boolean endSession(String sid) {
		closeQuietly(storage.getContext());
		return true;
	}

	public String getDriverId() {
		return driverId;
	}

	public String getSessionKey() {
		return session_key;
	}

	@Override
	public String getDataIdByName(String sid, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getContainerAndDataIDsByName(String sid, String cid, String id)  throws StorageCloudException{

		if(storage.blobExists(cid, id)){		
			return new String[]{cid, id};
		}else{
			throw new StorageCloudException("WAException:: getContByName");
		}
	}
	
	private static byte[] getBytesFromInputStream(InputStream is)
			throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];

		while ((nRead = is.read(data, 0, data.length)) != -1) {
		  buffer.write(data, 0, nRead);
		}

		buffer.flush();

		return buffer.toByteArray();

	}

	private void getBucketName() throws FileNotFoundException{

		String path = "config" + File.separator + "bucket_name.properties";
		FileInputStream fis;
		try {
			fis = new FileInputStream(path);
			Properties props = new Properties();  
			props.load(fis);  
			fis.close();  
			String name = props.getProperty("bucketname");
			if(name.length() == 0){
				char[] randname = new char[10];
				for(int i = 0; i < 10; i++){
					char rand = (char)(Math.random() * 26 + 'a');
					randname[i] = rand;
				}
				bucketname = bucketname.concat(new String(randname));
				props.setProperty("bucketname", bucketname);
				props.store(new FileOutputStream(path),"change");
			}else{
				bucketname = name;
			}
				
		}catch(IOException e){  
			e.printStackTrace();  
		} 
	}
	
	//not suported
	public boolean setAcl(String arg0, String arg1, String arg2, String arg3)
			throws StorageCloudException {
		// TODO Auto-generated method stub
		return false;
	}

}

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
import org.jclouds.cloudfiles.options.ListCdnContainerOptions;
import org.jclouds.io.Payload;
import org.jclouds.openstack.swift.CommonSwiftAsyncClient;
import org.jclouds.openstack.swift.CommonSwiftClient;
import org.jclouds.openstack.swift.domain.SwiftObject;
import org.jclouds.rest.RestContext;

import depskyDep.IDepSkySDriver;
import depskyDep.StorageCloudException;

/**
 * Class that interact directly with rackspace files api 
 * @author tiago oliveira
 *
 */
public class RackSpaceDriver implements IDepSkySDriver{

	private String driverId;
	private BlobStore storage;
	private String session_key;
	private RestContext<CommonSwiftClient, CommonSwiftAsyncClient> swift;
	private String accessKey;
	private String secretKey;
	private String bucketName = "depskys";

	public RackSpaceDriver(String driverId, String accessKey, String secretKey){
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
	
	//TODO: implement this method (will be used in the lock operation)
	public LinkedList<String> listNames(String prefix) throws StorageCloudException{
		return null;
	}

	public boolean deleteContainer(String sid, String[] allNames) throws StorageCloudException {

		for(String str : allNames){
			swift.getApi().removeObject(bucketName, str);
		}
		return true;
	}

	public boolean deleteData(String sid, String cid, String id)
			throws StorageCloudException {

		swift.getApi().removeObject(bucketName, id);
		return true;
	}

	public byte[] downloadData(String sid, String cid, String id) throws StorageCloudException {
		SwiftObject object = null;
		if(swift.getApi().objectExists(bucketName, id)){
			object = swift.getApi().getObject(bucketName, id);

			Payload pay = object.getPayload();
			InputStream in = pay.getInput();
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
		}else{
			return null;
		}
	}

	public boolean endSession(String arg0) throws StorageCloudException {
		closeQuietly(storage.getContext());
		return true;
	}

	public String[] getContainerAndDataIDsByName(String arg0, String cid, String id) throws StorageCloudException {

		if(swift.getApi().objectExists(cid, id)){
			return new String[]{cid, id};
		}else{
			throw new StorageCloudException("RSException:: getContByName");
		}		
	}

	public String getDataIdByName(String arg0, String arg1)
			throws StorageCloudException {
		return null;
	}

	public String getDriverId(){
		return driverId;
	}

	public String getSessionKey() {
		return session_key;
	}

	public String initSession() throws StorageCloudException {

		try{
			String location = "cloudfiles-uk";
			BlobStoreContext context = (BlobStoreContext) ContextBuilder.newBuilder(location)
					.credentials(accessKey, secretKey)
					.buildView(BlobStoreContext.class);

			storage = context.getBlobStore();
			swift = context.unwrap();
			swift.getApi().createContainer(bucketName);

		}catch(Exception e){
			System.out.println("Cannot connect with RackSpace.");
			//e.printStackTrace();
			throw new StorageCloudException(StorageCloudException.INVALID_SESSION);
		}
		session_key = "sid";
		return session_key;
	}

	public String uploadData(String sid, String cid, byte[] data, String id) throws StorageCloudException {

		SwiftObject object = swift.getApi().newSwiftObject();
		object.getInfo().setName(id);
		object.setPayload(data);
		swift.getApi().putObject(bucketName, object);
		return id;
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
				bucketName = bucketName.concat(new String(randname));
				props.setProperty("bucketname", bucketName);
				props.store(new FileOutputStream(path),"change");
			}else{
				bucketName = name;
			}
				
		}catch(IOException e){  
			e.printStackTrace();  
		} 
	}

	//Not suported 
	public boolean setAcl(String arg0, String arg1, String arg2, String arg3)
			throws StorageCloudException {
		return false;
	}

}

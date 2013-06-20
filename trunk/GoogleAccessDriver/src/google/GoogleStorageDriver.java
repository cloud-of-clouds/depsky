package google;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Properties;

import org.jets3t.service.ServiceException;
import org.jets3t.service.acl.Permission;
import org.jets3t.service.acl.gs.GSAccessControlList;
import org.jets3t.service.acl.gs.UserByIdGrantee;
import org.jets3t.service.impl.rest.httpclient.GoogleStorageService;
import org.jets3t.service.model.GSBucket;
import org.jets3t.service.model.GSObject;
import org.jets3t.service.security.GSCredentials;

import depskyDep.IDepSkySDriver;
import depskyDep.StorageCloudException;

/**
 * Class that interact directly with google storage api
 * @author tiago oliveira
 *
 */
public class GoogleStorageDriver implements IDepSkySDriver{

	private GoogleStorageService gsService;
	private String driverId;
	private String session_key;
	private String bucketname = "depskys";
	String accessKey;
	String secretKey;

	public GoogleStorageDriver(String driverID, String accessKey, String secretKey){
		this.driverId = driverID;
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		try {
			getBucketName();
		} catch (FileNotFoundException e) {
			System.out.println("Problem with bucket_name.properties file!");
			//e.printStackTrace();
		}
	}

	public boolean deleteContainer(String sid, String[] allNames) throws StorageCloudException {
		try {
			for(String str : allNames){
				gsService.deleteObject(bucketname, str);
			}
			return true;
		} catch (ServiceException e) {
			e.printStackTrace();
		}
		return false;
	}

	//TODO: implement this method (will be used in the lock operation)
	public LinkedList<String> listNames(String prefix) throws StorageCloudException{
		return null;
	}

	public boolean deleteData(String sid, String cid, String id) throws StorageCloudException {
		try {
			gsService.deleteObject(bucketname, id);
			return true;
		} catch (ServiceException e) {
			e.printStackTrace();
		}
		return false;
	}

	public byte[] downloadData(String sid, String cid, String id) throws StorageCloudException {

		try {
			GSObject objectComplete = gsService.getObject(bucketname, id);
			InputStream in = objectComplete.getDataInputStream();
			byte[] array = null;
			array = getBytesFromInputStream(in);
			if(array == null){
				throw new StorageCloudException("GoogleStorageException:: download data");
			}
			return array;
		} catch (ServiceException e) {
			throw new StorageCloudException("GoogleStorageException::" + e.getMessage());
		} catch (IOException e) {
			throw new StorageCloudException("GoogleStorageException::" + e.getMessage());
		}
	}

	public boolean endSession(String arg0) throws StorageCloudException {
		return false;
	}

	public String[] getContainerAndDataIDsByName(String sid, String cid, String id) throws StorageCloudException {


		try {
			if(gsService.isObjectInBucket(bucketname, id)){
				return new String[]{cid, id};
			}else{
				throw new StorageCloudException("RSException:: getContByName");
			}
		} catch (ServiceException e) {
			throw new StorageCloudException("AWSS3Exception:: Key not exist");
		}	
	}

	public String getDataIdByName(String arg0, String arg1)
			throws StorageCloudException {
		return null;
	}

	public String getDriverId() {
		return driverId;
	}

	public String getSessionKey() {
		return session_key;
	}

	public String initSession() throws StorageCloudException {

		try {
			GSCredentials gsCredentials = new GSCredentials(accessKey, secretKey);
			gsService = new GoogleStorageService(gsCredentials);
		} catch (ServiceException e) {
			System.out.println("Cannot connect with Google Storage.");
			//e.printStackTrace();
			throw new StorageCloudException(StorageCloudException.INVALID_SESSION);
		}
		try {
			gsService.createBucket(bucketname);
		} catch (ServiceException e) {
		}
		session_key = "sid";
		return "sid";
	}

	public String uploadData(String sid, String cid, byte[] data, String id) throws StorageCloudException {

		try {
			GSObject object = new GSObject(id);
			ByteArrayInputStream in = new ByteArrayInputStream(data);
			object.setDataInputStream(in);
			object.setContentLength(data.length);
			gsService.putObject(bucketname, object);
			return id;
		} catch (ServiceException e) {
			throw new StorageCloudException("AWSS3Exception::" + e.getMessage());
		}
	}

	private static byte[] getBytesFromInputStream(InputStream is) throws IOException {

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

	//FIXME: not finished
	public boolean setAcl(String cid, String id, String permission, String clientId) throws StorageCloudException {

		try {
			GSAccessControlList acl = new GSAccessControlList();
			if(permission.equals("rw"))
				acl.grantPermission(new UserByIdGrantee(clientId), Permission.PERMISSION_FULL_CONTROL);
			else if(permission.equals("r"))
				acl.grantPermission(new UserByIdGrantee(clientId), Permission.PERMISSION_READ);
			else if(permission.equals("w"))
				acl.grantPermission(new UserByIdGrantee(clientId), Permission.PERMISSION_WRITE);
			gsService.putBucketAcl(cid, acl);
			return true;
		} catch (ServiceException e) {
			throw new StorageCloudException("AWSS3Exception::" + e.getMessage());
		}
	}
}

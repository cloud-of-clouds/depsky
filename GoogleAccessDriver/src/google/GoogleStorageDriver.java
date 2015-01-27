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
import org.jets3t.service.acl.gs.UserByEmailAddressGrantee;
import org.jets3t.service.impl.rest.httpclient.GoogleStorageService;
import org.jets3t.service.model.GSObject;
import org.jets3t.service.security.GSCredentials;

import depskyDep.IDepSkySDriver;
import exceptions.ServiceSiteException;
import exceptions.StorageCloudException;

/**
 * Class that interact directly with google storage api
 * @author tiago oliveira
 *
 */
public class GoogleStorageDriver implements IDepSkySDriver{

	private GoogleStorageService gsService;
	private String driverId;
	private String defaultBacketName = "depskys";
	private String accessKey;
	private String secretKey;

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

	public boolean deleteContainer(String bucketName, String[] namesToDelete, String[] uploadToAnotherAccount) throws StorageCloudException {
		try {
			if(bucketName == null)
				for(String str : namesToDelete){
					gsService.deleteObject(defaultBacketName, str);
				}
			else
				for(String str : namesToDelete){
					gsService.deleteObject(bucketName, str);
				}
			return true;
		} catch (ServiceException e) {

		}
		return true;
	}

	public LinkedList<String> listNames(String prefix, String bucketName, String[] uploadToAnotherAccount) throws StorageCloudException{

		LinkedList<String> find = new LinkedList<String>();
		try {
			GSObject[] objectListing = null;
			if(bucketName != null)
				objectListing = gsService.listObjects(bucketName, prefix, null);
			else
				objectListing = gsService.listObjects(defaultBacketName, prefix, null);

			for(GSObject obj : objectListing){
				find.add(obj.getName());
			}

			return find;
		} catch (ServiceException e1) {
			//e.printStackTrace();
			throw new ServiceSiteException("GoogleStorageException::" + e1.getMessage());
		} catch (Exception e2) {
			throw new StorageCloudException("GoogleStorageException::" + e2.getMessage());
		}

	}

	public boolean deleteData(String bucketName, String fileId, String[] uploadToAnotherAccount) throws StorageCloudException {
		try {
			if(bucketName != null)
				gsService.deleteObject(bucketName, fileId);
			else
				gsService.deleteObject(defaultBacketName, fileId);
			return true;
		} catch (ServiceException e) {
			if(e.getErrorCode().equals("NoSuchKey"))
				return true;
			e.printStackTrace();
		}
		return false;
	}

	public byte[] downloadData(String bucketName, String fileId, String[] uploadToAnotherAccount) throws StorageCloudException {

		try {
			GSObject objectComplete = null;
			if(bucketName != null)
				objectComplete = gsService.getObject(bucketName, fileId);
			else
				objectComplete = gsService.getObject(defaultBacketName, fileId);
			InputStream in = objectComplete.getDataInputStream();
			byte[] array = null;
			array = getBytesFromInputStream(in);
			if(array == null){
				throw new StorageCloudException("GoogleStorageException:: download data");
			}
			return array;
		} catch (ServiceException e) {
			throw new ServiceSiteException("GoogleStorageException::" + e.getMessage());
		} catch (IOException e) {
			throw new StorageCloudException("GoogleStorageException::" + e.getMessage());
		} catch (Exception e) {
			throw new StorageCloudException("GoogleStorageException::" + e.getMessage());
		}
	}

	public boolean endSession(String arg0) throws StorageCloudException {
		return false;
	}

	public String getDriverId() {
		return driverId;
	}

	public String initSession() throws StorageCloudException {

		try {
			GSCredentials gsCredentials = new GSCredentials(accessKey, secretKey);
			gsService = new GoogleStorageService(gsCredentials);

		} catch (ServiceException e) {
			//System.out.println("Cannot connect with Google Storage.");
			//e.printStackTrace();
			throw new StorageCloudException("GoogleStorageException::" + StorageCloudException.INVALID_SESSION);
		}

		try {
			//gsService.createBucket(bucketname);
			gsService.getOrCreateBucket(defaultBacketName);
		} catch (ServiceException e) {}
		return "sid";
	}


	public String uploadData(String bucketName, byte[] data, String fileId, String[] canonicalIDs) throws StorageCloudException {
		try {
			GSObject object = new GSObject(fileId);
			ByteArrayInputStream in = new ByteArrayInputStream(data);
			object.setDataInputStream(in);
			object.setContentLength(data.length);
			if(bucketName != null){
				if(gsService.checkBucketStatus(bucketName)==1)
					gsService.createBucket(bucketName);
				if(canonicalIDs !=null){
					GSAccessControlList acl = new GSAccessControlList();
					for(int i = 0; i < canonicalIDs.length; i++){
						//System.out.println(canonicalIDs[i]);
						acl.grantPermission(new UserByEmailAddressGrantee(canonicalIDs[i]), Permission.PERMISSION_READ);
					}
					object.setAcl(acl);
				}
				gsService.putObject(bucketName, object);
			}else{
				if(gsService.checkBucketStatus(defaultBacketName)==1)
					gsService.createBucket(defaultBacketName);
				gsService.putObject(defaultBacketName, object);
			}
			return fileId;
		} catch (ServiceException e1) {
			//e.printStackTrace();
			throw new ServiceSiteException("GoogleStorageException::" + e1.getMessage());
		} catch (Exception e2) {
			throw new StorageCloudException("GoogleStorageException::" + e2.getMessage());
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
				defaultBacketName = defaultBacketName.concat(new String(randname));
				props.setProperty("bucketname", defaultBacketName);
				props.store(new FileOutputStream(path),"change");
			}else{
				defaultBacketName = name;
			}

		}catch(IOException e){  
			e.printStackTrace();  
		} 
	}

	public String[] setAcl(String bucketNameToShare, String[] canonicalId, String permission) throws StorageCloudException {
		try {
			boolean withRead = false;
			if(bucketNameToShare != null){
				gsService.getOrCreateBucket(bucketNameToShare);
			}else{
				return null;
			}

			GSAccessControlList acl = gsService.getBucketAcl(bucketNameToShare);
			for(int i = 0; i < canonicalId.length; i++){
				UserByEmailAddressGrantee user = new UserByEmailAddressGrantee(canonicalId[i]);
				if(permission.equals("rw")){ 
					acl.grantPermission(user, Permission.PERMISSION_FULL_CONTROL);
					withRead = true;
				}else if(permission.equals("r")){
					acl.grantPermission(new UserByEmailAddressGrantee(canonicalId[i]), Permission.PERMISSION_READ);
					withRead = true;
				}else if(permission.equals("w"))
					acl.grantPermission(new UserByEmailAddressGrantee(canonicalId[i]), Permission.PERMISSION_WRITE);
			}

			if(withRead){
				//StorageOwner bucketOwner = acl.getOwner();
				//System.out.println("-- " + bucketOwner.getId());
				GSObject[] objects = gsService.listObjects(bucketNameToShare);
				GSAccessControlList aclKeys = null;
				for(GSObject elem: objects) {
					aclKeys = (GSAccessControlList) gsService.getObjectAcl(bucketNameToShare, elem.getName());
					for(int i = 0; i < canonicalId.length; i++){
						aclKeys.grantPermission(new UserByEmailAddressGrantee(canonicalId[i]), Permission.PERMISSION_READ);
					}
					gsService.putObjectAcl(bucketNameToShare, elem.getKey(), aclKeys);
				}
			}
			gsService.putBucketAcl(bucketNameToShare, acl);

			return canonicalId;
		} catch (ServiceException e1) {
			//e.printStackTrace();
			throw new ServiceSiteException("GoogleStorageException::" + e1.getMessage());
		} catch (Exception e2) {
			throw new StorageCloudException("GoogleStorageException::" + e2.getMessage());
		}
	}
}

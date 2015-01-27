package amazon;


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
import java.util.Set;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CanonicalGrantee;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.Grant;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import depskyDep.IDepSkySDriver;
import exceptions.ClientServiceException;
import exceptions.ServiceSiteException;
import exceptions.StorageCloudException;


public class AmazonS3Driver implements IDepSkySDriver{

	private String driverId;
	private AmazonS3Client conn = null;
	private String session_key;
	private String defaultBucketName = "depskys";
	private String accessKey, secretKey;
	private String location;
	private Region region = null;

	/**
	 * Class that interact directly with amazon s3 api 
	 * 
	 * @author tiago oliveira
	 *
	 */
	public AmazonS3Driver(String driverId, String accessKey, String secretKey) {
		this.driverId = driverId;
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		try {
			getBucketName();
		} catch (FileNotFoundException e) {
			System.out.println("Problem with bucket_name.properties file!");
			//e.printStackTrace();
		}

		if(driverId.equals("cloud1")){
			defaultBucketName = defaultBucketName.concat("-coc1");
			location = "-coc1";
			region = Region.US_Standard;
		}else if(driverId.equals("cloud2")){
			defaultBucketName = defaultBucketName.concat("-coc2");
			location = "-coc2";
			region = Region.EU_Ireland;
		}else if(driverId.equals("cloud3")){
			defaultBucketName = defaultBucketName.concat("-coc3");
			location = "-coc3";
			region = Region.US_West;
		}else if(driverId.equals("cloud4")){
			defaultBucketName = defaultBucketName.concat("-coc4");
			location = "-coc4";
			region = Region.AP_Tokyo;
		}else if(driverId.equals("amazon")){
			defaultBucketName = defaultBucketName.concat("-publiccloud");
			location = "-publiccloud";
			region = Region.EU_Ireland;
		}
	}

	/**
	 * Make the connection with Amazon S3
	 */

	public String initSession() throws StorageCloudException {
		try {
			String mprops = "accessKey=" + accessKey + "\r\n"
					+ "secretKey = " + secretKey;
			PropertiesCredentials b = new PropertiesCredentials( new ByteArrayInputStream(mprops.getBytes()));

			conn = new AmazonS3Client(b);		
			conn.setEndpoint("http://s3.amazonaws.com"); //Para virtual Box funcionar

			if(!conn.doesBucketExist(defaultBucketName)){
				conn.createBucket(defaultBucketName, region);
			}

		} catch (IOException e) {
			//System.out.println("Cannot connect with Amazon S3.");
			//e.printStackTrace();
			throw new StorageCloudException(StorageCloudException.INVALID_SESSION);
		}

		session_key = "sid";
		return "sid";
	}

	/**
	 * writes the value 'data' in the file 'id'
	 */
	public String uploadData(String bucketName, byte[] data, String fileId, String[] canonicalIDs) throws StorageCloudException {
		try {
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(data.length);
			ByteArrayInputStream in = new ByteArrayInputStream(data);

			if(bucketName != null){
				bucketName = bucketName.concat(location);
				if(!conn.doesBucketExist(bucketName)){
					conn.createBucket(bucketName, region);
				}
				if(canonicalIDs !=null){
					AccessControlList acl = new AccessControlList();
					for(int i = 0; i < canonicalIDs.length; i++){
						acl.grantPermission(new CanonicalGrantee(canonicalIDs[i]), Permission.Read);
					}
					conn.putObject(new PutObjectRequest(bucketName, fileId, in, metadata).withAccessControlList(acl));	
				}else{
					conn.putObject(new PutObjectRequest(bucketName, fileId, in, metadata));	
				}
			}else{
				conn.putObject(new PutObjectRequest(defaultBucketName, fileId, in, metadata));
			}
			in.close();
			return fileId;
		} catch (AmazonServiceException e1) {
			throw new ServiceSiteException("AWSS3Exception::" + e1.getMessage());
		} catch (AmazonClientException e2) {
			throw new ClientServiceException("AWSS3Exception::" + e2.getMessage());
		} catch (IOException e3) {
			e3.printStackTrace();
			throw new StorageCloudException("AWSS3Exception::" + e3.getMessage());
		}
	}

	/**
	 * download the content of the file 'id'
	 */
	public byte[] downloadData(String bucketName, String fileId, String[] canonicalIDs) throws StorageCloudException {

		try {
			S3Object object = null;
			if(bucketName == null){
				object = conn.getObject(new GetObjectRequest(defaultBucketName, fileId));
			}else{
				bucketName = bucketName.concat(location);
				object = conn.getObject(new GetObjectRequest(bucketName, fileId));
			}
			byte[] array = getBytesFromInputStream(object.getObjectContent());

			object.getObjectContent().close();
			return array;
		} catch (AmazonServiceException e1) {
			throw new ServiceSiteException("AWSS3Exception::" + e1.getMessage());
		} catch (AmazonClientException e2) {
			throw new ClientServiceException("AWSS3Exception::" + e2.getMessage());
		} catch (IOException e3) {
			e3.printStackTrace();
			throw new StorageCloudException("AWSS3Exception::" + e3.getMessage());
		}
	}

	/**
	 * delete the file 'id'
	 */
	public boolean deleteData(String bucketName, String fileId, String[] canonicalIDs) throws StorageCloudException {
		try {
			if(bucketName == null)
				conn.deleteObject(defaultBucketName, fileId);
			else{
				bucketName = bucketName.concat(location);
				conn.deleteObject(bucketName, fileId);
			}
			return true;
		} catch (Exception ex) {
			return false;
		}
	}


	public LinkedList<String> listNames(String prefix, String bucketName, String[] canonicalIDs) throws StorageCloudException{
		LinkedList<String> find = new LinkedList<String>();
		try{
			ObjectListing objectListing = null;
			if(bucketName == null)
				objectListing = conn.listObjects(new ListObjectsRequest()
				.withBucketName(defaultBucketName).withPrefix(prefix));
			else{
				bucketName = bucketName.concat(location);
				objectListing = conn.listObjects(new ListObjectsRequest()
				.withBucketName(bucketName).withPrefix(prefix));
			}
			for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
				find.add(objectSummary.getKey());
			}
		} catch (AmazonServiceException e1) {
			throw new ServiceSiteException("AWSS3Exception::" + e1.getMessage());
		} catch (AmazonClientException e2) {
			throw new ClientServiceException("AWSS3Exception::" + e2.getMessage());
		}

		return find;
	}

	public boolean deleteContainer(String bucketName, String[] namesToDelete, String[] canonicalIDs) throws StorageCloudException {
		String container = bucketName == null ? defaultBucketName : bucketName.concat(location);
		try {		
			for(String fileId : namesToDelete){
				conn.deleteObject(container, fileId);
			}
			return true;
		} catch (Exception ex) {
			throw new StorageCloudException("AWSS3Exception::" + ex.getMessage());
		}
	}

	public boolean endSession(String sid) {
		return sid.equals("sid");
	}

	public String getDriverId() {
		return driverId;
	}

	public String getSessionKey() {
		return session_key;
	}

	public String[] setAcl(String bucketNameToShare, String[] canonicalId, String permission) throws StorageCloudException{
		boolean withRead = false;
		if(bucketNameToShare != null){
			bucketNameToShare = bucketNameToShare.concat(location);
			if(!conn.doesBucketExist(bucketNameToShare)){
				conn.createBucket(bucketNameToShare, region);
			}
		}else{
			return null;
		}

		// set acl
		AccessControlList acl = conn.getBucketAcl(bucketNameToShare);
		for(int i = 0; i < canonicalId.length; i++){
			if(permission.equals("rw")){
				CanonicalGrantee grantee = new CanonicalGrantee(canonicalId[i]);
				acl.grantPermission(grantee, Permission.Read);
				acl.grantPermission(grantee, Permission.Write);
				withRead = true;
			}else if(permission.equals("r")){
				acl.grantPermission(new CanonicalGrantee(canonicalId[i]), Permission.Read);
				withRead = true;
			}else if(permission.equals("w")){
				acl.grantPermission(new CanonicalGrantee(canonicalId[i]), Permission.Write);
			}
		}
		try{
			if(withRead){
				ObjectListing objectListing = conn.listObjects(bucketNameToShare);
				AccessControlList aclKeys = null;
				for(S3ObjectSummary elem: objectListing.getObjectSummaries()) {
					aclKeys = conn.getObjectAcl(bucketNameToShare, elem.getKey());
					for(int i = 0; i < canonicalId.length; i++){
						aclKeys.grantPermission(new CanonicalGrantee(canonicalId[i]), Permission.Read);
					}
					conn.setObjectAcl(bucketNameToShare, elem.getKey(), aclKeys);
				}
			}

			//confirm if acl well 
			conn.setBucketAcl(bucketNameToShare, acl);
			AccessControlList newAcl = conn.getBucketAcl(bucketNameToShare);
			Set<Grant> grants = newAcl.getGrants();
			boolean flag = false;
			for(Grant grant : grants){
				if(grant.getGrantee().getIdentifier().equals(canonicalId[0])){
					flag = true;
				}
			}
			if(!flag){
				throw new ServiceSiteException("AWSS3Exception:: ACL");
			}
		} catch (AmazonServiceException e1) {
			throw new ServiceSiteException("AWSS3Exception::" + e1.getMessage());
		} catch (AmazonClientException e2) {
			throw new ClientServiceException("AWSS3Exception::" + e2.getMessage());
		}
		return canonicalId;
	}

	public String getLocation(){
		return location;
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

	private String getBucketName() throws FileNotFoundException{
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
				defaultBucketName = defaultBucketName.concat(new String(randname));
				props.setProperty("bucketname", defaultBucketName);
				props.store(new FileOutputStream(path),"change");
			}else{
				defaultBucketName = name;
			}
		}catch(IOException e){  
			e.printStackTrace();  
		} 
		return null;
	}
}

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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.Bucket;
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
import depskyDep.StorageCloudException;


public class AmazonS3Driver implements IDepSkySDriver {

	private String driverId;
	private AmazonS3 conn = null;
	private String session_key;
	private String lastContainer;//container id small cache to improve driver performance
	private String bucketName = "depskys";
	private String accessKey, secretKey;
	private String location;
	Region region = null;

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


			if(driverId.equals("cloud1")){
				bucketName = bucketName.concat("-cloud1");
				location = "-cloud1";
				region = Region.US_Standard;
			}else if(driverId.equals("cloud2")){
				bucketName = bucketName.concat("-cloud2");
				location = "-cloud2";
				region = Region.EU_Ireland;
			}else if(driverId.equals("cloud3")){
				bucketName = bucketName.concat("-cloud3");
				location = "-cloud3";
				region = Region.US_West;
			}else if(driverId.equals("cloud4")){
				bucketName = bucketName.concat("-cloud4");
				location = "-cloud4";
				region = Region.AP_Tokyo;
			}

			if(!conn.doesBucketExist(bucketName)){
				conn.createBucket(bucketName, region);
			}
		} catch (IOException e) {
			System.out.println("Cannot connect with Amazon S3.");
			//e.printStackTrace();
			throw new StorageCloudException(StorageCloudException.INVALID_SESSION);
		}

		session_key = "sid";
		return "sid";
	}

	/**
	 * writes the value 'data' in the file 'id'
	 */
	public String uploadData(String sid, String cid, byte[] data, String id) throws StorageCloudException {
		try {

			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(data.length);
			ByteArrayInputStream in = new ByteArrayInputStream(data);
			if(sid == null){
				conn.putObject(new PutObjectRequest(bucketName, id, in, metadata));	
			}else{
				AccessControlList acl = new AccessControlList();
				sid = sid.concat(location);
				if(!conn.doesBucketExist(sid)){
					conn.createBucket(sid, region);
				}else{
					AccessControlList bAcl = conn.getBucketAcl(sid);
					Set<Grant> grants = bAcl.getGrants();
					for(Grant g : grants){
						acl.grantPermission(g.getGrantee(), Permission.Read);
					}
				}
				conn.putObject(new PutObjectRequest(sid, id, in, metadata).withAccessControlList(acl));	

			}
			in.close();
			return id;
		} catch (Exception ex) {
			//ex.printStackTrace();
			throw new StorageCloudException("AWSS3Exception::" + ex.getMessage());
		}
	}

	/**
	 * download the content of the file 'id'
	 */
	public byte[] downloadData(String sid, String cid, String id) throws StorageCloudException {
		try {
			S3Object object = null;
			
			if(sid == null){
				object = conn.getObject(new GetObjectRequest(bucketName, id));
			}else{
				sid=sid.concat(location);
				object = conn.getObject(new GetObjectRequest(sid, id));
			}
			byte[] array = getBytesFromInputStream(object.getObjectContent());

			object.getObjectContent().close();
			return array;
		} catch (Exception ex) {
			throw new StorageCloudException("AWSS3Exception::" + ex.getMessage());
		}
	}

	/**
	 * delete the file 'id'
	 */
	public boolean deleteData(String sid, String cid, String id) throws StorageCloudException {
		try {
			conn.deleteObject(bucketName, id);
			return true;
		} catch (Exception ex) {
			throw new StorageCloudException("AWSS3Exception::" + ex.getMessage());
		}
	}


	public LinkedList<String> listNames(String prefix) throws StorageCloudException{

		LinkedList<String> find = new LinkedList<String>();
		ObjectListing objectListing = conn.listObjects(new ListObjectsRequest()
		.withBucketName(bucketName).withPrefix(prefix));
		for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
			find.add(objectSummary.getKey());
		}

		return find;
	}

	public boolean deleteContainer(String sid, String[] allNames) throws StorageCloudException {
		try {		
			for(String str : allNames){
				conn.deleteObject(bucketName, str);
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

	public String getDataIdByName(String sid, String name)
			throws StorageCloudException {
		try {
			if (lastContainer == null) {
				for (Bucket b : conn.listBuckets()) {
					ObjectListing keylist = conn.listObjects(b.getName());
					for (S3ObjectSummary eo : keylist.getObjectSummaries()) {				
						if (eo.getKey().equals(name)) {
							return name;
						}
					}
				}
			} else {
				ObjectListing keylist = conn.listObjects(lastContainer);
				for (S3ObjectSummary eo : keylist.getObjectSummaries()) {				
					if (eo.getKey().equals(name)) {
						lastContainer = null;
						return name;
					}
				}
			}
			throw new Exception("could not get the file id");
		} catch (Exception ex) {
			throw new StorageCloudException("AWSS3Exception::" + ex.getMessage());
		}
	}

	public String[] getContainerAndDataIDsByName(String sid,
			String cid, String id) throws StorageCloudException {

		//S3Object container = conn.getObject(new GetObjectRequest(cid.concat(awsBucketLocation.toString()), id));
		//container.getObjectContent().close();
		if(exists(bucketName, cid+"/"+id))
			return new String[]{cid, id};
		else 
			throw new StorageCloudException("AWSS3Exception:: Key not exist");
	}

	public boolean setAcl(String cid, String id, String canonicalId, String permission){

		boolean f = false, v = false;
		cid=cid.concat(location);
		if(!conn.doesBucketExist(cid)){
			conn.createBucket(cid, region);
			v = true;
		}

		AccessControlList acl = conn.getBucketAcl(cid);
		if(permission.equals("rw")){
			acl.grantPermission(new CanonicalGrantee(canonicalId), Permission.FullControl);
			f = true;
		}else if(permission.equals("r")){
			acl.grantPermission(new CanonicalGrantee(canonicalId), Permission.Read);
			f = true;
		}else if(permission.equals("w")){
			acl.grantPermission(new CanonicalGrantee(canonicalId), Permission.Write);
		}

		if(!v && f){
			ObjectListing objectListing = conn.listObjects(cid);
			AccessControlList aclKeys = null;
			for(S3ObjectSummary elem: objectListing.getObjectSummaries()) {
				aclKeys = conn.getObjectAcl(cid, elem.getKey());
				aclKeys.grantPermission(new CanonicalGrantee(canonicalId), Permission.Read);
				aclKeys.grantPermission(new CanonicalGrantee(canonicalId), Permission.ReadAcp);
				conn.setObjectAcl(cid, elem.getKey(), aclKeys);
			}
		}

		conn.setBucketAcl(cid, acl);
		return true;
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
	private boolean exists(String cid, String id) {
		try {
			conn.getObjectMetadata(cid, id); 
		} catch(AmazonServiceException e) {
			return false;
		}
		return true;
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
				bucketName = bucketName.concat(new String(randname));
				props.setProperty("bucketname", bucketName);
				props.store(new FileOutputStream(path),"change");
			}else{
				bucketName = name;
			}
		}catch(IOException e){  
			e.printStackTrace();  
		} 
		return null;
	}

}

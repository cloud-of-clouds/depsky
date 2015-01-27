package azure;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TimeZone;

import com.microsoft.windowsazure.services.blob.client.CloudBlobClient;
import com.microsoft.windowsazure.services.blob.client.CloudBlobContainer;
import com.microsoft.windowsazure.services.blob.client.CloudBlockBlob;
import com.microsoft.windowsazure.services.blob.client.ListBlobItem;
import com.microsoft.windowsazure.services.blob.client.SharedAccessBlobPermissions;
import com.microsoft.windowsazure.services.blob.client.SharedAccessBlobPolicy;
import com.microsoft.windowsazure.services.core.storage.CloudStorageAccount;
import com.microsoft.windowsazure.services.core.storage.StorageException;

import depskyDep.IDepSkySDriver;
import exceptions.ServiceSiteException;
import exceptions.StorageCloudException;


public class WindowsAzureDriver implements IDepSkySDriver{

	private String driverId;
	private String accessKey, secretKey;
	private String defaultBucketName = "depskys";
	private CloudBlobClient blobClient;

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

		String storageConnectionString = 
				"DefaultEndpointsProtocol=https;" + 
						"AccountName=" + accessKey + ";" + 
						"AccountKey=" + secretKey + ";";

		// Retrieve storage account from connection-string
		CloudStorageAccount storageAccount = null;
		try {

			storageAccount = CloudStorageAccount.parse(storageConnectionString);
			blobClient = storageAccount.createCloudBlobClient();
			CloudBlobContainer container = blobClient.getContainerReference(defaultBucketName);
			container.createIfNotExist();

		} catch (URISyntaxException e) {
			throw new StorageCloudException("AzureStorageException::" + e.getMessage());
		} catch (StorageException e) {
			throw new StorageCloudException("AzureStorageException::" + e.getMessage());
		} catch (InvalidKeyException e) {
			throw new StorageCloudException("AzureStorageException::" + e.getMessage());
		}	

		return "sid";
	}

	public boolean deleteContainer(String bucketName, String[] namesToDelete, String[] uploadToAnotherAccount) throws StorageCloudException {
		//		try {
		//			CloudBlobContainer container;
		//			if(bucketName == null)
		//				container = blobClient.getContainerReference(bucketName);
		//			else
		//				container = blobClient.getContainerReference(bucketName);
		//			CloudBlockBlob blob;
		//			for(String str : namesToDelete){
		//				blob = container.getBlockBlobReference(str);
		//				blob.deleteIfExists();
		//			}
		for(String str : namesToDelete)
			deleteData(bucketName, str, uploadToAnotherAccount);
		//		} catch (URISyntaxException e) {
		//			e.printStackTrace();
		//		} catch (StorageException e) {
		//			e.printStackTrace();
		//		}
		return true;
	}

	public boolean deleteData(String bucketName, String fileId, String[] uploadToAnotherAccount) throws StorageCloudException {
		try {
			CloudBlobContainer container = null;
			if(bucketName != null)
				if(uploadToAnotherAccount == null)
					container = blobClient.getContainerReference(bucketName);
				else{
					String[] sharedAccess = uploadToAnotherAccount[0].split("/"+bucketName);
					URI baseuri = new URI(sharedAccess[0]);
					CloudBlobClient blobclient = new CloudBlobClient(baseuri);
					return deleteViaSAS(bucketName, fileId, sharedAccess[1], blobclient);
				}
			else
				container = blobClient.getContainerReference(defaultBucketName);
			CloudBlockBlob blob = container.getBlockBlobReference(fileId);
			blob.deleteIfExists();

		} catch (URISyntaxException e) {
			return false;
		} catch (StorageException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	public byte[] downloadData(String bucketName, String fileId, String[] uploadToAnotherAccount) throws StorageCloudException {

		byte[] data = null;
		try {
			CloudBlobContainer container = null;
			if(bucketName != null){
				if(uploadToAnotherAccount == null)
					container = blobClient.getContainerReference(bucketName);
				else{
//					String[] sharedAccess = uploadToAnotherAccount[0].split("/"+bucketName);
//					URI baseuri = new URI(sharedAccess[0]);
//					CloudBlobClient blobclient = new CloudBlobClient(baseuri);
//					return downloadViaSAS(bucketName, fileId, sharedAccess[1], blobclient);
				
					String[] sharedAccess = uploadToAnotherAccount[0].split("/"+bucketName);
					CloudBlobClient blobclient = new CloudBlobClient(new URI(sharedAccess[0]));

					URI uri = new URI(blobclient.getEndpoint().toString()+"/"+bucketName+"?"+sharedAccess[1]);
					container = new CloudBlobContainer(uri, blobclient);
					
					CloudBlockBlob blob = container.getBlockBlobReference(fileId);
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					blob.download(out);
					
					byte[] result = out.toByteArray();
					return result;
				
				}
			}else{
				container = blobClient.getContainerReference(defaultBucketName);
			}
			//	container.createIfNotExist();
			CloudBlockBlob blob = container.getBlockBlobReference(fileId);
			if(!blob.exists())
				throw new StorageCloudException("AzureStorage:: download data");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			blob.download(out);
			data = out.toByteArray();

		} catch (URISyntaxException e) {
			throw new StorageCloudException("AzureStorageException::" + e.getMessage());
		} catch (StorageException e) {
			throw new StorageCloudException("AzureStorageException::" + e.getMessage());
		} catch (IOException e) {
			throw new StorageCloudException("AzureStorageException::" + e.getMessage());
		}


		return data;
	}

	public boolean endSession(String arg0) throws StorageCloudException {
		return false;
	}

	public String getDriverId() {
		return driverId;
	}

	public LinkedList<String> listNames(String prefix, String bucketName, String[] uploadToAnotherAccount) throws StorageCloudException {

		LinkedList<String> find = new LinkedList<String>();
		try {
			CloudBlobContainer container = null;
			if(bucketName == null)
				container = blobClient.getContainerReference(defaultBucketName);
			else{
				if(uploadToAnotherAccount == null)
					container = blobClient.getContainerReference(bucketName);
				else{
					String[] sharedAccess = uploadToAnotherAccount[0].split("/"+bucketName);
					URI baseuri = new URI(sharedAccess[0]);
					CloudBlobClient blobclient = new CloudBlobClient(baseuri);
					return listViaSAS(bucketName, prefix, sharedAccess[1], blobclient);
				}
			}
			Iterable<ListBlobItem> listOfNames = container.listBlobs(prefix);
			for(ListBlobItem item : listOfNames){
				String[] name = item.getUri().getPath().split("/");
				find.add(name[name.length-1]);
			}

		} catch (StorageException e1) {
			throw new ServiceSiteException("AzureStorageException::" + e1.getMessage());
		} catch (URISyntaxException e2) {
			throw new ServiceSiteException("AzureStorageException::" + e2.getMessage());
		}
		return find;
	}

	public String[] setAcl(String bucketNameToShare, String[] canonicalId, String permission) throws StorageCloudException {
		try {
			EnumSet<SharedAccessBlobPermissions> permissions = null;
			if(permission.equals("rw")){
				permissions = EnumSet.of(SharedAccessBlobPermissions.READ, SharedAccessBlobPermissions.WRITE, 
						SharedAccessBlobPermissions.LIST, SharedAccessBlobPermissions.DELETE);
			}else if(permission.equals("r")){
				permissions = EnumSet.of(SharedAccessBlobPermissions.READ, SharedAccessBlobPermissions.LIST);
			}else if(permission.equals("w")){
				permissions = EnumSet.of(SharedAccessBlobPermissions.WRITE, 
						SharedAccessBlobPermissions.LIST, SharedAccessBlobPermissions.DELETE);
			}

			SharedAccessBlobPolicy sasConstraints = new SharedAccessBlobPolicy();
			GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
			calendar.setTime(new Date());
			sasConstraints.setSharedAccessStartTime(calendar.getTime());  //Immediately applicable
			calendar.add(Calendar.HOUR, 24*366); //366 days of duration
			sasConstraints.setSharedAccessExpiryTime(calendar.getTime());
			sasConstraints.setPermissions(permissions);
			String sas = "";
			CloudBlobContainer container = blobClient.getContainerReference(bucketNameToShare);
			container.createIfNotExist();
			sas = container.getUri() + container.generateSharedAccessSignature(sasConstraints, null);
			String[] toRet = {sas};
			return toRet;
		} catch (URISyntaxException e) {
			throw new ServiceSiteException("AzureStorageException::" + e.getMessage());
		} catch (StorageException e) {
			throw new ServiceSiteException("AzureStorageException::" + e.getMessage());
		} catch (InvalidKeyException e) {
			throw new ServiceSiteException("AzureStorageException::" + e.getMessage());
		}catch (Exception e){
			throw new ServiceSiteException("AzureStorageException::" + e.getMessage());
		}
	}

	public String uploadData(String bucketName, byte[] data, String fileId, String[] uploadToAnotherAccount) throws StorageCloudException {

		try {
			CloudBlobContainer container = null;
			if(bucketName != null){
				if(uploadToAnotherAccount == null)
					container = blobClient.getContainerReference(bucketName);
				else{
					//upload via SAS
					String[] sharedAccess = uploadToAnotherAccount[0].split("/"+bucketName);
					CloudBlobClient blobclient = new CloudBlobClient(new URI(sharedAccess[0]));

					URI uri = new URI(blobclient.getEndpoint().toString()+"/"+bucketName+"?"+sharedAccess[1]);
					container = new CloudBlobContainer(uri, blobclient);
					
					CloudBlockBlob blob = container.getBlockBlobReference(fileId);
					blob.upload(new ByteArrayInputStream(data), data.length);
//					uploadViaSAS(bucketName, fileId, data, sharedAccess[1], blobclient);
					return fileId;
				}
			}else
				container = blobClient.getContainerReference(defaultBucketName);

			container.createIfNotExist();
			CloudBlockBlob blob = container.getBlockBlobReference(fileId);
			blob.upload(new ByteArrayInputStream(data), data.length);
		} catch (URISyntaxException e) {
			throw new ServiceSiteException("AzureStorageException::" + e.getMessage());
		} catch (StorageException e) {
			throw new ServiceSiteException("AzureStorageException::" + e.getMessage());
		} catch (IOException e) {
			throw new ServiceSiteException("AzureStorageException::" + e.getMessage());
		}
		return fileId;
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

	private boolean deleteViaSAS(String bucketName, String fileId, String sas, CloudBlobClient blobclient)
			throws URISyntaxException, FileNotFoundException, IOException, StorageException{

		URI uri = new URI(blobclient.getEndpoint().toString()+"/"+bucketName+"/"+fileId+"?"+sas);
		CloudBlockBlob sasBlob = new CloudBlockBlob(uri, blobclient);
		sasBlob.deleteIfExists();
		return true;
	} 

	private byte[] downloadViaSAS(String bucketName, String fileId, String sas, CloudBlobClient blobclient) 
			throws URISyntaxException, StorageException, FileNotFoundException, IOException{

		URI uri = new URI(blobclient.getEndpoint().toString()+"/"+bucketName+"/"+fileId+"?"+sas);   
		CloudBlockBlob sasBlob = new CloudBlockBlob(uri, blobclient);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		sasBlob.download(out);
		byte[] data = out.toByteArray();
		return data;
	}

	private LinkedList<String> listViaSAS(String bucketName, String prefix, String sas, CloudBlobClient blobclient) 
			throws URISyntaxException, StorageException{

		LinkedList<String> find = new LinkedList<String>();
		URI uri = new URI(blobclient.getEndpoint().toString()+"/"+bucketName+"?"+sas);
		CloudBlobContainer container = new CloudBlobContainer(uri, blobclient);
		Iterable<ListBlobItem> listOfNames = container.listBlobs(prefix);
		for(ListBlobItem item : listOfNames){
			String[] name = item.getUri().getPath().split("/");
			find.add(name[name.length-1]);
		}

		return find;
	}  

	private void uploadViaSAS(String bucketName, String fileId, byte[] data, String sas, CloudBlobClient blobclient) 
			throws URISyntaxException, StorageException, FileNotFoundException, IOException{

		URI uri = new URI(blobClient.getEndpoint().toString()+"/"+bucketName+"/"+fileId+"?"+sas);
		CloudBlockBlob sasBlob = new CloudBlockBlob(uri, blobClient);
		sasBlob.upload(new ByteArrayInputStream(data), data.length);
		
	}
}

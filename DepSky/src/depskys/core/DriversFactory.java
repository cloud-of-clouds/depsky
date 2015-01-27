package depskys.core;

//import google.GoogleStorageDriver;
import org.jets3t.service.ServiceException;

import rackspace.RackSpaceDriver;
import amazon.AmazonS3Driver;
import azure.WindowsAzureDriver;
import depskyDep.IDepSkySDriver;
import exceptions.StorageCloudException;
import google.GoogleStorageDriver;

/**
 * Factory of IDepSkySDriver objects
 * 
 * @author tiago oliveira
 */
public class DriversFactory {

	/**
	 * (all this information come from the account.properties file)
	 * @param type - cloud type
	 * @param driverId - cloud id
	 * @param accessKey - cloud access key (unique for each user)
	 * @param secretKey - cloud secret key (unique for each user)
	 * @param clientId 
	 * @return an object IDepSkyDriver that contains the cloud access for one cloud type 
	 * @throws StorageCloudException 
	 * @throws ServiceException 
	 */
	public static IDepSkySDriver getDriver(String type, String driverId, String accessKey, String secretKey) throws StorageCloudException{
		IDepSkySDriver res=null;

		if(type.equals("AMAZON-S3")){
			res = new AmazonS3Driver(driverId, accessKey, secretKey);
		}else if(type.equals("GOOGLE-STORAGE")){
			res =  new GoogleStorageDriver(driverId, accessKey, secretKey);
		}else if(type.equals("WINDOWS-AZURE")){
			res = new WindowsAzureDriver(driverId, accessKey, secretKey);
		}else if(type.equals("RACKSPACE")){
			res = new RackSpaceDriver(driverId, accessKey, secretKey);
		}

		return res;
	}

}

package depskys.core;

import google.GoogleStorageDriver;
import rackazure.RackSpaceDriver;
import rackazure.WindowsAzureDriver;
import amazon.AmazonS3Driver;
import depskyDep.IDepSkySDriver;

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
	 * @return an object IDepSkyDriver that contains the cloud access for one cloud type 
	 */
	public static IDepSkySDriver getDriver(String type, String driverId, String accessKey, String secretKey){
		
		if(type.equals("AMAZON-S3")){
			return new AmazonS3Driver(driverId, accessKey, secretKey);
		}else if(type.equals("GOOGLE-STORAGE")){
			return new GoogleStorageDriver(driverId, accessKey, secretKey);
		}else if(type.equals("WINDOWS-AZURE")){
			return new WindowsAzureDriver(driverId, accessKey, secretKey);
		}else if(type.equals("RACKSPACE")){
			return new RackSpaceDriver(driverId, accessKey, secretKey);
		}
		
		return null;
	}
	
}

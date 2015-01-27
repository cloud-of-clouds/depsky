package depskyDep;

import java.util.LinkedList;

import exceptions.StorageCloudException;




/**
 * This interface represents the contract that drivers must fulfill
 * @author bruno
 */
public interface IDepSkySDriver{

    static final int CONNECT_TIMEOUT = 60000;
    static final int READ_TIMEOUT = 120000;

    /**
     * @param sid
     * @param cid
     * @param data
     * @param id
     * @return
     * @throws Exception
     */
    public String uploadData(String bucket, byte[] data, String id, String[] uploadToAnotherAccount) throws StorageCloudException;

    /**
     * Download file from cloud
     * @param sid
     * @param cid
     * @param id
     * @return data
     */
    public byte[] downloadData(String bucket, String id, String[] uploadToAnotherAccount) throws StorageCloudException;

    /**
     * Delete file from cloud
     * @param sid
     * @param cid
     * @param id
     * @return success?
     */
    public boolean deleteData(String bucket, String id, String[] uploadToAnotherAccount) throws StorageCloudException;

 
    /**
     * Delete drop/bucket/folder
     * @param sid
     * @param cid
     * @return success?
     */
    public boolean deleteContainer(String bucket, String[] allNames, String[] uploadToAnotherAccount) throws StorageCloudException;

    /**
     * Load cloud storage driver and initiate session
     * @param sessionProperties
     * @return session id
     */
    public String initSession() throws StorageCloudException;
    
    public LinkedList<String> listNames(String prefix, String sid, String[] uploadToAnotherAccount) throws StorageCloudException;
    
    /**
     * Terminate session, disables driver
     * @param sid
     * @return success?
     */
    public boolean endSession(String sid) throws StorageCloudException;

    /**
     * Get driver id -> refered cloud
     * @return the driver id from properties file
     */
    public String getDriverId();

    public String[] setAcl(String bucket, String[] canonicalId, String permission) throws StorageCloudException;
	
}

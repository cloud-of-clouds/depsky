package depskys.core;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

import pvss.PublicInfo;

/**
 * This class represents a DepSky-S register.
 * metadata file reference is regId+"metadata"
 * value file reference is regId+"value"+lastVersionNumber
 * 
 * @author tiago oliveira
 * @author bruno
 * 
 */
public class DepSkySDataUnit implements Serializable {

	private static final long serialVersionUID = 1L;
	public long lastVersionNumber;
    public String regId;
    //HashMaps for temporary info
    private HashMap<String, String> idsCache;//cache for cloud containers ids
    public HashMap<String, Long> cloudVersions;//cache for cloud data versions
    public HashMap<String, String> previousMetadata;
    public Long highestCloudVersion = null;
    private long currentHighestVersion = -1;
    public PublicInfo info;
    private boolean isPVSS;
    private boolean isErsCodes;
    private boolean isSecSharing;
    public final int n = 4; 
    public final int f = 1;
    private String str_ec_reedsol_meta;
    private String bucketName = null;

    /**
     * Class that represents a container
     * 
     * @param regId register unique identifier/name
     */
    public DepSkySDataUnit(String regId) {
        this.regId = regId;
        this.lastVersionNumber = -1;
        this.idsCache = new HashMap<String, String>();
        this.cloudVersions = new HashMap<String, Long>();
        this.previousMetadata = new HashMap<String, String>();
        this.isPVSS = this.isErsCodes = this.isSecSharing = false;
        this.bucketName = null;
    }
    
    public DepSkySDataUnit(String regId, String bucketName) {
        this.regId = regId;
        this.lastVersionNumber = -1;
        this.idsCache = new HashMap<String, String>();
        this.cloudVersions = new HashMap<String, Long>();
        this.previousMetadata = new HashMap<String, String>();
        this.isPVSS = this.isErsCodes = this.isSecSharing = false;
        this.bucketName = bucketName; 
    }
    public String getMetadataFileName() {
        return regId + "metadata";
    }

    public String getValueDataFileNameLastKnownVersion() {
        return regId + "value" + lastVersionNumber;
    }

    public String getGivenVersionValueDataFileName(String vn) {
        return regId + "value" + vn;
    }

    public String getContainerName() {
        return (regId + "container").toLowerCase();
        //must be lower case because of AWSs3 (only allows lowercase letters in container ids)
    }

    public String getContainerId(String cloudId) {
    	if(idsCache.get(cloudId) == null){
    		for(int i = 0; i < idsCache.size(); i++){
    			if(idsCache.get(i) != null)
    				return idsCache.get(i);
    		}
    		
    	}
        return idsCache.get(cloudId);
    }

    public void setContainerId(String cloudId, String cid) {
        if (!idsCache.containsKey(cloudId) && cid != null) {
            idsCache.put(cloudId, cid);
        }else if(cid == null){
        	for(int i = 0; i < idsCache.size(); i++){
    			if(idsCache.get(i) != null)
    				idsCache.put(cloudId, idsCache.get(i));
    		}
        }
    }

    public Long getCloudVersion(String cloudId) {
        return cloudVersions.get(cloudId);
    }

    public void setCloudVersion(String cloudId, long version) {
        cloudVersions.put(cloudId, version);
        if (version > currentHighestVersion) {
            currentHighestVersion = version;
        }
        if (cloudVersions.size() >= n - f) {
            highestCloudVersion = currentHighestVersion;
        }
    }

    public Long getMaxVersion() {
        if (cloudVersions.size() >= n - f) {
            return highestCloudVersion;
        }
        return null;
    }
    
    public String getBucketName(){
    	return this.bucketName;
    }
    
    public void setPreviousMetadata(String cloudId, String strmdinfo) {
        previousMetadata.put(cloudId, strmdinfo);
    }

    public String getPreviousMetadata(String cloudId) {
        return previousMetadata.get(cloudId);
    }

    public void clearAllCaches() {
        idsCache.clear();
        cloudVersions.clear();
        previousMetadata.clear();
        highestCloudVersion = null;
        currentHighestVersion = -1;
    }

    public String toString() {
        return "DepSkySRegister: " + regId + " # "
                + getContainerName() + "\n"
                + Arrays.toString(idsCache.keySet().toArray()) + "\n"
                + Arrays.toString(idsCache.values().toArray()) + "\n"
                + getMetadataFileName()
                + "lastVN = " + lastVersionNumber;
    }
    
    /**
     * @param isPVSS - set to true if want to use erasure codes and secret sharing
     */
    public void setUsingPVSS(boolean isPVSS) {
    	this.isSecSharing = false;
    	this.isErsCodes = false;
        this.isPVSS = isPVSS;
    }
    
    /**
     * @param isErsCodes - set true if want to use only erasure codes
     */
    public void setUsingErsCodes(boolean isErsCodes){
    	this.isPVSS = true;
    	this.isErsCodes = isErsCodes;
    }
    
    /**
     * @param isSecSharing - set to true if want to use only secret sharing
     */
    public void setUsingSecSharing(boolean isSecSharing){
    	this.isPVSS = true;
    	this.isSecSharing = isSecSharing;
    }
    
    public boolean isErsCodes(){
    	return this.isErsCodes;
    }
    
    public boolean isSecSharing(){
    	return this.isSecSharing;
    }
    public boolean isPVSS() {
        return this.isPVSS;
    }

    public void setPVSSinfo(PublicInfo info) {
        this.info = info;
    }

    public void setPVSSinfo(String[] info) {
//        if (info.length < 5) {
//            throw new RuntimeException("PVSS public info requires 5 parameters");
//        }
        this.info = new PublicInfo(Integer.parseInt(info[0]),
                Integer.parseInt(info[1]), new BigInteger(info[2]),
                new BigInteger(info[3]), new BigInteger(info[4]));
    }

    public String getPVSSPublicInfoAsString() {
        if (info == null) {
            return null;
        } else {
            return String.format("%s;%s;%s;%s;%s", new Object[]{info.getN(),
                        info.getT(), info.getGroupPrimeOrder(), info.getGeneratorg(),
                        info.getGeneratorG()});
        }
    }

    public void setErCodesReedSolMeta(byte[] metabytes) {
    	 Scanner s = new Scanner(new ByteArrayInputStream(metabytes));
         str_ec_reedsol_meta = String.format("%s;%s;%s;%s;%s;",s.nextLine(), s.nextLine(), s.nextLine(), s.nextLine(), s.nextLine());
         s.close();
    }

    public void setErCodesReedSolMeta(String meta) {
        str_ec_reedsol_meta = meta;
    }

    public String getErCodesReedSolMeta() {
        return str_ec_reedsol_meta;
    }
    
    public String getRegId(){
    	return this.regId;
    }
}

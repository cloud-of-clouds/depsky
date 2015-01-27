package depskys.clouds;

import java.util.Properties;

import depskys.core.DepSkySDataUnit;

/**
 * Class that represents a request for each cloud
 * 
 * @author bruno quaresma
 * @author tiago oliveira
 *
 */
public class CloudRequest {

    public int op, seqNumber, protoOp, retries;
    public String cid, did, vNumber, vHash, permission;
    public String[] namesToDelete, canonicalId, accessToOtherAccount;
    public DepSkySDataUnit reg;
    public byte[] w_data, allDataHash, hashMatching;
    public Properties props;
    public boolean isMetadataFile;
    public long startTime, metadataReceiveTime;
    public int invokerId, lockDuration, numVersionToKeep;
    public String resourceToLock;

    public CloudRequest(int op, int seqNumber, String cid, String did,
            byte[] w_data, Properties props, DepSkySDataUnit reg, int protoOp,
            boolean isMetadataFile, byte[] hashMatching, String[] accessToOtherAccount) {
        this.op = op;
        this.seqNumber = seqNumber;
        this.cid = cid;
        this.did = did;
        this.w_data = w_data;
        this.props = props;
        this.reg = reg;
        this.protoOp = protoOp;
        this.isMetadataFile = isMetadataFile;
        this.hashMatching = hashMatching;
        this.accessToOtherAccount = accessToOtherAccount;
    }

       
    public CloudRequest(int op, int seqNumber, String cid, String did,
            byte[] w_data, Properties props, DepSkySDataUnit reg, int protoOp,
            boolean isMetadataFile, String vNumber, String vHash, byte[] allDataHash, String[] accessToOtherAccount) {
        this.op = op;
        this.seqNumber = seqNumber;
        this.cid = cid;
        this.did = did;
        this.w_data = w_data;
        this.props = props;
        this.reg = reg;
        this.isMetadataFile = isMetadataFile;
        this.protoOp = protoOp;
        this.vNumber = vNumber;
        this.vHash = vHash;
        this.allDataHash = allDataHash;
        this.accessToOtherAccount = accessToOtherAccount;
    }
    
    public CloudRequest(int op, int seqNumber, String cid, String[] namesToDelete, 
    		DepSkySDataUnit reg, int protoOp, boolean isMetadataFile, String[] accessToOtherAccount) {
        this.op = op;
        this.seqNumber = seqNumber;
        this.cid = cid;
        this.namesToDelete = namesToDelete;
        this.reg = reg;
        this.isMetadataFile = isMetadataFile;
        this.protoOp = protoOp;
        this.accessToOtherAccount = accessToOtherAccount;
    }
    
    public CloudRequest(int op, int seqNumber, String cid, String did,
    		DepSkySDataUnit reg, int protoOp, String permission, String[] canonicalId){
    	   this.op = op;
           this.seqNumber = seqNumber;
           this.cid = cid;
           this.did = did;
           this.reg = reg;
           this.protoOp = protoOp;
           this.permission = permission;
           this.canonicalId = canonicalId;
    }
    
    public CloudRequest(int op, int seqNumber, String cid, String did,
    		DepSkySDataUnit reg, int protoOp, boolean isMetadataFile, int numVersionToKeep){
    	   this.op = op;
           this.seqNumber = seqNumber;
           this.cid = cid;
           this.did = did;
           this.reg = reg;
           this.protoOp = protoOp;
           this.isMetadataFile = isMetadataFile;
           this.numVersionToKeep = numVersionToKeep;
    }
    
    public CloudRequest(int op, int seqNumber, String cid, String did,
    		DepSkySDataUnit reg, int protoOp, int invokerId, int lockDuration, String resourceToLock, String[] accessToOtherAccount){
    	   this.op = op;
           this.seqNumber = seqNumber;
           this.cid = cid;
           this.did = did;
           this.reg = reg;
           this.protoOp = protoOp;
           this.invokerId = invokerId;
           this.lockDuration = lockDuration;
           this.resourceToLock = resourceToLock;
           this.accessToOtherAccount = accessToOtherAccount;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setMetadataReceiveTime(long metadataReceiveTime) {
        this.metadataReceiveTime = metadataReceiveTime;
    }

    public String toString() {
        return op + ":" + seqNumber + ":" + protoOp + ":" + cid + ":" + did;
    }

    public void incrementRetries() {
        retries++;
    }

    public void resetRetries() {
        retries = 0;
    }
}

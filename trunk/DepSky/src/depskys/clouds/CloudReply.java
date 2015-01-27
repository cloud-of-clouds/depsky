package depskys.clouds;

import java.util.LinkedList;

import depskys.core.DepSkySDataUnit;

/**
 * Class that contains the response for each cloud request
 * 
 * @author bruno quaresma
 * @author tiago oliveira
 *
 */
public class CloudReply {

    public int sequence, type, protoOp, numVersionToKeep;
    public String cloudId, container, vNumber, vHash, exceptionMessage, valueFileId;
    public String[] accessToOtherAccount;
    public DepSkySDataUnit reg;
    public Object response;
    public boolean isMetadataFile;
    public byte[] value, allDataHash, hashMatching;
    public long receiveTime, initReceiveTime, startTime, metadataReceiveTime;
    public LinkedList<String> listNames;

    @Override
    public String toString() {
        return "sn:" + sequence + "#cloud:" + cloudId + "#type:" + type
                + "#regId:" + (reg != null ? reg.regId : "null")
                + "#op:" + protoOp + "#vn:" + vNumber + "#mdfile?" + isMetadataFile;
    }

    public CloudReply(int type, int sequence, String cloudId, Object response,
            String container, DepSkySDataUnit reg, int protoOp, boolean isMetadataFile, byte[] hashMatching) {
        this.type = type;
        this.sequence = sequence;
        this.cloudId = cloudId;
        this.response = response;
        this.container = container;
        this.reg = reg;
        this.protoOp = protoOp;
        this.isMetadataFile = isMetadataFile;
        this.hashMatching = hashMatching;
    }

    public CloudReply(int type, int sequence, String cloudId, Object response,
            String container, DepSkySDataUnit reg, int protoOp, boolean isMetadataFile,
            byte[] value, String vNumber, byte[] allDataHash, LinkedList<String> list_names, byte[] hashMatching, String[] accessToOtherAccount) {
        this.type = type;
        this.sequence = sequence;
        this.cloudId = cloudId;
        this.response = response;
        this.container = container;
        this.reg = reg;
        this.protoOp = protoOp;
        this.isMetadataFile = isMetadataFile;
        this.value = value;
        this.vNumber = vNumber;
        this.allDataHash = allDataHash;
        this.listNames = list_names;
        this.hashMatching = hashMatching;
        this.accessToOtherAccount = accessToOtherAccount;
    }

    public CloudReply(int type, int sequence, String cloudId, Object response,
            String container, DepSkySDataUnit reg, int protoOp, boolean isMetadataFile,
            String vNumber, String vHash, byte[] allDataHash, byte[] hashMatching, String[] accessToOtherAccount, int numVersionToKeep) {
        this.type = type;
        this.sequence = sequence;
        this.cloudId = cloudId;
        this.response = response;
        this.container = container;
        this.reg = reg;
        this.isMetadataFile = isMetadataFile;
        this.protoOp = protoOp;
        this.vNumber = vNumber;
        this.vHash = vHash;
        this.allDataHash = allDataHash;
        this.hashMatching = hashMatching;
        this.accessToOtherAccount = accessToOtherAccount;
        this.numVersionToKeep = numVersionToKeep;
    }
    
    

    public void setVersionNumber(String vNumber) {
        this.vNumber = vNumber;
    }
    
    public void setListNames(LinkedList<String> listNames){
    	this.listNames = listNames;
    }

    public void setVersionHash(String vHash) {
        this.vHash = vHash;
    }

    public void setReceiveTime(long receiveTime) {
        this.receiveTime = receiveTime;
    }

    public void setInitReceiveTime(long initReceiveTime) {
        this.initReceiveTime = initReceiveTime;
    }

    public void setExceptionMessage(String msg) {
        this.exceptionMessage = msg;
    }

    public void setValueFileId(String valueFileId) {
        this.valueFileId = valueFileId;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CloudReply) || cloudId == null) {
            return false;
        }
        CloudReply r = (CloudReply) o;
        return sequence == r.sequence && cloudId.equals(r.cloudId);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + this.sequence;
        hash = 29 * hash + (this.cloudId != null ? this.cloudId.hashCode() : 0);
        return hash;
    }

    public void invalidateResponse() {
        this.response = null;
        this.vNumber = null;
        this.vHash = null;
    }

    void setMetadataReceiveTime(long metadataReceiveTime) {
        this.metadataReceiveTime = metadataReceiveTime;
    }
}

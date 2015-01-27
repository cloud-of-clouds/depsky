package depskys.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import depskyDep.DepSkySKeyLoader;
import depskyDep.IDepSkySDriver;
import depskys.clouds.CloudRepliesControlSet;
import depskys.clouds.CloudReply;
import depskys.clouds.CloudRequest;
import depskys.clouds.DepSkySCloudManager;
import depskys.clouds.ICloudDataManager;

/**
 * Class that process and construct new metadata files, and also do some security evaluations
 * @author tiago oliveira
 * @author bruno
 *
 */
public class DepSkySManager implements ICloudDataManager {

	public static final int RSA_SIG_LEN = 128;
	public static final int MAX_CLIENTS = 1000;//(ids: 0 to 999)
	public static final int READ_PROTO = 0;
	public static final int WRITE_PROTO = 1;
	public static final int ACL_PROTO = 2;
	public static final int DELETE_ALL = 3;
	public static final int LOCK_PROTO = 4;
	public static final int GC_PROTO = 5;
	public static final String CRLF = "\r\n";
	public DepSkySCloudManager[] driversManagers;
	public DepSkySKeyLoader keyLoader;
	public /*IDepSkySProtocol*/ LocalDepSkySClient depskys;
	public ConcurrentHashMap<String, LinkedList<DepSkyMetadata>> cloud1;
	public ConcurrentHashMap<String, LinkedList<DepSkyMetadata>> cloud2;
	public ConcurrentHashMap<String, LinkedList<DepSkyMetadata>> cloud3;
	public ConcurrentHashMap<String, LinkedList<DepSkyMetadata>> cloud4;

	public DepSkySManager(IDepSkySDriver[] drivers, IDepSkySProtocol depskys, DepSkySKeyLoader keyLoader) {
		this.driversManagers = new DepSkySCloudManager[drivers.length];
		this.keyLoader = keyLoader;
		this.depskys = (LocalDepSkySClient) depskys;

		cloud1 = new ConcurrentHashMap<String, LinkedList<DepSkyMetadata>>();
		cloud2 = new ConcurrentHashMap<String, LinkedList<DepSkyMetadata>>();
		cloud3 = new ConcurrentHashMap<String, LinkedList<DepSkyMetadata>>();
		cloud4 = new ConcurrentHashMap<String, LinkedList<DepSkyMetadata>>();

		init(drivers);
	}

	private void init(IDepSkySDriver[] drivers) {
		for (int i = 0; i < drivers.length; i++) {
			driversManagers[i] = new DepSkySCloudManager(drivers[i],
					this, depskys);
		}
	}
	/**
	 * Clear all request in queue to process
	 */
	public void clearAllRequestsToProcess() {
		for (int i = 0; i < driversManagers.length; i++) {
			driversManagers[i].resetRequests();
		}
	}

	/**
	 * Process metadata read from clouds. If is about an operation of write type, after verify a 
	 * correct metadata, the broadcast ends. If is operation fo read type,after verify a correct 
	 * metadata, is done a request to read the data requested
	 * 
	 * @param metadataReply - reply that contains information about the response of each request
	 * 
	 */
	public void processMetadata(CloudReply metadataReply) {
		try {
			metadataReply.setReceiveTime(System.currentTimeMillis());
			ByteArrayInputStream biss = new ByteArrayInputStream((byte[]) metadataReply.response);
			ObjectInputStream ins = new ObjectInputStream(biss);	
			biss.close();

			LinkedList<DepSkyMetadata> allmetadata = new LinkedList<DepSkyMetadata>();	

			int size = ins.readInt();
			byte[] metadataInit = new byte[size];
			DepSkyMetadata.readFromOI(ins, metadataInit);
			size = ins.readInt();
			byte[] allMetadataSignature = new byte[size];
			DepSkyMetadata.readFromOI(ins, allMetadataSignature);

			ins.close();

			biss = new ByteArrayInputStream(metadataInit);
			ins = new ObjectInputStream(biss);

			size = ins.readInt();
			for(int i = 0 ; i<size ; i++){
				DepSkyMetadata meta = new DepSkyMetadata();
				meta.readExternal(ins);
				allmetadata.add(meta);
			}

			biss.close();
			ins.close();

			String datareplied = null;
			DepSkyMetadata dm = null;
			int cont = 0;

			//if is a request to delete a entire dataUnit
			if(metadataReply.protoOp == DepSkySManager.DELETE_ALL){
				String[] namesToDelete = new String[allmetadata.size() + 1];
				namesToDelete[0] = metadataReply.reg.getMetadataFileName();
				for(int i = 1; i < allmetadata.size()+1; i++){
					namesToDelete[i] = allmetadata.get(i-1).getVersionFileId();
				}

				DepSkySCloudManager manager = getDriverManagerByDriverId(metadataReply.cloudId);
				CloudRequest r = new CloudRequest(DepSkySCloudManager.DEL_CONT,
						metadataReply.sequence,
						metadataReply.container,namesToDelete,metadataReply.reg, 
						DepSkySManager.DELETE_ALL, false, metadataReply.accessToOtherAccount);
				manager.doRequest(r);

				return ;
			}
			//if is a request do garbage collect a dataUnit
			if(metadataReply.protoOp == DepSkySManager.GC_PROTO){
				String[] namesToDelete = new String[allmetadata.size() - metadataReply.numVersionToKeep];
				int j = 0;
				for(int i = allmetadata.size() - 1; i >= 0; i--){
					if(i >= metadataReply.numVersionToKeep)
						namesToDelete[j] = allmetadata.get(i).getVersionFileId();
					j++;
				}
				for(String a : namesToDelete){
					System.out.println("-- " + a);
				}
				DepSkySCloudManager manager = getDriverManagerByDriverId(metadataReply.cloudId);
				CloudRequest r = new CloudRequest(DepSkySCloudManager.DEL_CONT,
						metadataReply.sequence,
						metadataReply.container,namesToDelete,metadataReply.reg, 
						DepSkySManager.GC_PROTO, false, metadataReply.accessToOtherAccount);
				manager.doRequest(r);

				return ;
				
			}

			//if is a read MAtching operation
			if(metadataReply.hashMatching != null){
				for(int i = 0; i < allmetadata.size(); i++){
					if(Arrays.equals(allmetadata.get(i).getAllDataHash(), metadataReply.hashMatching)){
						dm = allmetadata.get(i);
						datareplied = dm.getMetadata();
						if (datareplied.length() < 1) {
							throw new Exception("invalid metadata size received");
						}
						cont = allmetadata.size()+1;
					}
				}
				if(cont < allmetadata.size()+1 )
					throw new Exception("no matching version available");
			}else{ //if is a normal read (last version read)
				dm = allmetadata.getFirst();
				datareplied = dm.getMetadata();
				if (datareplied.length() < 1) {
					throw new Exception("invalid metadata size received");
				}
			}

			byte[] mdinfo = datareplied.getBytes();
			byte[] signature = dm.getsignature();
			Properties props = new Properties();
			props.load(new ByteArrayInputStream(mdinfo));
			//METADATA info
			String verNumber = props.getProperty("versionNumber");
			String verHash = props.getProperty("versionHash");
			String verValueFileId = props.getProperty("versionFileId");
			String verPVSSinfo = props.getProperty("versionPVSSinfo");
			String verECinfo = props.getProperty("versionECinfo");
			long versionfound = Long.parseLong(verNumber);
			//extract client id
			Long writerId = versionfound % MAX_CLIENTS;
			//metadata signature check
			if (!verifyMetadataSignature(writerId.intValue(), mdinfo, signature) ||
					!verifyMetadataSignature(writerId.intValue(), metadataInit, allMetadataSignature)) {
				//invalid signature
				System.out.println("...........................");
				throw new Exception("Signature verification failed for " + metadataReply);
			}
			//set the data unit to the protocol in use
			if ((verPVSSinfo != null || verECinfo != null)/* && metadataReply.reg.info == null*/) {
				if(verPVSSinfo != null && verECinfo == null){
					metadataReply.reg.setUsingSecSharing(true);
					metadataReply.reg.setPVSSinfo(verPVSSinfo.split(";"));
				}
				if(verECinfo != null && verPVSSinfo == null){
					metadataReply.reg.setUsingErsCodes(true);
					if(metadataReply.protoOp == DepSkySManager.READ_PROTO){
						metadataReply.reg.setErCodesReedSolMeta(verECinfo);							
					}
				}	
				if(verECinfo != null && verPVSSinfo !=null){
					metadataReply.reg.setUsingPVSS(true);
					metadataReply.reg.setPVSSinfo(verPVSSinfo.split(";"));
					if(metadataReply.protoOp == DepSkySManager.READ_PROTO){
						metadataReply.reg.setErCodesReedSolMeta(verECinfo);	

					}
				}
			}
			long ts = versionfound - writerId;//remove client id from versionNumber
			metadataReply.setVersionNumber(ts + "");//version received
			metadataReply.setVersionHash(verHash);
			metadataReply.setValueFileId(verValueFileId);//added

			if(metadataReply.protoOp == DepSkySManager.ACL_PROTO ){

				depskys.dataReceived(metadataReply);
				return;
			}
			
			if(metadataReply.protoOp == DepSkySManager.LOCK_PROTO){
				depskys.dataReceived(metadataReply);
				return;
			}

			if (metadataReply.protoOp == DepSkySManager.WRITE_PROTO) {
				if(metadataReply.cloudId.equals("cloud1")){
					cloud1.put(metadataReply.container, allmetadata);
				}else if(metadataReply.cloudId.equals("cloud2")){
					cloud2.put(metadataReply.container, allmetadata);
				}else if(metadataReply.cloudId.equals("cloud3")){
					cloud3.put(metadataReply.container, allmetadata);
				}else if(metadataReply.cloudId.equals("cloud4")){
					cloud4.put(metadataReply.container, allmetadata);
				}
				//				if (metadataReply.sequence < 0) {
				//					//System.out.println("read metadata, now sending write value...");
				//					DepSkySCloudManager manager = getDriverManagerByDriverId(metadataReply.cloudId);
				//					CloudRequest r = new CloudRequest(DepSkySCloudManager.NEW_DATA,
				//							metadataReply.sequence, manager.driver.getSessionKey(),
				//							metadataReply.container,
				//							metadataReply.reg.getGivenVersionValueDataFileName(ts + ""),
				//							depskys.testData, null,
				//							metadataReply.reg, metadataReply.protoOp, false,
				//							ts + "", verHash, metadataReply.allDataHash);
				//					r.setStartTime(metadataReply.startTime);
				//					r.setMetadataReceiveTime(metadataReply.metadataReceiveTime);
				//					manager.doRequest(r);//request valuedata file
				//					return;
				//				}
				depskys.dataReceived(metadataReply);
				return;
			}
			synchronized (this) {
				if (metadataReply.sequence == depskys.lastReadMetadataSequence) {
					if (depskys.lastMetadataReplies == null) {
						depskys.lastMetadataReplies = new ArrayList<CloudReply>();
					}
					depskys.lastMetadataReplies.add(metadataReply);
					metadataReply.reg.setCloudVersion(metadataReply.cloudId, ts);
					//System.out.println("IN:CLOUD VERSION " + ts + " for " + metadataReply.cloudId);
				}
				if (metadataReply.sequence >= 0
						&& canReleaseAndReturn(metadataReply)) {
					//check release
					return;
				}
				if (!depskys.sendingParallelRequests() && depskys.sentOne) {
					//                    depskys.dataReceived(metadataReply);
				} else {
					depskys.sentOne = true;
					DepSkySCloudManager manager = getDriverManagerByDriverId(metadataReply.cloudId);
					CloudRequest r = new CloudRequest(DepSkySCloudManager.GET_DATA,
							metadataReply.sequence,
							metadataReply.container, verValueFileId, null, null,
							metadataReply.reg, metadataReply.protoOp, false,
							ts + "", verHash, null, metadataReply.accessToOtherAccount);
					r.setStartTime(metadataReply.startTime);
					r.setMetadataReceiveTime(metadataReply.metadataReceiveTime);
					manager.doRequest(r);//request valuedata file
				}
			}//end synch this

		} catch (Exception ex) {
//			ex.printStackTrace();
//			System.out.println("ERROR_PROCESSING_METADATA: " + metadataReply);
			metadataReply.invalidateResponse();
		}
	}

	private boolean canReleaseAndReturn(CloudReply mdreply) {
		/*required in the case where we are waiting for n - f metadata replies and already have the value*/
		try {
			CloudRepliesControlSet rcs = depskys.replies.get(mdreply.sequence);

			if(rcs != null){
				if (mdreply.reg.cloudVersions.size() >= depskys.N - depskys.F
						&& rcs.replies.size() > depskys.F
						&& mdreply.reg.isPVSS()) {
					//System.out.println("pvssRnR_REPLIES SIZE = " + rcs.replies.size());
					for (int i = 0; i < rcs.replies.size() - 1; i++) {
						CloudReply r = rcs.replies.get(i);
						if (r.response != null && r.vNumber != null
								&& rcs.replies.get(i).vNumber.equals(mdreply.reg.getMaxVersion() + "")
								&& rcs.replies.get(i + 1).vNumber.equals(mdreply.reg.getMaxVersion() + "")) {
							//System.out.println("RELEASED#processMetadata");
							rcs.waitReplies.release();
							return true;
						}
					}
				}
				if (mdreply.reg.cloudVersions.size() >= depskys.N - depskys.F
						&& rcs.replies.size() > 0
						&& !mdreply.reg.isPVSS()) {
					//System.out.println("normalRnR_REPLIES SIZE = " + rcs.replies.size());
					for (int i = 0; i < rcs.replies.size(); i++) {
						CloudReply r = rcs.replies.get(i);
						if (r.response != null && r.vNumber != null
								&& rcs.replies.get(i).vNumber.equals(mdreply.reg.getMaxVersion() + "")) {
							//System.out.println("RELEASED#processMetadata");
							rcs.waitReplies.release();
							return true;
						}
					}
				}

			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return false;
	}

	/**
	 * Verify the data integrity
	 * 
	 * @param valuedataReply - reply with the metadata that contains the metedata e data for 
	 *                         a DataUnit
	 *                                                  
	 */
	public void checkDataIntegrity(CloudReply valuedataReply) {
		try {
			byte[] value = (byte[]) valuedataReply.response; //data value
			String valuehash = getHexString(getHash(value)); //hash of data value
			valuedataReply.setReceiveTime(System.currentTimeMillis());
			if (valuehash.equals(valuedataReply.vHash)) { //comparing hash of data value with the hash presented in metadata file
				depskys.dataReceived(valuedataReply);
			} else {
				throw new Exception("integrity verification failed... " + valuedataReply);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			valuedataReply.invalidateResponse();
			valuedataReply.setExceptionMessage(ex.getMessage());
		}
	}

	/**
	 * After the data file is write, new metadata is processed and then stored in the clouds
	 *
	 * @param reply - reply after a request processed
	 */
	public void writeNewMetadata(CloudReply reply) {
		ByteArrayOutputStream allmetadata = null;
		ObjectOutputStream out = null;
		try {
			//build new version metadata
			allmetadata = new ByteArrayOutputStream();
			out = new ObjectOutputStream(allmetadata);
			String valueDataFileId = (String) reply.response;
			String mprops = "versionNumber = " + reply.vNumber + CRLF
					+ "versionHash = " + getHexString(getHash(reply.value)) + CRLF
					+ "allDataHash = " + reply.allDataHash + CRLF
					+ "versionFileId = " + valueDataFileId + CRLF;
			if(reply.reg.isErsCodes()){
				mprops += "versionECinfo = " + reply.reg.getErCodesReedSolMeta() + CRLF;
			}else if(reply.reg.isSecSharing()){
				mprops += "versionPVSSinfo = " + reply.reg.getPVSSPublicInfoAsString() + CRLF;
			}else if (reply.reg.isPVSS()) {
				//if this refers to a Data Unit with PVSS
				mprops += "versionPVSSinfo = " + reply.reg.getPVSSPublicInfoAsString() + CRLF;
				mprops += "versionECinfo = " + reply.reg.getErCodesReedSolMeta() + CRLF;
			}

			//getting the last versions metadata information
			DepSkyMetadata newMD = new DepSkyMetadata(mprops, getSignature(mprops.getBytes()), reply.allDataHash, valueDataFileId);
			LinkedList<DepSkyMetadata> oldMetadata = new LinkedList<DepSkyMetadata>();
			if(reply.cloudId.equals("cloud1")){
				if(cloud1.containsKey(reply.container)){
					oldMetadata = new LinkedList<DepSkyMetadata>(cloud1.get(reply.container));
					cloud1.remove(reply.container);
				}
			}else if(reply.cloudId.equals("cloud2")){
				if(cloud2.containsKey(reply.container)){
					oldMetadata = new LinkedList<DepSkyMetadata>(cloud2.get(reply.container));
					cloud2.remove(reply.container);
				}
			}else if(reply.cloudId.equals("cloud3")){
				if(cloud3.containsKey(reply.container)){
					oldMetadata = new LinkedList<DepSkyMetadata>(cloud3.get(reply.container));
					cloud3.remove(reply.container);
				}
			}else if(reply.cloudId.equals("cloud4")){
				if(cloud4.containsKey(reply.container)){
					oldMetadata = new LinkedList<DepSkyMetadata>(cloud4.get(reply.container));
					cloud4.remove(reply.container);
				}
			}

			oldMetadata.addFirst(newMD);
			out.writeInt(oldMetadata.size());
			for(int i = 0 ; i<oldMetadata.size() ; i++){
				oldMetadata.get(i).writeExternal(out);
			}
			out.close();
			allmetadata.close();
			byte[] metadataInit = allmetadata.toByteArray();
			byte[] allMetadataSignature = getSignature(metadataInit);


			allmetadata = new ByteArrayOutputStream();
			out = new ObjectOutputStream(allmetadata);

			out.writeInt(metadataInit.length);
			out.write(metadataInit);
			out.writeInt(allMetadataSignature.length);
			out.write(allMetadataSignature);

			out.flush();
			allmetadata.flush();
			allmetadata.close();

			//request to write new metadata file
			DepSkySCloudManager manager = getDriverManagerByDriverId(reply.cloudId);
			CloudRequest r = new CloudRequest(DepSkySCloudManager.NEW_DATA,
					reply.sequence,
					reply.container, reply.reg.regId + "metadata",
					allmetadata.toByteArray(), null, reply.reg,
					reply.protoOp, true, reply.hashMatching, reply.accessToOtherAccount);
			r.setStartTime(reply.startTime);//added
			manager.doRequest(r);


		} catch (Exception ex) {
			ex.printStackTrace();
		} finally{
			try {
				out.close();
				//allmetadata.close();
			} catch (IOException e) {

			}
		}




	}

	/**
	 * Signs a byte array
	 * 
	 * @param v - content to sing
	 * @return the signature of v 
	 */
	public byte[] getSignature(byte[] v) {
		try {
			Signature sig = Signature.getInstance("SHA1withRSA");
			sig.initSign(keyLoader.loadPrivateKey(depskys.getClientId()));
			sig.update(v);
			return sig.sign();
		} catch (Exception ex) {
			//ex.printStackTrace();
		}
		return null;
	}

	/**
	 * Verify if a given signature for a given byte array is valid
	 * 
	 * @param clientId - client id
	 * @param v - metadata
	 * @param signature - metadata signature 
	 * @return true is a valid signature, false otherwise
	 */
	public boolean verifyMetadataSignature(int clientId, byte[] v, byte[] signature) {
		try {
			Signature sig = Signature.getInstance("SHA1withRSA");
			sig.initVerify(keyLoader.loadPublicKey(clientId));
			sig.update(v);
			return sig.verify(signature);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}

	public DepSkySCloudManager getDriverManagerByDriverId(String id) {
		for (int i = 0; i < driversManagers.length; i++) {
			if (driversManagers[i].driver.getDriverId().equals(id)) {
				return driversManagers[i];
			}
		}
		return null;
	}

	/**
	 * Compute a hash for a given byte array
	 * 
	 * @param o the byte array to be hashed
	 * @return the hash of the byte array
	 */
	private byte[] getHash(byte[] v) throws NoSuchAlgorithmException {
		//MessageDigest md = MessageDigest.getInstance("SHA-1");
		return MessageDigest.getInstance("SHA-1").digest(v);
	}
	//base16 char table (aux in getHexString)
	private static final byte[] HEX_CHAR_TABLE = {
		(byte) '0', (byte) '1', (byte) '2', (byte) '3',
		(byte) '4', (byte) '5', (byte) '6', (byte) '7',
		(byte) '8', (byte) '9', (byte) 'a', (byte) 'b',
		(byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f'
	};

	private static String getHexString(byte[] raw)
			throws UnsupportedEncodingException {
		byte[] hex = new byte[2 * raw.length];
		int index = 0;
		for (byte b : raw) {
			int v = b & 0xFF;
			hex[index++] = HEX_CHAR_TABLE[v >>> 4];
			hex[index++] = HEX_CHAR_TABLE[v & 0xF];
		}
		return new String(hex, "ASCII");
	}

	public void doRequest(String cloudId, CloudRequest request) {
		getDriverManagerByDriverId(cloudId).doRequest(request);
	}
}

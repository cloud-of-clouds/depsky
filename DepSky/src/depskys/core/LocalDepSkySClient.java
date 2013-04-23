package depskys.core;

import google.GoogleStorageDriver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.IOUtils;

import pvss.InvalidVSSScheme;
import pvss.PVSSEngine;
import pvss.PublicInfo;
import pvss.PublishedShares;
import pvss.Share;
import rackazure.RackSpaceDriver;
import rackazure.WindowsAzureDriver;
import reedsol.ReedSolDecoder;
import reedsol.ReedSolEncoder;
import util.Pair;
import amazon.AmazonS3Driver;
import depskyDep.IDepSkySDriver;
import depskys.clouds.CloudRepliesControlSet;
import depskys.clouds.CloudReply;
import depskys.clouds.CloudRequest;
import depskys.clouds.DepSkySCloudManager;
import depskys.clouds.drivers.LocalDiskDriver;


/**
 * Class for using DepSky
 * 
 * @author tiago oliveira
 * @author bruno quaresma
 */
public class LocalDepSkySClient implements IDepSkySProtocol{

	/**
	 * @param args
	 */
	private int clientId;
	public int N, F, T = 2/*jss_shares=f+1*/, NUM_BITS = 192;
	private int sequence = -1;
	private IDepSkySDriver cloud1, cloud2, cloud3, cloud4;
	private IDepSkySDriver[] drivers;
	private DepSkySManager manager;
	public HashMap<Integer, CloudRepliesControlSet> replies;
	public boolean parallelRequests = false;    //Optimized Read or Normal Read

	public List<CloudReply> lastReadReplies; //pointer for planet lab stats
	public List<CloudReply> lastMetadataReplies; //pointer for planet lab stats
	public int lastReadMetadataSequence = -1, lastReadRepliesMaxVerIdx = -1;
	public boolean sentOne = false;
	public byte[] testData = null;
	private byte[] response = null;
	private ReedSolDecoder decoder;
	private ReedSolEncoder encoder;

	/**
	 * 
	 * @param clientId - client ID
	 * @param useModel - if false, DepSky will work locally (all the data is stored in local machine - you
	 *                   must run the server first, see README file),
	 *                   if true, cloudofclouds are used instead
	 * 
	 */
	public LocalDepSkySClient(int clientId, boolean useModel){

		this.clientId = clientId;
		if(!useModel){
			this.cloud1 = new LocalDiskDriver("cloud1");
			this.cloud2 = new LocalDiskDriver("cloud2");
			this.cloud3 = new LocalDiskDriver("cloud3");
			this.cloud4 = new LocalDiskDriver("cloud4");
			this.drivers = new IDepSkySDriver[]{cloud1, cloud2, cloud3, cloud4};
		}else{	
			List<String[][]> credentials = null;
			try {
				credentials = readCredentials();
			} catch (FileNotFoundException e) {
				System.out.println("O ficheiro de configuração não existe!");
				e.printStackTrace();
			} catch (ParseException e) {
				System.out.println("O ficheiro de configuração está mal configurado!");		
				e.printStackTrace();
			}
			this.drivers = new IDepSkySDriver[4];
			String type = null, driverId = null, accessKey = null, secretKey = null;
			for(int i = 0 ; i < credentials.size(); i++){
				for(String[] pair : credentials.get(i)){
					if(pair[0].equalsIgnoreCase("driver.type")){
						type = pair[1];
					}else if(pair[0].equalsIgnoreCase("driver.id")){
						driverId = pair[1];
					}else if(pair[0].equalsIgnoreCase("accessKey")){
						accessKey = pair[1];
					}else if(pair[0].equalsIgnoreCase("secretKey")){
						secretKey = pair[1];
					}
				}
				drivers[i] = DriversFactory.getDriver(type, driverId, accessKey, secretKey);
			}

		}	
		this.manager = new DepSkySManager(drivers, this);
		this.replies = new HashMap<Integer, CloudRepliesControlSet>();
		this.N = drivers.length;
		this.F = 1;
		this.encoder = new ReedSolEncoder(2, 2, 8);
		this.decoder = new ReedSolDecoder(2, 2, 8);

		if(!startDrivers()){
			System.out.println("Erro na conexão");
		}	
	}

	public int getClientId() {
		return this.clientId;
	}

	/**
	 * Read the version of the file associated reg that match with hashMatching
	 * 
	 * @param reg - the DataUnit associated with the file
	 * @param hashMatching - the hash that represents the file version for read
	 * @return the read data
	 * @throws Exception
	 * 
	 */
	public synchronized byte[] readMatching(DepSkySDataUnit reg, byte[] hashMatching) throws Exception{


		parallelRequests = true;
		lastMetadataReplies = null;
		CloudRepliesControlSet rcs = null;

		try{

			int seq = getNextSequence();
			rcs = new CloudRepliesControlSet(N, seq);
			//broadcast to all clouds to get the metadata file and after the version requested
			broadcastGetMetadata(seq, reg,
					DepSkySManager.READ_PROTO, hashMatching);
			replies.put(seq, rcs);
			int nullResponses = 0;

			lastReadMetadataSequence = seq;
			rcs.waitReplies.acquire(); //blocks until this semaphore is released
			lastReadReplies = rcs.replies;
			int[] versionReceived = new int[N];
			int maxVerCounter = 0, oldVerCounter = 0;

			//process replies to analyze if we get correct responses 
			for (int i = 0; i < rcs.replies.size(); i++) {
				CloudReply r = rcs.replies.get(i);	
				if (r.response == null || r.type != DepSkySCloudManager.GET_DATA
						|| r.vNumber == null || r.reg == null) {
					nullResponses++;
					//Fault check #1
					if (nullResponses > N - F) {
						throw new Exception("READ ERROR: DepSky-S DataUnit does not exist or client is offline (internet connection failed)");
					} else if (nullResponses > F) {
						//System.out.println(r.response + "  \n" + r.type + "  \n" + r.vNumber + "  \n" + r.reg.toString());
						throw new Exception("READ ERROR: at least f + 1 clouds failed");
					}
				} else {
					Long maxVersionFound = r.reg.getMaxVersion();
					//process replies
					if (reg.isPVSS()) {
						//Data Unit using PVSS (retuns when having f + 1 sequential replies with maxVersionFound)
						if (r.reg.getMaxVersion()==null || maxVersionFound.longValue() == Long.parseLong(r.vNumber)) {
							//have max version
							versionReceived[i] = 1;
							maxVerCounter++;
						} else {
							//have old version
							versionReceived[i] = 2;
							oldVerCounter++;
						}
					} else {
						//Data Unit NOT using PVSS (returns first reply with maxVersionFound in metadata)
						if (maxVersionFound.longValue() == Long.parseLong(r.vNumber)) {
							//                            reg.clearAllCaches();
							lastReadRepliesMaxVerIdx = i;
							return (byte[]) r.response;
						}
					}
				}
			}//for replies

			//get the value of each response (each cloud could have differents blocks)
			Share[] keyshares = new Share[N];
			Map<String, byte[]> erasurec = new HashMap<>();
			if(reg.isErsCodes() || reg.isSecSharing() || reg.isPVSS()){

				for (int i = 0; i < rcs.replies.size(); i++) {
					if (maxVerCounter >= T && versionReceived[i] != 1) {
						CloudReply resp = rcs.replies.get(i);
						resp.invalidateResponse();
					} else if (oldVerCounter >= T && versionReceived[i] != 2) {
						CloudReply resp = rcs.replies.get(i);
						resp.invalidateResponse();
					}
				}
				for (CloudReply r : rcs.replies) {
					int i = 0;
					if(r.response != null){
						byte[] ecksobjbytes = (byte[]) r.response;
						ByteArrayInputStream bais = new ByteArrayInputStream(ecksobjbytes);
						ObjectInputStream ois = new ObjectInputStream(bais);
						ECKSObject ecksobj;
						ecksobj = (ECKSObject) ois.readObject();
						if(ecksobj.getECfilename() != null)
							erasurec.put(ecksobj.getECfilename(), ecksobj.getECbytes());
						Share sk_share = ecksobj.getSKshare();
						if(sk_share != null){
							keyshares[sk_share.getIndex()] = sk_share;
							if(i < 1)
								this.response = ecksobj.getECbytes();
							i++;
						}
					}
				}
			}

			//put together all blocks from the diferents clouds
			if(reg.isErsCodes()){
				return readErasureCodes(reg, erasurec);
			}else if(reg.isSecSharing()){
				return readSecretSharing(reg, keyshares);
			}else if (reg.isPVSS()) {
				return readSecretSharingErasureCodes(reg, keyshares, erasurec);			
			}//is PVSS

			//not pvss or something went wrong with metadata
			throw new Exception("READ ERROR: Could not get data after processing metadata");

		}catch (Exception ex) {
			//ex.printStackTrace();
			throw ex;
		} finally {
			parallelRequests = false;
			if (rcs != null) {
				replies.remove(rcs.sequence);
			}
		}

	}

	/**
	 *  Read the last version written for the file associated with reg
	 * 
	 *  @param reg - the DataUnit associated with the file
	 *  
	 */
	public synchronized byte[] read(DepSkySDataUnit reg) throws Exception {

		parallelRequests = true;
		lastMetadataReplies = null;
		CloudRepliesControlSet rcs = null;

		try{
			int seq = getNextSequence();
			rcs = new CloudRepliesControlSet(N, seq);
			//broadcast to all clouds to get the metadata file and after the version requested
			broadcastGetMetadata(seq, reg,
					DepSkySManager.READ_PROTO, null);
			replies.put(seq, rcs);
			int nullResponses = 0;
			//process value data responses
			lastReadMetadataSequence = seq;
			rcs.waitReplies.acquire(); //blocks until this semaphore is released
			lastReadReplies = rcs.replies;
			int[] versionReceived = new int[N];
			int maxVerCounter = 0, oldVerCounter = 0;

			//process replies to analyze if we get correct responses 
			for (int i = 0; i < rcs.replies.size(); i++) {
				CloudReply r = rcs.replies.get(i);	
				if (r.response == null || r.type != DepSkySCloudManager.GET_DATA
						|| r.vNumber == null || r.reg == null) {
					nullResponses++;
					//Fault check #1
					if (nullResponses > N - F) {
						throw new Exception("READ ERROR: DepSky-S DataUnit does not exist or client is offline (internet connection failed)");
					} else if (nullResponses > F) {
						//System.out.println(r.response + "  \n" + r.type + "  \n" + r.vNumber + "  \n" + r.reg.toString());
						throw new Exception("READ ERROR: at least f + 1 clouds failed");
					}
				} else {
					Long maxVersionFound = r.reg.getMaxVersion();
					//process replies
					if (reg.isPVSS()) {
						//Data Unit using PVSS (retuns when having f + 1 sequential replies with maxVersionFound)
						if (r.reg.getMaxVersion()==null || maxVersionFound.longValue() == Long.parseLong(r.vNumber)) {
							//have max version
							versionReceived[i] = 1;
							maxVerCounter++;
						} else {
							//have old version
							versionReceived[i] = 2;
							oldVerCounter++;
						}
					} else {
						//Data Unit NOT using PVSS (returns first reply with maxVersionFound in metadata)
						if (maxVersionFound.longValue() == Long.parseLong(r.vNumber)) {
							lastReadRepliesMaxVerIdx = i;
							return (byte[]) r.response; // using DepSky-A
						}
					}
				}
			}//for replies

			Share[] keyshares = new Share[N];
			Map<String, byte[]> erasurec = new HashMap<>();
			if(reg.isErsCodes() || reg.isSecSharing() || reg.isPVSS()){

				for (int i = 0; i < rcs.replies.size(); i++) {
					if (maxVerCounter >= T && versionReceived[i] != 1) {
						CloudReply resp = rcs.replies.get(i);
						resp.invalidateResponse();
					} else if (oldVerCounter >= T && versionReceived[i] != 2) {
						CloudReply resp = rcs.replies.get(i);
						resp.invalidateResponse();
					}
				}

				for (CloudReply r : rcs.replies) {
					int i = 0;
					if(r.response != null){
						byte[] ecksobjbytes = (byte[]) r.response;
						ByteArrayInputStream bais = new ByteArrayInputStream(ecksobjbytes);
						ObjectInputStream ois = new ObjectInputStream(bais);
						ECKSObject ecksobj;
						ecksobj = (ECKSObject) ois.readObject();
						if(ecksobj.getECfilename() != null)
							erasurec.put(ecksobj.getECfilename(), ecksobj.getECbytes());
						Share sk_share = ecksobj.getSKshare();
						if(sk_share != null){
							keyshares[sk_share.getIndex()] = sk_share;
							if(i < 1)
								this.response = ecksobj.getECbytes();
							i++;
						}
					}
				}

			}
			if(reg.isErsCodes()){ // unsing only erasure codes
				return readErasureCodes(reg, erasurec);
			}else if(reg.isSecSharing()){ // using only secret sharing
				return readSecretSharing(reg, keyshares);
			}else if (reg.isPVSS()){ // using DepSky-CA
				return readSecretSharingErasureCodes(reg, keyshares, erasurec);			
			}

			//not pvss or something went wrong with metadata
			throw new Exception("READ ERROR: Could not get data after processing metadata");

		}catch (Exception ex) {
			//ex.printStackTrace();
			throw ex;
		} finally {
			parallelRequests = false;
			if (rcs != null) {
				replies.remove(rcs.sequence);
			}
		}
	}

	/**
	 * Delete all the data (data and metadata files) associated with this Data Unit
	 * 
	 * @param reg - Data Unit
	 */
	public void deleteContainer(DepSkySDataUnit reg){

		CloudRepliesControlSet rcs = null;
		try {
			int seq = getNextSequence();
			rcs = new CloudRepliesControlSet(N, seq);
			replies.put(seq, rcs);
			broadcastGetMetadata(seq, reg, DepSkySManager.DELETE_ALL, null);
			rcs.waitReplies.acquire();
			lastMetadataReplies = rcs.replies;

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes the value value in the corresponding dataUnit reg
	 * 
	 * @param reg - the DataUnit associated with the file
	 * @param value - value to be written
	 * @return the hash of the value written
	 * 
	 */
	public synchronized byte[] write(DepSkySDataUnit reg, byte[] value) throws Exception {

		CloudRepliesControlSet rcs = null, wrcs = null;

		try{
			int seq = getNextSequence();
			rcs = new CloudRepliesControlSet(N, seq);
			replies.put(seq, rcs);
			//broadcast to all clouds to get the Metadata associated with this dataUnit
			broadcastGetMetadata(seq, reg,
					DepSkySManager.WRITE_PROTO, null);
			rcs.waitReplies.acquire(); //blocks until the semaphore is released
			lastMetadataReplies = rcs.replies;
			//process replies and actualize version
			int nullCounter = 0;
			long maxVersionFound = -1;

			//proccess metadata replies
			for (int i = 0; i < rcs.replies.size(); i++) {
				CloudReply r = rcs.replies.get(i);
				if (r.response == null || r.type != DepSkySCloudManager.GET_DATA
						|| r.vNumber == null) {
					nullCounter++;
					continue;
				} else {
					long version = Long.parseLong(r.vNumber);
					if (version > maxVersionFound) {
						maxVersionFound = version;
					}
				}
			}

			//when is the first write for this dataUnit (none version was found)
			if (nullCounter > F) {
				maxVersionFound = 0;
			}

			//calcule the name of the version to be written
			long nextVersion = maxVersionFound + DepSkySManager.MAX_CLIENTS + clientId;

			seq = getNextSequence();
			wrcs = new CloudRepliesControlSet(N, seq);
			replies.put(seq, wrcs);
			byte[] allDataHash = generateSHA1Hash(value);


			//do the broadcast depending on the protocol selected for use (CA, A, only erasure codes or only secret sharing)
			if(reg.isErsCodes()){
				broadcastWriteValueErasureCodes(sequence, reg, value, nextVersion + "", allDataHash);
			}else if(reg.isSecSharing()){
				broadcastWriteValueSecretKeyShares(sequence, reg, value, nextVersion + "", allDataHash);
			}else if (reg.isPVSS()) {
				broadcastWriteValueErasureCodesAndSecretKeyShares(sequence, reg, value, nextVersion + "", allDataHash);
			} else {
				broadcastWriteValueRequests(seq, reg, value, nextVersion + "", allDataHash);
			}

			wrcs.waitReplies.acquire();
			lastReadReplies = wrcs.replies;

			reg.lastVersionNumber = nextVersion;
			return allDataHash;
		} catch (Exception ex) {
			System.out.println("DEPSKYS WRITE ERROR:");
			//ex.printStackTrace();
			throw ex;
		} 

	}

	/**
	 * NOT SUPORTED YET (waiting that all clouds support ACLs by container)
	 */
	public void setAcl(DepSkySDataUnit reg, String permission, LinkedList<Pair<String, String>> cannonicalIds) throws Exception {

		CloudRepliesControlSet rcs = null, wrcs = null;

		try{
			int seq = getNextSequence();
			rcs = new CloudRepliesControlSet(N, seq);
			replies.put(seq, rcs);
			broadcastGetMetadata(seq, reg,
					DepSkySManager.ACL_PROTO, null);
			rcs.waitReplies.acquire();
			lastMetadataReplies = rcs.replies;
			//process replies and actualize version
			int nullCounter = 0;
			long maxVersionFound = -1;
			for (int i = 0; i < rcs.replies.size(); i++) {
				CloudReply r = rcs.replies.get(i);
				if (r.response == null || r.type != DepSkySCloudManager.GET_DATA
						|| r.vNumber == null) {
					nullCounter++;
					continue;
				} else {
					long version = Long.parseLong(r.vNumber);
					if (version > maxVersionFound) {
						maxVersionFound = version;
					}
				}
			}
			if(nullCounter > F){
				//fazer qualquer coisa
			}

			seq = getNextSequence();
			wrcs = new CloudRepliesControlSet(N, seq);
			replies.put(seq, wrcs);
			broadcastSetContainersACL(seq, reg, DepSkySManager.ACL_PROTO, maxVersionFound, permission, cannonicalIds);

			wrcs.waitReplies.acquire();
			lastReadReplies = wrcs.replies;


		} catch (Exception ex) {
			//System.out.println("DEPSKYS WRITE ERROR: " + ex.getMessage());
			//ex.printStackTrace();
			throw ex;
		} 
	}


	/**
	 * Method that releases (when receive N-F replies (in most of the cases)) all the locks made by
	 * broadcasts
	 * 
	 * @param reply - reply received by each broadcast containing the response of the clouds
	 * 
	 */
	public void dataReceived(CloudReply reply) {

		if (!replies.containsKey(reply.sequence)) {
			//System.out.println("NOTE: sequence " + reply.sequence
			//	+ " replies already removed - " + reply);
			return;
		}

		CloudRepliesControlSet rcs = replies.get(reply.sequence);

		if (rcs != null) {
			rcs.replies.add(reply);
		} else {
			return;
		}

		//individual test measures (only add reply)
		if (reply.sequence < 0) {
			return;
		}

		//processing reply
		if (reply.protoOp == DepSkySManager.ACL_PROTO){
			if(rcs.replies.size() == 4){
				rcs.waitReplies.release();
			}
		}else if (reply.protoOp == DepSkySManager.READ_PROTO
				&& reply.reg != null
				&& reply.reg.cloudVersions != null
				&& reply.reg.cloudVersions.size() >= (N - F)
				&& rcs.replies != null
				&& rcs.replies.size() > 0
				&& !reply.reg.isPVSS()
				&& !reply.isMetadataFile) {
			//normal & optimized read trigger (reg without PVSS)
			Long maxVersion = reply.reg.getMaxVersion();
			Long foundVersion = reply.reg.getCloudVersion(reply.cloudId);
			if (maxVersion != null
					&& maxVersion.longValue() == foundVersion.longValue()) {
				rcs.waitReplies.release();
				return;
			} else {
				//System.out.println(reply.cloudId + " does not have max version "
				//		+ maxVersion + " but has " + foundVersion);
			}
		} else if (reply.protoOp == DepSkySManager.READ_PROTO
				&& reply.reg != null
				&& reply.reg.cloudVersions != null
				&& reply.reg.cloudVersions.size() >= (N - F)
				&& reply.reg.isPVSS()
				&& rcs.replies != null
				&& rcs.replies.size() > F) {
			//SecretSharing read trigger (reg with PVSS)
			Long maxVersion = reply.reg.getMaxVersion();
			int maxcounter = 0, othercounter = 0;
			for (int i = 0; i < rcs.replies.size(); i++) {
				CloudReply r = rcs.replies.get(i);
				if (r.response != null
						&& maxVersion != null
						&& Long.parseLong(r.vNumber) == maxVersion.longValue()) {
					maxcounter++;
				} else {
					othercounter++;
				}
			}
			//check release -> have F + 1 shares of same version
			if (maxcounter > F || othercounter > F) {
				rcs.waitReplies.release();
				return;
			}
		} else if (reply.protoOp == DepSkySManager.READ_PROTO
				&& rcs.replies.size() >= N - F) {
			int nonNull = 0, nulls = 0;
			for (int i = 0; i < rcs.replies.size(); i++) {
				if (rcs.replies.get(i).response != null) {
					nonNull++;
				} else {
					nulls++;
				}
			}
			//release wait messages semaphore
			if (nonNull >= N - F || rcs.replies.size() > N - F || nulls > N-2) {
				rcs.waitReplies.release();
				return;
			}
		} else if (reply.protoOp >= DepSkySManager.WRITE_PROTO
				&& rcs.replies.size() >= N - F && reply.reg != null) {
			//write trigger (writes in all clouds)
			rcs.waitReplies.release();
			return;
		}
		
		//wait 4 replies when is about the start of the connections with the clouds and the delete operation
		if (rcs.replies.size() > N - F && reply.protoOp != DepSkySManager.WRITE_PROTO) {
			rcs.waitReplies.release();
			replies.remove(rcs.sequence);
		}

	}

	public boolean sendingParallelRequests() {
		return parallelRequests;
	}

	/**
	 * Start all connections with the clouds (this operations only get success if all the four 
	 * connections are performed correctly)
	 * 
	 * @return true if all the connections are correct, false otherwise
	 */
	private boolean startDrivers(){
		System.out.println("starting drivers...");

		int seq = getNextSequence();
		CloudRepliesControlSet rcs = new CloudRepliesControlSet(N, seq);
		replies.put(seq, rcs);
		for(int i = 0; i < 4; i++)
			manager.driversManagers[i].doRequest(
					new CloudRequest(DepSkySCloudManager.INIT_SESS, seq, null,
							null, null, null, new Properties(), null, -1, false, null));

		try {
			rcs.waitReplies.acquire();
		} catch (InterruptedException e) {
			//erro semï¿½foro
		}
		int nullCounter = 0;
		for (int i = 0; i < rcs.replies.size(); i++) {
			CloudReply r = rcs.replies.get(i);
			if (r.response == null) {
				System.out.println("erro na cloud " + r.cloudId);
				nullCounter++;
				if (nullCounter > 0) {
					System.out.println("ERROR: drivers initialization failed");
					return false;
				}
			}
		}

		replies.remove(rcs.sequence);
		System.out.println("All drivers started.");


		return true;
	}

	/* Compute the original data block when using secret sharing and erasure codes to replicate
	 * the data 
	 */
	private synchronized byte[] readSecretSharingErasureCodes(DepSkySDataUnit reg, Share[] keyshares, Map<String, byte[]> erasurec) throws Exception {

		int originalSize = Integer.parseInt(reg.getErCodesReedSolMeta().split(";")[0]);
		byte[] enc_sk = recombineSecretKeyShares(reg, keyshares);
		byte[] ecmeta = reg.getErCodesReedSolMeta().replace(";", "\r\n").getBytes();
		erasurec.put("metadata", ecmeta);
		byte[] decode = concatAll(decoder.decode(erasurec), originalSize);
		return MyAESCipher.myDecrypt(new SecretKeySpec(enc_sk, "AES"), decode);
	}

	/* Compute the original data block when using secret sharing to replicate the data 
	 */
	private synchronized byte[] readSecretSharing(DepSkySDataUnit reg, Share[] keyshares) throws Exception {

		byte[] enc_sk = recombineSecretKeyShares(reg, keyshares);

		return MyAESCipher.myDecrypt(new SecretKeySpec(enc_sk, "AES"), this.response);
	}

	/* Compute the original data block when using erasure codes to replicate the data 
	 */
	private synchronized byte[] readErasureCodes(DepSkySDataUnit reg, Map<String, byte[]> erasurec) throws Exception {

		int originalSize = Integer.parseInt(reg.getErCodesReedSolMeta().split(";")[0]);
		byte[] ecmeta = reg.getErCodesReedSolMeta().replace(";", "\r\n").getBytes();
		erasurec.put("metadata", ecmeta);
		byte[] decode = concatAll(decoder.decode(erasurec), originalSize);

		return decode;
	}


	/**
	 * 
	 * @return the identifier for the next broadcast
	 */
	private synchronized int getNextSequence() {
		sequence++;
		return sequence;
	}

	//to be utilized by the lock algorithm
	private void writeQuorum(DepSkySDataUnit reg, byte[] value, String filename){

		CloudRepliesControlSet rcs = null;
		try{
			int seq = getNextSequence();
			rcs = new CloudRepliesControlSet(N, seq);
			replies.put(seq, rcs);


			for (int i = 0; i < drivers.length; i++) {
				CloudRequest r = new CloudRequest(DepSkySCloudManager.NEW_DATA, sequence,
						drivers[i].getSessionKey(), reg.getContainerId(drivers[i].getDriverId()),
						filename, value, null,
						reg, DepSkySManager.WRITE_PROTO, true, null, null, null); //allmetadata no ultimo parametro
				manager.doRequest(drivers[i].getDriverId(), r);
			}

			rcs.waitReplies.acquire();
		}catch(Exception e){

		}
	}

	//to be utilized by the lock algorithm
	private LinkedList<LinkedList<String>> listQuorum(DepSkySDataUnit reg, String prefix) throws Exception{

		CloudRepliesControlSet rcs = null;
		try{
			int seq = getNextSequence();
			rcs = new CloudRepliesControlSet(N, seq);
			replies.put(seq, rcs);




			for (int i = 0; i < drivers.length; i++) {
				CloudRequest r = new CloudRequest(DepSkySCloudManager.LIST, seq,
						drivers[i].getSessionKey(), reg.getContainerName(), prefix,
						null, null, reg, -1, true, null);
				manager.doRequest(drivers[i].getDriverId(), r);
			}

			LinkedList<LinkedList<String>> listPerClouds = new LinkedList<LinkedList<String>>();
			rcs.waitReplies.acquire();
			int nullcounter = 0;
			for (int i = 0; i < rcs.replies.size(); i++) {
				CloudReply r = rcs.replies.get(i);	

				if(r.listNames == null){
					nullcounter++;
				}else{
					listPerClouds.add(r.listNames);
				}			
			}
			if(nullcounter > N){
				throw new Exception("sfdsdf");
			}
			return listPerClouds;

		}catch(Exception e){

		}
		return null;
	}

	//to be utilized by the lock algorithm
	private void deleteData(DepSkySDataUnit reg, String name){

		CloudRepliesControlSet rcs = null;
		try{
			int seq = getNextSequence();
			rcs = new CloudRepliesControlSet(N, seq);
			replies.put(seq, rcs);

			for (int i = 0; i < drivers.length; i++) {
				CloudRequest r = new CloudRequest(DepSkySCloudManager.DEL_DATA, seq,
						drivers[i].getSessionKey(), reg.getContainerName(), name,
						null, null, reg, -1, true, null);
				manager.doRequest(drivers[i].getDriverId(), r);
			}
			rcs.waitReplies.acquire();

		}catch(Exception e){

		}

	}

	/*
	 * Get the metadata file (used for read, write and delete operations)
	 */
	private void broadcastGetMetadata(int sequence,
			DepSkySDataUnit reg, int protoOp, byte[] hashMatching) {

		for (int i = 0; i < drivers.length; i++) {
			CloudRequest r = new CloudRequest(DepSkySCloudManager.GET_DATA, sequence,
					drivers[i].getSessionKey(), reg.getContainerName(), reg.getMetadataFileName(),
					null, null, reg, protoOp, true, hashMatching);

			if (reg.getContainerId(drivers[i].getDriverId()) == null) {
				reg.setContainerId(drivers[i].getDriverId(), reg.getContainerName());
			}
			manager.doRequest(drivers[i].getDriverId(), r);
		}

	}

	/**
	 * NOT CURRENTLY USED
	 */
	private void broadcastSetContainersACL(int sequence, DepSkySDataUnit reg, int protoOp, long version,
			String permission, LinkedList<Pair<String, String>> cannonicalIds){


		for(int i = 0; i < drivers.length; i++){
			Pair<String, String> pair = cannonicalIds.get(i);
			if(drivers[i] instanceof AmazonS3Driver && pair.getKey().equals("AMAZON-S3")){
				perDriverAclRequest(sequence, reg, protoOp, version, i, permission, pair.getValue());
			}else if(drivers[i] instanceof GoogleStorageDriver && pair.getKey().equals("GOOGLE-STORAGE")){
				perDriverAclRequest(sequence, reg, protoOp, version, i, permission, pair.getValue());
			}else if(drivers[i] instanceof WindowsAzureDriver && pair.getKey().equals("WINDOWS-AZURE")){
				perDriverAclRequest(sequence, reg, protoOp, version, i, permission, pair.getValue());
			}else if(drivers[i] instanceof RackSpaceDriver && pair.getKey().equals("RACKSPACE")){
				perDriverAclRequest(sequence, reg, protoOp, version, i, permission, pair.getValue());
			}
		}
	}

	/**
	 * NOT CURRENTLY USED
	 */
	private void perDriverAclRequest(int sequence, DepSkySDataUnit reg, int protoOp, long version, 
			int driverPosition, String permission, String canonicalId){

		CloudRequest r = new CloudRequest(DepSkySCloudManager.SET_ACL, sequence, 
				drivers[driverPosition].getSessionKey(), reg.getContainerName(), 
				reg.getGivenVersionValueDataFileName(version+""), reg, protoOp,
				permission, canonicalId);			
		manager.doRequest(drivers[driverPosition].getDriverId(), r);
	}


	/*
	 * Broadcast to write new data when using DepSky-A
	 */
	private void broadcastWriteValueRequests(int sequence,
			DepSkySDataUnit reg, byte[] value, String version, byte[] allDataHash) {
		for (int i = 0; i < drivers.length; i++) {
			CloudRequest r = new CloudRequest(DepSkySCloudManager.NEW_DATA, sequence,
					drivers[i].getSessionKey(), reg.getContainerId(drivers[i].getDriverId()),
					reg.getGivenVersionValueDataFileName(version), value, null,
					reg, DepSkySManager.WRITE_PROTO, false, version, null, allDataHash);
			manager.doRequest(drivers[i].getDriverId(), r);
		}
	}

	/*
	 * Broadcast to write new data when using DepSky-CA
	 */
	private void broadcastWriteValueErasureCodesAndSecretKeyShares(int sequence,
			DepSkySDataUnit reg, byte[] value, String version, byte[] allDataHash) throws Exception {


		SecretKey key = MyAESCipher.generateSecretKey();
		byte[] ciphValue = MyAESCipher.myEncrypt(key, value);

		Share[] keyshares = getKeyShares(reg, key.getEncoded());

		Map<String, byte[]> valueErasureCodes = encoder.encode(ciphValue);
		byte[] metabytes = valueErasureCodes.get("metadata");
		valueErasureCodes.remove("metadata");
		reg.setErCodesReedSolMeta(metabytes);


		//MERGE 1 ERASURE CODE AND 1 KEY SHARE AND SEND TO CLOUDS
		ByteArrayOutputStream data2write;
		ObjectOutputStream oos;
		Object[] ec_fnames = valueErasureCodes.keySet().toArray();
		for (int i = 0; i < drivers.length; i++) {
			data2write = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(data2write);
			String ec_filename = (String) ec_fnames[i];
			ECKSObject obj = new ECKSObject(keyshares[i],
					ec_filename, valueErasureCodes.get(ec_filename));
			oos.writeObject(obj);
			oos.close();
			//send request
			CloudRequest r = new CloudRequest(DepSkySCloudManager.NEW_DATA, sequence,
					drivers[i].getSessionKey(), reg.getContainerId(drivers[i].getDriverId()),
					reg.getGivenVersionValueDataFileName(version), data2write.toByteArray(), null,
					reg, DepSkySManager.WRITE_PROTO, false, version, null, allDataHash);
			manager.doRequest(drivers[i].getDriverId(), r);
		}

	}

	/*
	 * Broadcast to write new data when using only erasure codes (not use secret sharing)
	 */
	private void broadcastWriteValueErasureCodes(int sequence,
			DepSkySDataUnit reg, byte[] value, String version, byte[] allDataHash) throws Exception{

		Map<String, byte[]> valueErasureCodes = encoder.encode(value);
		byte[] metabytes = valueErasureCodes.get("metadata");
		valueErasureCodes.remove("metadata");
		//SET META OF ENCODE FILE
		reg.setErCodesReedSolMeta(metabytes);
		//SEND EACH ERASURE CODE TO EACH CLOUD
		ByteArrayOutputStream data2write;
		ObjectOutputStream oos;
		Object[] ec_fnames = valueErasureCodes.keySet().toArray();
		String aux = "";
		for (int i = 0; i < drivers.length; i++) {
			data2write = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(data2write);
			String ec_filename = (String) ec_fnames[i];
			ECKSObject obj = new ECKSObject(ec_filename, valueErasureCodes.get(ec_filename));
			oos.writeObject(obj);
			oos.close();
			//send request
			aux = reg.getContainerId(drivers[i].getDriverId());
			if(aux == null){
				for(int j = 0; j < drivers.length; j++){
					if(reg.getContainerId(drivers[j].getDriverId()) != null)
						aux = reg.getContainerId(drivers[j].getDriverId());
				}
			}
			CloudRequest r = new CloudRequest(DepSkySCloudManager.NEW_DATA, sequence,
					drivers[i].getSessionKey(), aux,
					reg.getGivenVersionValueDataFileName(version), data2write.toByteArray(), null,
					reg, DepSkySManager.WRITE_PROTO, false, version, null, allDataHash);
			manager.doRequest(drivers[i].getDriverId(), r);
		}
	}

	/*
	 * Broadcast to write new data when using only secret sharing (not use erasure codes)
	 */
	private void broadcastWriteValueSecretKeyShares(int sequence,
			DepSkySDataUnit reg, byte[] value, String version, byte[] allDataHash) throws Exception{

		SecretKey key = MyAESCipher.generateSecretKey();
		byte[] ciphValue = MyAESCipher.myEncrypt(key, value);

		Share[] keyshares = getKeyShares(reg, key.getEncoded());

		ByteArrayOutputStream data2write;
		ObjectOutputStream oos;

		for (int i = 0; i < drivers.length; i++) {
			data2write = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(data2write);
			ECKSObject obj = new ECKSObject(keyshares[i], ciphValue);
			oos.writeObject(obj);
			oos.close();
			//send request
			CloudRequest r = new CloudRequest(DepSkySCloudManager.NEW_DATA, sequence,
					drivers[i].getSessionKey(), reg.getContainerId(drivers[i].getDriverId()),
					reg.getGivenVersionValueDataFileName(version), data2write.toByteArray(), null,
					reg, DepSkySManager.WRITE_PROTO, false, version, null, allDataHash);
			manager.doRequest(drivers[i].getDriverId(), r);
		}


	}

	private Share[] getKeyShares(
			DepSkySDataUnit reg, byte[] secretkey) throws Exception {


		PVSSEngine engine = PVSSEngine.getInstance(N, T, NUM_BITS);
		PublicInfo info = engine.getPublicInfo();
		reg.setPVSSinfo(info);
		BigInteger[] secretKeys = engine.generateSecretKeys();
		BigInteger[] publicKeys = new BigInteger[N];
		for (int i = 0; i < N; i++) {
			publicKeys[i] = engine.generatePublicKey(secretKeys[i]);
		}
		PublishedShares publishedShares = engine.generalPublishShares(
				secretkey, publicKeys, 1);//generate shares
		Share[] shares = new Share[N];
		for (int i = 0; i < N; i++) {
			shares[i] = publishedShares.getShare(i, secretKeys[i], info, publicKeys);
		}


		return shares;
	}

	private byte[] generateSHA1Hash(byte[] data){
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			return sha1.digest(data);

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	private byte[] recombineSecretKeyShares(DepSkySDataUnit reg, Share[] shares)
			throws IOException, ClassNotFoundException, InvalidVSSScheme {
		PVSSEngine engine = PVSSEngine.getInstance(reg.info);
		Share[] orderedShares = new Share[N];
		//share ordering for recombination to process or else it fails
		for (int i = 0; i < shares.length; i++) {
			Share s = shares[i];
			if (s == null) {
				continue;
			}
			orderedShares[s.getIndex()] = s;
		}

		return engine.generalCombineShares(orderedShares);
	}

	private byte[] concatAll(byte[][] decode, int originalSize){

		//put all blocks together after decode
		byte[] result = new byte[originalSize];
		int offset = 0;
		for(int i = 0; i < decode.length; i++){
			if(offset + decode[i].length < originalSize){
				//copy all
				System.arraycopy(decode[i], 0, result, offset, decode[i].length);
				offset+=decode[i].length;
			}else{
				//copy originalSize-offset
				System.arraycopy(decode[i], 0, result, offset, originalSize - offset);
				break;
			}
		}
		return result;
	}

	/**
	 * Read the credentials of drivers accounts
	 */
	private List<String[][]> readCredentials() throws FileNotFoundException, ParseException{
		Scanner sc=new Scanner(new File("config"+File.separator+"accounts.properties"));
		//Scanner sc=new Scanner(new File("accounts.properties"));
		String line;
		String [] splitLine;
		LinkedList<String[][]> list = new LinkedList<String[][]>();
		int lineNum =-1;
		LinkedList<String[]> l2 = new LinkedList<String[]>();
		boolean firstTime = true;
		while(sc.hasNext()){
			lineNum++;
			line = sc.nextLine();
			if(line.startsWith("#") || line.equals(""))
				continue;
			else{
				splitLine = line.split("=", 2);
				if(splitLine.length!=2){
					sc.close();
					throw new ParseException("Bad formated accounts.properties file.", lineNum);
				}else{
					if(splitLine[0].equals("driver.type")){
						if(!firstTime){
							String[][] array= new String[l2.size()][2];
							for(int i = 0;i<array.length;i++)
								array[i] = l2.get(i);
							list.add(array);
							l2 = new LinkedList<String[]>();
						}else
							firstTime = false;
					}
					l2.add(splitLine);
				}
			}
		}
		String[][] array= new String[l2.size()][2];
		for(int i = 0;i<array.length;i++)
			array[i] = l2.get(i);
		list.add(array);
		sc.close();
		return list;
	}

	public static void main(String[] args) {
		System.out.println("USAGE:  commands             funtion");
		System.out.println("       pick_du 'name' - change the container");
		System.out.println("       write 'data'   - write a new version in the selected container");
		System.out.println("       read           - read the last version of the selected container");
		System.out.println("       delete         - delete all the files in the selected container");
		System.out.println("       read_m 'hash'  - read the version that match with 'hash' in the selected container");
		System.out.println();
		boolean useClouds = true;
		if(new Integer(args[2]) == 1){
			useClouds = false;
		}			
		LocalDepSkySClient localDS = new LocalDepSkySClient(new Integer(args[0]), useClouds);
		DepSkySDataUnit dataU = null;
		int protocol_mode = 0;
		if(args[1].equals("1")){
			protocol_mode = 1;
		}else if(args[1].equals("2")){
			protocol_mode = 2;
		}else if(args[1].equals("3")){
			protocol_mode = 3;
		}

		boolean terminate = false;
		Scanner in = new Scanner(System.in);
		String input;
		byte[] rdata;
		HashMap<String, LinkedList<byte[]>> map = new HashMap<String, LinkedList<byte[]>>();
		while(!terminate){
			input = in.nextLine();

			if(input.length() > 7 && input.substring(0, 7).equals("pick_du") && input.split(" ").length > 0){
				StringBuilder sb = new StringBuilder(input.substring(8));
				dataU = new DepSkySDataUnit(sb.toString());
				if(protocol_mode == 1){
					dataU.setUsingPVSS(true);
				}else if(protocol_mode == 2){
					dataU.setUsingErsCodes(true);
				}else if(protocol_mode == 3){
					dataU.setUsingSecSharing(true);
				}
				if(!map.containsKey(sb.toString())){
					LinkedList<byte[]> hashs = new LinkedList<byte[]>();
					map.put(sb.toString(), hashs);
				}
				System.out.println("DataUnit '" + sb.toString() + "' selected!");
			}else{

				if(dataU != null){
					if(input.equals("delete")){

						localDS.deleteContainer(dataU);
						System.out.println("I'm finished delete");

					}else if(input.equals("read")){
						System.out.println("I´m reading");
						try{
							dataU.clearAllCaches();
							long acMil = System.currentTimeMillis();
							rdata = localDS.read(dataU);
							long tempo = System.currentTimeMillis() - acMil;
							System.out.println("I'm finished read -> " + Long.toString(tempo) + " milis");
							if (rdata != null) {
								System.out.println("READ RESULT = " + new String(rdata));
							}
							System.out.println("finish READ");
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						System.out.println("I'm finished read");

					}else if(input.substring(0, 5).equals("write") && input.split(" ").length > 0){
						StringBuilder sb = new StringBuilder(input.substring(6));
						System.out.println("WRITING: " + sb);
						File file = new File (sb.toString());
						byte[] value = null;
						if(file.exists()){
							FileInputStream reader;
							try {
								reader = new FileInputStream(file);
								value = IOUtils.toByteArray(reader);
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}else{
							value = sb.toString().getBytes();
						}
						try {
							long acMil = System.currentTimeMillis();

							byte[] hash = localDS.write(dataU, value);
							LinkedList<byte[]> current = map.get(dataU.getRegId());
							current.addFirst(hash);
							map.put(dataU.getRegId(), current);
							//hashs.addFirst(hash);
							long tempo = System.currentTimeMillis() - acMil;
							System.out.println("I'm finished write -> " + Long.toString(tempo) + " milis");
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}else if(input.substring(0, 6).equals("read_m") && input.split(" ").length > 0){
						StringBuilder sb = new StringBuilder(input.substring(7));
						int ver = new Integer(sb.toString());
						System.out.println("readm: " + ver);
						System.out.println("I'm reading");
						try{
							dataU.clearAllCaches();
							long acMil = System.currentTimeMillis();
							LinkedList<byte[]> current = map.get(dataU.getRegId());
							rdata = localDS.readMatching(dataU, current.get(ver));
							long tempo = System.currentTimeMillis() - acMil;
							System.out.println("I'm finished read -> " + Long.toString(tempo) + " milis");
							if (rdata != null) {
								System.out.println("READ RESULT = " + new String(rdata));
							}
							System.out.println("finish READ");
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						System.out.println("I'm finished read");

					}else{
						System.out.println("Comando inválido!");
					}
				}else{
					System.out.println("You need do pick a container to use. use the command pick_du.");
				}
			}
		}
		in.close();
	}
}

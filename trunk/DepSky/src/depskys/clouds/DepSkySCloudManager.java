package depskys.clouds;

import java.util.ArrayList;
import java.util.LinkedList;

import depskyDep.IDepSkySDriver;
import depskys.core.IDepSkySProtocol;

/**
 * Class that represents all the request and replies for each cloud
 * Note there is N object of this class (each object e connected with each cloud)
 * 
 * @author tiago oliveira
 * @author bruno quaresma
 * 
 */
public class DepSkySCloudManager extends Thread {

	public static final int INIT_SESS = 0;
	public static final int NEW_CONT = 1;
	public static final int DEL_CONT = 2;
	public static final int NEW_DATA = 3;
	public static final int GET_DATA = 4;
	public static final int DEL_DATA = 5;
	public static final int GET_DATA_ID = 6;
	public static final int GET_CONT_AND_DATA_ID = 7;
	public static final int SET_ACL = 8;
	public static final int LIST = 9;
	//***//
	private static final int MAX_RETRIES = 3;
	public IDepSkySDriver driver;
	public ArrayList<CloudRequest> requests;
	public ArrayList<CloudReply> replies;
	public ICloudDataManager cloudDataManager;
	public IDepSkySProtocol depskys;
	private boolean terminate = false;

	public DepSkySCloudManager(IDepSkySDriver driver,
			ICloudDataManager cloudDataManager, IDepSkySProtocol depskys) {
		this.driver = driver;
		this.requests = new ArrayList<CloudRequest>();
		this.replies = new ArrayList<CloudReply>();
		this.cloudDataManager = cloudDataManager;
		this.depskys = depskys;
		this.start();
	}

	//Thread code
	public void run() {
		while (true) {
			try {
				if (terminate && replies.isEmpty() && requests.isEmpty()) {
					break;//exit infinite loop
				} else if (!replies.isEmpty()) {

					processReply(); // Process next reply in queue

				} else if (!requests.isEmpty()) {

					processRequest(); // Process next request in queue

				} else {
					sleepAnInstant();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	//Do a request to this manager cloud driver
	public void doRequest(CloudRequest request) {
		if (!terminate) {
			requests.add(request);
			//            System.out.println("Added request to list. result=" + res);
		}

	}

	public void addReply(CloudReply reply) {
		if (!terminate) {
			replies.add(reply);
			//            System.out.println("Added reply to list. result=" + res);
		}
	}

	//Process received requests
	private void processRequest() {
		CloudRequest request = null;
		CloudReply r = null;
		long init = 0;
		try {
			request = requests.remove(0);
			switch (request.op) {
			case INIT_SESS:
				terminate = false;

				//Start the connection with the cloud associated to this driver
				String sessid = driver.initSession();

				//add the reply for this operation
				addReply(
						new CloudReply(request.op, request.seqNumber,
								driver.getDriverId(), sessid, request.cid,
								request.reg, request.protoOp, request.isMetadataFile, request.hashMatching));
				break;
			case DEL_CONT:

				//Delete all the files in this container (metadata and data files)
				boolean delContRes = driver.deleteContainer(request.sid, request.namesToDelete);

				addReply(
						new CloudReply(request.op, request.seqNumber,
								driver.getDriverId(), delContRes, request.cid,
								request.reg, request.protoOp, request.isMetadataFile, request.hashMatching));
				break;
			case NEW_DATA:
				init = System.currentTimeMillis();
				//Writes new data in the cloud
//				if(driver.getDriverId().equals("cloud1"))
//					System.out.println("write -> " + new String(request.w_data));
				String ssid = driver.uploadData(request.reg.getBucketName(),
						request.cid, request.w_data, request.did);
				r = new CloudReply(request.op, request.seqNumber,
						driver.getDriverId(), ssid, request.cid,
						request.reg, request.protoOp, request.isMetadataFile,
						request.w_data, request.vNumber, request.allDataHash, null, null);

				r.setStartTime(request.startTime);//added
				r.setInitReceiveTime(init);
				if (request.did.contains("metadata") && request.isMetadataFile) {
					r.setReceiveTime(System.currentTimeMillis());
					r.setStartTime(request.startTime);
				}

				addReply(r);
				break;
			case GET_DATA:
				init = System.currentTimeMillis();
				//download a file from the cloud
				byte[] data = driver.downloadData(
						request.reg.getBucketName(), request.cid, request.did);
//				if(driver.getDriverId().equals("cloud1"))
//					System.out.println("read -> " + new String(data));
				r = new CloudReply(request.op, request.seqNumber,
						driver.getDriverId(), data, request.cid, request.reg,
						request.protoOp, request.isMetadataFile,
						request.vNumber, request.vHash, request.allDataHash, request.hashMatching);
				r.setInitReceiveTime(init);
				r.setStartTime(request.startTime);
				if (request.isMetadataFile) {
					r.setMetadataReceiveTime(System.currentTimeMillis());
				} else {
					r.setMetadataReceiveTime(request.metadataReceiveTime);
				}

				addReply(r);
				break;
			case DEL_DATA:

				//delete a file from the cloud
				boolean delRes = driver.deleteData(request.sid, request.cid, request.did);

				addReply(
						new CloudReply(request.op, request.seqNumber,
								driver.getDriverId(), delRes, request.cid,
								request.reg, request.protoOp, request.isMetadataFile, request.hashMatching));
				break;
			case LIST:

				//list all the files in the cloud that contains the string 'request.did'
				LinkedList<String> name = driver.listNames(request.did);

				r = new CloudReply(request.op, request.seqNumber,
						driver.getDriverId(), request.sid, request.cid,
						request.reg, request.protoOp, request.isMetadataFile,
						request.w_data, request.vNumber, request.allDataHash, name, null);


				addReply(r);
				break;
			case SET_ACL:
				boolean bool = driver.setAcl(request.reg.getBucketName(), request.did, request.canonicalId, request.permission);

				r = new CloudReply(request.op, request.seqNumber,
						driver.getDriverId(), bool, request.cid,
						request.reg, request.protoOp, request.isMetadataFile, request.hashMatching);
				addReply(r);
			default:
				//                    System.out.println("Operation does not exist");
				addReply(new CloudReply(request.op, request.seqNumber,
						driver.getDriverId(), null, request.cid,
						request.reg, request.protoOp, request.isMetadataFile, null));
				break;
			}
			// System.out.println("Request Processed: " + request);
		} catch (Exception ex) {
			//ex.printStackTrace();//testing purposes
			if (request.retries < MAX_RETRIES) {
				//retry (issue request to cloud again)
				request.incrementRetries();
				doRequest(request);
				//System.out.println("Retrying(#" + request.retries + ") request: " + request + " " + driver.getDriverId());
				return;
			}
			//after MAX_REPLIES return null response
			r = new CloudReply(request.op, request.seqNumber,
					driver.getDriverId(), null, request.cid, request.reg,
					request.protoOp, request.isMetadataFile, request.vNumber, request.vHash, null, null);
			r.setReceiveTime(System.currentTimeMillis());
			r.setInitReceiveTime(init);
			r.setExceptionMessage(ex.getMessage());
			r.setStartTime(request.startTime);
			r.invalidateResponse();
			addReply(r);
		}
	}

	//process received replies
	private void processReply() {
		try {
			CloudReply reply = replies.remove(0);//processing removed reply next
			if (reply == null) {
				//                System.out.println("REPLY IS NULL!!");
				return;
			}
			//if error
			if (reply.response == null) {
				depskys.dataReceived(reply);
				return;
			}
			//response may be processed
			if (reply.response != null) {
				//process not null response
				if(reply.type == SET_ACL){
					depskys.dataReceived(reply);
				}else if (reply.type == GET_DATA && reply.isMetadataFile) {
					/*metadata file*/
					cloudDataManager.processMetadata(reply);
				} else if (reply.type == GET_DATA) {

					if(reply.vHash == null)
						depskys.dataReceived(reply); /*to read quorum operation (out of the protocols)*/
					else
						cloudDataManager.checkDataIntegrity(reply); /*valuedata file*/
				} else if (reply.type == GET_CONT_AND_DATA_ID) {
					//send file request for metadata file ids received
					String[] ids = (String[]) reply.response;
					//update container id in local register (cid is a constant value)
					if (reply.reg.getContainerId(reply.cloudId) == null) {
						reply.reg.setContainerId(reply.cloudId, ((String[]) reply.response)[0]);
					}
					CloudRequest r = new CloudRequest(GET_DATA, reply.sequence,
							driver.getSessionKey(), ids[0], ids[1], null, null,
							reply.reg, reply.protoOp, true, reply.hashMatching);
					r.setStartTime(reply.startTime);
					doRequest(r);
				} else if (reply.type == NEW_DATA && !reply.isMetadataFile
						&& reply.value != null) {
					//System.out.println("WRITING METADATA for this reply" + reply);
					cloudDataManager.writeNewMetadata(reply);
				} else if (reply.type == NEW_DATA && reply.isMetadataFile
						&& reply.value != null) {
					depskys.dataReceived(reply);
					return;
				} else {
					depskys.dataReceived(reply);
					return;
				}
			}
			//if after processing response was invalidated
			if (reply.response == null) {
				//deliver reply if response was null
				depskys.dataReceived(reply);
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void terminate() {
		terminate = true;
	}

	public void resetRequests() {
		terminate = false;
		replies.clear();
		requests.clear();
	}
	private int counter = 0;

	private void sleepAnInstant() {
		try {
			counter++;
			if (counter % 50 == 0) {
				counter = 0;
			}
			Thread.sleep(50);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

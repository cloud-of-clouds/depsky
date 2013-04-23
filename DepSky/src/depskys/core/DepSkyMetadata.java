package depskys.core;

import java.io.Serializable;

/**
 * Class that stores the metadata information for each version
 * 
 * @author tiago oliveira
 *
 */
public class DepSkyMetadata implements Serializable{

	private static final long serialVersionUID = 1L;
	private int versionNumber;
	private byte[] versionHash;
	private byte[] allDataHash;
	private String versionFileId;
	private String versionPVSSinfo;
	private String versionECinfo;
	private byte[] signature;
	
	private String metadata;


	public DepSkyMetadata(int versionNumber, byte[] versionHash, String versionFileId,
			String versionPVSSinfo, String versionECinfo, byte[] allDataHash, byte[] signature){

		this.versionNumber = versionNumber;
		this.versionHash = versionHash;
		this.versionFileId = versionFileId;
		this.versionPVSSinfo = versionPVSSinfo;
		this.versionECinfo = versionECinfo;
		this.allDataHash = allDataHash;
		this.signature = signature;

	}
	
	public DepSkyMetadata(String medatada, byte[] signature, byte[] allDataHash, String versionFileId){
		
		this.metadata = medatada;
		this.signature = signature;
		this.allDataHash = allDataHash;
		this.versionFileId = versionFileId;
	}

	public int getVersionNumber(){
		return versionNumber;
	}

	public byte[] getVersionHash(){
		return versionHash;
	}

	public String getVersionFileId(){
		return versionFileId;
	}

	public String getVersionPVSSinfo(){
		return versionPVSSinfo;
	}

	public String getVersionECinfo(){
		return versionECinfo;
	}

	public byte[] getAllDataHash(){
		return allDataHash;
	}

	public byte[] getsignature(){
		return signature;
	}
	
	public String getMetadata(){
		return metadata;
	}
}

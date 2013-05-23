package depskys.core;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Class that stores the metadata information for each version
 * 
 * @author tiago oliveira
 *
 */
public class DepSkyMetadata {

	private byte[] allDataHash;
	private String versionFileId;
	private byte[] signature;	
	private String metadata;

	public DepSkyMetadata() {
	}

	public DepSkyMetadata(String medatada, byte[] signature, byte[] allDataHash, String versionFileId){
		
		this.metadata = medatada;
		this.signature = signature;
		this.allDataHash = allDataHash;
		this.versionFileId = versionFileId;
	}

	public String getVersionFileId(){
		return versionFileId;
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

	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		allDataHash = new byte[in.readInt()];
		in.read(allDataHash);
		versionFileId = in.readUTF();
		signature = new byte[in.readInt()];
		in.read(signature);
		metadata = in.readUTF();
	}

	public void writeExternal(ObjectOutput out) throws IOException {		
		out.writeInt(allDataHash.length);
		out.write(allDataHash);
		out.writeUTF(versionFileId);
		out.writeInt(signature.length);
		out.write(signature);
		out.writeUTF(metadata);	
	}
}

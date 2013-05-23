package depskys.core;

import java.io.Serializable;

import pvss.Share;

/**
 * Represents the object that is written in each cloud for each write (if not DepSky-A is used)
 * 
 * @author tiago oliveira
 * @author koras
 */
public class ECKSObject implements Serializable {

	private static final long serialVersionUID = 1L;
	private Share sk_share;
    private byte[] ec_bytes;
    private String ec_filename;

    //is PVSS
    public ECKSObject(Share sk_share,
            String ec_filename, byte[] ec_bytes) {
        this.sk_share = sk_share;
        this.ec_bytes = ec_bytes;
        this.ec_filename = ec_filename;
    }
    
    //is Erasure Codes
    public ECKSObject(String ec_filename, byte[] ec_bytes){
        this.sk_share = null;
        this.ec_bytes = ec_bytes;
        this.ec_filename = ec_filename;
    }
    
    //is Secret Sharing
    public ECKSObject(Share sk_share, byte[] ec_bytes) {
        this.sk_share = sk_share;
        this.ec_bytes = ec_bytes;
        this.ec_filename = null;
    }

    /**
     * @return the sk_share
     */
    public Share getSKshare() {
        return sk_share;
    }

    /**
     * @return the ec_bytes
     */
    public byte[] getECbytes() {
        return ec_bytes;
    }

    /**
     * @return the ec_filename
     */
    public String getECfilename() {
        return ec_filename;
    }
}

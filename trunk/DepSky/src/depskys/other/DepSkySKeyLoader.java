package depskys.other;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Used to load RSA public and private keys from AQSconfig/keys/publickey<id> and
 * AQSconfig/keys/privatekey<id>
 *
 * @author bruno
 */
public class DepSkySKeyLoader {

    private String path;
    private PublicKey[] pubKeys;
    private PrivateKey prk;

    /** Creates a new instance of AQSKeyLoader */
    public DepSkySKeyLoader(String configHome) {
        if (configHome == null || configHome.equals("")) {
            path = "config" + System.getProperty("file.separator") + "keysDepSky"
                    + System.getProperty("file.separator");
        } else {
            path = configHome + System.getProperty("file.separator") + "keysDepSky"
                    + System.getProperty("file.separator");
        }
    }

    /**
     * Load the public keys from processes 0..conf.getN()-1 (all servers).
     *
     * @return the array of public keys loaded
     * @throws Exception problems reading or parsing the keys
     */
    /**
     * Load the public keys from the writer 0 to until
     * @param until
     * @return an array of public keys loaded
     * @throws Exception problems reading or parsing the keys
     */
    public PublicKey[] loadKnownWritersPublicKeys(int until) throws Exception {
        if (pubKeys == null) {
            pubKeys = new PublicKey[until];
            for (int i = 0; i < pubKeys.length; i++) {
                pubKeys[i] = loadPublicKey(i);
            }
        }
        return pubKeys;
    }

    /**     
     * Loads the public key of some processes from configuration files
     *
     * @param id the id of the process that we want to load the public key
     * @return the PublicKey loaded from AQSconfig/keys/publickey<id>
     * @throws Exception problems reading or parsing the key
     */
    public PublicKey loadPublicKey(int id) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(path + "publickey" + id));
        PublicKey puk = (PublicKey) ois.readObject();
        return puk;
    }

    /**
     * Loads a private key
     *
     * @return the PrivateKey loaded from config/keys/publickey<id>
     * @throws Exception problems reading or parsing the key
     */
    public PrivateKey loadPrivateKey(int id) throws Exception {
        if (prk == null) {
            ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(path + "privatekey" + id));
            prk = (PrivateKey) ois.readObject();
        }
        return prk;
    }
}

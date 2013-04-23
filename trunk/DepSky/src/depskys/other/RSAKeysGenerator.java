package depskys.other;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Utility class used to generate a key pair for some process id on
 * AQSconfig/keys/publickey<id> and AQSconfig/keys/privatekey<id>
 *
 * @author bruno
 */
public class RSAKeysGenerator {

    public void generateRSAKeyPair(int id) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair kp = keyGen.generateKeyPair();
        PublicKey puk = kp.getPublic();
        PrivateKey prk = kp.getPrivate();
        saveToFiles(id, puk, prk);
        System.out.println("KeyPair number " + id + " created in config/keys directory.");
    }

    private void saveToFiles(int id, PublicKey puk, PrivateKey prk) throws Exception {
        String path = "config" + System.getProperty("file.separator") +
                "keys" + System.getProperty("file.separator");
        File filepath = new File(path);
        if (!filepath.exists()) {
            if (!filepath.mkdirs()) {
                throw new RuntimeException("Cannot create config dirs!");
            }
        }
        File pukfile = new File(path + "publickey" + id);
        if (pukfile.exists()) {
            if (!pukfile.canWrite()) {
                throw new RuntimeException("File cannot be written!");
            }
        } else {
            pukfile.createNewFile();
        }
        ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(pukfile));
        oos.writeObject(puk);
        oos.flush();
        oos.close();
        File prkfile = new File(path + "privatekey" + id);
        if (prkfile.exists()) {
            if (!prkfile.canWrite()) {
                throw new RuntimeException("File cannot be written!");
            }
        } else {
            prkfile.createNewFile();
        }
        oos = new ObjectOutputStream(
                new FileOutputStream(prkfile));
        oos.writeObject(prk);
        oos.flush();
        oos.close();
    }

    // Generate public/private key pairs
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("USAGE: RSAKeysGenerator <id>\n" +
                    "- parameter <id> is the key pair identifier");
        } else {
            try {
                new RSAKeysGenerator().generateRSAKeyPair(Integer.parseInt(args[0]));
            } catch (Exception ex) {
                System.out.println("EXCEPTION: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}

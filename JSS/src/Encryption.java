import java.math.BigInteger;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/*
 * Encryption.java
 *
 * Created on 15 de Abril de 2006, 16:23
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author alysson
 */
public class Encryption {
    
    /** Creates a new instance of Encryption */
    public Encryption() {
    }
    
    public static void main(String[] args) throws Exception {
        //System.out.println(Security.getAlgorithms("KeyGenerator"));
        //System.out.println(Security.getAlgorithms("SecretKeyFactory"));
        //System.out.println(Security.getAlgorithms("Cipher"));
        
        SecretKey key = SecretKeyFactory.getInstance("DESEDE").generateSecret(
                new DESedeKeySpec(new BigInteger(192,new Random()).
                toByteArray()));

        ///////////////////////////////////////////////////////////////////
        
        Cipher c1 = Cipher.getInstance("DESEDE");
        c1.init(Cipher.ENCRYPT_MODE,key);
        
        long t0 = System.nanoTime();
        byte[] encripted = c1.doFinal("abc".getBytes());
        long t1 = System.nanoTime();

        System.out.println("encrypt: "+(t1-t0));
        
        ///////////////////////////////////////////////////////////////////
        
        Cipher c2 = Cipher.getInstance("DESEDE");
        c2.init(Cipher.DECRYPT_MODE,key);
        
        t0 = System.nanoTime();
        byte[] decripted = c2.doFinal(encripted);
        t1 = System.nanoTime();
        
        System.out.println("decrypt: "+(t1-t0));        
        System.out.println(new String(decripted));
    }
    
}

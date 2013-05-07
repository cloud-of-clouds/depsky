import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import pvss.PVSSEngine;
import pvss.PublicInfo;
import pvss.PublishedShares;
import pvss.Share;
/*
 * BasicVSS.java
 *
 * Created on 2 de Setembro de 2006, 16:55
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author alysson
 */
public class BasicPVSS {
    
    static final int N = 7;
    static final int T = 3;
    static final int NUM_BITS = 192;
    
    static final SecureRandom random = new SecureRandom();
    
    public static void main(String[] args) throws Exception {
        //inicializacao
        PVSSEngine engine = PVSSEngine.getInstance(N,T,NUM_BITS);
        
        PublicInfo info = engine.getPublicInfo();
        
        BigInteger[] secretKeys = engine.generateSecretKeys();
        
        BigInteger[] publicKeys = new BigInteger[N];
        
        for(int i=0; i<N; i++){
            publicKeys[i] = engine.generatePublicKey(secretKeys[i]);
        }
        
        System.out.println("secret keys: "+Arrays.toString(secretKeys));
        System.out.println("public keys: "+Arrays.toString(publicKeys));
        
        //distribuicao
        BigInteger secret = 
            //BigInteger.valueOf(7);
            engine.generateSecret();
        
        System.out.println("secret: "+secret);
        
        BigInteger expectedResult = engine.getPublicInfo().getGeneratorG().
          modPow(secret,engine.getPublicInfo().getGroupPrimeOrder());
        
        System.out.println("encriptedSecret: "+expectedResult);
        
        long t0 = System.currentTimeMillis();
        PublishedShares publishedShares = engine.publishShares(secret,null,publicKeys);
        long t1 = System.currentTimeMillis();
        
        System.out.println("passou: "+(t1-t0)+"ms");
        
        System.out.println("published shares: "+publishedShares);
        
        t0 = System.currentTimeMillis();
        System.out.println(publishedShares.verify(info,publicKeys)?
          "valid shares":"invalid shares");
        t1 = System.currentTimeMillis();
        
        System.out.println("passou: "+(t1-t0)+"ms");
        
        Share[] shares = new Share[N];
        
        for(int i=0; i<N; i++) {
            shares[i] = publishedShares.getShare(i,secretKeys[i],info,publicKeys);
            System.out.println(shares[i]+(shares[i].verify(info,publicKeys[i])?" valid":"invalid"));
        }

        int[] x = new int[T];
        
        for(int i=0; i<T; i++) {
            x[i] = i;
        }
        
        t0 = System.currentTimeMillis();
        BigInteger result = engine.combineShares(x,shares);
        t1 = System.currentTimeMillis();
        System.out.println("passou: "+(t1-t0)+"ms");
        
        System.out.println("result encripted secret: "+result);
    }
}

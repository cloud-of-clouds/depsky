/*
 * VSSEngine.java
 *
 * Created on 28 de Junho de 2005, 13:54
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package pvss;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

/**
 *
 * @author neves
 */
public class PVSSEngine {
    
    public static SecureRandom random = new SecureRandom();
    private PublicInfo publicInfo;
    
    private PVSSEngine(PublicInfo publicInfo) {
        this.publicInfo = publicInfo;
    }
    
    public static PVSSEngine getInstance(int n, int t, int numBits) throws InvalidVSSScheme {
    
        if(t > n) {
            throw new InvalidVSSScheme("(t,n)=("+t+","+n+")");
        }
       
        //generate a prime number with the specified number of bits
        BigInteger groupPrimeOrder = 
          BigInteger.probablePrime(numBits, random);
        
        //generates two distinct generators for Z_q
        BigInteger generatorg = 
          BigInteger.probablePrime(numBits-1, random);
        BigInteger generatorG = 
          BigInteger.probablePrime(numBits-1, random);
        
        PublicInfo publicInfo = new PublicInfo(n,t,groupPrimeOrder,generatorg,generatorG);
        
        return new PVSSEngine(publicInfo);
    }
    
    public static PVSSEngine getInstance(PublicInfo publicInfo) throws InvalidVSSScheme {
        return new PVSSEngine(publicInfo);
    }
    
    public PublicInfo getPublicInfo() {
        return publicInfo;
    }
    
    public BigInteger generateSecret() {
        return new BigInteger(getPublicInfo().getNumBits()-1,random);
    }

    public BigInteger[] generateSecretKeys() {
        BigInteger[] secretKeys = new BigInteger[getPublicInfo().getN()];
        
        boolean passed = false;
        
        for(int i=0; i<secretKeys.length; i++) {
            secretKeys[i] = generateRandomNumber();
            //verifica se a chave i e igual a alguma das chaves geradas 
            //anteriormente
            for(int j=0; j<i; j++) {
                if(secretKeys[i].equals(secretKeys[j])) {
                    //e igual! i deve ser regerado!!!
                    i--;
                }
            }
        }
/*            
        secretKeys[0] = BigInteger.valueOf(3);
        secretKeys[1] = BigInteger.valueOf(2);
        secretKeys[2] = BigInteger.valueOf(5);
        secretKeys[3] = BigInteger.valueOf(7);
*/      
        
        return secretKeys;
    }

    public BigInteger generatePublicKey(BigInteger secretKey) {
        return getPublicInfo().getGeneratorG().modPow(secretKey, 
                getPublicInfo().getGroupPrimeOrder());
    }

    public PublishedShares generalPublishShares(byte[] data, BigInteger[] publicKeys) throws InvalidVSSScheme{
        BigInteger secret = null;
        byte[] ensecret = null;
        do{
            //long l = System.currentTimeMillis();
            secret = generateSecret();
            //System.out.println("Time = "+(System.currentTimeMillis()-l));
            BigInteger encryptedSecret = getPublicInfo().getGeneratorG().modPow(
                    secret, getPublicInfo().getGroupPrimeOrder());
            ensecret = encryptedSecret.toByteArray();
        }while(ensecret.length < 24);
        byte[] U = encrypt(getPublicInfo(),ensecret,data);
        return publishShares(secret,U,publicKeys);
    }
    
    public PublishedShares publishShares(BigInteger secret, byte[] U, BigInteger[] publicKeys) throws InvalidVSSScheme{
    
        int t = getPublicInfo().getT();
        int n = getPublicInfo().getN();
        BigInteger g = getPublicInfo().getGeneratorg();
        
        BigInteger q = getPublicInfo().getGroupPrimeOrder();
        BigInteger qm1 = q.subtract(BigInteger.ONE);
        
        BigInteger[] coefs = new BigInteger[t];
        //coefs[0] = BigInteger.valueOf(7);
        //coefs[1] = BigInteger.valueOf(10);
        
        BigInteger[] commitments = new BigInteger[t];
        
        coefs[0] = secret;
        coefs[1] = BigInteger.valueOf(3);
        //coefs[2] = BigInteger.valueOf(10);
        //commitments[0] = g.modPow(secret,q);
        
        for(int j=0; j<t; j++) {
            if(j!=0){ 
                coefs[j] = new BigInteger(getPublicInfo().getNumBits()-1,random);
            }
            //coefs[j] = generateRandomNumber();
            commitments[j] = g.modPow(coefs[j],q);
        }
        
        BigInteger[] shares = new BigInteger[n];
        BigInteger[] encriptedShares = new BigInteger[n];
        
        BigInteger[] X = new BigInteger[n];
        BigInteger[] a1 = new BigInteger[n];
        BigInteger[] a2 = new BigInteger[n];
        BigInteger[] proofsr = new BigInteger[n];
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        
        BigInteger w = BigInteger.valueOf(11);
    
        for(int i=0; i<n; i++) {
            shares[i] = poly(coefs,i+1);
            encriptedShares[i] = publicKeys[i].modPow(shares[i],q);
            
            //calcs Xi
            BigInteger exp = BigInteger.ONE;
            BigInteger mult = commitments[0];
            for(int j=1; j<t; j++) {
                exp = exp.multiply(BigInteger.valueOf(i+1)).mod(qm1);
                mult = mult.multiply(commitments[j].modPow(exp,q)).mod(q);
            }
            X[i] = mult;
            
            a1[i] = g.modPow(w,q);
            a2[i] = publicKeys[i].modPow(w,q);
            
            try{
                baos.write(X[i].toByteArray());
                baos.write(encriptedShares[i].toByteArray());
                baos.write(a1[i].toByteArray());
                baos.write(a2[i].toByteArray());
            }catch(IOException ioe){
                throw new InvalidVSSScheme("Problems crating hash for proof");
            }
        }
        
        
        //System.out.println("Published data:");
        //System.out.println(" X="+Arrays.toString(X));
        //System.out.println(" a1="+Arrays.toString(a1));
        //System.out.println(" a2="+Arrays.toString(a2));
        //System.out.println(" shares="+Arrays.toString(shares));
        //System.out.println(" encryptedshares="+Arrays.toString(encriptedShares));
                
        BigInteger proofc = PVSSEngine.hash(getPublicInfo(),baos.toByteArray()).mod(qm1);
        
        for(int i=0; i<n; i++) {
            proofsr[i] = w.subtract(shares[i].multiply(proofc)).mod(qm1);
        }
        
        return new PublishedShares(commitments,encriptedShares,proofsr,proofc,U);
    }
   
    
    public byte[] generalCombineShares(Share[] shares) throws InvalidVSSScheme {
        int[] x = new int[getPublicInfo().getT()];
        int j = 0;
        format(shares);
        for(int i=0; i < shares.length; i++) {
            if(shares[i] != null){
                x[j++] = i;
                
                if(j == x.length) {
                    break;
                }
            }
        }
        BigInteger encryptedSecret = combineShares(x,shares);
        return decrypt(getPublicInfo(),encryptedSecret.toByteArray(),shares[x[0]].getU());
    }
    
    public byte[] generalCombineShares(int[] x, Share[] shares) throws InvalidVSSScheme {
        BigInteger encryptedSecret = combineShares(x,shares);
        
        return decrypt(getPublicInfo(),encryptedSecret.toByteArray(),shares[0].getU());
    }
    
    
    public BigInteger combineShares(int x[], Share[] shares) {
        int t = publicInfo.getT();
        int n = publicInfo.getN();
        
        if(x.length != t) {
            throw new RuntimeException("There must be "+t+
                    " diferent and valid shares");
        }
        
        for(int i:x) {
            if(shares[i] == null) {
                throw new RuntimeException("There must be "+t+
                        " diferent and valid shares");
            }
        }

        BigInteger q = publicInfo.getGroupPrimeOrder();
        BigInteger qm1 = q.subtract(BigInteger.ONE);
        
        BigInteger secret = BigInteger.ONE;
        
        //System.out.print("PVSSEngine.combineShares: Using shares");
        for(int i=0; i<t; i++) { //iterates over x[i]
            //System.out.print(" "+x[i]);
            
            float lambda = 1;
            for(int j=0; j<t; j++) { //iterates over x[j]
                if(j != i){
                    lambda = lambda*((float)(x[j]+1)/(float)(x[j]-x[i]));
                }
            }
            
            secret = secret.multiply(shares[x[i]].getShare().modPow(BigInteger.valueOf((long)lambda),q)).mod(q);
        }
        
               
       // System.out.println();
        
        return secret;
    }
    
    //some private methods
    
    private BigInteger generateRandomNumber() {
        return BigInteger.probablePrime(getPublicInfo().getNumBits()-1,random);
        //return new BigInteger(getPublicInfo().getNumBits()-1,random);
    }

    private BigInteger poly(BigInteger[] coefs, int val) {
        BigInteger y = coefs[0];
        
        for(int j=1; j<coefs.length; j++){
            BigInteger term = coefs[j].multiply(BigInteger.valueOf((long)Math.pow(val,j)));
            y = y.add(term);
        }
        
        //poly mod (q-1)
        return y.mod(getPublicInfo().getGroupPrimeOrder().subtract(BigInteger.ONE));
    }
    
    private void format(Share[] shares){
        int t = getPublicInfo().getT();
        int last=-2;
        int seq=0;
        
        for(int i=0; i<shares.length; i++) {
            if(shares[i] != null){
                if(last+1 == i){
                    seq++;
                    if(seq >= t){
                        return;
                    }
                }else{
                    seq = 1;
                    for(int j=0; j < i; j++) {
                        shares[j] = null;
                    }
                }
                last=i;
            }
        }
    }
    
    /////////////////////// some static utilities methods //////////////////////
    
    private static MessageDigest md = null;
    
    public static BigInteger hash(PublicInfo info, byte[] data) throws InvalidVSSScheme {
        try{
            if(md == null) {
                md = MessageDigest.getInstance(info.getHashAlgorithm());
            }else {
                md.reset();
            }
            return new BigInteger(md.digest(data));
        }catch(NoSuchAlgorithmException e){
            throw new InvalidVSSScheme("Invalid hash algorithm "+info.getHashAlgorithm());
        }
    }

    public static byte[] encrypt(PublicInfo info, byte[] key, byte[] data) throws InvalidVSSScheme {
        try {
            SecretKey k = SecretKeyFactory.getInstance("DESEDE").generateSecret(
                    new DESedeKeySpec(key));
            Cipher cipher = Cipher.getInstance("DESEDE");
            cipher.init(Cipher.ENCRYPT_MODE,k);
            
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException ex) {
            throw new InvalidVSSScheme("Invalid block cipher algorithm "+info.getHashAlgorithm());
        } catch (Exception e) {
            throw new RuntimeException("Problems encrypting",e);
        }
    }

    public static byte[] decrypt(PublicInfo info, byte[] key, byte[] data) throws InvalidVSSScheme {
        try{
            SecretKey k = SecretKeyFactory.getInstance("DESEDE").generateSecret(
                    new DESedeKeySpec(key));
            Cipher cipher = Cipher.getInstance("DESEDE");
            cipher.init(Cipher.DECRYPT_MODE,k);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException ex) {
            throw new InvalidVSSScheme("Invalid block cipher algorithm "+info.getHashAlgorithm());
        } catch (Exception e) {
            throw new RuntimeException("Problems decrypting",e);
        }
    }    
}

/*
 * PublishedShares.java
 *
 * Created on 28 de Junho de 2005, 14:02
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package pvss;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;

/**
 *
 * @author neves
 */
public class PublishedShares implements Serializable {
    
    private BigInteger[] commitments;
    private BigInteger[] encriptedShares;
    private BigInteger[] proofsr;
    private BigInteger proofc;
    
    private byte[] U; //general secret share

    private int hashcode;
    
    /** Creates a new instance of PublishedShares */
    public PublishedShares(BigInteger[] commitments, BigInteger[] encriptedShares,
            BigInteger[] proofsr, BigInteger proofc, byte[] U) {
    
        this.commitments = commitments;
        this.encriptedShares = encriptedShares;
        this.proofsr = proofsr;
        this.proofc = proofc;
        
        this.U = U;
    }

    public BigInteger[] getCommitments() {
        return commitments;
    }

    public BigInteger[] getEncriptedShares() {
        return encriptedShares;
    }

    public BigInteger[] getProofsr() {
        return proofsr;
    }

    public BigInteger getProofc() {
        return proofc;
    }
    
    public boolean verify(PublicInfo info, BigInteger[] publicKeys) throws InvalidVSSScheme{
        int n = info.getN();
        int t = info.getT();
        BigInteger []z = info.getInterpolationPoints();
        BigInteger q = info.getGroupPrimeOrder();
        BigInteger qm1 = q.subtract(BigInteger.ONE);
        
        BigInteger[] X = new BigInteger[n];
        BigInteger[] a1 = new BigInteger[n];
        BigInteger[] a2 = new BigInteger[n];
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        
        for(int i=0; i<n; i++) {

            //calcs Xi
            BigInteger exp = BigInteger.ONE;
            BigInteger mult = commitments[0];
            for(int j=1; j<t; j++) {
                exp = exp.multiply(z[i]).mod(qm1);
                mult = mult.multiply(commitments[j].modPow(exp,q)).mod(q);
            }
            X[i] = mult;
            
            BigInteger a1Temp = info.getGeneratorg().modPow(proofsr[i],q);
            a1[i] = a1Temp.multiply(X[i].modPow(proofc,q)).mod(q);
            
            BigInteger a2Temp = publicKeys[i].modPow(proofsr[i],q);
            a2[i] = a2Temp.multiply(encriptedShares[i].modPow(proofc,q)).mod(q);

            try{
                baos.write(X[i].toByteArray());
                baos.write(encriptedShares[i].toByteArray());
                baos.write(a1[i].toByteArray());
                baos.write(a2[i].toByteArray());
            }catch(IOException ioe){
                System.out.println("Got exception while verifying share!");
                return false;
            }
        }
     
        
        System.out.println("Verification of distributed shares: ");
        System.out.println(" Xs ="+Arrays.toString(X));
        System.out.println(" Ys ="+Arrays.toString(encriptedShares));
        System.out.println(" a1s="+Arrays.toString(a1));
        System.out.println(" a2s="+Arrays.toString(a2));
        System.out.println(" hs="+PVSSEngine.hash(info,baos.toByteArray()).mod(q));
        System.out.println(" cs="+proofc);
        System.out.println(" rs="+Arrays.toString(proofsr));
        
        return PVSSEngine.hash(info,baos.toByteArray()).mod(qm1).equals(proofc);
    }
    
    //modified by bavbralak
    public Share getShare(int index, BigInteger privateKeyServer, PublicInfo info, 
            BigInteger[] publicKeys) throws InvalidVSSScheme{
        
        BigInteger q = info.getGroupPrimeOrder();
        BigInteger qm1 = q.subtract(BigInteger.ONE);
        
        BigInteger xinverse = privateKeyServer.modInverse(qm1);
        
        //System.out.println("1/x="+xinverse);
        
        BigInteger share = encriptedShares[index].modPow(xinverse,q);
        
        //System.out.println("server enc share "+index+": "+encriptedShares[index]);
        //System.out.println("server share "+index+": "+share);
        
        
        //check if this w is necessary here??? it should just be necessary to
        //proofc 
        BigInteger w = BigInteger.valueOf(11);
        
        BigInteger a1 = info.getGeneratorG().modPow(w,q);
        BigInteger a2 = share.modPow(w,q);
        
        //System.out.println("a1="+a1+", a2="+a2);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        
        try{
            baos.write(publicKeys[index].toByteArray());
            baos.write(encriptedShares[index].toByteArray());
            baos.write(a1.toByteArray());
            baos.write(a2.toByteArray());
        }catch(IOException ioe){
            throw new InvalidVSSScheme("Problems crating hash for proof");
        }
        
        BigInteger proofc = PVSSEngine.hash(info,baos.toByteArray()).mod(q);        
        BigInteger proofr = w.subtract(privateKeyServer.multiply(proofc)).
          mod(q.subtract(BigInteger.ONE));
        
        return new Share(index, encriptedShares[index], share, proofc, proofr, U);
    }
    
    public int hashCode() {
        if(hashcode == 0) {
            hashcode = toString().hashCode();
        }
        return hashcode;
    }
    
    public String toString() {
        return "commitments="+Arrays.toString(commitments)+", encriptedShares="+
                Arrays.toString(encriptedShares)+", r\'s="+Arrays.toString(proofsr)+
                ", c="+proofc;
    }
}

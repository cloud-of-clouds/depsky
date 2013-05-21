/*
 * Share.java
 *
 * Created on 28 de Junho de 2005, 15:36
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

/**
 *
 * @author neves
 */
public class Share implements Serializable {
    
    private int index;
    private BigInteger interpolationPoint;
    private BigInteger encryptedShare;
    private BigInteger share;
    private BigInteger proofc;
    private BigInteger proofr;
    
    private byte[] U; //for general secret sharing


    /** Creates a new instance of Share */
    public Share(int index, BigInteger encryptedShare, BigInteger share,
            BigInteger proofc, BigInteger proofr, byte[] U) {
        
        this.index = index;
        this.encryptedShare = encryptedShare;
        this.share = share;
        this.proofc = proofc;
        this.proofr = proofr;
        
        this.U = U;
    }
    


    public boolean verify(PublicInfo info, BigInteger publicKey) throws InvalidVSSScheme{
        
        BigInteger q = info.getGroupPrimeOrder();
        //ok   System.out.println("proofr"+proofr+"q"+q+"info"+info+info.getGeneratorG());
        System.out.println("info.getGeneratorG().modPow(proofr,q)"+info.getGeneratorG().modPow(proofr,q));
        System.out.println("publicKey :: "+publicKey );
        System.out.println("publicKey.modPow(proofc,q)"+publicKey.modPow(proofc,q));

        BigInteger a1 = info.getGeneratorG().modPow(proofr,q).
                multiply(publicKey.modPow(proofc,q)).mod(q);
        BigInteger a2 = share.modPow(proofr,q).
                multiply(encryptedShare.modPow(proofc,q)).mod(q);
        
        System.out.println("a1="+a1+", a2="+a2);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        
        try{
            baos.write(publicKey.toByteArray());
            baos.write(encryptedShare.toByteArray());
            baos.write(a1.toByteArray());
            baos.write(a2.toByteArray());
        }catch(IOException ioe){
            throw new InvalidVSSScheme("Problems crating hash for proof");
        }
        
        BigInteger h = PVSSEngine.hash(info,baos.toByteArray()).mod(q);
        
        System.out.println("h="+h);
        
        return h.equals(proofc);
    }

    public int getIndex() {
        return index;
    }

    public BigInteger getShare() {
        return share;
    }

    public BigInteger getProofc() {
        return proofc;
    }

    public BigInteger getProofr() {
        return proofr;
    }
    
    public byte[] getU() {
        return U;
    }
    
    public String toString() {
        return "secret("+index+")="+share+", proof=("+proofc+","+proofr+")";
    }
}

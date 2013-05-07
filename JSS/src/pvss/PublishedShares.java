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
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;

/**
 *
 * @author neves
 */
public class PublishedShares implements Externalizable {
    
    private BigInteger[] commitments;
    private BigInteger[] encriptedShares;
    private BigInteger[] proofsr;
    private BigInteger proofc;
    
    private byte[] U; //general secret share
    
    private int hashcode;
    
    public PublishedShares(){
    }
    
    /** Creates a new instance of PublishedShares */
    public PublishedShares(BigInteger[] commitments, BigInteger[] encriptedShares,
            BigInteger[] proofsr, BigInteger proofc, byte[] U) {
        
        this.commitments = commitments;
        this.encriptedShares = encriptedShares;
        this.proofsr = proofsr;
        this.proofc = proofc;
        
        this.U = U;
    }
    
    public void writeExternal(ObjectOutput out) throws IOException{
        out.writeInt(hashcode);
        
        out.writeInt(U.length);
        out.write(U);
        
        writeBIArray(commitments,out);
        writeBIArray(encriptedShares,out);
        writeBIArray(proofsr,out);
        writeBI(proofc,out);
        
    }
    
    private void writeBI(BigInteger bi, ObjectOutput out)throws IOException{
        byte[] b = bi.toByteArray();
        out.writeInt(b.length);
        out.write(b);
    }
    
    private void writeBIArray(BigInteger[] bis, ObjectOutput out)throws IOException{
        out.writeInt(bis.length);
        for(int i = 0; i < bis.length; i++){
            writeBI(bis[i],out);
        }
    }
    
    private BigInteger readBI(ObjectInput in)throws IOException{
        byte[] b = new byte[in.readInt()];
        in.readFully(b);
        return new BigInteger(b);
    }

    private BigInteger[] readBIArray(ObjectInput in)throws IOException{
        int tam = in.readInt();
        BigInteger[] ret = new BigInteger[tam];
        for(int i = 0; i < tam; i++){
            ret[i] = readBI(in);
        }
        return ret;
    }
    
    public void readExternal(ObjectInput in) throws IOException,ClassNotFoundException{
        hashcode = in.readInt();
        U = new byte[in.readInt()];
        in.readFully(U);
        commitments = readBIArray(in);
        encriptedShares = readBIArray(in);
        proofsr = readBIArray(in);
        proofc = readBI(in);
    }
    
    
    public byte[] getU(){
        return this.U;
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
                exp = exp.multiply(BigInteger.valueOf(i+1)).mod(qm1);
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
                return false;
            }
        }
        
        /*
        System.out.println("Verificacao dos Shares Distribuidos:");
        System.out.println(" a1="+Arrays.toString(a1));
        System.out.println(" a2="+Arrays.toString(a2));
        System.out.println(" h="+PVSSEngine.hash(info,baos.toByteArray()).mod(q));
         */
        
        
        return PVSSEngine.hash(info,baos.toByteArray()).mod(qm1).equals(proofc);
    }
    
    public Share getShare(int index, BigInteger secretKey, PublicInfo info,
            BigInteger[] publicKeys) throws InvalidVSSScheme{
        
        BigInteger q = info.getGroupPrimeOrder();
        BigInteger qm1 = q.subtract(BigInteger.ONE);
        
        BigInteger xinverse = secretKey.modInverse(qm1);
        
        //System.out.println("1/x="+xinverse);
        
        BigInteger share = encriptedShares[index].modPow(xinverse,q);
        
        //System.out.println("server enc share "+index+": "+encriptedShares[index]);
        //System.out.println("server share "+index+": "+share);
        
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
        
        BigInteger proofr = w.subtract(secretKey.multiply(proofc)).
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

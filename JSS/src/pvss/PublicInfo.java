package pvss;

import java.io.Serializable;
import java.math.BigInteger;

/*
 * PublicInfo.java
 *
 * Created on 28 de Junho de 2005, 13:25
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

/**
 *
 * @author neves
 */
public class PublicInfo implements Serializable {
    
    private int n;
    private int t;
    
    private int numBits;
    
    private BigInteger groupPrimeOrder;
    
    private BigInteger generatorg;
    private BigInteger generatorG;
    
    private String hashAlgorithm = "SHA-1";
    private BigInteger [] interpolationPoints;
    /**
     * Creates a new instance of PublicInfo 
     */
    public PublicInfo(int n, int t, BigInteger groupPrimeOrder,
            BigInteger generatorg, BigInteger generatorG) {
        
        this.n = n;
        this.t = t;

        this.numBits = groupPrimeOrder.bitLength()-1;
        interpolationPoints = new BigInteger[n];
        this.groupPrimeOrder = groupPrimeOrder;
        this.generatorg = generatorg;
        this.generatorG = generatorG;
    }

    /** BavBraLakh **/
    public BigInteger [] getInterpolationPoints()
    {
        return interpolationPoints;
    }
    
    public int getN() {
        return n;
    }

    public int getT() {
        return t;
    }

    public BigInteger getGroupPrimeOrder() {
        return groupPrimeOrder;
    }

    public BigInteger getGeneratorg() {
        return generatorg;
    }

    public BigInteger getGeneratorG() {
        return generatorG;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }
    
    public int getNumBits() {
        return numBits;
    }
    
    public String toString() 
    {
        return "("+n+","+t+")-"+numBits+" bits. q="+groupPrimeOrder+", g="+
                generatorg+", G="+generatorG;
    }

    /* The function fills the interpolation points which the server 
     * use to while verifying the shares
     *
     * @author Avni
     * @author Luis
     * @author Prabhakar
     */  
    void setInterpolationPoints() 
    {
        // z = 1 2 3 4
        // BavBraLak : 1,2,3,4 was the previous default value
        // but lagrange coeeff are not integer for some pairs of servers shares
        //z[0] = new BigInteger("1");z[1] = new BigInteger("2");
        //z[1] = new BigInteger("3");z[3] = new BigInteger("4");
        // BavBraLak : 6,8,9,12 work for all pairs of servers
        if(n==4&&t==2)
        {
            interpolationPoints[0] = new BigInteger("6");
            interpolationPoints[1] = new BigInteger("8");
            interpolationPoints[2] = new BigInteger("9");
            interpolationPoints[3] = new BigInteger("12");
        }

        if(n!=4||t!=2)
        {
            for(int i=0;i<n;i++)
            {
               interpolationPoints[i]=BigInteger.valueOf(i+1);
            }
        }
    }
}

/*
    public static PublicInfo createVSSPublicInfo(String fileName) throws InvalidVSSScheme, IOException{
        FileInputStream fileInputStream = new FileInputStream(fileName);
        
        Properties props = new Properties();
        props.load(fileInputStream);
    
        fileInputStream.close();
        
        return createVSSPublicInfo(props);
    }

    public static PublicInfo createVSSPublicInfo(Properties props) throws InvalidVSSScheme {

        int n = new Integer(props.getProperty("n"));
        int t = new Integer(props.getProperty("t"));

        
        BigInteger groupPrimeOrder = new BigInteger(props.getProperty("groupPrimeOrder"));
        BigInteger generatorg = ;
        BigInteger generatorG = ;
        BigInteger[] publicKeys = new BigInteger[n];
        return new PublicInfo(n,t,groupPrimeOrder,generatorg,generatorG,publicKeys);
    }
*/  


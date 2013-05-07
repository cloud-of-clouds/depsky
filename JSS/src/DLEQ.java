import java.math.BigInteger;
/*
 * DLEQ.java
 *
 * Created on 1 de Julho de 2005, 12:44
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

/**
 *
 * @author neves
 */
public class DLEQ {
    
    /** Creates a new instance of DLEQ */
    public DLEQ() {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        BigInteger Q = BigInteger.valueOf(13);
        BigInteger Q1 = Q.subtract(BigInteger.ONE);
        BigInteger ALPHA = BigInteger.valueOf(2);
        BigInteger W = BigInteger.valueOf(8);
        BigInteger C = BigInteger.valueOf(4);
        
        // TODO code application logic here
        BigInteger g1 = new BigInteger(args[0]);
        BigInteger g2 = new BigInteger(args[1]);
        
        BigInteger h1 = g1.modPow(ALPHA,Q);
        BigInteger h2 = g2.modPow(ALPHA,Q);
        
        BigInteger a1 = g1.modPow(W,Q);
        BigInteger a2 = g2.modPow(W,Q);
        
        System.out.println(a1+","+a2);
        
        BigInteger r = W.subtract(ALPHA.multiply(C)).mod(Q1);
        
        BigInteger ver_a1 = g1.modPow(r,Q).multiply(h1.modPow(C,Q)).mod(Q);
        BigInteger ver_a2 = g2.modPow(r,Q).multiply(h2.modPow(C,Q)).mod(Q);
        
        System.out.println(ver_a1+","+ver_a2);
        
        System.out.println(new BigInteger(args[2]).modInverse(Q1));
    }
    
}

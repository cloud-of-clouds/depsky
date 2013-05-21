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
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public static PVSSEngine getInstance(int n, int t, int numBits)
            throws InvalidVSSScheme {

        if (t > n) {
            throw new InvalidVSSScheme("(t,n)=(" + t + "," + n + ")");
        }

        /**
         * Fill in public information
         */
        BigInteger groupPrimeOrder = new BigInteger("2373168401");
        BigInteger generatorg = new BigInteger("1964832733");
        BigInteger generatorG = new BigInteger("1476385517");

        //System.out.println("groupPrimeOrder : " + groupPrimeOrder);
        //System.out.println("generatorg : " + generatorg);
        //System.out.println("generatorG : " + generatorG);

        /*
         * Initialize public information structure
         */
        PublicInfo publicInfo = new PublicInfo(n, t, groupPrimeOrder,
                generatorg, generatorG);

        /*
         * Generate interpolation points
         */
        publicInfo.setInterpolationPoints();

        return new PVSSEngine(publicInfo);
    }

    public static PVSSEngine getInstance(PublicInfo publicInfo) throws InvalidVSSScheme {
    	publicInfo.setInterpolationPoints();
        return new PVSSEngine(publicInfo);
    }

    public PublicInfo getPublicInfo() {
        return publicInfo;
    }

    public BigInteger generateSecret() {
        return new BigInteger(getPublicInfo().getNumBits() - 1, random);
    }

    public BigInteger[] generateSecretKeys() {
        BigInteger[] secretKeys = new BigInteger[getPublicInfo().getN()];

        boolean passed = false;

        for (int i = 0; i < secretKeys.length; i++) {
            secretKeys[i] = generateRandomNumber();
            //verifica se a chave i e igual a alguma das chaves geradas 
            //anteriormente
            for (int j = 0; j < i; j++) {
                if (secretKeys[i].equals(secretKeys[j])) {
                    //e igual! i deve ser regerado!!!
                    i--;
                }
            }
        }
        /*
         * secretKeys[0] = BigInteger.valueOf(3); secretKeys[1] =
         * BigInteger.valueOf(2); secretKeys[2] = BigInteger.valueOf(5);
         * secretKeys[3] = BigInteger.valueOf(7);
         */

        return secretKeys;
    }

    public BigInteger generatePublicKey(BigInteger secretKey) {
        return getPublicInfo().getGeneratorG().modPow(secretKey,
                getPublicInfo().getGroupPrimeOrder());
    }

    public PublishedShares generalPublishShares(byte[] data,
            BigInteger[] publicKeys, int choice) throws InvalidVSSScheme {

        BigInteger secret = generateSecret();

        //System.out.println("Time = "+(System.currentTimeMillis()-l));
        BigInteger encryptedSecret = getPublicInfo().getGeneratorG().modPow(
                secret, getPublicInfo().getGroupPrimeOrder());

        //System.out.println("Encrypted secret: " + encryptedSecret);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(24);
        byte[] ensecret = encryptedSecret.toByteArray();
        byte[] pad = new byte[24];
        int len = encryptedSecret.toByteArray().length;
        
        //System.out.println("Orig byte length of encrypted secret: " + len);
        for (int i = 0; i < len; i++) {
            pad[i] = encryptedSecret.toByteArray()[i];
        }

        for (int i = len; i < 24; i++) {
            pad[i] = 0;
        }

        //System.out.println("Converting ensecret.len =" + ensecret.length);
        baos.write(pad, 0, 24);
        //System.out.println("Converted ensecret to byte array with len =" + baos.size());

        byte[] U = encrypt(getPublicInfo(), pad, data);
        return publishShares(secret, U, publicKeys, choice);
    }

    public PublishedShares publishShares(BigInteger secret, byte[] U, BigInteger[] publicKeys, int choice) throws InvalidVSSScheme {

        int t = getPublicInfo().getT();
        int n = getPublicInfo().getN();
        BigInteger g = getPublicInfo().getGeneratorg();

        BigInteger q = getPublicInfo().getGroupPrimeOrder();
        BigInteger qm1 = q.subtract(BigInteger.ONE);

        BigInteger[] coefs = new BigInteger[t];
        /*
         * for(int j=1; j<t; j++) { coefs[j] = new
         * BigInteger(getPublicInfo().getNumBits()-1,random); //coefs[j] =
         * generateRandomNumber(); commitments[j] = g.modPow(coefs[j],q); }
         */
        coefs[0] = secret;
        coefs[1] = BigInteger.valueOf(131);
        //coefs[2] = BigInteger.valueOf(10);

        //Commitments of the coeficients
        BigInteger[] commitments = new BigInteger[t];
        for (int j = 0; j < t; j++) {
            commitments[j] = g.modPow(coefs[j], q);
        }

        BigInteger[] shares = new BigInteger[n];
        BigInteger[] encriptedShares = new BigInteger[n];

        BigInteger[] X = new BigInteger[n];
        BigInteger[] a1 = new BigInteger[n];
        BigInteger[] a2 = new BigInteger[n];
        BigInteger[] proofsr = new BigInteger[n];
        if (n != 4 || t != 2) {
            System.out.println("Warning : Will proceed "
                    + "with default/wrong coefs ");
        }

        // BavBraLak : z[i] is the interpolation point selected for server i
        BigInteger[] z = this.publicInfo.getInterpolationPoints();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        //BavBraLak : W should be a vector not a single value
        BigInteger w = BigInteger.valueOf(11);

        for (int i = 0; i < n; i++) {
            //workingshares[i] = poly(coefs,i+1);

            //shares[i] = poly(coefs,z[i].intValue());
            shares[i] = poly(coefs, z[i].intValue());
            encriptedShares[i] = publicKeys[i].modPow(shares[i], q);
            overwriteBasedOnChoice(encriptedShares, choice, i);
            //calcs Xi
            BigInteger exp = BigInteger.ONE;
            BigInteger mult = commitments[0];
            for (int j = 1; j < t; j++) {
                //exp = exp.multiply(BigInteger.valueOf(i+1)).mod(qm1);
                exp = exp.multiply(z[i]).mod(qm1);
                mult = mult.multiply(commitments[j].modPow(exp, q)).mod(q);
            }
            X[i] = mult;
            a1[i] = g.modPow(w, q);
            a2[i] = publicKeys[i].modPow(w, q);

            try {
                baos.write(X[i].toByteArray());
                baos.write(encriptedShares[i].toByteArray());
                baos.write(a1[i].toByteArray());
                baos.write(a2[i].toByteArray());
            } catch (IOException ioe) {
                throw new InvalidVSSScheme("Problems crating hash for proof");
            }
        }
        //System.out.println("Xc  =" + Arrays.toString(X));
        //System.out.println("Yc  =" + Arrays.toString(encriptedShares));
        //System.out.println("a1c =" + Arrays.toString(a1));
        //System.out.println("a2c =" + Arrays.toString(a2));


        //System.out.println("Published data:");
        //System.out.println(" X="+Arrays.toString(X));
        //System.out.println(" a1="+Arrays.toString(a1));
        //System.out.println(" a2="+Arrays.toString(a2));
        //System.out.println(" shares="+Arrays.toString(shares));
        //System.out.println(" encryptedshares="+Arrays.toString(encriptedShares));

        BigInteger proofc = PVSSEngine.hash(getPublicInfo(), baos.toByteArray()).mod(qm1);
        //System.out.println("cc       :" + proofc);

        for (int i = 0; i < n; i++) {
            proofsr[i] = w.subtract(shares[i].multiply(proofc)).mod(qm1);
        }
        //System.out.println("rc      :" + Arrays.toString(proofsr));
        return new PublishedShares(commitments, encriptedShares, proofsr, proofc, U);
    }

    public byte[] generalCombineShares(Share[] shares)
            throws InvalidVSSScheme {
        int[] x = new int[getPublicInfo().getT()];
        int j = 0;
        int k = 0;
        Share[] acceptedShares = new Share[getPublicInfo().getN()];

        //Removed the format function from here. Format function has a bug
        /*
         * The accepted share doing nothing!
         */
        for (int i = 0; i < shares.length; i++) {
            if (shares[i] != null) {
                x[j++] = k;
                acceptedShares[k++] = shares[i];

                if (j == x.length) {
                    break;
                }
            } else {
                acceptedShares[k++] = null;
            }
        }


        for (int i = 0; i < acceptedShares.length; i++) {
            //System.out.println("Share [" + i + "] is : " + acceptedShares[i]);
            if (acceptedShares[i] != null) {
            }
        }

        BigInteger encryptedSecret;
        try {
            encryptedSecret = combineShares(x, acceptedShares);
            int len = encryptedSecret.toByteArray().length;
            byte[] pad = new byte[24];

            //System.out.println("Orig byte length of encrypted secret: " + len);
            for (int i = 0; i < len; i++) {
                pad[i] = encryptedSecret.toByteArray()[i];
            }

            for (int i = len; i < 24; i++) {
                pad[i] = 0;
            }

            //note that U is equal for every share/server
            return decrypt(getPublicInfo(),
                    pad,
                    acceptedShares[x[0]].getU());
        } catch (Exception ex) {
            Logger.getLogger(PVSSEngine.class.getName()).log(Level.SEVERE,
                    null, ex);
        }

        return null;
    }

    public byte[] generalCombineShares(int[] x, Share[] shares) throws InvalidVSSScheme, Exception {
        BigInteger encryptedSecret = combineShares(x, shares);
        //System.out.println("U :" + shares[0].getU());
        return decrypt(getPublicInfo(), encryptedSecret.toByteArray(), shares[0].getU());
    }

    public BigInteger combineShares(int x[], Share[] shares) throws Exception {
        int t = publicInfo.getT();
        int n = publicInfo.getN();
        BigInteger[] z = this.publicInfo.getInterpolationPoints();

        if (x.length != t) {
            throw new RuntimeException("There must be " + t
                    + " diferent and valid shares");
        }

        for (int i : x) {
            if (shares[i] == null) {
                throw new RuntimeException("There must be " + t
                        + " diferent and valid shares");
            }
        }

        BigInteger q = publicInfo.getGroupPrimeOrder();
        BigInteger qm1 = q.subtract(BigInteger.ONE);

        BigInteger secret = BigInteger.ONE;

        //System.out.print("PVSSEngine.combineShares: Using shares");
        for (int i = 0; i < t; i++) { //iterates over x[i]
            //System.out.print(" "+x[i]);

            /*
             * float lambda = 1; for(int j=0; j<t; j++) { //iterates over x[j]
             * if(j != i){ lambda = lambda*((float)(x[j]+1)/(float)(x[j]-x[i]));
             * } }
             *
             * secret = secret.multiply(shares[x[i]].getShare()
                    .modPow(BigInteger.valueOf((long)lambda),q)).mod(q);
             */

            BigInteger lambda = BigInteger.ONE;
            BigInteger lambdadenom = BigInteger.ONE;
            BigInteger lambdanumer = BigInteger.ONE;
            BigInteger gcd = BigInteger.ONE;

            for (int j = 0; j < t; j++) { //iterates over x[j]
                if (j != i) {
                    //It,s essential that mod is not done here, because of gcd
                    lambdanumer = lambdanumer.multiply(z[x[j]]);
                    lambdadenom = lambdadenom.multiply(z[x[j]].subtract(z[x[i]]));
                }
            }

           //System.out.println("lambdanumer " + lambdanumer + ", lambdadenom " + lambdadenom);

            gcd = lambdadenom.gcd(lambdanumer);
            lambdadenom = lambdadenom.divide(gcd);
            lambdanumer = lambdanumer.divide(gcd);

            /**
             * Sanity check *
             */
            gcd = lambdadenom.gcd(qm1);

            //System.out.println("GCD :" + gcd.intValue());

            if (gcd.intValue() != 1) {
                //System.out.println("i :" + i);
                //System.out.println("lambdanumer " + lambdanumer);
                //System.out.println("lambdadenom " + lambdadenom);
                //System.out.println("z array :" + Arrays.toString(z));
                //System.out.println("x array :" + Arrays.toString(x));

                throw invalidLagrangeCoeff();
            }
            BigInteger invdenom = lambdadenom.modInverse(qm1);
            lambda = lambdanumer.multiply(invdenom).mod(qm1);
            //System.out.println("i = " + i + ", lambda = " + lambda + ", lambdanumer "
               //     + lambdanumer + ", lambdadenom " + lambdadenom);

            secret = secret.multiply(shares[x[i]].getShare().modPow(lambda, q)).mod(q);
        }


        /*
         * BigInteger secret = BigInteger.ONE;
         *
         * //System.out.print("PVSSEngine.combineShares: Using shares"); for(int
         * i=0; i<t; i++){ //iterates over x[i] //System.out.print(" "+x[i]);
         *
         * BigInteger lambda2 = BigInteger.valueOf(1); for(int j=0; j<t; j++) {
         * //iterates over x[j] if(j != i){
         *
         * BigInteger denominator = BigInteger.valueOf(x[j]-x[i]); lambda2 =
         * lambda2.multiply( BigInteger.valueOf((x[j]+1)).multiply(
         * denominator.modInverse(q)) ).mod(qm1); } }
         *
         * System.out.println("shares[x[i]].getShare()
         * ="+shares[x[i]].getShare()); System.out.println("Our lambda2 :
         * "+lambda2); System.out.println("q : "+q);
         *
         * BigInteger temp2 = (shares[x[i]].getShare()).modPow(lambda2,q);
         * System.out.println("temp2 ="+temp2);          *
         * float lambda = 1; for(int j=0; j<t; j++) { //iterates over x[j] if(j
         * != i){ lambda = lambda*((float)(x[j]+1)/(float)(x[j]-x[i])); } }
         *
         * BigInteger temp1 =
         * shares[x[i]].getShare().modPow(BigInteger.valueOf((long)lambda),q);
         * System.out.println("lambda : "+lambda); System.out.println("temp1
         * ="+temp1);
         *
         * System.out.println("q : "+q);
         *
         * secret =
         * secret.multiply(shares[x[i]].getShare().modPow(BigInteger.valueOf((long)lambda),q)).mod(q);
        }
         */


        //System.out.println("Secret returning from combine share :" + secret);
        return secret;
    }

    //some private methods
    private BigInteger generateRandomNumber() {
        return BigInteger.probablePrime(getPublicInfo().getNumBits() - 1, random);
        //return new BigInteger(getPublicInfo().getNumBits()-1,random);
    }

    private BigInteger poly(BigInteger[] coefs, int val) {
        BigInteger y = coefs[0];

        for (int j = 1; j < coefs.length; j++) {
            BigInteger term = coefs[j].multiply(BigInteger.valueOf((long) Math.pow(val, j)));
            y = y.add(term);
        }

        //poly mod (q-1)
        return y.mod(getPublicInfo().getGroupPrimeOrder().subtract(BigInteger.ONE));
    }

    private void format(Share[] shares) {
        int t = getPublicInfo().getT();
        int last = -2;
        int seq = 0;

        for (int i = 0; i < shares.length; i++) {
            if (shares[i] != null) {
                if (last + 1 == i) {
                    seq++;
                    if (seq >= t) {
                        return;
                    }
                } else {
                    seq = 1;
                    for (int j = 0; j < i; j++) {
                        shares[j] = null;
                    }
                }
                last = i;
            }
        }
    }
    /////////////////////// some static utilities methods //////////////////////
    private static MessageDigest md = null;

    public static BigInteger hash(PublicInfo info, byte[] data) throws InvalidVSSScheme {
        try {
            if (md == null) {
                md = MessageDigest.getInstance(info.getHashAlgorithm());
            } else {
                md.reset();
            }
            return new BigInteger(md.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidVSSScheme("Invalid hash algorithm "
                    + info.getHashAlgorithm());
        }
    }

    public static byte[] encrypt(PublicInfo info, byte[] key, byte[] data) throws InvalidVSSScheme {


        try {
            //System.out.println("Key length " + key.length);
            SecretKey k = SecretKeyFactory.getInstance("DESEDE").generateSecret(
                    new DESedeKeySpec(key));
            Cipher cipher = Cipher.getInstance("DESEDE");
            cipher.init(Cipher.ENCRYPT_MODE, k);

            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException ex) {
            throw new InvalidVSSScheme("Invalid block cipher algorithm " + info);
        } catch (Exception e) {
            throw new RuntimeException("Problems encrypting", e);
        }
    }

    public static byte[] decrypt(PublicInfo info, byte[] key, byte[] data) throws InvalidVSSScheme {
        try {

            //System.out.println("keylen in decrypt is " + key.length);
            SecretKey k = SecretKeyFactory.getInstance("DESEDE").generateSecret(
                    new DESedeKeySpec(key));

            Cipher cipher = Cipher.getInstance("DESEDE");
            cipher.init(Cipher.DECRYPT_MODE, k);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException ex) {
            throw new InvalidVSSScheme("Invalid block cipher algorithm " + info);
        } catch (Exception e) {
            throw new RuntimeException("Problems decrypting", e);
        }
    }

    private Exception invalidLagrangeCoeff() {
        Exception e = new Exception();
        System.out.println("Invalid lagrange coefficient for current "
                + "modulus");
        return e;
    }

    private void overwriteBasedOnChoice(BigInteger[] encriptedShares, int choice, int i) {
        if (choice == 2) {
            encriptedShares[i] = BigInteger.valueOf(111111);
        }
        if (choice == 3) {
            encriptedShares[0] = BigInteger.valueOf(123456);
        }

    }
}

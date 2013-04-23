package depskys.core;

//RSA - Rivest, Shamir, & Adleman
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MyAESCipher {

	Cipher ecipher;
	Cipher dcipher;
	// Buffer used to transport the bytes from one stream to another
	byte[] buf = new byte[1024];

	/**
	 * Encryption with AES
	 * @param key
	 */
	public MyAESCipher(SecretKey key) {

		// Create an 8-byte initialization vector
		byte[] iv = new byte[]{
				0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f
		};

		AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);

		try {
			ecipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			dcipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

			// CBC requires an initialization vector
			ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
			dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Encrypts a file
	 * @param in
	 * @param out
	 */
	public void encrypt(InputStream in, OutputStream out) {
		try {
			// Bytes written to out will be encrypted
			out = new CipherOutputStream(out, ecipher);

			// Read in the cleartext bytes and write to out to encrypt
			int numRead = 0;
			while ((numRead = in.read(buf)) >= 0) {
				out.write(buf, 0, numRead);
			}
			out.close();
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Decrypts a file
	 * @param in
	 * @param out
	 */
	public void decrypt(InputStream in, OutputStream out) {
		try {
			// Bytes read from in will be decrypted
			in = new CipherInputStream(in, dcipher);

			// Read in the decrypted bytes and write the cleartext to out
			int numRead = 0;
			while ((numRead = in.read(buf)) >= 0) {
				out.write(buf, 0, numRead);
			}
			out.close();
		} catch (java.io.IOException e) {
		}
	}

	/*************************************************/
	/**
	 *
	 * @param key
	 */
	public static byte[] myEncrypt(SecretKey mykeyENC, byte[] data) {
		// Create encrypter/decrypter class
		MyAESCipher encrypter = new MyAESCipher(mykeyENC);

		// Encrypt
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		encrypter.encrypt(new ByteArrayInputStream(data), baos);
		return baos.toByteArray();

	}//myEncrypt

	/**
	 *
	 * @param key
	 */
	public static byte[] myDecrypt(SecretKey mykeyDEC, byte[] encData) {
		// Create encrypter/decrypter class
		MyAESCipher encrypter = new MyAESCipher(mykeyDEC);

		// Decrypt
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		encrypter.decrypt(new ByteArrayInputStream(encData), baos);
		return baos.toByteArray();

	}//myDecrypt

	public static SecretKey generateSecretKey() throws Exception {
		// Generate a temporary key. In practice, you would save this key.
		// See also e464 Encrypting with DES Using a Pass Phrase.
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		kgen.init(128);
		SecretKey key = kgen.generateKey();

		/****TESTES********/
		byte[] keyBytes = key.getEncoded();
		String algorithm = key.getAlgorithm();
		SecretKey secretkey = new SecretKeySpec(keyBytes, algorithm);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		dos.writeInt(keyBytes.length);
		dos.write(keyBytes);
		dos.close();
		return key;

	}
}


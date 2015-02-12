package jec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import exception.UnsuccessfulDecodeException;

public class ReedSolomon {

	private static final int BYTE_SIZE = Byte.SIZE/8;
	private static int INT_SIZE = Integer.SIZE/8;


	//================= ENCODE ==================
	/**
	 * @param k
	 * @param m
	 * @param w
	 * @requires k>0 && m>=0 && w>0 && packetsize>=0 && buffersize>=0;
	 * @requires w == 8 || w == 16 || w == 32;
	 */
	public static EncodeResult encode(byte[] srcData, int k, int m, int w) {

		Map<String, byte[]> map = doEncode(srcData, k, m ,w);

		return new EncodeResult(insertMetadata(map, srcData.length, k, m, w), new ReedSolInfo(srcData.length, k, m, w));

	}

	private static Map<String, byte[]> doEncode(byte[] srcData, int k, int m, int w) {
		/* Determine original size of file */
		int size = srcData.length;

		int newsize = size;

		while (newsize%(k*w*INT_SIZE) != 0) 
			newsize++;

		/* Determine size of k+m files */
		int blocksize = newsize/k;

		/* Allocate data and coding */
		byte[][] data = new byte[k][blocksize];
		byte[][] coding = new byte[m][];
		for (int i = 0; i < m; i++) {
			coding[i] = new byte[BYTE_SIZE*blocksize];
		}

		/* Create coding matrix or bitmatrix and schedule */
		int[] matrix = ReedSolOperations.vandermondeCodingMatrix(k, m, w);

		/* Set pointers to point to file data */
		int copied = 0;
		for (int i = 0; i < k && copied<srcData.length; i++) {
			int len = (i < k-1 && srcData.length >= blocksize) ? blocksize : ((srcData.length - (i*blocksize)))>0 ? srcData.length - (i*blocksize) : 0;
			System.arraycopy(srcData, i*blocksize, data[i], 0, len );
			copied+=len;
		}

		/* Encode */
		ErasureCodes.matrixEncode(k, m, w, matrix, data, coding, blocksize);

		Map<String, byte[]> res = new HashMap<String, byte[]>();
		/* Write data and encoded data to k+m files */
		for	(int i = 1; i <= k; i++) {
			res.put("k"+i, data[i-1]);
		}
		for	(int i = 1; i <= m; i++) {
			res.put("m"+i, coding[i-1]);
		}

		return res;
	}

	private static byte[][] insertMetadata(Map<String, byte[]> map, int size, int k, int m, int w){
		try {

			byte[][] res = new byte[map.size()][];
			int index = 0;
			Set<String> keys =  map.keySet();
			for(String key : keys){
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos);

				oos.writeUTF(key);
				oos.writeInt(map.get(key).length);
				oos.write(map.get(key));

				oos.flush();
				oos.close();
				baos.close();
				map.remove(key);
				res[index++] = baos.toByteArray(); 
			}

			return res;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}

	//===========================================

	//================= DECODE ==================
	private static byte[] doDecode(Map<String, byte[]> srcData, int k, int m, int w, int originalSize) throws UnsuccessfulDecodeException {

		int[] erased = new int[k+m];
		int[] erasures = new int[k+m];
		byte[][] data = new byte[k][];
		byte[][] coding = new byte[m][];

		/* Create coding matrix or bitmatrix */
		int[] matrix = ReedSolOperations.vandermondeCodingMatrix(k, m, w);

		/* Begin decoding process */
		int blocksize = 0;				// size of individual files

		int numErased = 0;				// number of erased files
		byte[] temp;

		for (int i = 1; i <= k; i++) {
			temp = srcData.get("k"+i);
			if (temp==null) {
				erased[i-1] = 1;
				erasures[numErased] = i-1;
				numErased++;
			} else {
				blocksize = temp.length;
				data[i-1] = temp;
			}
		}
		for (int i = 1; i <= m; i++) {
			temp = srcData.get("m"+i);
			if (temp==null) {
				erased[k+(i-1)] = 1;
				erasures[numErased] = k+i-1;
				numErased++;
			}
			else {
				blocksize = temp.length;
				coding[i-1]=temp;
			}
		}

		/* Finish allocating data/coding if needed */
		for (int i = 0; i < numErased; i++) {
			if (erasures[i] < k) {
				data[erasures[i]] = new byte[blocksize];
			}
			else {
				coding[erasures[i]-k] = new byte[blocksize];
			}
		}

		erasures[numErased] = -1;

		/* Choose proper decoding method */
		if (ErasureCodes.matrixDecode(k, m, w, matrix, 1, erasures, data, coding, blocksize) == -1){
			/* Exit if decoding was unsuccessful */
			throw new UnsuccessfulDecodeException("Unsuccessful Decode!");
		}
		return concatBlocks(data, originalSize);

	}

	public static byte[] decode(byte[][] src, ReedSolInfo info) throws UnsuccessfulDecodeException {
		Map<String, byte[]> map = new HashMap<String,byte[]>();
		for(int i=0 ; i<src.length ; i++){
			try {
				ByteArrayInputStream bais = new ByteArrayInputStream(src[i]);
				ObjectInputStream ois = new ObjectInputStream(bais);
				String key = ois.readUTF();

				byte[] block = new byte[ois.readInt()];
				for(int j=0; j<block.length ; j++)
					block[i]=ois.readByte();

				map.put(key, block);

				bais.close();
				ois.close();
			} catch (IOException e) {
				continue;
			}
		}

		return doDecode(map, info.getK(), info.getM(), info.getW(), info.getSize());


	}
	
	private static byte[] concatBlocks(byte[][] decode, int originalSize){

		//put all blocks together after decode
		byte[] result = new byte[originalSize];
		int offset = 0;
		for(int i = 0; i < decode.length; i++){
			if(offset + decode[i].length < originalSize){
				//copy all
				System.arraycopy(decode[i], 0, result, offset, decode[i].length);
				decode[i]=null;
				offset+=decode[i].length;
			}else{
				//copy originalSize-offset
				System.arraycopy(decode[i], 0, result, offset, originalSize - offset);
				decode[i]=null;
				break;
			}
		}
		return result;
	}
	//===========================================

}

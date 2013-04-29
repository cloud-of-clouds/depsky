package jec;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ReedSolEncoder {

	private final int BYTE_SIZE = Byte.SIZE/8;
	private int INT_SIZE = Integer.SIZE/8;

	private int k;
	private int m;
	private int w;

	/**
	 * @param k
	 * @param m
	 * @param w
	 * @requires k>0 && m>=0 && w>0 && packetsize>=0 && buffersize>=0;
	 * @requires w == 8 || w == 16 || w == 32;
	 */
	public ReedSolEncoder(int k, int m, int w) {
		this.k = k;
		this.m = m;
		this.w = w;
	}

	public Map<String, byte[]> encode(byte[] srcData) {

		/* Create Coding directory */
		File dir = new File("Coding");
		if(!dir.exists()){
			dir.mkdir();
		}

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
		int[] matrix = ReedSolomon.vandermondeCodingMatrix(k, m, w);

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

		/* Create metadata file */
		String s = size+"\n";
		s = s.concat(k + " " + m +" "+w+" "+"0 " + size +"\n");
		s = s.concat("reed_sol_van\n");
		s = s.concat("0\n");
		s = s.concat("1\n");

		res.put("metadata", s.getBytes());
		return res;

	}

}

package jec;
import java.util.Map;


public class ReedSolDecoder {

	private int w;
	private int k;
	private int m;

	
	/**
	 * @param k
	 * @param m
	 * @param w
	 * @requires k>0 && m>=0 && w>0 && packetsize>=0 && buffersize>=0;
	 * @requires w == 8 || w == 16 || w == 32;
	 */
	public ReedSolDecoder(int m, int k, int w) {
		this.m = m;
		this.k = k;
		this.w = w;
	}

	public byte[][] decode(Map<String, byte[]> srcData) throws Exception{

		int[] erased = new int[k+m];
		int[] erasures = new int[k+m];
		byte[][] data = new byte[k][];
		byte[][] coding = new byte[m][];

		/* Create coding matrix or bitmatrix */
		int[] matrix = ReedSolomon.vandermondeCodingMatrix(k, m, w);

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
			throw new Exception("Unsuccessful Decode!");
		}
		return data;

	}

	

}


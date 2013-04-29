package jec;
import java.nio.ByteBuffer;

public class Galois {

	private static final int NONE = 10;
	private static final int TABLE = 11;
	private static final int SHIFT = 12;
	private static final int LOGS = 13;
	private static final int SPLITW8 = 14;

	private static int[] multType = { NONE,
	/* 1 */TABLE,
	/* 2 */TABLE,
	/* 3 */TABLE,
	/* 4 */TABLE,
	/* 5 */TABLE,
	/* 6 */TABLE,
	/* 7 */TABLE,
	/* 8 */TABLE,
	/* 9 */TABLE,
	/* 10 */LOGS,
	/* 11 */LOGS,
	/* 12 */LOGS,
	/* 13 */LOGS,
	/* 14 */LOGS,
	/* 15 */LOGS,
	/* 16 */LOGS,
	/* 17 */LOGS,
	/* 18 */LOGS,
	/* 19 */LOGS,
	/* 20 */LOGS,
	/* 21 */LOGS,
	/* 22 */LOGS,
	/* 23 */SHIFT,
	/* 24 */SHIFT,
	/* 25 */SHIFT,
	/* 26 */SHIFT,
	/* 27 */SHIFT,
	/* 28 */SHIFT,
	/* 29 */SHIFT,
	/* 30 */SHIFT,
	/* 31 */SHIFT,
	/* 32 */SPLITW8 };

	private static int[] primPoly = { 0,
	/* 1 */1,
	/* 2 */07,
	/* 3 */013,
	/* 4 */023,
	/* 5 */045,
	/* 6 */0103,
	/* 7 */0211,
	/* 8 */0435,
	/* 9 */01021,
	/* 10 */02011,
	/* 11 */04005,
	/* 12 */010123,
	/* 13 */020033,
	/* 14 */042103,
	/* 15 */0100003,
	/* 16 */0210013,
	/* 17 */0400011,
	/* 18 */01000201,
	/* 19 */02000047,
	/* 20 */04000011,
	/* 21 */010000005,
	/* 22 */020000003,
	/* 23 */040000041,
	/* 24 */0100000207,
	/* 25 */0200000011,
	/* 26 */0400000107,
	/* 27 */01000000047,
	/* 28 */02000000011,
	/* 29 */04000000005,
	/* 30 */010040000007,
	/* 31 */020000000011,
	/* 32 */00020000007 }; 
	/* Really 40020000007, but we're omitting the high
	 * order bit
	 */

	private static int[] nw = { 0, (1 << 1), (1 << 2), (1 << 3), (1 << 4), (1 << 5), (1 << 6), (1 << 7), (1 << 8),
			(1 << 9), (1 << 10), (1 << 11), (1 << 12), (1 << 13), (1 << 14), (1 << 15), (1 << 16), (1 << 17),
			(1 << 18), (1 << 19), (1 << 20), (1 << 21), (1 << 22), (1 << 23), (1 << 24), (1 << 25), (1 << 26),
			(1 << 27), (1 << 28), (1 << 29), (1 << 30), (1 << 31), -1 };

	private static int[] nwm1 = { 0, (1 << 1) - 1, (1 << 2) - 1, (1 << 3) - 1, (1 << 4) - 1, (1 << 5) - 1,
			(1 << 6) - 1, (1 << 7) - 1, (1 << 8) - 1, (1 << 9) - 1, (1 << 10) - 1, (1 << 11) - 1, (1 << 12) - 1,
			(1 << 13) - 1, (1 << 14) - 1, (1 << 15) - 1, (1 << 16) - 1, (1 << 17) - 1, (1 << 18) - 1, (1 << 19) - 1,
			(1 << 20) - 1, (1 << 21) - 1, (1 << 22) - 1, (1 << 23) - 1, (1 << 24) - 1, (1 << 25) - 1, (1 << 26) - 1,
			(1 << 27) - 1, (1 << 28) - 1, (1 << 29) - 1, (1 << 30) - 1, 0x7fffffff, 0xffffffff };

	private static int logTables[][] = new int[33][];
	private static int multTables[][] = new int[33][];
	private static int divTables[][] = new int[33][];
	private static int ilogTables[][] = new int[33][];
	private static int[] iLogTablesIndex = new int[33];

	/* Special case for w = 32 */
	private static int splitW8[][] = new int[7][];

	/**
	 * 
	 * @param r1
	 *            - Region 1
	 * @param r2
	 *            - Region 2
	 * @param r3
	 *            - Sum region (r3 = r1 ^ r2) -- can be r1 or r2
	 * @param nbytes
	 *            - Number of bytes in region
	 */
	public static void regionXor(byte[] r1, byte[] r2, byte[] r3, int nbytes) {
		for (int i = 0; i < nbytes; i++) {
			r3[i] = (byte) (r1[i] ^ r2[i]);
		}
	}

	/**
	 * @param region - Region to multiply
	 * @param multby - Number to multiply by
	 * @param nbytes - Number of bytes in region
	 * @param r2 - If r2 != null, products go here
	 * @param add
	 * @throws Exception
	 */
	public static void regionMultiplyW08(byte[] region, int multby, int nbytes, byte[] r2, boolean add)	throws Exception {
		byte[] ur1, ur2;
		int prod;
		int srow;

		ur1 = region;
		ur2 = (r2 == null) ? ur1 : r2;

		if (multTables[8] == null) {
			if (createMultTables(8) < 0) {
				throw new Exception("galois_08_region_multiply -- couldn't make multiplication tables\n");
			}
		}
		srow = multby * nw[8];

		if (r2 == null || !add) {
			for (int i = 0; i < nbytes; i++) {
				prod = multTables[8][srow + (ur1[i] & 0xFF)];
				ur2[i] = (byte) prod;
			}
		} else {
			for (int i = 0; i < nbytes; i++) {
				prod = multTables[8][srow + (ur1[i] & 0xFF)];
				ur2[i] = (byte) (((int) ur2[i]) ^ prod);
			}
		}

		return;
	}

	/**
	 * @param region - Region to multiply
	 * @param multby - Number to multiply by
	 * @param nbytes - Number of bytes in region
	 * @param r2 - If r2 != null, products go here
	 * @param add
	 * @throws Exception
	 */
	public static void regionMultiplyW16(byte[] region, int multby, int nbytes, byte[] r2, boolean add) throws Exception {
		byte[] ur1, ur2;
		int prod;
		int log1;

		ur1 = region;
		ur2 = (r2 == null) ? ur1 : r2;
		nbytes /= 2;

		if (multby == 0) {
			if (!add) {
				for (int i = 0; i < nbytes; i++)
					ur2[i] = 0;
			}
			return;
		}

		if (logTables[16] == null) {
			try {
				createLogTables(16);
			} catch (Exception e) {
				throw new Exception("galois_16_region_multiply -- couldn't make log tables\n");
			}
		}
		log1 = logTables[16][multby];

		for (int i = 0; i < nbytes; i++) {
			if (ur1[i] == 0) {
				ur2[i] = 0;
			} else {
				prod = logTables[16][ur1[i]] + log1;
				ur2[i] = (byte) ilogTables[16][iLogTablesIndex[16] + prod];
			}
		}
	}

	/**
	 * @param region - Region to multiply
	 * @param multby - Number to multiply by
	 * @param nbytes - Number of bytes in region
	 * @param r2 - If r2 != null, products go here
	 * @param add
	 * @throws Exception
	 */
	public static void regionMultiplyW32(byte[] region, int multby, int nbytes, byte[] r2, boolean add) throws Exception {
		int[] ur1, ur2;
		int a, b, accumulator, i8, j8;
		int[] acache = new int[4];

		ur1 = ByteBuffer.wrap(region).asIntBuffer().array();
		ur2 = (r2 == null) ? ur1 : ByteBuffer.wrap(r2).asIntBuffer().array();
		nbytes /= 4;

		if (splitW8[0] == null) {
			if (createSplitTablesW8() < 0) {
				throw new Exception("Galois.regionMultiplyW32 -- couldn't make split multiplication tables\n");
			}
		}

		i8 = 0;
		for (int i = 0; i < 4; i++) {
			acache[i] = (((multby >> i8) & 255) << 8);
			i8 += 8;
		}
		if (!add) {
			for (int k = 0; k < nbytes; k++) {
				accumulator = 0;
				for (int i = 0; i < 4; i++) {
					a = acache[i];
					j8 = 0;
					for (int j = 0; j < 4; j++) {
						b = ((ur1[k] >> j8) & 255);
						accumulator ^= splitW8[i + j][a | b];
						j8 += 8;
					}
				}
				ur2[k] = accumulator;
			}
		} else {
			for (int k = 0; k < nbytes; k++) {
				accumulator = 0;
				for (int i = 0; i < 4; i++) {
					a = acache[i];
					j8 = 0;
					for (int j = 0; j < 4; j++) {
						b = ((ur1[k] >> j8) & 255);
						accumulator ^= splitW8[i + j][a | b];
						j8 += 8;
					}
				}
				ur2[k] = (ur2[k] ^ accumulator);
			}
		}

		ByteBuffer byteBuffer = ByteBuffer.allocate(ur2.length * 4);
		byteBuffer.asIntBuffer().put(ur2);
		r2 = byteBuffer.array();

	}

	public static int singleDivide(int a, int b, int w) throws Exception {

		if (multType[w] == TABLE) {
			if (divTables[w] == null) {
				try {
					createMultTables(w);
				} catch (Exception e) {
					throw new Exception("ERROR -- cannot make multiplication tables for w=" + w + "\n");
				}
			}
			return divTables[w][(a << w) | b];
		} else if (multType[w] == LOGS) {
			if (b == 0)
				return -1;
			if (a == 0)
				return 0;
			if (logTables[w] == null) {
				if (createLogTables(w) < 0) {
					throw new Exception("ERROR -- cannot make log tables for w=" + w);
				}
			}
			int sum_j = logTables[w][a] - logTables[w][b];
			return ilogTables[w][iLogTablesIndex[w] + sum_j];
		} else {
			if (b == 0)
				return -1;
			if (a == 0)
				return 0;
			int sum_j = inverse(b, w);
			return singleMultiply(a, sum_j, w);
		}
	}

	public static int inverse(int y, int w) throws Exception {

		if (y == 0)
			return -1;
		if (multType[w] == SHIFT || multType[w] == SPLITW8)
			return shiftInverse(y, w);
		return singleDivide(1, y, w);
	}

	public static int shiftInverse(int y, int w) throws Exception {
		int[] mat2 = new int[32];
		int[] inv2 = new int[32];

		for (int i = 0; i < w; i++) {
			mat2[i] = y;

			if ((y & nw[w - 1]) != 0) {
				y = y << 1;
				y = (y ^ primPoly[w]) & nwm1[w];
			} else {
				y = y << 1;
			}
		}

		invertBinaryMatrix(mat2, inv2, w);

		return inv2[0];
	}

	public static void invertBinaryMatrix(int[] mat, int[] inv, int rows) throws Exception {
		int cols, i, j;
		int tmp;

		cols = rows;

		for (i = 0; i < rows; i++)
			inv[i] = (1 << i);

		/* First -- convert into upper triangular */
		for (i = 0; i < cols; i++) {

			/*
			 * Swap rows if we have a zero [i][i] element. If we can't swap, then
			 * the matrix was not invertible
			 */

			if ((mat[i] & (1 << i)) == 0) {
				for (j = i + 1; j < rows && (mat[j] & (1 << i)) == 0; j++)
					;
				if (j == rows) {
					throw new Exception("galois.invertBinaryMatrix: Matrix not invertible!!\n");
				}
				tmp = mat[i];
				mat[i] = mat[j];
				mat[j] = tmp;
				tmp = inv[i];
				inv[i] = inv[j];
				inv[j] = tmp;
			}

			/* Now for each j>i, add A_ji*Ai to Aj */
			for (j = i + 1; j != rows; j++) {
				if ((mat[j] & (1 << i)) != 0) {
					mat[j] ^= mat[i];
					inv[j] ^= inv[i];
				}
			}
		}

		/*
		 * Now the matrix is upper triangular. Start at the top and multiply
		 * down
		 */
		for (i = rows - 1; i >= 0; i--) {
			for (j = 0; j < i; j++) {
				if ((mat[j] & (1 << i)) != 0) {
					inv[j] ^= inv[i];
				}
			}
		}
	}

	public static int singleMultiply(int x, int y, int w) throws Exception {

		if (x == 0 || y == 0)
			return 0;

		if (multType[w] == TABLE) {
			if (multTables[w] == null) {
				if (createMultTables(w) < 0) {
					throw new Exception("ERROR -- cannot make multiplication tables for w=" + w);
				}
			}
			return multTables[w][(x << w) | y];
		} else if (multType[w] == LOGS) {
			
			if (logTables[w] == null) {
				if (createLogTables(w) < 0) {
					throw new Exception("ERROR -- cannot make log tables for w=" + w);
				}
			}
			int sum_j = logTables[w][x] + logTables[w][y];
			return ilogTables[w][iLogTablesIndex[w] + sum_j];
		} else if (multType[w] == SPLITW8) {
			if (splitW8[0] == null) {
				if (createSplitTablesW8() < 0) {
					throw new Exception("ERROR -- cannot make log split_w8_tables for w=" + w);
				}
			}
			return splitMultiplyW08(x, y);
		} else if (multType[w] == SHIFT) {
			return shiftMultiply(x, y, w);
		}
		throw new Exception("Galois_single_multiply - no implementation for w" + w);
	}

	public static int createLogTables(int w) throws Exception {

		if (w > 30)
			return -1;
		if (logTables[w] != null)
			return 0;

		logTables[w] = new int[nw[w]];

		ilogTables[w] = new int[nw[w] * 3];

		for (int i = 0; i < nw[w]; i++) {
			logTables[w][i] = nwm1[w];
			ilogTables[w][i] = 0;
		}

		int b = 1;
		for (int i = 0; i < nwm1[w]; i++) {
			if (logTables[w][b] != nwm1[w]) {
				throw new Exception("Galois.createLogTables Error: i=" + i + ", b=" + b + ", B->J[b]="
						+ logTables[w][b] + ", J->B[i]=" + ilogTables[w][i] + " (0" + ((b << 1) ^ primPoly[w]) + ")\n");

			}
			logTables[w][b] = i;
			ilogTables[w][i] = b;
			b = b << 1;
			if ((b & nw[w]) != 0)
				b = (b ^ primPoly[w]) & nwm1[w];
		}
		for (int i = 0; i < nwm1[w]; i++) {
			ilogTables[w][i + nwm1[w]] = ilogTables[w][i];
			ilogTables[w][i + nwm1[w] * 2] = ilogTables[w][i];
		}

		iLogTablesIndex[w] = nwm1[w];

		return 0;
	}

	public static int shiftMultiply(int x, int y, int w) {
		int j, ind;
		int scratch[] = new int[33];

		int prod = 0;
		for (int i = 0; i < w; i++) {
			scratch[i] = y;
			if ((y & (1 << (w - 1))) != 0) {
				y = y << 1;
				y = (y ^ primPoly[w]) & nwm1[w];
			} else {
				y = y << 1;
			}
		}
		for (int i = 0; i < w; i++) {
			ind = (1 << i);
			if ((ind & x) != 0) {
				j = 1;
				for (int k = 0; k < w; k++) {
					prod = prod ^ (j & scratch[i]);
					j = (j << 1);
				}
			}
		}
		return prod;
	}

	public static int splitMultiplyW08(int x, int y) {
		int a, b, accumulator, i8, j8;

		accumulator = 0;

		i8 = 0;
		for (int i = 0; i < 4; i++) {
			a = (((x >> i8) & 255) << 8);
			j8 = 0;
			for (int j = 0; j < 4; j++) {
				b = ((y >> j8) & 255);
				accumulator ^= splitW8[i + j][a | b];
				j8 += 8;
			}
			i8 += 8;
		}
		return accumulator;
	}

	public static int createMultTables(int w) throws Exception {
		
		if (w >= 14)
			return -1;

		if (multTables[w] != null)
			return 0;
		multTables[w] = new int[nw[w] * nw[w]];

		divTables[w] = new int[nw[w] * nw[w]];

		if (logTables[w] == null) {
			try {
				createLogTables(w);
			} catch (Exception e) {
				multTables[w] = null;
				divTables[w] = null;
				throw e;
			}
		}

		/* Set mult/div tables for x = 0 */
		int j = 0;
		multTables[w][j] = 0; /* y = 0 */
		divTables[w][j] = -1;
		j++;
		for (int y = 1; y < nw[w]; y++) { /* y > 0 */
			multTables[w][j] = 0;
			divTables[w][j] = 0;
			j++;
		}
		
		for (int x = 1; x < nw[w]; x++) { /* x > 0 */
			multTables[w][j] = 0; /* y = 0 */
			divTables[w][j] = -1;
			j++;
			for (int y = 1; y < nw[w]; y++) { /* y > 0 */
				int index1 = logTables[w][x] + logTables[w][y];
				multTables[w][j] = ilogTables[w][iLogTablesIndex[w] + index1];

				int index2 = logTables[w][x] - logTables[w][y];
				divTables[w][j] = ilogTables[w][(iLogTablesIndex[w]) + index2];
				j++;
			}
		}
		return 0;
	}

	public static int createSplitTablesW8() {

		if (splitW8[0] != null)
			return 0;

		try {
			createMultTables(8);
		} catch (Exception e) {
			return -1;
		}

		for (int i = 0; i < 7; i++) {
			splitW8[i] = new int[(1 << 16)];
		}

		int  zElt, yElt, index, ishift, jshift;
		for (int i = 0; i < 4; i += 3) {
			ishift = i * 8;
			for (int j = ((i == 0) ? 0 : 1); j < 4; j++) {
				jshift = j * 8;
				index = 0;
				for (int z = 0; z < 256; z++) {
					zElt = (z << ishift);
					for (int y = 0; y < 256; y++) {
						yElt = (y << jshift);
						splitW8[i + j][index] = shiftMultiply(zElt, yElt, 32);
						index++;
					}
				}
			}
		}
		return 0;
	}
}

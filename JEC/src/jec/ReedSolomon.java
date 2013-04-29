package jec;


public class ReedSolomon {

	public static int [] vandermondeCodingMatrix(int k, int m, int w) {

		int[] vdm = bigVandermondeDistributionMatrix(k+m, k, w);
		if (vdm == null) return null;
		int [] dist = new int[m*k];

		int i = k*k;
		for (int j = 0; j < m*k; j++) {
			dist[j] = vdm[i];
			i++;
		}
		return dist;
	}

	public static int [] bigVandermondeDistributionMatrix(int rows, int cols, int w) {
		int i, j, k;

		if (cols >= rows) 
			return null;

		int [] dist = extendedVandermondeMatrix(rows, cols, w);
		if (dist == null) 
			return null;

		int sindex = 0, srindex;
		for (i = 1; i < cols; i++) {
			sindex += cols;

			/* Find an appropriate row -- where i,i != 0 */
			srindex = sindex+i;
			for (j = i; j < rows && dist[srindex] == 0; j++) srindex += cols;
			if (j >= rows) {   /* This should never happen if rows/w are correct */
				System.err.println("reed_sol_big_vandermonde_distribution_matrix(" + rows +", " + cols +"," + w + ") - couldn't make matrix\n");
				System.exit(1);
			}

			/* If necessary, swap rows */
			int tmp;
			if (j != i) {
				srindex -= i;
				for (k = 0; k < cols; k++) {
					tmp = dist[srindex+k];
					dist[srindex+k] = dist[sindex+k];
					dist[sindex+k] = tmp;
				}
			}

			/* If Element i,i is not equal to 1, multiply the column by 1/i */

			try {
				if (dist[sindex+i] != 1) {
					tmp = Galois.singleDivide(1, dist[sindex+i], w);
					srindex = i;
					for (j = 0; j < rows; j++) {
						dist[srindex] = Galois.singleMultiply(tmp, dist[srindex], w);
						srindex += cols;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}

			/* Now, for each element in row i that is not in column 1, you need
	       to make it zero.  Suppose that this is column j, and the element
	       at i,j = e.  Then you want to replace all of column j with 
	       (col-j + col-i*e).   Note, that in row i, col-i = 1 and col-j = e.
	       So (e + 1e) = 0, which is indeed what we want. */
			int siindex;
			for (j = 0; j < cols; j++) {
				tmp = dist[sindex+j];
				if (j != i && tmp != 0) {
					srindex = j;
					siindex = i;
					try {
						for (k = 0; k < rows; k++) {
							dist[srindex] = dist[srindex] ^ Galois.singleMultiply(tmp, dist[siindex], w);
							srindex += cols;
							siindex += cols;
						}
					} catch (Exception e) {
						e.printStackTrace();
						System.exit(1);
					}
				}
			}
		}
		/* We desire to have row k be all ones.  To do that, multiply
	     the entire column j by 1/dist[k,j].  Then row j by 1/dist[j,j]. */
		sindex = cols*cols;
		int tmp;
		for (j = 0; j < cols; j++) {
			tmp = dist[sindex];
			try {
				if (tmp != 1) { 
					tmp = Galois.singleDivide(1, tmp, w);
					srindex = sindex;
					for (i = cols; i < rows; i++) {
						dist[srindex] = Galois.singleMultiply(tmp, dist[srindex], w);
						srindex += cols;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			sindex++;
		}

		/* Finally, we'd like the first column of each row to be all ones.  To
	     do that, we multiply the row by the inverse of the first element. */

		sindex = cols*(cols+1);
		for (i = cols+1; i < rows; i++) {
			tmp = dist[sindex];
			try {
				if (tmp != 1) { 
					tmp = Galois.singleDivide(1, tmp, w);
					for (j = 0; j < cols; j++) dist[sindex+j] = Galois.singleMultiply(dist[sindex+j], tmp, w);
				}
				sindex += cols;
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		return dist;
	}
	
	public static int [] extendedVandermondeMatrix(int rows, int cols, int w)	{
		
		if (w < 30 && (1 << w) < rows) return null;
		if (w < 30 && (1 << w) < cols) return null;

		int[] vdm = new int[rows*cols];

		vdm[0] = 1;
		int j;
		for (j = 1; j < cols; j++) 
			vdm[j] = 0;
		if (rows == 1) 
			return vdm;

		
		int i=(rows-1)*cols, k;
		for (j = 0; j < cols-1; j++) 
			vdm[i+j] = 0;
		vdm[i+j] = 1;
		if (rows == 2) 
			return vdm;

		for (i = 1; i < rows-1; i++) {
			k = 1;
			for (j = 0; j < cols; j++) {
				vdm[i*cols+j] = k;
				try {
					k = Galois.singleMultiply(k, i, w);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(0);
				}
			}
		}
		return vdm;
	}

}

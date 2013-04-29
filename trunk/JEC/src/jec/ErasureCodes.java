package jec;
import java.util.Arrays;

public class ErasureCodes {

	static double total_xor_bytes = 0;
	static double total_gf_bytes = 0;
	static double total_memcpy_bytes = 0;

	public static int matrixDecode(int k, int m, int w, int[] matrix, int row_k_ones, int[] erasures, byte[][] data_ptrs,
			byte[][] coding_ptrs, int size) {
		
		if (w != 8 && w != 16 && w != 32)
			return -1;

		int [] erased = erasuresToErased(k, m, erasures);

		/* Find the number of data drives failed */

		int lastdrive = k;

		int edd = 0;
		for (int i = 0; i < k; i++) {
			if (erased[i] != 0) {
				edd++;
				lastdrive = i;
			}
		}

		/*
		 * You only need to create the decoding matrix in the following cases:
		 * 
		 * 1. edd > 0 and row_k_ones is false. 2. edd > 0 and row_k_ones is true
		 * and coding device 0 has been erased. 3. edd > 1
		 * 
		 * We're going to use lastdrive to denote when to stop decoding data. At
		 * this point in the code, it is equal to the last erased data device.
		 * However, if we can't use the parity row to decode it (i.e.
		 * row_k_ones=0 or erased[k] = 1, we're going to set it to k so that the
		 * decoding pass will decode all data.
		 */

		if (row_k_ones != 0 || erased[k] == 0)
			lastdrive = k;

		int [] dm_ids = null;
		int[] decoding_matrix = null;

		if (edd > 1 || (edd > 0 && (row_k_ones != 0 || erased[k] == 0))) {
			dm_ids = new int[k];

			decoding_matrix = new int[k * k];

			if(makeDecodingMatrix(k, m, w, matrix, erased, decoding_matrix, dm_ids)<0){
				return -1;
			}
		}

		/*
		 * Decode the data drives. If row_k_ones is true and coding device 0 is
		 * intact, then only decode edd-1 drives. This is done by stopping at
		 * lastdrive. We test whether edd > 0 so that we can exit the loop early
		 * if we're done.
		 */

		for (int i = 0; edd > 0 && i < lastdrive; i++) {
			if (erased[i] != 0) {
				matrixDotprod(k, w, Arrays.copyOfRange(decoding_matrix, i * k, decoding_matrix.length), dm_ids, i,
						data_ptrs, coding_ptrs, size);
				edd--;
			}
		}

		/* Then if necessary, decode drive lastdrive */

		if (edd > 0) {
			int[] tmpids = new int[k];
			for (int i = 0; i < k; i++) {
				tmpids[i] = (i < lastdrive) ? i : i + 1;
			}
			matrixDotprod(k, w, matrix, tmpids, lastdrive, data_ptrs, coding_ptrs, size);
		}

		/* Finally, re-encode any erased coding devices */

		for (int i = 0; i < m; i++) {
			if (erased[k + i] == 0) {
				matrixDotprod(k, w, Arrays.copyOfRange(matrix, i * k, matrix.length), null, i + k, data_ptrs,
						coding_ptrs, size);
			}
		}

		return 0;
	}
	
	public static void matrixDotprod(int k, int w, int[] matrix_row, int[] src_ids, int dest_id, byte[][] data_ptrs,
			byte[][] coding_ptrs, int size) {
		
		if (w != 1 && w != 8 && w != 16 && w != 32) {
			System.err.println("ERROR: matrix_dotprod() called and w is not 1, 8, 16 or 32\n");
			System.exit(1);
		}

		boolean init = false;

		byte[] dest = (dest_id < k) ? data_ptrs[dest_id] : coding_ptrs[dest_id - k];

		/*
		 * First copy or xor any data that does not need to be multiplied by a
		 * factor
		 */
		byte[] src;
		for (int i = 0; i < k; i++) {
			if (matrix_row[i] == 1) {
				if (src_ids == null) {
					src = data_ptrs[i];
				} else if (src_ids[i] < k) {
					src = data_ptrs[src_ids[i]];
				} else {
					src = coding_ptrs[src_ids[i] - k];
				}
				if (!init) {
					memcpy(dest, src, size);
					total_memcpy_bytes += size;
					init = true;
				} else {
					Galois.regionXor(src, dest, dest, size);
					total_xor_bytes += size;
				}
			}
		}

		/* Now do the data that needs to be multiplied by a factor */
		for (int i = 0; i < k; i++) {
			if (matrix_row[i] != 0 && matrix_row[i] != 1) {
				if (src_ids == null) {
					src = data_ptrs[i];
				} else if (src_ids[i] < k) {
					src = data_ptrs[src_ids[i]];
				} else {
					src = coding_ptrs[src_ids[i] - k];
				}
				try {
					switch (w) {
					case 8:
						Galois.regionMultiplyW08(src, matrix_row[i], size, dest, init);
						break;
					case 16:
						Galois.regionMultiplyW16(src, matrix_row[i], size, dest, init);
						break;
					case 32:
						Galois.regionMultiplyW32(src, matrix_row[i], size, dest, init);
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
				total_gf_bytes += size;
				init = true;
			}
		}
	}
	
	public static int[] erasuresToErased(int k, int m, int[] erasures) {

		int td = k + m;
		int[] erased = new int[td];
		int t_non_erased = td;

		for (int i = 0; i < td; i++)
			erased[i] = 0;

		for (int i = 0; erasures[i] != -1; i++) {
			if (erased[erasures[i]] == 0) {
				erased[erasures[i]] = 1;
				t_non_erased--;
				if (t_non_erased < k) {
					return null;
				}
			}
		}
		return erased;
	}
	
	public static int makeDecodingMatrix(int k, int m, int w, int[] matrix, int[] erased, int[] decoding_matrix, int[] dm_ids) {
		int[] tmpmat;
		int j = 0;

		for (int i = 0; j < k; i++) {
			if (erased[i] == 0) {
				dm_ids[j] = i;
				j++;
			}
		}

		tmpmat = new int[k * k];

		for (int i = 0; i < k; i++) {
			if (dm_ids[i] < k) {
				for (j = 0; j < k; j++)
					tmpmat[i * k + j] = 0;
				tmpmat[i * k + dm_ids[i]] = 1;
			} else {
				for (j = 0; j < k; j++) {
					tmpmat[i * k + j] = matrix[(dm_ids[i] - k) * k + j];
				}
			}
		}

		return invertMatrix(tmpmat, decoding_matrix, k, w);
	}
	
	public static int invertMatrix(int[] mat, int[] inv, int rows, int w) {
		int j, x, rs2;
		int row_start, tmp, inverse;

		int cols = rows;

		int k = 0;
		for (int i = 0; i < rows; i++) {
			for (j = 0; j < cols; j++) {
				inv[k] = (i == j) ? 1 : 0;
				k++;
			}
		}

		/* First -- convert into upper triangular */
		for (int i = 0; i < cols; i++) {
			row_start = cols * i;

			/*
			 * Swap rows if we ave a zero i,i element. If we can't swap, then
			 * the matrix was not invertible
			 */

			if (mat[row_start + i] == 0) {
				for (j = i + 1; j < rows && mat[cols * j + i] == 0; j++)
					;
				if (j == rows)
					return -1;
				rs2 = j * cols;
				for (k = 0; k < cols; k++) {
					tmp = mat[row_start + k];
					mat[row_start + k] = mat[rs2 + k];
					mat[rs2 + k] = tmp;
					tmp = inv[row_start + k];
					inv[row_start + k] = inv[rs2 + k];
					inv[rs2 + k] = tmp;
				}
			}

			/* Multiply the row by 1/element i,i */
			tmp = mat[row_start + i];
			if (tmp != 1) {
				try {
					inverse = Galois.singleDivide(1, tmp, w);
					for (j = 0; j < cols; j++) {
						mat[row_start + j] = Galois.singleMultiply(mat[row_start + j], inverse, w);
						inv[row_start + j] = Galois.singleMultiply(inv[row_start + j], inverse, w);
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}

			/* Now for each j>i, add A_ji*Ai to Aj */
			k = row_start + i;
			for (j = i + 1; j != cols; j++) {
				k += cols;
				if (mat[k] != 0) {
					if (mat[k] == 1) {
						rs2 = cols * j;
						for (x = 0; x < cols; x++) {
							mat[rs2 + x] ^= mat[row_start + x];
							inv[rs2 + x] ^= inv[row_start + x];
						}
					} else {
						tmp = mat[k];
						rs2 = cols * j;
						for (x = 0; x < cols; x++) {
							try {
								mat[rs2 + x] ^= Galois.singleMultiply(tmp, mat[row_start + x], w);
								inv[rs2 + x] ^= Galois.singleMultiply(tmp, inv[row_start + x], w);
							} catch (Exception e) {
								e.printStackTrace();
								System.exit(1);
							}
						}
					}
				}
			}
		}

		/*
		 * Now the matrix is upper triangular. Start at the top and multiply
		 * down
		 */

		for (int i = rows - 1; i >= 0; i--) {
			row_start = i * cols;
			for (j = 0; j < i; j++) {
				rs2 = j * cols;
				if (mat[rs2 + i] != 0) {
					tmp = mat[rs2 + i];
					mat[rs2 + i] = 0;
					for (k = 0; k < cols; k++) {
						try {
							inv[rs2 + k] ^= Galois.singleMultiply(tmp, inv[row_start + k], w);
						} catch (Exception e) {
							e.printStackTrace();
							System.exit(1);
						}
					}
				}
			}
		}
		return 0;
	}
	
	public static void matrixEncode(int k, int m, int w, int[] matrix, byte[][] data_ptrs, byte[][] coding_ptrs, int size) {
		if (w != 8 && w != 16 && w != 32) {
			System.err.println("ERROR: matrix_encode() and w is not 8, 16 or 32\n");
			System.exit(1);
		}

		for (int i = 0; i < m; i++) {
			matrixDotprod(k, w, Arrays.copyOfRange(matrix, i * k, matrix.length), null, k + i, data_ptrs, coding_ptrs,
					size);
		}
	}
	
	private static void memcpy(byte[] dest, byte[] src, int size) {
		System.arraycopy(src, 0, dest, 0, size);
	}
}

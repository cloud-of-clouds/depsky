package jec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Map;


public class TimeComparator {

	/**
	 * @param args
	 * @throws Exception 
	 * @throws  
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		int w = 8, k = 2, m=2;


		Map<String,byte[]> res = null;
		ReedSolEncoder encoder = new ReedSolEncoder(k, m, w);
		ReedSolDecoder decoder = new ReedSolDecoder(m, k, w);
		FileInputStream fis = null;

		File filesDir = new File("files");

		Runtime.getRuntime().exec("chmod 777 encoder decoder");

		for(File file : filesDir.listFiles()){

			System.out.println("-- Measuring " + file.getName() + ":");


			fis = new FileInputStream(file);
			byte[] b = new byte[(int)file.length()];
			fis.read(b);
			fis.close();

			long startJ = System.currentTimeMillis();
			int n = 0;
			while(n<4){
				try{
					res = encoder.encode(b);
				}catch(	OutOfMemoryError e){
					n++;
					if(n==4)
						System.err.println("OutOfMemoryError Encoder");
				}
				n=4;
			}
			long endJ = System.currentTimeMillis();

			long startC = System.currentTimeMillis();
			File f = new File((file.getName().endsWith(".val") ? file.getName() : file.getName()+".val"));
			if(!f.exists())
				f.createNewFile();
			FileOutputStream fout = new FileOutputStream(f);
			fout.write(b);
			fout.close();

			Runtime.getRuntime().exec(new String[] {"./encoder" , file.getName()+".val", "2", "2", "reed_sol_van", "8", "0", "0"}).waitFor();			

			fis = new FileInputStream(new File("Coding/"+file.getName()+"_k1.val"));
			fis.read(b);
			fis.close();
			fis = new FileInputStream(new File("Coding/"+file.getName()+"_k2.val"));
			fis.read(b);
			fis.close();
			fis = new FileInputStream(new File("Coding/"+file.getName()+"_m1.val"));
			fis.read(b);
			fis.close();
			fis = new FileInputStream(new File("Coding/"+file.getName()+"_m2.val"));
			fis.read(b);
			fis.close();
			fis = new FileInputStream(new File("Coding/"+file.getName()+"_meta.txt"));
			fis.read(b);
			fis.close();
			long endC = System.currentTimeMillis();

			System.out.println("\tEncode:  Java: " + (int) (endJ-startJ) + " ms\tC: " + (int) (endC-startC)  + " ms.");

			while(n<4){
				try{
					startJ = System.currentTimeMillis();
					decoder.decode(res);
					endJ = System.currentTimeMillis();
					n=4;
				}catch(	OutOfMemoryError e){
					n++;
					if(n==4)
						System.err.println("OutOfMemoryError Decoder");
				}
			}

			f = new File("Coding/"+file.getName()+"_k1.val");
			f.deleteOnExit();
			f = new File("Coding/"+file.getName()+"_k2.val");
			f.deleteOnExit();
			f = new File("Coding/"+file.getName()+"_m1.val");
			f.deleteOnExit();
			f = new File("Coding/"+file.getName()+"_m2.val");
			f.deleteOnExit();
			f = new File("Coding/"+file.getName()+"_meta.txt");
			f.deleteOnExit();


			startC = System.currentTimeMillis();

			f = new File("Coding/"+file.getName()+"_k1.val");
			if(f.exists())f.createNewFile();
			f = new File("Coding/"+file.getName()+"_k2.val");
			if(f.exists())f.createNewFile();
			f = new File("Coding/"+file.getName()+"_m1.val");
			if(f.exists())f.createNewFile();
			f = new File("Coding/"+file.getName()+"_m2.val");
			if(f.exists())f.createNewFile();
			f = new File("Coding/"+file.getName()+"_meta.txt");
			if(f.exists())f.createNewFile();

			fout = new FileOutputStream(new File("Coding/"+file.getName()+"_k1.val"));
			fout.write(res.get("k1"));
			fout.close();
			fout = new FileOutputStream(new File("Coding/"+file.getName()+"_k2.val"));
			fout.write(res.get("k2"));
			fout.close();
			fout = new FileOutputStream(new File("Coding/"+file.getName()+"_m1.val"));
			fout.write(res.get("m1"));
			fout.close();
			fout = new FileOutputStream(new File("Coding/"+file.getName()+"_m2.val"));
			fout.write(res.get("m1"));
			fout.close();
			fout = new FileOutputStream(new File("Coding/"+file.getName()+"_meta.txt"));
			fout.write(res.get("metadata"));
			fout.close();

			Process p = Runtime.getRuntime().exec(new String[] {"./decoder" , file.getName()+".val"});
			p.waitFor();

			try{
				f = new File("Coding/" + file.getName() + "_decoded.val");
				fis = new FileInputStream(f);

				fis.read(b);
				fis.close();
			}catch(FileNotFoundException e){
				int r = p.getInputStream().read();
				String s = new String();
				while(r != -1){
					s = s.concat(""+(char)r);
					r = p.getInputStream().read();
				}
				System.err.println("C out: " + s);
			}
			endC = System.currentTimeMillis();
			f.deleteOnExit();





			System.out.println("\tDecode:  Java: " + (int) (endJ-startJ) + " ms\tC: " + (int) (endC-startC)  + " ms.\n");

		}



	}
}

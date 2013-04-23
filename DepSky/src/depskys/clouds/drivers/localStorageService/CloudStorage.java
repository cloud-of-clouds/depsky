package depskys.clouds.drivers.localStorageService;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;

import depskyDep.IDepSkySDriver;
import depskyDep.StorageCloudException;

/**
 * 
 * @author tiago oliveira
 */
public class CloudStorage implements IDepSkySDriver {

	private String mainPath;
	private String staticpath = "ds-local" + File.separator;

	public CloudStorage() {
	}

	public CloudStorage(String driverpath) {
		mainPath = driverpath;
		init();
	}

	private void init() {
		File f = new File(staticpath + File.separator + mainPath);
		if (!f.exists()) {
			f.mkdirs();
		} else {
			System.out.println("path " + f.getPath() + " already exist... ");
		}
	}

	public String uploadData(String sid, String cid, byte[] data, String id) throws StorageCloudException {
		try {
			String p = staticpath + mainPath + File.separator + id;
			File newf = new File(p);
			FileOutputStream fos = new FileOutputStream(newf);
			fos.write(data);
			fos.close();
			return id;
		} catch (IOException iOException) {
			throw new StorageCloudException(iOException.getMessage());
		}
	}

	public byte[] downloadData(String sid, String cid, String id) throws StorageCloudException {
		try {
			String p = staticpath + mainPath + File.separator + id;
			FileInputStream fis = new FileInputStream(new File(p));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int b = 0;
			while ((b = fis.read()) > -1) {
				baos.write(b);
			}
			fis.close();
			return baos.toByteArray();
		} catch (Exception e) {
			throw new StorageCloudException(e.getMessage());
		}
	}

	public boolean deleteData(String sid, String cid, String id) throws StorageCloudException {
		File f = new File(staticpath + File.separator + mainPath + File.separator + id);
		if(f.exists()){
			f.delete();
		}

		return true;
	}

	public boolean deleteContainer(String sid, String[] allNames ) throws StorageCloudException {

		File file;
		for (int i = 0; i < allNames.length; i++){
			file = new File(staticpath + mainPath + File.separator + allNames[i]);
			if(file.exists())
				file.delete();
		}
		return true;
	}

	public String initSession(Properties sessionProperties) throws StorageCloudException {
		return mainPath;
	}

	public boolean endSession(String sid) throws StorageCloudException {
		return true;
	}

	public String getDriverId() {
		return mainPath;
	}

	public String getSessionKey() {
		return "sid";
	}

	public LinkedList<String> listNames(String prefix) {

		LinkedList<String> names = new LinkedList<String>();
		String p = staticpath + mainPath + File.separator;
		File f = new File(p);
		File[]list = f.listFiles();
		for(File file : list){
			names.add(file.getName());
		}
		return names;
	}

	public String getDataIdByName(String sid, String data_filename) throws StorageCloudException {
		return null;
	}

	public String[] getContainerAndDataIDsByName(String sid, String containername, String dataname) throws StorageCloudException {
		File f = new File(staticpath + mainPath + File.separator + containername + File.separator + dataname);
		if (f.exists()) {
			return new String[]{containername, dataname};
		} else {
			throw new StorageCloudException(containername + " and/or " + dataname + " may not exist...");
		}
	}

	public String initSession() throws StorageCloudException {
		return null;
	}

	public boolean setAcl(String arg0, String arg1, String arg2, String arg3)
			throws StorageCloudException {
		return false;
	}
}

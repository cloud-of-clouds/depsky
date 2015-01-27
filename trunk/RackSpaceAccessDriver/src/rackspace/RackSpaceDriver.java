package rackspace;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import depskyDep.IDepSkySDriver;
import exceptions.ServiceSiteException;
import exceptions.StorageCloudException;

public class RackSpaceDriver implements IDepSkySDriver{

	private String driverId;
	protected String defaultBucketName = "depskys";
	private String accessKey;
	private String secretKey;
	private final String accessURL = "https://lon.identity.api.rackspacecloud.com/v2.0/";
	private final String getToken = "tokens";
	private final String addUsers = "users";
	protected String tokenId;
	protected String operationURL;
	protected CloseableHttpClient client;
	protected HashMap<String, String[]> tokensToOthersAccounts;

	public RackSpaceDriver(String driverID, String accessKey, String secretKey){
		this.driverId = driverID;
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.tokensToOthersAccounts = new HashMap<String, String[]>();
		try {
			getBucketName();
		} catch (FileNotFoundException e) {
			System.out.println("Problem with bucket_name.properties file!");
			//e.printStackTrace();
		}
	}

	public boolean deleteContainer(String bucketName, String[] namesToDelete, String[] uploadToAnotherAccount) throws StorageCloudException {
		String container = bucketName == null ? defaultBucketName : bucketName;
		HttpDelete delete = null;
		CloseableHttpResponse response;
		try {
			for(String name : namesToDelete){
				if(uploadToAnotherAccount == null){
					delete = new HttpDelete(operationURL+"/"+container+"/"+name);
					delete.addHeader("X-Auth-Token", tokenId);
					delete.addHeader("Accept", "application/json");
				}else{
					authenticateAsSubAccount(uploadToAnotherAccount[0], uploadToAnotherAccount[1]);
					String[] acc = tokensToOthersAccounts.get(uploadToAnotherAccount[0]);
					delete = new HttpDelete(acc[1]+"/"+container+"/"+name);
					delete.addHeader("X-Auth-Token", acc[0]);
					delete.addHeader("Accept", "application/json");
				}
				response = client.execute(delete);
				//				EntityUtils.consume(response.getEntity());
				response.close();
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	public boolean deleteData(String bucketName, String fileId, String[] uploadToAnotherAccount) throws StorageCloudException{
		String container = bucketName == null ? defaultBucketName : bucketName;
		try {
			HttpDelete delete = null;
			if(uploadToAnotherAccount == null){
				delete = new HttpDelete(operationURL+"/"+container+"/"+fileId);
				delete.addHeader("X-Auth-Token", tokenId);
				delete.addHeader("Accept", "application/json");
			}else{
				authenticateAsSubAccount(uploadToAnotherAccount[0], uploadToAnotherAccount[1]);
				String[] acc = tokensToOthersAccounts.get(uploadToAnotherAccount[0]);
				delete = new HttpDelete(acc[1]+"/"+container+"/"+fileId);
				delete.addHeader("X-Auth-Token", acc[0]);
				delete.addHeader("Accept", "application/json");
			}
			CloseableHttpResponse response = client.execute(delete);

			boolean result = response.getStatusLine().getStatusCode() == 204 || response.getStatusLine().getStatusCode() == 404;
			//			EntityUtils.consume(response.getEntity());
			response.close();
			return result;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}
	//uploadToAnotherAccount -> 1' position -> username, 2' position -> password (to access other account)
	public byte[] downloadData(String bucketName, String fileId, String[] uploadToAnotherAccount) throws StorageCloudException{
		String container = bucketName == null ? defaultBucketName : bucketName;
		try {
			HttpGet get = null;
			if(uploadToAnotherAccount == null){
				get = new HttpGet(operationURL+"/"+container+"/"+fileId);
				get.addHeader("X-Auth-Token", tokenId);
				get.addHeader("Accept", "application/json");
			}else{
				authenticateAsSubAccount(uploadToAnotherAccount[0], uploadToAnotherAccount[1]);
				String[] acc = tokensToOthersAccounts.get(uploadToAnotherAccount[0]);
				get = new HttpGet(acc[1]+"/"+container+"/"+fileId);
				get.addHeader("X-Auth-Token", acc[0]);
				get.addHeader("Accept", "application/json");
			}
			CloseableHttpResponse response = client.execute(get);
			if(response.getStatusLine().getStatusCode() == 404){
				response.close();
				throw new ServiceSiteException("RackSpaceException::" + " NoSuchBucketOrBlob");
			}


			byte[] bytes = getBytesFromInputStream(response.getEntity().getContent());
			//			EntityUtils.consume(response.getEntity());
			response.close();
			return bytes;
		} catch (ClientProtocolException e) {
			throw new ServiceSiteException("RackSpaceException::" + e.getMessage());
		} catch (IOException e) {
			throw new StorageCloudException("RackSpaceException::" + e.getMessage());
		} catch (Exception e) {
			throw new StorageCloudException("RackSpaceException::" + e.getMessage());
		}
	}

	public boolean endSession(String arg0) throws StorageCloudException {
		return false;
	}

	public String getDriverId() {
		return driverId;
	}

	public String initSession() throws StorageCloudException {

		try {
			String content = "{"+
					"\"auth\": {"+
					"\"RAX-KSKEY:apiKeyCredentials\": {"+
					"\"username\": \""+accessKey+"\","+
					"\"apiKey\": \""+ secretKey+ "\"" +
					"}}}";
			client = HttpClients.createDefault();
			//authenticate
			HttpPost post = new HttpPost(accessURL+getToken);
			post.addHeader("Content-Type", "application/json");
			post.addHeader("Accept", "application/json");
			HttpEntity entity;
			entity = new StringEntity(content);
			post.setEntity(entity);
			CloseableHttpResponse response = client.execute(post);

			//get token and operationURL using response
			JsonFactory f = new JsonFactory();
			JsonParser jp = f.createJsonParser(response.getEntity().getContent());

			JsonToken token;
			boolean tokenTag = false;
			boolean idTag = false;
			boolean nameTag = false;
			boolean isCloudFiles = false;
			boolean publicUrlTag = false;

			while((token = jp.nextToken()) != null){
				if(token == JsonToken.FIELD_NAME){
					if(jp.getCurrentName().equals("token"))
						tokenTag = true;
					else if(tokenTag && jp.getCurrentName().equals("id"))
						idTag = true;
					else if(jp.getCurrentName().equals("name"))
						nameTag = true;
					else if(isCloudFiles && jp.getCurrentName().equals("publicURL"))
						publicUrlTag = true;
				}
				if(token == JsonToken.VALUE_STRING){
					if(tokenTag && idTag){
						tokenId = jp.getText();
						tokenTag = idTag = false;
					}else if(nameTag && jp.getText().equals("cloudFiles")){
						isCloudFiles = true;
					}else if(publicUrlTag){
						operationURL = jp.getText();
						publicUrlTag = isCloudFiles = nameTag = false;
					}

				}
			}
			//			EntityUtils.consume(response.getEntity());
			response.close();
			//create deafult container
			HttpPut put = new HttpPut(operationURL+"/"+defaultBucketName);
			put.addHeader("X-Auth-Token", tokenId);
			put.addHeader("Accept", "application/json");	
			response = client.execute(put);
			//			if( response.getEntity() != null ) {
			//				response.getEntity().consumeContent();
			//			}
			//			EntityUtils.consume(response.getEntity());

			response.close();

		} catch (UnsupportedEncodingException e) {
			throw new ServiceSiteException("RackSpaceException::" + e.getMessage());
		} catch (ClientProtocolException e) {
			throw new ServiceSiteException("RackSpaceException::" + e.getMessage());
		} catch (IOException e) {
			throw new StorageCloudException("RackSpaceException::" + e.getMessage());
		} catch (Exception e) {
			throw new StorageCloudException("RackSpaceException::" + e.getMessage());
		}

		return "sid";
	}

	public LinkedList<String> listNames(String prefix, String bucketName, String[] uploadToAnotherAccount)
			throws StorageCloudException {

		String container = bucketName == null ? defaultBucketName : bucketName;
		//String url = operationURL+"/"+container;
		if(prefix!=null)
			prefix = "?prefix="+prefix;
		else
			prefix = "";

		HttpGet get = null;
		if(uploadToAnotherAccount == null){
			get = new HttpGet(operationURL+"/"+container+prefix);
			get.addHeader("X-Auth-Token", tokenId);
			get.addHeader("Accept", "application/json");
		}else{
			authenticateAsSubAccount(uploadToAnotherAccount[0], uploadToAnotherAccount[1]);
			String[] acc = tokensToOthersAccounts.get(uploadToAnotherAccount[0]);
			get = new HttpGet(acc[1]+container+prefix);
			get.addHeader("X-Auth-Token", acc[0]);
			get.addHeader("Accept", "application/json");
		}

		LinkedList<String> l = new LinkedList<String>();
		try {
			CloseableHttpResponse response = client.execute(get);
			if(response.getStatusLine().getStatusCode() == 404){
				response.close();
				throw new ServiceSiteException("RackSpaceException::" + "NoSuchBucket");
			}
			JsonFactory f = new JsonFactory();
			HttpEntity entity = response.getEntity();
			if(entity == null || entity.getContent() == null){
				response.close();
				return l;
			}
			JsonParser jp = f.createJsonParser(entity.getContent());

			boolean next=false;
			JsonToken token;
			while((token = jp.nextToken()) != null){
				if(token == JsonToken.FIELD_NAME){
					if(jp.getCurrentName().equals("name"))
						next=true;
				}
				if(token == JsonToken.VALUE_STRING){
					if(next){
						l.add(jp.getText());
						next=false;
					}
				}
			}
			response.close();
		} catch (ClientProtocolException e) {
			throw new ServiceSiteException("RackSpaceException::" + e.getMessage());
		} catch (IOException e) {
			throw new StorageCloudException("RackSpaceException::" + e.getMessage());
		}
		return l;
	}

	public String[] setAcl(String bucketNameToShare, String[] canonicalId, String permission) throws StorageCloudException {
		try {
			HttpPut put = new HttpPut(operationURL+"/"+bucketNameToShare);
			put.addHeader("X-Auth-Token", tokenId);
			put.addHeader("Accept", "application/json");	
			CloseableHttpResponse response = client.execute(put);
			response.close();
			String[] usersToAdd = addUser(canonicalId);
			HttpPost post = null;
			String names = "";
			for(int i = 0; i < usersToAdd.length; i+=2){
				if(i+2 < usersToAdd.length)
					names = names.concat(usersToAdd[i]+",");
				else
					names = names.concat(usersToAdd[i]);
			}
			post = new HttpPost(operationURL+"/"+bucketNameToShare);
			post.addHeader("X-Auth-Token", tokenId);
			post.addHeader("Accept", "application/json");
			if(permission.equals("rw")){
				post.addHeader("x-container-read", names);
				post.addHeader("x-container-write", names);
			}else if(permission.equals("r")){
				post.addHeader("x-container-read", names);
			}else if(permission.equals("w")){
				post.addHeader("x-container-write", names);
			}
			response = client.execute(post);
			response.close();
			if(response.getStatusLine().getStatusCode() == 404){
				response.close();
				throw new ServiceSiteException("RackSpaceException::" + "NoSuchBucket");
			}
			return usersToAdd;

		} catch (ClientProtocolException e) {
			throw new ServiceSiteException("RackSpaceException::" + e.getMessage());
		} catch (IOException e) {
			throw new StorageCloudException("RackSpaceException::" + e.getMessage());
		}

	}

	public String uploadData(String bucketName, byte[] data, String fileId, String[] uploadToAnotherAccount) throws StorageCloudException {
		String container = bucketName == null ? defaultBucketName : bucketName;
		CloseableHttpResponse response;
		try {
			if(uploadToAnotherAccount == null){
				HttpPut put;
				put = new HttpPut(operationURL+"/"+container+"/"+fileId);
				put.addHeader("X-Auth-Token", tokenId);
				put.addHeader("Accept", "application/json");
				HttpEntity entity = new ByteArrayEntity(data);
				put.setEntity(entity);
				response = client.execute(put);
				if(response.getStatusLine().getStatusCode() == 404){
					response.close();
					HttpPut putCont = new HttpPut(operationURL+"/"+container);
					putCont.addHeader("X-Auth-Token", tokenId);
					putCont.addHeader("Accept", "application/json");	
					response = client.execute(putCont);
					response.close();
					response = client.execute(put);
				}
			}else{
				authenticateAsSubAccount(uploadToAnotherAccount[0], uploadToAnotherAccount[1]);
				String[] acc = tokensToOthersAccounts.get(uploadToAnotherAccount[0]);
				HttpPut put = new HttpPut(acc[1]+"/"+container+"/"+fileId);
				put.addHeader("X-Auth-Token", acc[0]);
				put.addHeader("Accept", "application/json");
				HttpEntity entity = new ByteArrayEntity(data);
				put.setEntity(entity);
				response = client.execute(put);
			}

			if(response.getStatusLine().getStatusCode() == 404){
				response.close();
				throw new ServiceSiteException("RackSpaceException::" + "NoSuchBucket");
			}
			response.close();
		} catch (ClientProtocolException e) {
			throw new ServiceSiteException("RackSpaceException::" + e.getMessage());
		} catch (IOException e) {
			throw new StorageCloudException("RackSpaceException::" + e.getMessage());
		}

		return fileId;
	}

	private void getBucketName() throws FileNotFoundException{

		String path = "config" + File.separator + "bucket_name.properties";
		FileInputStream fis;
		try {
			fis = new FileInputStream(path);
			Properties props = new Properties();  
			props.load(fis);  
			fis.close();  
			String name = props.getProperty("bucketname");
			if(name.length() == 0){
				char[] randname = new char[10];
				for(int i = 0; i < 10; i++){
					char rand = (char)(Math.random() * 26 + 'a');
					randname[i] = rand;
				}
				defaultBucketName = defaultBucketName.concat(new String(randname));
				props.setProperty("bucketname", defaultBucketName);
				props.store(new FileOutputStream(path),"change");
			}else{
				defaultBucketName = name;
			}

		}catch(IOException e){  
			e.printStackTrace();  
		} 
	}

	//user and password
	public void authenticateAsSubAccount(String username, String password){

		try {
			if(!tokensToOthersAccounts.containsKey(username)){
				String content = "{"+
						"\"auth\":{"+
						"\"passwordCredentials\": {"+
						"\"username\": \""+username+"\","+
						"\"password\": \""+password+"\""+
						"}}}";

				HttpPost post = new HttpPost(accessURL+getToken);
				post.addHeader("Content-Type", "application/json");
				post.addHeader("Accept", "application/json");
				HttpEntity entity = new StringEntity(content);
				post.setEntity(entity);
				CloseableHttpResponse response = client.execute(post);
				//get token and operationURL using response
				//				System.out.println(response);

				//				BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
				//
				//				String output;
				//				System.out.println("Output from Server .... \n");
				//				while ((output = br.readLine()) != null) {
				//					System.out.println(output);
				//
				//				}

				JsonFactory f = new JsonFactory();
				JsonParser jp = f.createJsonParser(response.getEntity().getContent());

				JsonToken token;
				boolean tokenTag = false;
				boolean idTag = false;
				boolean nameTag = false;
				boolean isCloudFiles = false;
				boolean publicUrlTag = false;

				String tok = "";
				String URL = "";
				while((token = jp.nextToken()) != null){
					if(token == JsonToken.FIELD_NAME){
						if(jp.getCurrentName().equals("token"))
							tokenTag = true;
						else if(tokenTag && jp.getCurrentName().equals("id"))
							idTag = true;
						else if(jp.getCurrentName().equals("name"))
							nameTag = true;
						else if(isCloudFiles && jp.getCurrentName().equals("publicURL"))
							publicUrlTag = true;
					}
					if(token == JsonToken.VALUE_STRING){
						if(tokenTag && idTag){
							tok = jp.getText();
							tokenTag = idTag = false;
						}else if(nameTag && jp.getText().equals("cloudFiles")){
							isCloudFiles = true;
						}else if(publicUrlTag){
							URL = jp.getText();
							publicUrlTag = isCloudFiles = nameTag = false;
						}
					}
				}
				//				System.out.println(URL);
				//				System.out.println(tok);
				//				EntityUtils.consume(response.getEntity());
				String[] acc = {tok,URL};
				tokensToOthersAccounts.put(username, acc);
				response.close();
			}


		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String[] addUser(String[] names){
		String name = "";
		String[] userPass = new String[names.length];
		try {
			HttpGet get = new HttpGet(accessURL+addUsers);
			get.addHeader("X-Auth-Token", tokenId);
			get.addHeader("Accept", "application/json");
			CloseableHttpResponse response = client.execute(get);
			response.close();

			for(int i = 0; i < names.length; i+=2){
				char[] randname = new char[10];
				for(int j = 0; j < 10; j++){
					char rand = (char)(Math.random() * 26 + 'a');
					randname[j] = rand;
				}
				name = names[i].concat("-subuser-".concat(new String(randname)));
				String content = "{"+
						"\"user\": {"+
						"\"username\": \""+name+"\","+
						"\"email\": \""+names[i+1]+"\","+
						"\"enabled\": true"+
						"}"+
						"}";
				HttpPost post = new HttpPost(accessURL+addUsers);
				post.addHeader("X-Auth-Token", tokenId);
				post.addHeader("Content-Type", "application/json");
				post.addHeader("Accept", "application/json");
				HttpEntity entity;
				entity = new StringEntity(content);
				post.setEntity(entity);
				response = client.execute(post);

				//				BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
				//
				//				String output;
				//				System.out.println("Output from Server .... \n");
				//				while ((output = br.readLine()) != null) {
				//					System.out.println(output);
				//
				//				}


				//get token and operationURL using response
				JsonFactory f = new JsonFactory();
				JsonParser jp = f.createJsonParser(response.getEntity().getContent());

				JsonToken token;
				boolean passTag = false;

				while((token = jp.nextToken()) != null){
					if(token == JsonToken.FIELD_NAME){
						if(jp.getCurrentName().equals("OS-KSADM:password"))
							passTag = true;
					}
					if(token == JsonToken.VALUE_STRING){
						if(passTag){
							userPass[i+1] = jp.getText();
							passTag = false;
						}
					}
				}	
				userPass[i] = name;
				//				EntityUtils.consume(response.getEntity());
				//userPass[i+1] = "pass";
				response.close();
			}

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return userPass;
	}

	private static byte[] getBytesFromInputStream(InputStream is) throws IOException {

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];

		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		buffer.flush();

		return buffer.toByteArray();
	}


	public String[] getContainerAndDataIDsByName(String arg0, String arg1,
			String arg2) throws StorageCloudException {
		return null;
	}
}

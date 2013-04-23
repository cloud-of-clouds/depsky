package message;

import java.io.Serializable;
import java.util.LinkedList;


public class Message implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5300414683707823903L;
	private String op;
	private LinkedList<String> args;
	private byte[] arrayToUp;
	public Message(String op, LinkedList<String> args, byte[] arraytoUp){
		this.op = op;
		this.args = args;
		this.arrayToUp = arraytoUp;
	}
	
	public String getOp(){
		return this.op;
	}
	
	public LinkedList<String> getArgs(){
		return this.args;
	}
	
	public byte[] getBytes(){
		return this.arrayToUp;
	}
	
	
	
	

}

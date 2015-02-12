package jec;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ReedSolInfo implements Externalizable {

	private int size;
	private int w;
	private int k;
	private int m;
	
	public ReedSolInfo(int size, int k, int m, int w) {
		this.size = size;
		this.k = k;
		this.m = m;
		this.w = w;
	}

	public int getSize() {
		return size;
	}

	public int getW() {
		return w;
	}

	public int getK() {
		return k;
	}

	public int getM() {
		return m;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		size = in.readInt();
		w = in.readInt();
		k = in.readInt();
		m = in.readInt();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(size);
		out.writeInt(w);
		out.writeInt(k);
		out.writeInt(m);
	}
	
	
}

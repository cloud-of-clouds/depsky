package jec;

public class EncodeResult {

	private ReedSolInfo info;
	private byte[][] blocks;
	
	public EncodeResult(byte[][] blocks, ReedSolInfo info) {
		this.info = info;
		this.blocks = blocks;
	}

	public ReedSolInfo getInfo() {
		return info;
	}

	public byte[][] getBlocks() {
		return blocks;
	}
	
}

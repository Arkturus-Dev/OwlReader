package dev.arkturus.owl.buffer;

public interface Bufferable {

	public void toBuffer(WritableBuffer b);

	public default Buffer toBuffer() {
		Buffer b = new Buffer();
		toBuffer(b);
		return b;
	}

}
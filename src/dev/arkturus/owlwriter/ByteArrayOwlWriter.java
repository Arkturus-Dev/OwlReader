package dev.arkturus.owlwriter;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;

import dev.arkturus.owl.Endian;

public class ByteArrayOwlWriter extends BaseOwlWriter {

	protected final Object writeLock = new Object();
	protected byte[]       out;
	protected int          position  = 0;

	public ByteArrayOwlWriter(byte[] out) {
		this.out = out;
	}

	public ByteArrayOwlWriter(byte[] out, Endian endian) {
		this(out);
		setEndian(endian);
	}

	@Override
	public synchronized void writeByte(byte b) throws IOException {
		try {
			out[position] = b;
			position++;
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EOFException(e.getMessage());
		}
	}

	@Override
	public synchronized void writeShort(short s) throws IOException {
		try {
			getEndian().writeShort(s, out, position);
			position += 2;
		} catch (ArrayIndexOutOfBoundsException e) {
			position = out.length;
			throw new EOFException(e.getMessage());
		}
	}

	@Override
	public synchronized void writeInt(int i) throws IOException {
		try {
			getEndian().writeInt(i, out, position);
			position += 4;
		} catch (ArrayIndexOutOfBoundsException e) {
			position = out.length;
			throw new EOFException(e.getMessage());
		}
	}

	@Override
	public synchronized void writeLong(long l) throws IOException {
		try {
			getEndian().writeLong(l, out, position);
			position += 8;
		} catch (ArrayIndexOutOfBoundsException e) {
			position = out.length;
			throw new EOFException(e.getMessage());
		}
	}

	@Override
	public synchronized void write(byte[] bytes, int off, int len) throws IOException {
		try {
			System.arraycopy(bytes, off, out, position, len);
			position += len;
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EOFException(e.getMessage());
		}
	}

	@Override
	public synchronized void writeNullString(String s, Charset encoding) throws IOException {
		super.writePascalByte(s, encoding);
	}

	@Override
	public synchronized void writePascalByte(String s, Charset encoding) throws IOException {
		super.writePascalByte(s, encoding);
	}

	@Override
	public synchronized void writePascalShort(String s, Charset encoding) throws IOException {
		super.writePascalShort(s, encoding);
	}

	@Override
	public synchronized void writePascalInt(String s, Charset encoding) throws IOException {
		super.writePascalInt(s, encoding);
	}

	@Override
	public synchronized void close() throws IOException {
		position = out.length;
	}

	public String asString(String encoding){
		return asString(Charset.forName(encoding));
	}

	public String asString(Charset encoding){
		return new String(out, 0, position);
	}

	public String asString(){
		return asString(encoding);
	}

}

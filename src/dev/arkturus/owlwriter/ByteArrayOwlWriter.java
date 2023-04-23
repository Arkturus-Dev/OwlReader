package dev.arkturus.owlwriter;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;

import dev.arkturus.owl.Endian;

public class ByteArrayOwlWriter extends BaseOwlWriter {

	protected final Object writeLock = new Object();
	protected byte[]       out;
	protected volatile int position  = 0;

	public ByteArrayOwlWriter(byte[] out) {
		this.out = out;
	}

	public ByteArrayOwlWriter(byte[] out, Endian endian) {
		this(out);
		setEndian(endian);
	}

	@Override
	public void writeByte(byte b) throws IOException {
		synchronized (writeLock) {
			try {
				out[position] = b;
				position++;
			} catch (ArrayIndexOutOfBoundsException e) {
				position = out.length;
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public void writeShort(short s) throws IOException {
		synchronized (writeLock) {
			try {
				getEndian().writeShort(s, out, position);
				position += 2;
			} catch (ArrayIndexOutOfBoundsException e) {
				position = out.length;
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public void writeInt(int i) throws IOException {
		synchronized (writeLock) {
			try {
				getEndian().writeInt(i, out, position);
				position += 4;
			} catch (ArrayIndexOutOfBoundsException e) {
				position = out.length;
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public void writeLong(long l) throws IOException {
		synchronized (writeLock) {
			try {
				getEndian().writeLong(l, out, position);
				position += 8;
			} catch (ArrayIndexOutOfBoundsException e) {
				position = out.length;
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public void write(byte[] bytes, int off, int len) throws IOException {
		Objects.checkFromIndexSize(off, len, bytes.length);
		synchronized (writeLock) {
			try {
				System.arraycopy(bytes, off, out, position, len);
				position += len;
			} catch (ArrayIndexOutOfBoundsException e) {
				position = out.length;
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public void writeNullString(String s, Charset encoding) throws IOException {
		synchronized (writeLock) {
			super.writeNullString(s, encoding);
		}
	}

	@Override
	public void writePascalByte(String s, Charset encoding) throws IOException {
		synchronized (writeLock) {
			super.writePascalByte(s, encoding);
		}
	}

	@Override
	public void writePascalShort(String s, Charset encoding) throws IOException {
		synchronized (writeLock) {
			super.writePascalShort(s, encoding);
		}
	}

	@Override
	public void writePascalInt(String s, Charset encoding) throws IOException {
		synchronized (writeLock) {
			super.writePascalInt(s, encoding);
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (writeLock) {
			position = out.length;
		}
	}

	public int position() {
		synchronized (writeLock) {
			return position;
		}
	}

	public String asString(String encoding){
		return asString(Charset.forName(encoding));
	}

	public String asString(Charset encoding){
		synchronized (writeLock) {
			return new String(out, 0, position, encoding);
		}
	}

	public String asString(){
		return asString(encoding);
	}

}

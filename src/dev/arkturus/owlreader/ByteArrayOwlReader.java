package dev.arkturus.owlreader;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;

public class ByteArrayOwlReader extends BaseOwlReader {

	protected final Object readLock = new Object();
	protected byte[]       in;
	protected int          position = 0;

	/**
	 * Creates an {@link OwlReader} backed by the given byte[].
	 * @param in the backing byte[].
	 */
	public ByteArrayOwlReader(byte[] in) {
		this.in = in;
	}

	@Override
	public byte readByte() throws IOException {
		synchronized (readLock) {
			try {
				return in[position++];
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public short readShort() throws IOException {
		synchronized (readLock) {
			try {
				return (short) (((in[position++] & 0xFF) << 8) + (in[position++] & 0xFF));
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public int readInt() throws IOException {
		synchronized (readLock) {
			try {
				return ((in[position++] & 0xFF) << 24) + ((in[position++] & 0xFF) << 16)
						+ ((in[position++] & 0xFF) << 8) + (in[position++] & 0xFF);
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public long readLong() throws IOException {
		synchronized (readLock) {
			try {
				return (((long) (in[position++] & 0xFF)) << 56) + (((long) (in[position++] & 0xFF)) << 48)
						+ (((long) (in[position++] & 0xFF)) << 40) + (((long) (in[position++] & 0xFF)) << 32)
						+ ((in[position++] & 0xFF) << 24) + ((in[position++] & 0xFF) << 16)
						+ ((in[position++] & 0xFF) << 8) + (in[position++] & 0xFF);
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public int read(byte[] buffer, int off, int len) throws IOException {
		synchronized (readLock) {
			int size = Math.min(len, in.length - position);
			System.arraycopy(in, position, buffer, off, size);
			position += len;
			return size;
		}
	}

	@Override
	public String readString(int bytes, Charset encoding) throws IOException {
		synchronized (readLock) {
			try {
				position += bytes;
				return new String(in, position - bytes, position, encoding);
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public String readNullString(Charset encoding) throws IOException {
		synchronized (readLock) {
			try {
				int size = 0;
				while (in[position + size++] != 0)
					;
				position += size;
				return new String(in, position - size, position - 1, encoding);
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public String readPascalByte(Charset encoding) throws IOException {
		synchronized (readLock) {
			return super.readPascalByte(encoding);
		}
	}

	@Override
	public String readPascalShort(Charset encoding) throws IOException {
		synchronized (readLock) {
			return super.readPascalShort(encoding);
		}
	}

	@Override
	public String readPascalInt(Charset encoding) throws IOException {
		synchronized (readLock) {
			return super.readPascalInt(encoding);
		}
	}

	@Override
	public String readLine(Charset encoding) throws IOException {
		synchronized (readLock) {
			int index;
			for (index = position; index < in.length; index++)
				if (in[index] == '\n')
					break;
			String result = new String(in, position, index - position - 1, encoding);
			position = index;
			return result;
		}
	}

	@Override
	public void skipBytes(long count) throws IOException {
		synchronized (readLock) {
			position += count;
		}
	}

	@Override
	public boolean hasData() {
		return position < in.length;
	}

	@Override
	public int position() {
		return position;
	}

}

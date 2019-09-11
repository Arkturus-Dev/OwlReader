package dev.arkturus.owlreader;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import dev.arkturus.owl.Endian;

public class InputStreamOwlReader extends BaseOwlReader {

	private final Object	readLock	= new Object();
	private InputStream		in;
	private byte[]			buffer		= new byte[1024];
	private int				position;

	/**
	 * Creates an {@link OwlReader} backed by the given {@link InputStream}.
	 * 
	 * @param in the backing InputStream.
	 */
	public InputStreamOwlReader(InputStream in) {
		this.in = in;
	}

	public InputStreamOwlReader(InputStream in, Endian endian) {
		this(in);
		setEndian(endian);
	}

	private void fullRead(byte[] buffer, int count) throws IOException {
		fullRead(buffer, 0, count);
	}

	private void fullRead(byte[] buffer, int offset, int count) throws IOException {
		if (buffer.length < offset + count)
			buffer = Arrays.copyOf(buffer, Math.max(buffer.length * 2, offset + count));
		int read = 0;
		int i = 0;
		while (read < count) {
			i = in.read(buffer, read + offset, count - read);
			if (i == -1)
				throw new EOFException("Unexpected EOF. Expected " + (count - read) + " more bytes.");
			read += i;
		}
		position += read;
	}

	@Override
	public byte readByte() throws IOException {
		synchronized (readLock) {
			fullRead(buffer, 1);
			return buffer[0];
		}
	}

	@Override
	public short readShort() throws IOException {
		synchronized (readLock) {
			fullRead(buffer, 2);
			return getEndian().readShort(buffer, 0);
		}
	}

	@Override
	public int readInt() throws IOException {
		synchronized (readLock) {
			fullRead(buffer, 4);
			return getEndian().readInt(buffer, 0);
		}
	}

	@Override
	public long readLong() throws IOException {
		synchronized (readLock) {
			fullRead(buffer, 8);
			return getEndian().readLong(buffer, 0);
		}
	}

	@Override
	public byte[] read(int length) throws IOException {
		synchronized (readLock) {
			fullRead(buffer, length);
			return Arrays.copyOf(buffer, length);
		}
	}

	@Override
	public byte[] read(byte[] buffer, int off, int len) throws IOException {
		synchronized (readLock) {
			fullRead(this.buffer, len);
			System.arraycopy(this.buffer, 0, buffer, off, len);
			return buffer;
		}
	}

	@Override
	public String readString(int bytes, Charset encoding) throws IOException {
		synchronized (readLock) {
			fullRead(buffer, bytes);
			return new String(buffer, 0, bytes, encoding);
		}
	}

	@Override
	public String readNullString(Charset encoding) throws IOException {
		synchronized (readLock) {
			fullRead(buffer, 1);
			int size = 0;
			while (buffer[size++] != 0)
				fullRead(buffer, size, 1);
			return new String(buffer, 0, size - 1, encoding);
		}
	}

	@Override
	public String readPascalByte(Charset encoding) throws IOException {
		synchronized (readLock) {
			fullRead(buffer, 1);
			byte b = buffer[0];
			fullRead(buffer, b);
			return new String(buffer, 0, b, encoding);
		}
	}

	@Override
	public String readPascalShort(Charset encoding) throws IOException {
		synchronized (readLock) {
			fullRead(buffer, 2);
			short s = (short) (((buffer[0] & 0xFF) << 8) + (buffer[1] & 0xFF));
			fullRead(buffer, s);
			return new String(buffer, 0, s, encoding);
		}
	}

	@Override
	public String readPascalInt(Charset encoding) throws IOException {
		synchronized (readLock) {
			fullRead(buffer, 2);
			int i = ((buffer[0] & 0xFF) << 24) + ((buffer[1] & 0xFF) << 16) + ((buffer[2] & 0xFF) << 8)
					+ (buffer[3] & 0xFF);
			fullRead(buffer, i);
			return new String(buffer, 0, i, encoding);
		}
	}

	@Override
	public String readLine(Charset encoding) throws IOException {
		synchronized (readLock) {
			int read = 0;
			buffer[read] = readByte();
			while (buffer[read++] != '\n') {
				ensureBuffer(read + 1);
				buffer[read] = readByte();
			}
			if (read == 1)
				return "";
			int len = buffer[read - 2] == '\r' ? read - 2 : read - 1;
			return new String(buffer, 0, len, encoding);
		}
	}

	@Override
	public void skipBytes(long count) throws IOException {
		synchronized (readLock) {
			long i = 0;
			while (i != count)
				i += in.skip(count - i);
			position += count;
		}
	}

	@Override
	public boolean hasData() throws IOException {
		return in.available() > 0;
	}

	@Override
	public int position() {
		return position;
	}

}

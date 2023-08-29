package dev.arkturus.owlreader;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import dev.arkturus.owl.Endian;

public class InputStreamOwlReader extends BaseOwlReader {

	private final Object readLock = new Object();
	private InputStream in;
	private byte[] buffer = new byte[1024];
	private int position;

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

	private byte[] fullRead(byte[] buffer, int count) throws IOException {
		return fullRead(buffer, 0, count);
	}

	private byte[] fullRead(byte[] buffer, int offset, int count) throws IOException {
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
		return buffer;
	}

	@Override
	public byte readByte() throws IOException {
		synchronized (readLock) {
			buffer = fullRead(buffer, 1);
			return buffer[0];
		}
	}

	@Override
	public short readShort() throws IOException {
		synchronized (readLock) {
			buffer = fullRead(buffer, 2);
			return getEndian().readShort(buffer, 0);
		}
	}

	@Override
	public int readInt() throws IOException {
		synchronized (readLock) {
			buffer = fullRead(buffer, 4);
			return getEndian().readInt(buffer, 0);
		}
	}

	@Override
	public long readLong() throws IOException {
		synchronized (readLock) {
			buffer = fullRead(buffer, 8);
			return getEndian().readLong(buffer, 0);
		}
	}

	@Override
	public byte[] read(int length) throws IOException {
		synchronized (readLock) {
			buffer = fullRead(buffer, length);
			return Arrays.copyOf(buffer, length);
		}
	}

	@Override
	public int read(byte[] buffer, int off, int len) throws IOException {
		synchronized (readLock) {		//ONO
			ensureBuffer(len);
			int read = 0;
			int i = 0;
			while (read < len) {
				i = in.read(buffer, read, len - read);
				if (i == -1)
					break;
				read += i;
			}
			position += read;
			return read;
		}
	}

	@Override
	public String readString(int bytes, Charset encoding) throws IOException {
		synchronized (readLock) {
			buffer = fullRead(buffer, bytes);
			return new String(buffer, 0, bytes, encoding);
		}
	}

	@Override
	public String readNullString(Charset encoding) throws IOException {
		synchronized (readLock) {
			buffer = fullRead(buffer, 1);
			int size = 0;
			while (buffer[size++] != 0)
				buffer = fullRead(buffer, size, 1);
			return new String(buffer, 0, size - 1, encoding);
		}
	}

	@Override
	public String readPascalByte(Charset encoding) throws IOException {
		synchronized (readLock) {
			short b = readUByte();
			buffer = fullRead(buffer, b);
			return new String(buffer, 0, b, encoding);
		}
	}

	@Override
	public String readPascalShort(Charset encoding) throws IOException {
		synchronized (readLock) {
			int s = readUShort();
			buffer = fullRead(buffer, s);
			return new String(buffer, 0, s, encoding);
		}
	}

	@Override
	public String readPascalInt(Charset encoding) throws IOException {
		synchronized (readLock) {
			int i = readSUInt();
			buffer = fullRead(buffer, i);
			return new String(buffer, 0, i, encoding);
		}
	}

	@Override
	public String readLine(Charset encoding) throws IOException {
		synchronized (readLock) {
			int read = 0;
			buffer[read] = readByte();
			while (buffer[read++] != '\n') {
				if (buffer.length < read + 1)
					buffer = Arrays.copyOf(buffer, Math.max(buffer.length * 2, read + 1));
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

	@Override
	public void close() throws IOException {
		in.close();
	}

	@Override
	public OwlReader frame(int length) {
		return new FramedInputStreamOwlReader(length);
	}

	private class FramedInputStreamOwlReader extends BaseOwlReader {

		private int position;
		private int length;

		public FramedInputStreamOwlReader(int length) {
			this.length = length;
		}

		@Override
		public byte readByte() throws IOException {
			advance(1);
			return InputStreamOwlReader.this.readByte();
		}

		@Override
		public short readShort() throws IOException {
			advance(2);
			return InputStreamOwlReader.this.readShort();
		}

		@Override
		public int readInt() throws IOException {
			advance(4);
			return InputStreamOwlReader.this.readInt();
		}

		@Override
		public long readLong() throws IOException {
			advance(8);
			return InputStreamOwlReader.this.readLong();
		}

		@Override
		public String readString(int bytes, Charset encoding) throws IOException {
			advance(bytes);
			return InputStreamOwlReader.this.readString(bytes, encoding);
		}

		@Override
		public String readNullString(Charset encoding) throws IOException {
			synchronized (readLock) {
				advance(1);
				buffer = fullRead(buffer, 1);
				int size = 0;
				while (buffer[size++] != 0) {
					advance(1);
					buffer = fullRead(buffer, size, 1);
				}
				return new String(buffer, 0, size - 1, encoding);
			}
		}

		@Override
		public String readPascalByte(Charset encoding) throws IOException {
			synchronized (readLock) {
				advance(1);
				buffer = fullRead(buffer, 1);
				byte b = buffer[0];
				advance(b);
				buffer = fullRead(buffer, b);
				return new String(buffer, 0, b, encoding);
			}
		}

		@Override
		public String readPascalShort(Charset encoding) throws IOException {
			synchronized (readLock) {
				advance(2);
				buffer = fullRead(buffer, 2);
				short s = (short) (((buffer[0] & 0xFF) << 8) + (buffer[1] & 0xFF));
				advance(s);
				buffer = fullRead(buffer, s);
				return new String(buffer, 0, s, encoding);
			}
		}

		@Override
		public String readPascalInt(Charset encoding) throws IOException {
			synchronized (readLock) {
				advance(4);
				buffer = fullRead(buffer, 4);
				int i = ((buffer[0] & 0xFF) << 24) + ((buffer[1] & 0xFF) << 16) + ((buffer[2] & 0xFF) << 8)
						+ (buffer[3] & 0xFF);

				advance(i);
				buffer = fullRead(buffer, i);
				return new String(buffer, 0, i, encoding);
			}
		}

		@Override
		public byte[] read(int length) throws IOException {
			advance(length);
			return InputStreamOwlReader.this.read(length);
		}

		@Override
		public int read(byte[] buffer, int off, int len) throws IOException {
			advance(len);
			return InputStreamOwlReader.this.read(buffer, off, len);
		}

		@Override
		public String readLine(Charset encoding) throws IOException {
			// TODO EOF handling.
			return InputStreamOwlReader.this.readLine(encoding);
		}

		@Override
		public void skipBytes(long count) throws IOException {
			advance(count);
			InputStreamOwlReader.this.skipBytes(count);
		}

		@Override
		public boolean hasData() throws IOException {
			return position < length && InputStreamOwlReader.this.hasData();
		}

		@Override
		public int position() {
			return position;
		}

		@Override
		public void close() throws IOException {
			int remaining = length - position;
			advance(remaining);
			InputStreamOwlReader.this.skipBytes(remaining);
		}

		private void advance(long count) throws IOException {
			int remaining = length - position;
			if (count > remaining)
				throw new EOFException("Unexpected EOF.");
			position += count;
		}

	}

}

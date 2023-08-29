package dev.arkturus.owlreader;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

import dev.arkturus.owl.Endian;

public class InputStreamOwlReader extends BaseOwlReader {

	private final Object	readLock	= new Object();
	private InputStream		in;
	private byte[]			buffer		= new byte[1024];
	private volatile int	position;

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

	private void ensureBuffer(int len) {
		if (buffer.length < len)
			buffer = Arrays.copyOf(buffer, Math.max(buffer.length * 2, len));
	}

	private byte[] fullRead(int count) throws IOException {
		return fullRead(0, count);
	}

	private byte[] fullRead(int offset, int count) throws IOException {
		ensureBuffer(offset + count);
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
			int b = in.read();
			if (b == -1)
				throw new EOFException("Unexpected EOF.");
			position++;
			return (byte) b;
		}
	}

	@Override
	public short readShort() throws IOException {
		synchronized (readLock) {
			return getEndian().readShort(fullRead(2), 0);
		}
	}

	@Override
	public int readInt() throws IOException {
		synchronized (readLock) {
			return getEndian().readInt(fullRead(4), 0);
		}
	}

	@Override
	public long readLong() throws IOException {
		synchronized (readLock) {
			return getEndian().readLong(fullRead(8), 0);
		}
	}

	@Override
	public byte[] read(int length) throws IOException {
		synchronized (readLock) {
			return Arrays.copyOf(fullRead(length), length);
		}
	}

	@Override
	public int read(byte[] buffer, int off, int len) throws IOException {
		Objects.checkFromIndexSize(off, len, buffer.length);
		int read = 0;
		int i = 0;
		synchronized (readLock) {
			while (read < len) {
				i = in.read(buffer, off + read, len - read);
				if (i == -1)
					break;
				read += i;
			}
			position += read;
		}
		return read;
	}

	@Override
	public String readString(int bytes, Charset encoding) throws IOException {
		synchronized (readLock) {
			return new String(fullRead(bytes), 0, bytes, encoding);
		}
	}

	@Override
	public String readNullString(Charset encoding) throws IOException {
		synchronized (readLock) {
			fullRead(1);
			int size = 0;
			while (buffer[size++] != 0)
				fullRead(size, 1);
			return new String(buffer, 0, size - 1, encoding);
		}
	}

	@Override
	public String readPascalByte(Charset encoding) throws IOException {
		synchronized (readLock) {
			short b = readUByte();
			return new String(fullRead(b), 0, b, encoding);
		}
	}

	@Override
	public String readPascalShort(Charset encoding) throws IOException {
		synchronized (readLock) {
			int s = readUShort();
			return new String(fullRead(s), 0, s, encoding);
		}
	}

	@Override
	public String readPascalInt(Charset encoding) throws IOException {
		synchronized (readLock) {
			int i = readSUInt();
			return new String(fullRead(i), 0, i, encoding);
		}
	}

	@Override
	public String readLine(Charset encoding) throws IOException {
		synchronized (readLock) {
			int read = 0;
			try {
				byte b = readByte();
				while (b != '\n') {
					ensureBuffer(read + 1);
					buffer[read++] = b;
					b = readByte();
				}
			} catch (EOFException e) {
				// EOF reached before or without trailing newline
			}
			if (read == 0)
				return "";
			int len = (buffer[read - 1] == '\r') ? read - 1 : read;
			return new String(buffer, 0, len, encoding);
		}
	}

	@Override
	public void skipBytes(long count) throws IOException {
		if (count < 0)
			throw new IndexOutOfBoundsException("count cannot be negative: " + count);
		synchronized (readLock) {
			long skipped = 0;
			while (skipped < count) {
				long n = in.skip(count - skipped);
				if (n == 0) {
					int b = in.read();
					if (b == -1)
						throw new EOFException("Unexpected EOF while skipping bytes.");
					skipped++;
				} else {
					skipped += n;
				}
			}
			position += skipped;
		}
	}

	@Override
	public boolean hasData() throws IOException {
		synchronized (readLock) {
			return in.available() > 0;
		}
	}

	@Override
	public int position() {
		synchronized (readLock) {
			return position;
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (readLock) {
			in.close();
		}
	}

	@Override
	public OwlReader frame(int length) {
		return new FramedInputStreamOwlReader(length);
	}

	private class FramedInputStreamOwlReader extends BaseOwlReader {

		private int	position;
		private int	length;

		public FramedInputStreamOwlReader(int length) {
			this.length = length;
		}

		@Override
		public byte readByte() throws IOException {
			synchronized (readLock) {
				advance(1);
				return InputStreamOwlReader.this.readByte();
			}
		}

		@Override
		public short readShort() throws IOException {
			synchronized (readLock) {
				advance(2);
				return InputStreamOwlReader.this.readShort();
			}
		}

		@Override
		public int readInt() throws IOException {
			synchronized (readLock) {
				advance(4);
				return InputStreamOwlReader.this.readInt();
			}
		}

		@Override
		public long readLong() throws IOException {
			synchronized (readLock) {
				advance(8);
				return InputStreamOwlReader.this.readLong();
			}
		}

		@Override
		public String readString(int bytes, Charset encoding) throws IOException {
			synchronized (readLock) {
				advance(bytes);
				return InputStreamOwlReader.this.readString(bytes, encoding);
			}
		}

		@Override
		public String readNullString(Charset encoding) throws IOException {
			synchronized (readLock) {
				advance(1);
				fullRead(1);
				int size = 0;
				while (buffer[size++] != 0) {
					advance(1);
					fullRead(size, 1);
				}
				return new String(buffer, 0, size - 1, encoding);
			}
		}

		@Override
		public String readPascalByte(Charset encoding) throws IOException {
			synchronized (readLock) {
				advance(1);
				fullRead(1);
				int b = buffer[0] & 0xFF;
				advance(b);
				fullRead(b);
				return new String(buffer, 0, b, encoding);
			}
		}

		@Override
		public String readPascalShort(Charset encoding) throws IOException {
			synchronized (readLock) {
				advance(2);
				fullRead(2);
				int s = ((buffer[0] & 0xFF) << 8) | (buffer[1] & 0xFF);
				advance(s);
				fullRead(s);
				return new String(buffer, 0, s, encoding);
			}
		}

		@Override
		public String readPascalInt(Charset encoding) throws IOException {
			synchronized (readLock) {
				advance(4);
				fullRead(4);
				int i = ((buffer[0] & 0xFF) << 24) | ((buffer[1] & 0xFF) << 16) | ((buffer[2] & 0xFF) << 8)
						| (buffer[3] & 0xFF);
				if (i < 0)
					throw new IndexOutOfBoundsException("Negative length for PascalInt: " + i);
				advance(i);
				fullRead(i);
				return new String(buffer, 0, i, encoding);
			}
		}

		@Override
		public byte[] read(int length) throws IOException {
			synchronized (readLock) {
				advance(length);
				return InputStreamOwlReader.this.read(length);
			}
		}

		@Override
		public int read(byte[] buffer, int off, int len) throws IOException {
			Objects.checkFromIndexSize(off, len, buffer.length);
			synchronized (readLock) {
				advance(len);
				return InputStreamOwlReader.this.read(buffer, off, len);
			}
		}

		@Override
		public String readLine(Charset encoding) throws IOException {
			synchronized (readLock) {
				int remaining = length - position;
				if (remaining <= 0)
					return "";
				int read = 0;
				try {
					while (position < length) {
						advance(1);
						byte b = InputStreamOwlReader.this.readByte();
						if (b == '\n')
							break;
						ensureBuffer(read + 1);
						buffer[read++] = b;
					}
				} catch (EOFException e) {
				}
				if (read == 0)
					return "";
				int len = (buffer[read - 1] == '\r') ? read - 1 : read;
				return new String(buffer, 0, len, encoding);
			}
		}

		@Override
		public void skipBytes(long count) throws IOException {
			synchronized (readLock) {
				advance(count);
				InputStreamOwlReader.this.skipBytes(count);
			}
		}

		@Override
		public boolean hasData() throws IOException {
			synchronized (readLock) {
				return position < length && InputStreamOwlReader.this.hasData();
			}
		}

		@Override
		public int position() {
			synchronized (readLock) {
				return position;
			}
		}

		@Override
		public void close() throws IOException {
			synchronized (readLock) {
				int remaining = length - position;
				advance(remaining);
				InputStreamOwlReader.this.skipBytes(remaining);
			}
		}

		private void advance(long count) throws IOException {
			if (count < 0)
				throw new IndexOutOfBoundsException("count cannot be negative: " + count);
			int remaining = length - position;
			if (count > remaining)
				throw new EOFException("Unexpected EOF.");
			position += count;
		}

	}

}

package dev.arkturus.owlreader;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;

import dev.arkturus.owl.Endian;

public class ByteArrayOwlReader extends BaseOwlReader {

	protected final Object readLock = new Object();
	protected final byte[] in;
	protected final int    offset;
	protected final int    length;
	protected volatile int position = 0;

	/**
	 * Creates an {@link OwlReader} backed by the given byte[].
	 * @param in the backing byte[].
	 */
	public ByteArrayOwlReader(byte[] in) {
		this(in, 0, in.length, Endian.BIG);
	}

	public ByteArrayOwlReader(byte[] in, Endian endian) {
		this(in, 0, in.length, endian);
	}

	public ByteArrayOwlReader(byte[] in, int offset, int length) {
		this(in, offset, length, Endian.BIG);
	}

	public ByteArrayOwlReader(byte[] in, int offset, int length, Endian endian) {
		Objects.requireNonNull(in);
		Objects.checkFromIndexSize(offset, length, in.length);
		this.in = in;
		this.offset = offset;
		this.length = length;
		setEndian(endian);
	}

	@Override
	public byte readByte() throws IOException {
		synchronized (readLock) {
			if (position >= length) {
				throw new EOFException("Unexpected EOF.");
			}
			return in[offset + position++];
		}
	}

	@Override
	public short readShort() throws IOException {
		synchronized (readLock) {
			if (position + 2 > length) {
				throw new EOFException("Unexpected EOF.");
			}
			short value = getEndian().readShort(in, offset + position);
			position += 2;
			return value;
		}
	}

	@Override
	public int readInt() throws IOException {
		synchronized (readLock) {
			if (position + 4 > length) {
				throw new EOFException("Unexpected EOF.");
			}
			int value = getEndian().readInt(in, offset + position);
			position += 4;
			return value;
		}
	}

	@Override
	public long readLong() throws IOException {
		synchronized (readLock) {
			if (position + 8 > length) {
				throw new EOFException("Unexpected EOF.");
			}
			long value = getEndian().readLong(in, offset + position);
			position += 8;
			return value;
		}
	}

	@Override
	public int read(byte[] buffer, int off, int len) throws IOException {
		Objects.checkFromIndexSize(off, len, buffer.length);
		synchronized (readLock) {
			int size = Math.max(0, Math.min(len, length - position));
			System.arraycopy(in, offset + position, buffer, off, size);
			position += size;
			return size;
		}
	}

	@Override
	public String readString(int bytes, Charset encoding) throws IOException {
		if (bytes < 0)
			throw new IndexOutOfBoundsException("bytes cannot be negative: " + bytes);
		synchronized (readLock) {
			if (position + bytes > length)
				throw new EOFException("Unexpected EOF.");
			String s = new String(in, offset + position, bytes, encoding);
			position += bytes;
			return s;
		}
	}

	@Override
	public String readNullString(Charset encoding) throws IOException {
		synchronized (readLock) {
			int size = 0;
			while (position + size < length && in[offset + position + size] != 0) {
				size++;
			}
			if (position + size >= length) {
				throw new EOFException("Unexpected EOF searching for null terminator.");
			}
			String s = new String(in, offset + position, size, encoding);
			position += size + 1;
			return s;
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
			for (index = position; index < length; index++) {
				if (in[offset + index] == '\n')
					break;
			}
			int rawLen = index - position;
			int len = (rawLen > 0 && in[offset + position + rawLen - 1] == '\r') ? rawLen - 1 : rawLen;
			String result = new String(in, offset + position, len, encoding);
			position = (index < length) ? index + 1 : length;
			return result;
		}
	}

	@Override
	public void skipBytes(long count) throws IOException {
		if (count < 0)
			throw new IndexOutOfBoundsException("count cannot be negative: " + count);
		synchronized (readLock) {
			position = (int) Math.min((long) length, (long) position + count);
		}
	}

	@Override
	public boolean hasData() {
		synchronized (readLock) {
			return position < length;
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
			position = length;
		}
	}

	@Override
	public OwlReader slice(int sliceLength) throws IOException {
		if (sliceLength < 0)
			throw new IndexOutOfBoundsException("sliceLength cannot be negative: " + sliceLength);
		synchronized (readLock) {
			if (position + sliceLength > length) {
				throw new EOFException("Unexpected EOF: requested " + sliceLength + ", available " + (length - position));
			}
			int sliceOffset = offset + position;
			position += sliceLength;
			return new ByteArrayOwlReader(in, sliceOffset, sliceLength, getEndian());
		}
	}

	@Override
	public OwlReader slice(int sliceOffset, int sliceLength) throws IOException {
		if (sliceOffset < 0 || sliceLength < 0 || sliceOffset + sliceLength > length) {
			throw new IndexOutOfBoundsException("Invalid slice bounds: offset=" + sliceOffset + ", length=" + sliceLength + ", limit=" + length);
		}
		synchronized (readLock) {
			return new ByteArrayOwlReader(in, offset + sliceOffset, sliceLength, getEndian());
		}
	}

	@Override
	public OwlReader frame(int frameLength) {
		if (frameLength < 0)
			throw new IndexOutOfBoundsException("frameLength cannot be negative: " + frameLength);
		synchronized (readLock) {
			if (position + frameLength > length) {
				throw new IllegalArgumentException("frameLength exceeds available data: " + frameLength + " > " + (length - position));
			}
			return new FramedByteArrayOwlReader(frameLength);
		}
	}

	private class FramedByteArrayOwlReader extends BaseOwlReader {
		private final int frameBound;
		private int       framePos = 0;

		public FramedByteArrayOwlReader(int frameBound) {
			this.frameBound = frameBound;
			setEndian(ByteArrayOwlReader.this.getEndian());
		}

		private void advanceFrame(int count) throws EOFException {
			if (framePos + count > frameBound) {
				throw new EOFException("End of frame reached.");
			}
			framePos += count;
		}

		@Override
		public byte readByte() throws IOException {
			synchronized (readLock) {
				advanceFrame(1);
				return ByteArrayOwlReader.this.readByte();
			}
		}

		@Override
		public short readShort() throws IOException {
			synchronized (readLock) {
				advanceFrame(2);
				return ByteArrayOwlReader.this.readShort();
			}
		}

		@Override
		public int readInt() throws IOException {
			synchronized (readLock) {
				advanceFrame(4);
				return ByteArrayOwlReader.this.readInt();
			}
		}

		@Override
		public long readLong() throws IOException {
			synchronized (readLock) {
				advanceFrame(8);
				return ByteArrayOwlReader.this.readLong();
			}
		}

		@Override
		public int read(byte[] buffer, int off, int len) throws IOException {
			Objects.checkFromIndexSize(off, len, buffer.length);
			synchronized (readLock) {
				int toRead = Math.min(len, frameBound - framePos);
				int read = ByteArrayOwlReader.this.read(buffer, off, toRead);
				framePos += read;
				return read;
			}
		}

		@Override
		public String readString(int bytes, Charset encoding) throws IOException {
			synchronized (readLock) {
				advanceFrame(bytes);
				return ByteArrayOwlReader.this.readString(bytes, encoding);
			}
		}

		@Override
		public String readNullString(Charset encoding) throws IOException {
			synchronized (readLock) {
				byte[] buf = new byte[frameBound - framePos];
				int size = 0;
				boolean foundNull = false;
				while (framePos < frameBound) {
					advanceFrame(1);
					byte b = ByteArrayOwlReader.this.readByte();
					if (b == 0) {
						foundNull = true;
						break;
					}
					buf[size++] = b;
				}
				if (!foundNull) {
					throw new EOFException("Unexpected EOF searching for null terminator.");
				}
				return new String(buf, 0, size, encoding);
			}
		}

		@Override
		public String readLine(Charset encoding) throws IOException {
			synchronized (readLock) {
				int remaining = frameBound - framePos;
				if (remaining <= 0) {
					return "";
				}
				byte[] lineBuf = new byte[remaining];
				int size = 0;
				boolean foundNewline = false;
				while (framePos < frameBound) {
					advanceFrame(1);
					byte b = ByteArrayOwlReader.this.readByte();
					if (b == '\n') {
						foundNewline = true;
						break;
					}
					lineBuf[size++] = b;
				}
				if (size == 0 && !foundNewline) {
					return "";
				}
				int len = (size > 0 && lineBuf[size - 1] == '\r') ? size - 1 : size;
				return new String(lineBuf, 0, len, encoding);
			}
		}

		@Override
		public void skipBytes(long count) throws IOException {
			synchronized (readLock) {
				long toSkip = Math.min((long) (frameBound - framePos), count);
				advanceFrame((int) toSkip);
				ByteArrayOwlReader.this.skipBytes(toSkip);
			}
		}

		@Override
		public boolean hasData() {
			synchronized (readLock) {
				return framePos < frameBound && ByteArrayOwlReader.this.hasData();
			}
		}

		@Override
		public int position() {
			synchronized (readLock) {
				return framePos;
			}
		}

		@Override
		public void close() throws IOException {
			synchronized (readLock) {
				int unread = frameBound - framePos;
				if (unread > 0) {
					ByteArrayOwlReader.this.skipBytes(unread);
					framePos = frameBound;
				}
			}
		}
	}

}

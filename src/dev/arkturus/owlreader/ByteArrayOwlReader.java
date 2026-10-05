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

	/**
	 * Creates a {@link ByteArrayOwlReader} spanning a sub-range of the provided byte array with big-endian byte order.
	 *
	 * @param in the backing byte array.
	 * @param offset the starting index within the array.
	 * @param length the number of bytes to read.
	 * @throws NullPointerException if <code>in</code> is null.
	 * @throws IndexOutOfBoundsException if <code>offset</code> or <code>length</code> are invalid for <code>in</code>.
	 */
	public ByteArrayOwlReader(byte[] in, int offset, int length) {
		this(in, offset, length, Endian.BIG);
	}

	/**
	 * Creates a {@link ByteArrayOwlReader} spanning a sub-range of the provided byte array with the specified byte order.
	 *
	 * @param in the backing byte array.
	 * @param offset the starting index within the array.
	 * @param length the number of bytes to read.
	 * @param endian the endianness to use for multi-byte primitives.
	 * @throws NullPointerException if <code>in</code> is null.
	 * @throws IndexOutOfBoundsException if <code>offset</code> or <code>length</code> are invalid for <code>in</code>.
	 */
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
	public int readVarInt() throws IOException {
		synchronized (readLock) {
			int result = 0;
			int shift = 0;
			while (shift < 35) {
				if (position >= length) {
					throw new EOFException("Unexpected EOF while reading VarInt.");
				}
				byte b = in[offset + position++];
				result |= (b & 0x7F) << shift;
				if ((b & 0x80) == 0) {
					return result;
				}
				shift += 7;
			}
			throw new IOException("Malformed VarInt: exceeds 5 bytes");
		}
	}

	@Override
	public long readVarLong() throws IOException {
		synchronized (readLock) {
			long result = 0;
			int shift = 0;
			while (shift < 70) {
				if (position >= length) {
					throw new EOFException("Unexpected EOF while reading VarLong.");
				}
				byte b = in[offset + position++];
				result |= ((long) (b & 0x7F)) << shift;
				if ((b & 0x80) == 0) {
					return result;
				}
				shift += 7;
			}
			throw new IOException("Malformed VarLong: exceeds 10 bytes");
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

	/**
	 * Creates a detached, concurrent-safe zero-copy {@link OwlReader} containing the next <code>sliceLength</code>
	 * bytes starting from the current {@link #position()}.
	 * <p>
	 * <b>Zero-Copy:</b> Shares the underlying byte array directly at {@code offset + position} without copying.
	 * The parent reader's {@link #position()} is immediately advanced by <code>sliceLength</code>.
	 *
	 * @param sliceLength number of bytes to slice.
	 * @return a zero-copy {@link ByteArrayOwlReader} spanning the sliced range.
	 * @throws IndexOutOfBoundsException if <code>sliceLength &lt; 0</code>.
	 * @throws EOFException if fewer than <code>sliceLength</code> bytes remain in this reader window.
	 */
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

	/**
	 * Creates a detached, concurrent-safe zero-copy {@link OwlReader} containing <code>sliceLength</code> bytes
	 * starting at the specified absolute byte index <code>sliceOffset</code> within this reader window.
	 * <p>
	 * <b>Untouched Cursor:</b> This reader's current {@link #position()} is completely unmodified.
	 *
	 * @param sliceOffset relative offset within this reader window (relative to this reader's base offset).
	 * @param sliceLength number of bytes in the slice.
	 * @return a zero-copy {@link ByteArrayOwlReader} spanning the specified sub-window.
	 * @throws IndexOutOfBoundsException if <code>sliceOffset &lt; 0</code>, <code>sliceLength &lt; 0</code>, or
	 *                                   <code>sliceOffset + sliceLength &gt; length</code>.
	 */
	@Override
	public OwlReader slice(int sliceOffset, int sliceLength) throws IOException {
		if (sliceOffset < 0 || sliceLength < 0 || sliceOffset + sliceLength > length) {
			throw new IndexOutOfBoundsException("Invalid slice bounds: offset=" + sliceOffset + ", length=" + sliceLength + ", limit=" + length);
		}
		synchronized (readLock) {
			return new ByteArrayOwlReader(in, offset + sliceOffset, sliceLength, getEndian());
		}
	}

	/**
	 * Creates a sequential bounded viewing window of size <code>frameLength</code> bytes starting from the current
	 * {@link #position()}.
	 * <p>
	 * Reading from the framed reader advances this reader's {@link #position()}. Closing the framed reader
	 * automatically skips any unconsumed bytes remaining in the frame window.
	 *
	 * @param frameLength number of bytes in the framed window. Must be non-negative and &lt;= remaining bytes.
	 * @return an {@link OwlReader} scoped to read at most <code>frameLength</code> bytes.
	 * @throws IndexOutOfBoundsException if <code>frameLength &lt; 0</code>.
	 * @throws IllegalArgumentException if <code>frameLength</code> exceeds available remaining bytes.
	 */
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

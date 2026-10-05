package dev.arkturus.owlreader;

import java.io.EOFException;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Objects;

import dev.arkturus.owl.Endian;

/**
 * An {@link OwlReader} implementation backed by a Panama {@link MemorySegment}.
 * Supports on-heap segments, off-heap native memory segments, and memory-mapped segments.
 */
public class MemorySegmentOwlReader extends BaseOwlReader {

	protected final Object        readLock = new Object();
	protected final MemorySegment segment;
	protected volatile long       position = 0;

	private ValueLayout.OfShort  shortLayout;
	private ValueLayout.OfInt    intLayout;
	private ValueLayout.OfLong   longLayout;
	private ValueLayout.OfFloat  floatLayout;
	private ValueLayout.OfDouble doubleLayout;

	public MemorySegmentOwlReader(MemorySegment segment) {
		this.segment = Objects.requireNonNull(segment);
		updateLayouts(getEndian());
	}

	public MemorySegmentOwlReader(MemorySegment segment, Endian endian) {
		this.segment = Objects.requireNonNull(segment);
		setEndian(endian);
	}

	@Override
	public void setEndian(Endian endian) {
		super.setEndian(endian);
		synchronized (readLock) {
			updateLayouts(endian);
		}
	}

	private void updateLayouts(Endian endian) {
		ByteOrder order = (endian == Endian.LITTLE) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
		shortLayout = ValueLayout.JAVA_SHORT.withOrder(order).withByteAlignment(1);
		intLayout = ValueLayout.JAVA_INT.withOrder(order).withByteAlignment(1);
		longLayout = ValueLayout.JAVA_LONG.withOrder(order).withByteAlignment(1);
		floatLayout = ValueLayout.JAVA_FLOAT.withOrder(order).withByteAlignment(1);
		doubleLayout = ValueLayout.JAVA_DOUBLE.withOrder(order).withByteAlignment(1);
	}

	public MemorySegment getSegment() {
		return segment;
	}

	private void checkAvailable(long bytes) throws EOFException {
		if (position + bytes > segment.byteSize()) {
			throw new EOFException("Unexpected EOF.");
		}
	}

	@Override
	public byte readByte() throws IOException {
		synchronized (readLock) {
			checkAvailable(1);
			byte val = segment.get(ValueLayout.JAVA_BYTE, position);
			position += 1;
			return val;
		}
	}

	@Override
	public short readShort() throws IOException {
		synchronized (readLock) {
			checkAvailable(2);
			short val = segment.get(shortLayout, position);
			position += 2;
			return val;
		}
	}

	@Override
	public int readInt24() throws IOException {
		return (readUInt24() << 8) >> 8;
	}

	@Override
	public int readUInt24() throws IOException {
		synchronized (readLock) {
			checkAvailable(3);
			byte b0 = segment.get(ValueLayout.JAVA_BYTE, position);
			byte b1 = segment.get(ValueLayout.JAVA_BYTE, position + 1);
			byte b2 = segment.get(ValueLayout.JAVA_BYTE, position + 2);
			position += 3;
			return (getEndian() == Endian.LITTLE)
					? ((b2 & 0xFF) << 16) | ((b1 & 0xFF) << 8) | (b0 & 0xFF)
					: ((b0 & 0xFF) << 16) | ((b1 & 0xFF) << 8) | (b2 & 0xFF);
		}
	}

	@Override
	public int readInt() throws IOException {
		synchronized (readLock) {
			checkAvailable(4);
			int val = segment.get(intLayout, position);
			position += 4;
			return val;
		}
	}

	@Override
	public long readLong() throws IOException {
		synchronized (readLock) {
			checkAvailable(8);
			long val = segment.get(longLayout, position);
			position += 8;
			return val;
		}
	}

	@Override
	public int readVarInt() throws IOException {
		synchronized (readLock) {
			int result = 0;
			int shift = 0;
			while (shift < 35) {
				checkAvailable(1);
				byte b = segment.get(ValueLayout.JAVA_BYTE, position++);
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
				checkAvailable(1);
				byte b = segment.get(ValueLayout.JAVA_BYTE, position++);
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
	public float readFloat() throws IOException {
		synchronized (readLock) {
			checkAvailable(4);
			float val = segment.get(floatLayout, position);
			position += 4;
			return val;
		}
	}

	@Override
	public double readDouble() throws IOException {
		synchronized (readLock) {
			checkAvailable(8);
			double val = segment.get(doubleLayout, position);
			position += 8;
			return val;
		}
	}

	@Override
	public int read(byte[] out, int off, int len) throws IOException {
		Objects.checkFromIndexSize(off, len, out.length);
		synchronized (readLock) {
			long remaining = segment.byteSize() - position;
			int toRead = (int) Math.max(0, Math.min((long) len, remaining));
			if (toRead > 0) {
				MemorySegment dst = MemorySegment.ofArray(out);
				MemorySegment.copy(segment, position, dst, off, toRead);
				position += toRead;
			}
			return toRead;
		}
	}

	@Override
	public String readString(int bytes, Charset encoding) throws IOException {
		if (bytes < 0) {
			throw new IndexOutOfBoundsException("bytes cannot be negative: " + bytes);
		}
		synchronized (readLock) {
			checkAvailable(bytes);
			byte[] tmp = new byte[bytes];
			MemorySegment dst = MemorySegment.ofArray(tmp);
			MemorySegment.copy(segment, position, dst, 0, bytes);
			position += bytes;
			return new String(tmp, encoding);
		}
	}

	@Override
	public String readNullString(Charset encoding) throws IOException {
		synchronized (readLock) {
			long size = 0;
			long limit = segment.byteSize();
			while (position + size < limit && segment.get(ValueLayout.JAVA_BYTE, position + size) != 0) {
				size++;
			}
			if (position + size >= limit) {
				throw new EOFException("Unexpected EOF searching for null terminator.");
			}
			byte[] tmp = new byte[(int) size];
			MemorySegment dst = MemorySegment.ofArray(tmp);
			MemorySegment.copy(segment, position, dst, 0, size);
			position += size + 1; // skip null terminator
			return new String(tmp, encoding);
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
			long limit = segment.byteSize();
			if (position >= limit) {
				return "";
			}
			long newlinePos = -1;
			for (long i = position; i < limit; i++) {
				if (segment.get(ValueLayout.JAVA_BYTE, i) == '\n') {
					newlinePos = i;
					break;
				}
			}
			if (newlinePos != -1) {
				int rawLen = (int) (newlinePos - position);
				int len = (rawLen > 0 && segment.get(ValueLayout.JAVA_BYTE, newlinePos - 1) == '\r') ? rawLen - 1 : rawLen;
				byte[] tmp = new byte[rawLen];
				MemorySegment dst = MemorySegment.ofArray(tmp);
				MemorySegment.copy(segment, position, dst, 0, rawLen);
				position = newlinePos + 1; // consume '\n'
				return new String(tmp, 0, len, encoding);
			} else {
				int len = (int) (limit - position);
				int effectiveLen = (len > 0 && segment.get(ValueLayout.JAVA_BYTE, limit - 1) == '\r') ? len - 1 : len;
				byte[] tmp = new byte[len];
				MemorySegment dst = MemorySegment.ofArray(tmp);
				MemorySegment.copy(segment, position, dst, 0, len);
				position = limit;
				return new String(tmp, 0, effectiveLen, encoding);
			}
		}
	}

	@Override
	public void skipBytes(long count) throws IOException {
		if (count < 0) {
			throw new IndexOutOfBoundsException("count cannot be negative: " + count);
		}
		synchronized (readLock) {
			position = Math.min(segment.byteSize(), position + count);
		}
	}

	@Override
	public boolean hasData() {
		synchronized (readLock) {
			return position < segment.byteSize();
		}
	}

	@Override
	public int position() {
		synchronized (readLock) {
			return (int) position;
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (readLock) {
			position = segment.byteSize();
		}
	}

	/**
	 * Creates a detached, concurrent-safe zero-copy {@link OwlReader} containing the next <code>sliceLength</code>
	 * bytes starting from the current {@link #position()}.
	 * <p>
	 * <b>Zero-Copy:</b> Shares the underlying Panama {@link MemorySegment} via {@link MemorySegment#asSlice(long, long)}
	 * without copying. The parent reader's {@link #position()} is immediately advanced by <code>sliceLength</code>.
	 *
	 * @param sliceLength number of bytes to slice.
	 * @return a zero-copy {@link MemorySegmentOwlReader} spanning the sliced segment.
	 * @throws IndexOutOfBoundsException if <code>sliceLength &lt; 0</code>.
	 * @throws EOFException if fewer than <code>sliceLength</code> bytes remain in this segment.
	 */
	@Override
	public OwlReader slice(int sliceLength) throws IOException {
		if (sliceLength < 0) {
			throw new IndexOutOfBoundsException("sliceLength cannot be negative: " + sliceLength);
		}
		synchronized (readLock) {
			if (position + sliceLength > segment.byteSize()) {
				throw new EOFException("Unexpected EOF: requested " + sliceLength + ", available " + (segment.byteSize() - position));
			}
			MemorySegment child = segment.asSlice(position, sliceLength);
			position += sliceLength;
			return new MemorySegmentOwlReader(child, getEndian());
		}
	}

	/**
	 * Creates a detached, concurrent-safe zero-copy {@link OwlReader} containing <code>sliceLength</code> bytes
	 * starting at the specified absolute byte index <code>sliceOffset</code> within this memory segment.
	 * <p>
	 * <b>Untouched Cursor:</b> This reader's current {@link #position()} is completely unmodified.
	 *
	 * @param sliceOffset absolute byte offset within this memory segment.
	 * @param sliceLength number of bytes in the slice.
	 * @return a zero-copy {@link MemorySegmentOwlReader} spanning the specified segment slice.
	 * @throws IndexOutOfBoundsException if <code>sliceOffset &lt; 0</code>, <code>sliceLength &lt; 0</code>, or
	 *                                   <code>sliceOffset + sliceLength &gt; segment.byteSize()</code>.
	 */
	@Override
	public OwlReader slice(int sliceOffset, int sliceLength) throws IOException {
		if (sliceOffset < 0 || sliceLength < 0 || sliceOffset + sliceLength > segment.byteSize()) {
			throw new IndexOutOfBoundsException("Invalid slice bounds: offset=" + sliceOffset + ", length=" + sliceLength + ", limit=" + segment.byteSize());
		}
		synchronized (readLock) {
			MemorySegment child = segment.asSlice(sliceOffset, sliceLength);
			return new MemorySegmentOwlReader(child, getEndian());
		}
	}

	/**
	 * Creates a sequential bounded viewing window of size <code>frameLength</code> bytes starting from the current
	 * {@link #position()}.
	 * <p>
	 * Reading from the framed reader advances this reader's position in lockstep. Closing the framed reader
	 * automatically skips any unconsumed bytes remaining in the frame window.
	 *
	 * @param frameLength number of bytes in the framed window. Must be non-negative and &lt;= remaining bytes.
	 * @return an {@link OwlReader} scoped to read at most <code>frameLength</code> bytes.
	 * @throws IndexOutOfBoundsException if <code>frameLength &lt; 0</code>.
	 * @throws IllegalArgumentException if <code>frameLength</code> exceeds available remaining bytes.
	 */
	@Override
	public OwlReader frame(int frameLength) {
		if (frameLength < 0) {
			throw new IndexOutOfBoundsException("frameLength cannot be negative: " + frameLength);
		}
		synchronized (readLock) {
			if (position + frameLength > segment.byteSize()) {
				throw new IllegalArgumentException("frameLength exceeds available data: " + frameLength + " > " + (segment.byteSize() - position));
			}
			return new FramedMemorySegmentOwlReader(frameLength);
		}
	}

	private class FramedMemorySegmentOwlReader extends BaseOwlReader {
		private final int frameBound;
		private int       framePos = 0;

		public FramedMemorySegmentOwlReader(int frameBound) {
			this.frameBound = frameBound;
			setEndian(MemorySegmentOwlReader.this.getEndian());
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
				return MemorySegmentOwlReader.this.readByte();
			}
		}

		@Override
		public short readShort() throws IOException {
			synchronized (readLock) {
				advanceFrame(2);
				return MemorySegmentOwlReader.this.readShort();
			}
		}

		@Override
		public int readInt() throws IOException {
			synchronized (readLock) {
				advanceFrame(4);
				return MemorySegmentOwlReader.this.readInt();
			}
		}

		@Override
		public long readLong() throws IOException {
			synchronized (readLock) {
				advanceFrame(8);
				return MemorySegmentOwlReader.this.readLong();
			}
		}

		@Override
		public int read(byte[] out, int off, int len) throws IOException {
			Objects.checkFromIndexSize(off, len, out.length);
			synchronized (readLock) {
				int toRead = Math.min(len, frameBound - framePos);
				int read = MemorySegmentOwlReader.this.read(out, off, toRead);
				framePos += read;
				return read;
			}
		}

		@Override
		public String readString(int bytes, Charset encoding) throws IOException {
			synchronized (readLock) {
				advanceFrame(bytes);
				return MemorySegmentOwlReader.this.readString(bytes, encoding);
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
					byte b = MemorySegmentOwlReader.this.readByte();
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
					byte b = MemorySegmentOwlReader.this.readByte();
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
				MemorySegmentOwlReader.this.skipBytes(toSkip);
			}
		}

		@Override
		public boolean hasData() {
			synchronized (readLock) {
				return framePos < frameBound && MemorySegmentOwlReader.this.hasData();
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
					MemorySegmentOwlReader.this.skipBytes(unread);
					framePos = frameBound;
				}
			}
		}
	}

}

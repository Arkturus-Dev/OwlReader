package dev.arkturus.owlreader;

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Objects;

import dev.arkturus.owl.Endian;

/**
 * An {@link OwlReader} implementation backed by a Java NIO {@link ByteBuffer}.
 * Supports heap buffers, direct off-heap buffers, and memory-mapped buffers.
 */
public class NIOReader extends BaseOwlReader {

	protected final Object     readLock = new Object();
	protected final ByteBuffer buffer;

	public NIOReader(ByteBuffer buffer) {
		this.buffer = Objects.requireNonNull(buffer);
		syncByteOrder(getEndian());
	}

	public NIOReader(ByteBuffer buffer, Endian endian) {
		this.buffer = Objects.requireNonNull(buffer);
		setEndian(endian);
	}

	@Override
	public void setEndian(Endian endian) {
		super.setEndian(endian);
		synchronized (readLock) {
			syncByteOrder(endian);
		}
	}

	private void syncByteOrder(Endian endian) {
		if (endian == Endian.LITTLE) {
			buffer.order(ByteOrder.LITTLE_ENDIAN);
		} else {
			buffer.order(ByteOrder.BIG_ENDIAN);
		}
	}

	public ByteBuffer getByteBuffer() {
		return buffer;
	}

	@Override
	public byte readByte() throws IOException {
		synchronized (readLock) {
			try {
				return buffer.get();
			} catch (BufferUnderflowException e) {
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public short readShort() throws IOException {
		synchronized (readLock) {
			try {
				return buffer.getShort();
			} catch (BufferUnderflowException e) {
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public int readInt() throws IOException {
		synchronized (readLock) {
			try {
				return buffer.getInt();
			} catch (BufferUnderflowException e) {
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public long readLong() throws IOException {
		synchronized (readLock) {
			try {
				return buffer.getLong();
			} catch (BufferUnderflowException e) {
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public int readVarInt() throws IOException {
		synchronized (readLock) {
			int result = 0;
			int shift = 0;
			while (shift < 35) {
				if (!buffer.hasRemaining()) {
					throw new EOFException("Unexpected EOF while reading VarInt.");
				}
				byte b = buffer.get();
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
				if (!buffer.hasRemaining()) {
					throw new EOFException("Unexpected EOF while reading VarLong.");
				}
				byte b = buffer.get();
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
			try {
				return buffer.getFloat();
			} catch (BufferUnderflowException e) {
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public double readDouble() throws IOException {
		synchronized (readLock) {
			try {
				return buffer.getDouble();
			} catch (BufferUnderflowException e) {
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public int read(byte[] out, int off, int len) throws IOException {
		Objects.checkFromIndexSize(off, len, out.length);
		synchronized (readLock) {
			int remaining = buffer.remaining();
			int toRead = Math.max(0, Math.min(len, remaining));
			if (toRead > 0) {
				buffer.get(out, off, toRead);
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
			if (buffer.remaining() < bytes) {
				throw new EOFException("Unexpected EOF.");
			}
			byte[] tmp = new byte[bytes];
			buffer.get(tmp);
			return new String(tmp, encoding);
		}
	}

	@Override
	public String readNullString(Charset encoding) throws IOException {
		synchronized (readLock) {
			int pos = buffer.position();
			int limit = buffer.limit();
			int nullPos = -1;
			for (int i = pos; i < limit; i++) {
				if (buffer.get(i) == 0) {
					nullPos = i;
					break;
				}
			}
			if (nullPos == -1) {
				throw new EOFException("Unexpected EOF searching for null terminator.");
			}
			int len = nullPos - pos;
			byte[] tmp = new byte[len];
			buffer.get(tmp);
			buffer.get(); // consume null byte
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
			int pos = buffer.position();
			int limit = buffer.limit();
			if (pos >= limit) {
				return "";
			}
			int newlinePos = -1;
			for (int i = pos; i < limit; i++) {
				if (buffer.get(i) == '\n') {
					newlinePos = i;
					break;
				}
			}
			if (newlinePos != -1) {
				int rawLen = newlinePos - pos;
				int len = (rawLen > 0 && buffer.get(newlinePos - 1) == '\r') ? rawLen - 1 : rawLen;
				byte[] tmp = new byte[rawLen];
				buffer.get(tmp);
				buffer.get(); // consume '\n'
				return new String(tmp, 0, len, encoding);
			} else {
				int len = limit - pos;
				int effectiveLen = (len > 0 && buffer.get(limit - 1) == '\r') ? len - 1 : len;
				byte[] tmp = new byte[len];
				buffer.get(tmp);
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
			long newPos = Math.min((long) buffer.limit(), (long) buffer.position() + count);
			buffer.position((int) newPos);
		}
	}

	@Override
	public boolean hasData() {
		synchronized (readLock) {
			return buffer.hasRemaining();
		}
	}

	@Override
	public int position() {
		synchronized (readLock) {
			return buffer.position();
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (readLock) {
			buffer.position(buffer.limit());
		}
	}

	/**
	 * Creates a detached, concurrent-safe zero-copy {@link OwlReader} containing the next <code>sliceLength</code>
	 * bytes starting from the current {@link #position()}.
	 * <p>
	 * <b>Zero-Copy:</b> Shares the underlying {@link ByteBuffer} via {@link ByteBuffer#slice()} without copying.
	 * The parent reader's {@link #position()} is immediately advanced by <code>sliceLength</code>.
	 * The direct/heap nature and byte order of the parent buffer are preserved.
	 *
	 * @param sliceLength number of bytes to slice.
	 * @return a zero-copy {@link NIOReader} spanning the sliced range.
	 * @throws IndexOutOfBoundsException if <code>sliceLength &lt; 0</code>.
	 * @throws EOFException if fewer than <code>sliceLength</code> bytes remain in this buffer.
	 */
	@Override
	public OwlReader slice(int sliceLength) throws IOException {
		if (sliceLength < 0) {
			throw new IndexOutOfBoundsException("sliceLength cannot be negative: " + sliceLength);
		}
		synchronized (readLock) {
			if (buffer.remaining() < sliceLength) {
				throw new EOFException("Unexpected EOF: requested " + sliceLength + ", available " + buffer.remaining());
			}
			int oldLimit = buffer.limit();
			buffer.limit(buffer.position() + sliceLength);
			ByteBuffer child = buffer.slice();
			child.order(buffer.order());
			buffer.limit(oldLimit);
			buffer.position(buffer.position() + sliceLength);
			return new NIOReader(child, getEndian());
		}
	}

	/**
	 * Creates a detached, concurrent-safe zero-copy {@link OwlReader} containing <code>sliceLength</code> bytes
	 * starting at the specified absolute byte index <code>sliceOffset</code> within this buffer.
	 * <p>
	 * <b>Untouched Cursor:</b> This reader's current {@link #position()} is completely unmodified.
	 *
	 * @param sliceOffset absolute byte offset within this buffer.
	 * @param sliceLength number of bytes in the slice.
	 * @return a zero-copy {@link NIOReader} spanning the specified range.
	 * @throws IndexOutOfBoundsException if <code>sliceOffset &lt; 0</code>, <code>sliceLength &lt; 0</code>, or
	 *                                   <code>sliceOffset + sliceLength &gt; buffer.limit()</code>.
	 */
	@Override
	public OwlReader slice(int sliceOffset, int sliceLength) throws IOException {
		if (sliceOffset < 0 || sliceLength < 0 || sliceOffset + sliceLength > buffer.limit()) {
			throw new IndexOutOfBoundsException("Invalid slice bounds: offset=" + sliceOffset + ", length=" + sliceLength + ", limit=" + buffer.limit());
		}
		synchronized (readLock) {
			ByteBuffer duplicate = buffer.duplicate();
			duplicate.position(sliceOffset);
			duplicate.limit(sliceOffset + sliceLength);
			ByteBuffer child = duplicate.slice();
			child.order(buffer.order());
			return new NIOReader(child, getEndian());
		}
	}

	/**
	 * Creates a sequential bounded viewing window of size <code>frameLength</code> bytes starting from the current
	 * {@link #position()}.
	 * <p>
	 * Reading from the framed reader advances this buffer's position in lockstep. Closing the framed reader
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
			if (buffer.remaining() < frameLength) {
				throw new IllegalArgumentException("frameLength exceeds available data: " + frameLength + " > " + buffer.remaining());
			}
			return new FramedNIOReader(frameLength);
		}
	}

	private class FramedNIOReader extends BaseOwlReader {
		private final int frameBound;
		private int       framePos = 0;

		public FramedNIOReader(int frameBound) {
			this.frameBound = frameBound;
			setEndian(NIOReader.this.getEndian());
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
				return NIOReader.this.readByte();
			}
		}

		@Override
		public short readShort() throws IOException {
			synchronized (readLock) {
				advanceFrame(2);
				return NIOReader.this.readShort();
			}
		}

		@Override
		public int readInt() throws IOException {
			synchronized (readLock) {
				advanceFrame(4);
				return NIOReader.this.readInt();
			}
		}

		@Override
		public long readLong() throws IOException {
			synchronized (readLock) {
				advanceFrame(8);
				return NIOReader.this.readLong();
			}
		}

		@Override
		public int read(byte[] out, int off, int len) throws IOException {
			Objects.checkFromIndexSize(off, len, out.length);
			synchronized (readLock) {
				int toRead = Math.min(len, frameBound - framePos);
				int read = NIOReader.this.read(out, off, toRead);
				framePos += read;
				return read;
			}
		}

		@Override
		public String readString(int bytes, Charset encoding) throws IOException {
			synchronized (readLock) {
				advanceFrame(bytes);
				return NIOReader.this.readString(bytes, encoding);
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
					byte b = NIOReader.this.readByte();
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
					byte b = NIOReader.this.readByte();
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
				NIOReader.this.skipBytes(toSkip);
			}
		}

		@Override
		public boolean hasData() {
			synchronized (readLock) {
				return framePos < frameBound && NIOReader.this.hasData();
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
					NIOReader.this.skipBytes(unread);
					framePos = frameBound;
				}
			}
		}
	}

}

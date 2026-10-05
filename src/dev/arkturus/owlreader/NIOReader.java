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

}

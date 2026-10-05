package dev.arkturus.owlwriter;

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Objects;

import dev.arkturus.owl.Endian;

/**
 * An {@link OwlWriter} implementation backed by a Java NIO {@link ByteBuffer}.
 * Supports heap and direct off-heap buffers.
 */
public class NIOWriter extends BaseOwlWriter {

	protected final Object     writeLock = new Object();
	protected final ByteBuffer buffer;

	public NIOWriter(ByteBuffer buffer) {
		this.buffer = Objects.requireNonNull(buffer);
		syncByteOrder(getEndian());
	}

	public NIOWriter(ByteBuffer buffer, Endian endian) {
		this.buffer = Objects.requireNonNull(buffer);
		setEndian(endian);
	}

	@Override
	public void setEndian(Endian endian) {
		super.setEndian(endian);
		synchronized (writeLock) {
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

	public int position() {
		synchronized (writeLock) {
			return buffer.position();
		}
	}

	@Override
	public void writeByte(byte b) throws IOException {
		synchronized (writeLock) {
			try {
				buffer.put(b);
			} catch (BufferOverflowException e) {
				buffer.position(buffer.limit());
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public void writeShort(short s) throws IOException {
		synchronized (writeLock) {
			try {
				buffer.putShort(s);
			} catch (BufferOverflowException e) {
				buffer.position(buffer.limit());
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public void writeInt(int i) throws IOException {
		synchronized (writeLock) {
			try {
				buffer.putInt(i);
			} catch (BufferOverflowException e) {
				buffer.position(buffer.limit());
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public void writeLong(long l) throws IOException {
		synchronized (writeLock) {
			try {
				buffer.putLong(l);
			} catch (BufferOverflowException e) {
				buffer.position(buffer.limit());
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public void writeVarInt(int value) throws IOException {
		synchronized (writeLock) {
			try {
				while ((value & ~0x7F) != 0) {
					buffer.put((byte) ((value & 0x7F) | 0x80));
					value >>>= 7;
				}
				buffer.put((byte) (value & 0x7F));
			} catch (BufferOverflowException e) {
				buffer.position(buffer.limit());
				throw new EOFException("End of buffer reached while writing VarInt.");
			}
		}
	}

	@Override
	public void writeVarLong(long value) throws IOException {
		synchronized (writeLock) {
			try {
				while ((value & ~0x7FL) != 0) {
					buffer.put((byte) (((int) value & 0x7F) | 0x80));
					value >>>= 7;
				}
				buffer.put((byte) ((int) value & 0x7F));
			} catch (BufferOverflowException e) {
				buffer.position(buffer.limit());
				throw new EOFException("End of buffer reached while writing VarLong.");
			}
		}
	}

	@Override
	public void writeFloat(float f) throws IOException {
		synchronized (writeLock) {
			try {
				buffer.putFloat(f);
			} catch (BufferOverflowException e) {
				buffer.position(buffer.limit());
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public void writeDouble(double d) throws IOException {
		synchronized (writeLock) {
			try {
				buffer.putDouble(d);
			} catch (BufferOverflowException e) {
				buffer.position(buffer.limit());
				throw new EOFException(e.getMessage());
			}
		}
	}

	@Override
	public void write(byte[] bytes, int off, int len) throws IOException {
		Objects.checkFromIndexSize(off, len, bytes.length);
		synchronized (writeLock) {
			try {
				buffer.put(bytes, off, len);
			} catch (BufferOverflowException e) {
				buffer.position(buffer.limit());
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

	public byte[] toByteArray() {
		synchronized (writeLock) {
			int pos = buffer.position();
			byte[] result = new byte[pos];
			ByteBuffer duplicate = buffer.duplicate();
			duplicate.position(0);
			duplicate.get(result, 0, pos);
			return result;
		}
	}

	public String asString(Charset encoding) {
		return new String(toByteArray(), encoding);
	}

	@Override
	public void close() throws IOException {
		// No-op for ByteBuffer
	}

}

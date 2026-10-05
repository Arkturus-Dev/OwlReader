package dev.arkturus.owl.buffer;

import java.io.IOException;
import java.io.UncheckedIOException;

import dev.arkturus.owl.UnsafeUnsignedException;
import dev.arkturus.owl.VarInt;
import dev.arkturus.owlreader.OwlReader;
import dev.arkturus.owlreader.Readable;

public interface ReadableBuffer extends Readable {

	public byte peek();

	public byte read();

	@Override
	public byte[] read(int len);

	@Override
	public int read(byte[] buffer, int off, int len);

	public boolean pread(byte b);

	public default boolean pread(char c) {
		return pread((byte) c);
	}

	public byte[] readUntilBytes(byte b);

	public byte[] readThroughBytes(byte b);

	public default byte[] readUntilBytes(char c) {
		return readUntilBytes((byte) c);
	}

	public default byte[] readThroughBytes(char c) {
		return readThroughBytes((byte) c);
	}

	public String readUntil(byte b);

	public String readThrough(byte b);

	public default String readUntil(char c) {
		return readUntil((byte) c);
	}

	public default String readThrough(char c) {
		return readThrough((byte) c);
	}

	public void advance(int len);

	public byte next(byte... bs);

	public default byte next(char... bs) {
		byte[] bytes = new byte[bs.length];
		for (int i = 0; i < bs.length; i++)
			bytes[i] = (byte) bs[i];
		return next(bytes);
	}

	public boolean readEol();

	public boolean available();

	public OwlReader reader();

	@Override
	public default short readShort() {
		return getEndian().readShort(read(2), 0);
	}

	@Override
	public default int readInt24() {
		return getEndian().readInt24(read(3), 0);
	}

	@Override
	public default int readMedium() {
		return readInt24();
	}

	@Override
	public default int readInt() {
		return getEndian().readInt(read(4), 0);
	}

	@Override
	public default long readLong() {
		return getEndian().readLong(read(8), 0);
	}

	@Override
	public default float readFloat() {
		return Float.intBitsToFloat(readInt());
	}

	@Override
	public default double readDouble() {
		return Double.longBitsToDouble(readLong());
	}

	@Override
	public default short readUByte() {
		return (short) Byte.toUnsignedInt(readByte());
	}

	@Override
	public default byte readSUByte() {
		byte b = readByte();
		if (b < 0)
			throw new UnsafeUnsignedException(byte.class);
		return b;
	}

	@Override
	public default int readUShort() {
		return Short.toUnsignedInt(readShort());
	}

	@Override
	public default short readSUShort() {
		short s = readShort();
		if (s < 0)
			throw new UnsafeUnsignedException(short.class);
		return s;
	}

	@Override
	public default int readUInt24() {
		return getEndian().readUInt24(read(3), 0);
	}

	@Override
	public default int readUMedium() {
		return readUInt24();
	}

	@Override
	public default int readUnsignedInt24() {
		return readUInt24();
	}

	@Override
	public default int readUnsignedMedium() {
		return readUInt24();
	}

	@Override
	public default int readSUInt24() {
		int i = readInt24();
		if (i < 0) {
			throw new UnsafeUnsignedException(int.class);
		}
		return i;
	}

	@Override
	public default int readSUMedium() {
		return readSUInt24();
	}

	@Override
	public default long readUInt() {
		return Integer.toUnsignedLong(readInt());
	}

	@Override
	public default int readSUInt() {
		int i = readInt();
		if (i < 0)
			throw new UnsafeUnsignedException(int.class);
		return i;
	}

	@Override
	public default long readSULong() {
		long l = readLong();
		if (l < 0)
			throw new UnsafeUnsignedException(long.class);
		return l;
	}

	@Override
	public default byte readByte() {
		return read();
	}

	@Override
	public default int readVarInt() {
		int result = 0;
		int shift = 0;
		while (shift < 35) {
			byte b = readByte();
			result |= (b & 0x7F) << shift;
			if ((b & 0x80) == 0) {
				return result;
			}
			shift += 7;
		}
		throw new UncheckedIOException(new IOException("Malformed VarInt: exceeds 5 bytes"));
	}

	@Override
	public default long readVarLong() {
		long result = 0;
		int shift = 0;
		while (shift < 70) {
			byte b = readByte();
			result |= ((long) (b & 0x7F)) << shift;
			if ((b & 0x80) == 0) {
				return result;
			}
			shift += 7;
		}
		throw new UncheckedIOException(new IOException("Malformed VarLong: exceeds 10 bytes"));
	}

	@Override
	public default int readSignedVarInt() {
		return VarInt.decodeZigZag32(readVarInt());
	}

	@Override
	public default int readZigZagVarInt() {
		return readSignedVarInt();
	}

	@Override
	public default long readSignedVarLong() {
		return VarInt.decodeZigZag64(readVarLong());
	}

	@Override
	public default long readZigZagVarLong() {
		return readSignedVarLong();
	}

	@Override
	public default int read(byte[] buffer) {
		return read(buffer, 0, buffer.length);
	}

	public default int read(java.nio.ByteBuffer dst) {
		throw new UnsupportedOperationException();
	}

	public default java.nio.ByteBuffer asByteBuffer() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Creates a detached, concurrent-safe {@link OwlReader} containing the next <code>length</code> readable bytes
	 * starting from the current read position.
	 * <p>
	 * <b>Semantics:</b>
	 * <ul>
	 *   <li><b>Read Index Advancement:</b> The buffer's read position is immediately advanced by <code>length</code> bytes.</li>
	 *   <li><b>Zero-Copy:</b> Shares the underlying storage directly without allocating duplicate arrays or copying data.</li>
	 *   <li><b>Independent Cursor:</b> The returned reader has its own isolated position cursor, and closing it has no effect on this buffer.</li>
	 * </ul>
	 *
	 * @param length the number of bytes to slice. Must be non-negative and &lt;= readable bytes.
	 * @return an independent {@link OwlReader} view over the sliced bytes.
	 * @throws IndexOutOfBoundsException if <code>length &lt; 0</code>.
	 * @throws java.io.UncheckedIOException if fewer than <code>length</code> bytes are available.
	 */
	public default OwlReader slice(int length) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Creates a detached, concurrent-safe {@link OwlReader} containing <code>length</code> bytes starting at the specified
	 * absolute <code>offset</code> within this buffer.
	 * <p>
	 * <b>Semantics:</b>
	 * <ul>
	 *   <li><b>Untouched Read Index:</b> The buffer's current read and write indices are completely unaffected.</li>
	 *   <li><b>Zero-Copy:</b> Shares the underlying storage directly without copying data.</li>
	 *   <li><b>Independent Cursor:</b> The returned reader has its own isolated position cursor starting at 0.</li>
	 * </ul>
	 *
	 * @param offset the absolute byte offset within this buffer.
	 * @param length the number of bytes to slice.
	 * @return an independent {@link OwlReader} view over the specified slice.
	 * @throws IndexOutOfBoundsException if <code>offset &lt; 0</code>, <code>length &lt; 0</code>, or
	 *                                   <code>offset + length</code> exceeds the buffer bounds.
	 */
	public default OwlReader slice(int offset, int length) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Creates a sequential bounded viewing window of size <code>length</code> bytes starting from the current read position.
	 * <p>
	 * <b>Semantics:</b>
	 * <ul>
	 *   <li><b>Lockstep Cursor:</b> Reading through the framed reader advances this buffer's read position.</li>
	 *   <li><b>Auto-Alignment on Close:</b> Closing the framed reader automatically skips any unread frame bytes, aligning
	 *       this buffer's read position to the end of the frame window.</li>
	 *   <li><b>Boundary Enforcement:</b> Reading past <code>length</code> bytes throws {@link java.io.EOFException}.</li>
	 * </ul>
	 *
	 * @param length the number of bytes in the framed window. Must be non-negative.
	 * @return an {@link OwlReader} scoped to read at most <code>length</code> bytes.
	 * @throws IndexOutOfBoundsException if <code>length &lt; 0</code>.
	 * @throws IllegalArgumentException if <code>length</code> exceeds available readable bytes.
	 */
	public default OwlReader frame(int length) {
		throw new UnsupportedOperationException();
	}

}

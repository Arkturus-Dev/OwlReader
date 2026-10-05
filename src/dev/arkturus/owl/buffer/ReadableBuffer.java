package dev.arkturus.owl.buffer;

import dev.arkturus.owl.UnsafeUnsignedException;
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
	public default int read(byte[] buffer) {
		return read(buffer, 0, buffer.length);
	}

	public default int read(java.nio.ByteBuffer dst) {
		throw new UnsupportedOperationException();
	}

	public default java.nio.ByteBuffer asByteBuffer() {
		throw new UnsupportedOperationException();
	}

}

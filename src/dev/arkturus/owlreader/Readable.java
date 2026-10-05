package dev.arkturus.owlreader;

import java.io.EOFException;
import java.io.IOException;

import dev.arkturus.owl.Endian;
import dev.arkturus.owl.UnsafeUnsignedException;
import dev.arkturus.owl.VarInt;

public interface Readable {

	/**
	 * Reads a byte from the input.
	 *
	 * @return a byte.
	 * @throws IOException
	 */
	public byte readByte() throws IOException;

	/**
	 * Reads a short from the input.
	 *
	 * @return a short.
	 * @throws IOException
	 */
	public default short readShort() throws IOException {
		return getEndian().readShort(read(2), 0);
	}

	/**
	 * Reads an int from the input.
	 *
	 * @return an int.
	 * @throws IOException
	 */
	public default int readInt() throws IOException {
		return getEndian().readInt(read(4), 0);
	}

	/**
	 * Reads a long from the input.
	 *
	 * @return a long.
	 * @throws IOException
	 */
	public default long readLong() throws IOException {
		return getEndian().readLong(read(8), 0);
	}

	/**
	 * Reads a float from the input.
	 *
	 * @return a float.
	 * @throws IOException
	 */
	public default float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}

	/**
	 * Reads a double from the input.
	 *
	 * @return a double.
	 * @throws IOException
	 */
	public default double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
	}

	/**
	 * Reads a variable-length 32-bit integer (VarInt) using 7-bit continuation chunks.
	 * <p>
	 * <b>Security Safeguard:</b> To protect against unbounded memory consumption and denial-of-service
	 * from corrupt streams with endless continuation bits, this method enforces a strict 5-byte limit.
	 * If more than 5 bytes are encountered with the continuation bit set, an {@link IOException} is thrown.
	 *
	 * @return the decoded integer.
	 * @throws IOException if an I/O error occurs, premature EOF is hit, or the VarInt exceeds 5 bytes.
	 */
	public default int readVarInt() throws IOException {
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
		throw new IOException("Malformed VarInt: exceeds 5 bytes");
	}

	/**
	 * Reads a variable-length 64-bit long (VarLong) using 7-bit continuation chunks.
	 * <p>
	 * <b>Security Safeguard:</b> To protect against unbounded memory consumption and denial-of-service
	 * from corrupt streams with endless continuation bits, this method enforces a strict 10-byte limit.
	 * If more than 10 bytes are encountered with the continuation bit set, an {@link IOException} is thrown.
	 *
	 * @return the decoded long.
	 * @throws IOException if an I/O error occurs, premature EOF is hit, or the VarLong exceeds 10 bytes.
	 */
	public default long readVarLong() throws IOException {
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
		throw new IOException("Malformed VarLong: exceeds 10 bytes");
	}

	/**
	 * Reads a ZigZag-encoded 32-bit signed integer.
	 *
	 * @return the decoded signed integer.
	 * @throws IOException if an I/O error occurs or the VarInt is malformed.
	 */
	public default int readSignedVarInt() throws IOException {
		return VarInt.decodeZigZag32(readVarInt());
	}

	/**
	 * Reads a ZigZag-encoded 32-bit signed integer. Alias for {@link #readSignedVarInt()}.
	 *
	 * @return the decoded signed integer.
	 * @throws IOException if an I/O error occurs or the VarInt is malformed.
	 */
	public default int readZigZagVarInt() throws IOException {
		return readSignedVarInt();
	}

	/**
	 * Reads a ZigZag-encoded 64-bit signed long.
	 *
	 * @return the decoded signed long.
	 * @throws IOException if an I/O error occurs or the VarLong is malformed.
	 */
	public default long readSignedVarLong() throws IOException {
		return VarInt.decodeZigZag64(readVarLong());
	}

	/**
	 * Reads a ZigZag-encoded 64-bit signed long. Alias for {@link #readSignedVarLong()}.
	 *
	 * @return the decoded signed long.
	 * @throws IOException if an I/O error occurs or the VarLong is malformed.
	 */
	public default long readZigZagVarLong() throws IOException {
		return readSignedVarLong();
	}

	/**
	 * Reads an unsigned byte from the input. Promotes type to avoid overflow.
	 *
	 * @return a short with the value of the unsigned byte.
	 * @throws IOException
	 * @see {@link #readSUByte()} for overflow safe reads.
	 */
	public default short readUByte() throws IOException {
		return (short) Byte.toUnsignedInt(readByte());
	}

	/**
	 * Reads an unsigned byte from the input. Throws exceptions on overflow.
	 *
	 * @return an unsigned byte.
	 * @throws UnsafeUnsignedException if the value would trigger an overflow.
	 * @throws IOException
	 * @see {@link #readByte()} for overflow unsafe reads.
	 */
	public default byte readSUByte() throws IOException {
		byte b = readByte();
		if (b < 0)
			throw new UnsafeUnsignedException(byte.class);
		return b;
	}

	/**
	 * Reads an unsigned short from the input. Promotes type to avoid overflow.
	 *
	 * @return an int with the value of the unsigned short.
	 * @throws IOException
	 * @see {@link #readSUShort()} for overflow safe reads.
	 */
	public default int readUShort() throws IOException {
		return Short.toUnsignedInt(readShort());
	}

	/**
	 * Reads an unsigned short from the input. Throws exceptions on overflow.
	 *
	 * @return an unsigned short.
	 * @throws UnsafeUnsignedException if the value would trigger an overflow.
	 * @throws IOException
	 * @see {@link #readShort()} for overflow unsafe reads.
	 */
	public default short readSUShort() throws IOException {
		short s = readShort();
		if (s < 0)
			throw new UnsafeUnsignedException(short.class);
		return s;
	}

	/**
	 * Reads an unsigned int from the input. Promotes type to avoid overflow.
	 *
	 * @return a long with the value of the unsigned int.
	 * @throws IOException
	 * @see {@link #readSUInt()} for overflow safe reads.
	 */
	public default long readUInt() throws IOException {
		return Integer.toUnsignedLong(readInt());
	}

	/**
	 * Reads an unsigned int from the input. Throws exceptions on overflow.
	 *
	 * @return an unsigned int.
	 * @throws UnsafeUnsignedException if the value would trigger an overflow.
	 * @throws IOException
	 * @see {@link #readInt()} for overflow unsafe reads.
	 */
	public default int readSUInt() throws IOException {
		int i = readInt();
		if (i < 0)
			throw new UnsafeUnsignedException(int.class);
		return i;
	}

	/**
	 * Reads an unsigned long from the input. Throws exceptions on overflow.
	 *
	 * @return an unsigned long.
	 * @throws UnsafeUnsignedException if the value would trigger an overflow.
	 * @throws IOException
	 * @see {@link #readLong()} for overflow unsafe reads.
	 */
	public default long readSULong() throws IOException {
		long l = readLong();
		if (l < 0)
			throw new UnsafeUnsignedException(long.class);
		return l;
	}

	/**
	 * Reads <code>length</code> bytes from the input.
	 *
	 * @return bytes read
	 * @throws IOException
	 */
	public default byte[] read(int length) throws IOException {
		byte[] buffer = new byte[length];
		int read = 0;
		while (read < length){
			int i = read(buffer, read, length - read);
			if (i == 0)
				throw new EOFException("Unexpected EOF.");
			read += i;
		}
		return buffer;
	}

	/**
	 * Fully reads bytes into the buffer from the input.
	 *
	 * @param buffer storage for read data.
	 * @return bytes read
	 * @throws IOException
	 */
	public default int read(byte[] buffer) throws IOException {
		return read(buffer, 0, buffer.length);
	}

	/**
	 * Reads <code>len</code> bytes from the input into the buffer starting at
	 * <code>off</code>.
	 *
	 * @param buffer storage for read data.
	 * @param off    offset in the buffer to write read data into.
	 * @param len    number of bytes to read.
	 * @return bytes read
	 * @throws IOException
	 */
	public int read(byte[] buffer, int off, int len) throws IOException;

	/**
	 * Get the default endianness.
	 *
	 * @return the default ednianness.
	 */
	Endian getEndian();

}

package dev.arkturus.owl;

import java.io.EOFException;
import java.io.IOException;
import java.util.Objects;

/**
 * Utility codecs and constants for variable-length integer encodings (VarInt, VarLong) and ZigZag representations.
 * <p>
 * <b>Wire Format:</b>
 * Uses the standard Protocol Buffers / LEB128 variable-length integer format. Each byte except the last has its
 * most significant bit (MSB, {@code 0x80}) set as a continuation indicator, while the lower 7 bits carry the payload.
 * Chunks are serialized least significant 7 bits first.
 * <p>
 * <b>ZigZag Encoding:</b>
 * Standard VarInt encodes negative numbers into their maximum byte length (5 bytes for 32-bit int, 10 bytes for 64-bit long)
 * due to sign-extension in two's complement. ZigZag encoding maps signed integers to small positive integers so that
 * numbers with small absolute values (both positive and negative) encode into 1 or 2 bytes.
 */
public final class VarInt {

	/**
	 * Maximum number of bytes required to encode a 32-bit integer in VarInt format.
	 */
	public static final int MAX_VARINT_BYTES = 5;

	/**
	 * Maximum number of bytes required to encode a 64-bit long in VarLong format.
	 */
	public static final int MAX_VARLONG_BYTES = 10;

	private VarInt() {
	}

	/**
	 * Encodes a 32-bit signed integer using ZigZag encoding.
	 * <p>
	 * Maps:
	 * <ul>
	 *   <li>{@code 0 -> 0}</li>
	 *   <li>{@code -1 -> 1}</li>
	 *   <li>{@code 1 -> 2}</li>
	 *   <li>{@code -2 -> 3}</li>
	 * </ul>
	 *
	 * @param n signed 32-bit integer.
	 * @return unsigned ZigZag-mapped 32-bit representation.
	 */
	public static int encodeZigZag32(int n) {
		return (n << 1) ^ (n >> 31);
	}

	/**
	 * Decodes a 32-bit signed integer from ZigZag encoding.
	 *
	 * @param n ZigZag-encoded 32-bit integer.
	 * @return original signed 32-bit integer.
	 */
	public static int decodeZigZag32(int n) {
		return (n >>> 1) ^ -(n & 1);
	}

	/**
	 * Encodes a 64-bit signed long using ZigZag encoding.
	 *
	 * @param n signed 64-bit long.
	 * @return unsigned ZigZag-mapped 64-bit representation.
	 */
	public static long encodeZigZag64(long n) {
		return (n << 1) ^ (n >> 63);
	}

	/**
	 * Decodes a 64-bit signed long from ZigZag encoding.
	 *
	 * @param n ZigZag-encoded 64-bit long.
	 * @return original signed 64-bit long.
	 */
	public static long decodeZigZag64(long n) {
		return (n >>> 1) ^ -(n & 1L);
	}

	/**
	 * Computes the number of bytes required to encode {@code value} as a 32-bit VarInt.
	 *
	 * @param value the integer value.
	 * @return the byte size (1 to 5).
	 */
	public static int computeVarIntSize(int value) {
		if ((value & (0xFFFFFFFF << 7)) == 0) {
			return 1;
		}
		if ((value & (0xFFFFFFFF << 14)) == 0) {
			return 2;
		}
		if ((value & (0xFFFFFFFF << 21)) == 0) {
			return 3;
		}
		if ((value & (0xFFFFFFFF << 28)) == 0) {
			return 4;
		}
		return 5;
	}

	/**
	 * Computes the number of bytes required to encode {@code value} as a 64-bit VarLong.
	 *
	 * @param value the long value.
	 * @return the byte size (1 to 10).
	 */
	public static int computeVarLongSize(long value) {
		if ((value & (0xFFFFFFFFFFFFFFFFL << 7)) == 0) {
			return 1;
		}
		if ((value & (0xFFFFFFFFFFFFFFFFL << 14)) == 0) {
			return 2;
		}
		if ((value & (0xFFFFFFFFFFFFFFFFL << 21)) == 0) {
			return 3;
		}
		if ((value & (0xFFFFFFFFFFFFFFFFL << 28)) == 0) {
			return 4;
		}
		if ((value & (0xFFFFFFFFFFFFFFFFL << 35)) == 0) {
			return 5;
		}
		if ((value & (0xFFFFFFFFFFFFFFFFL << 42)) == 0) {
			return 6;
		}
		if ((value & (0xFFFFFFFFFFFFFFFFL << 49)) == 0) {
			return 7;
		}
		if ((value & (0xFFFFFFFFFFFFFFFFL << 56)) == 0) {
			return 8;
		}
		if ((value & (0xFFFFFFFFFFFFFFFFL << 63)) == 0) {
			return 9;
		}
		return 10;
	}

	/**
	 * Encodes a 32-bit integer into {@code buffer} starting at {@code offset}.
	 *
	 * @param value  the integer value to encode.
	 * @param buffer the destination array.
	 * @param offset the starting offset in the array.
	 * @return number of bytes written (1 to 5).
	 */
	public static int writeVarInt(int value, byte[] buffer, int offset) {
		Objects.requireNonNull(buffer, "buffer cannot be null");
		int pos = offset;
		while ((value & ~0x7F) != 0) {
			buffer[pos++] = (byte) ((value & 0x7F) | 0x80);
			value >>>= 7;
		}
		buffer[pos++] = (byte) (value & 0x7F);
		return pos - offset;
	}

	/**
	 * Encodes a 64-bit long into {@code buffer} starting at {@code offset}.
	 *
	 * @param value  the long value to encode.
	 * @param buffer the destination array.
	 * @param offset the starting offset in the array.
	 * @return number of bytes written (1 to 10).
	 */
	public static int writeVarLong(long value, byte[] buffer, int offset) {
		Objects.requireNonNull(buffer, "buffer cannot be null");
		int pos = offset;
		while ((value & ~0x7FL) != 0) {
			buffer[pos++] = (byte) (((int) value & 0x7F) | 0x80);
			value >>>= 7;
		}
		buffer[pos++] = (byte) ((int) value & 0x7F);
		return pos - offset;
	}

	/**
	 * Encodes a 32-bit signed integer using ZigZag encoding into {@code buffer} starting at {@code offset}.
	 *
	 * @param value  the signed integer to encode.
	 * @param buffer the destination array.
	 * @param offset the starting offset in the array.
	 * @return number of bytes written.
	 */
	public static int writeZigZagVarInt(int value, byte[] buffer, int offset) {
		return writeVarInt(encodeZigZag32(value), buffer, offset);
	}

	/**
	 * Encodes a 64-bit signed long using ZigZag encoding into {@code buffer} starting at {@code offset}.
	 *
	 * @param value  the signed long to encode.
	 * @param buffer the destination array.
	 * @param offset the starting offset in the array.
	 * @return number of bytes written.
	 */
	public static int writeZigZagVarLong(long value, byte[] buffer, int offset) {
		return writeVarLong(encodeZigZag64(value), buffer, offset);
	}

}

package dev.arkturus.owlreader.test;

import org.junit.jupiter.api.Test;

import dev.arkturus.owl.Endian;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EndianTest {

	@Test
	public void testShortRoundTrip() {
		short[] testValues = { 0, 1, -1, 42, -42, Short.MAX_VALUE, Short.MIN_VALUE, (short) 0x1234, (short) 0x8000,
				(short) 0xFF80 };

		for (Endian endian : Endian.values()) {
			byte[] buffer = new byte[2];
			for (short val : testValues) {
				endian.writeShort(val, buffer, 0);
				short read = endian.readShort(buffer, 0);
				assertEquals(val, read, "Failed for " + endian + " short " + val);
			}
		}
	}

	@Test
	public void testIntRoundTrip() {
		int[] testValues = { 0, 1, -1, 42, -42, Integer.MAX_VALUE, Integer.MIN_VALUE, 0x12345678, 0x80000000,
				0x00800000, 0x00008000, 0x00000080 };

		for (Endian endian : Endian.values()) {
			byte[] buffer = new byte[4];
			for (int val : testValues) {
				endian.writeInt(val, buffer, 0);
				int read = endian.readInt(buffer, 0);
				assertEquals(val, read, "Failed for " + endian + " int 0x" + Integer.toHexString(val));
			}
		}
	}

	@Test
	public void testIntExactByteLayout() {
		byte[] buffer = new byte[4];

		Endian.BIG.writeInt(0x01020304, buffer, 0);
		assertArrayEquals(new byte[] { 0x01, 0x02, 0x03, 0x04 }, buffer, "Big Endian int byte order");

		Endian.LITTLE.writeInt(0x01020304, buffer, 0);
		assertArrayEquals(new byte[] { 0x04, 0x03, 0x02, 0x01 }, buffer, "Little Endian int byte order");
	}

	@Test
	public void testLongRoundTrip() {
		long[] testValues = { 0L, 1L, -1L, 42L, -42L, Long.MAX_VALUE, Long.MIN_VALUE, 0x123456789ABCDEF0L,
				0x0000000080000000L, 0x8000000000000000L, 0x0000008000000000L, 0x0102030405060708L };

		for (Endian endian : Endian.values()) {
			byte[] buffer = new byte[8];
			for (long val : testValues) {
				endian.writeLong(val, buffer, 0);
				long read = endian.readLong(buffer, 0);
				assertEquals(val, read, "Failed for " + endian + " long 0x" + Long.toHexString(val));
			}
		}
	}

	@Test
	public void testLongExactByteLayout() {
		byte[] buffer = new byte[8];

		Endian.BIG.writeLong(0x0102030405060708L, buffer, 0);
		assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }, buffer, "Big Endian long byte order");

		Endian.LITTLE.writeLong(0x0102030405060708L, buffer, 0);
		assertArrayEquals(new byte[] { 8, 7, 6, 5, 4, 3, 2, 1 }, buffer, "Little Endian long byte order");
	}

	@Test
	public void testLongHighBitByteSignExtension() {
		// When byte 4 (in BIG) or byte 3 (in LITTLE) has its MSB set (>= 0x80),
		// it must not sign-extend into upper 32 bits.
		byte[] bigBytes = new byte[] { 0x00, 0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x00, 0x00 };
		assertEquals(0x0000000080000000L, Endian.BIG.readLong(bigBytes, 0));

		byte[] littleBytes = new byte[] { 0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x00, 0x00, 0x00 };
		assertEquals(0x0000000080000000L, Endian.LITTLE.readLong(littleBytes, 0));
	}
}

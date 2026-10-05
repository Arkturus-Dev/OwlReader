package dev.arkturus.owlreader.test;

import org.junit.jupiter.api.Test;

import dev.arkturus.owl.VarInt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VarIntTest {

	@Test
	public void testZigZag32KnownMappings() {
		assertEquals(0, VarInt.encodeZigZag32(0));
		assertEquals(1, VarInt.encodeZigZag32(-1));
		assertEquals(2, VarInt.encodeZigZag32(1));
		assertEquals(3, VarInt.encodeZigZag32(-2));
		assertEquals(4, VarInt.encodeZigZag32(2));
		assertEquals(-2, VarInt.encodeZigZag32(Integer.MAX_VALUE)); // 0xFFFFFFFE
		assertEquals(-1, VarInt.encodeZigZag32(Integer.MIN_VALUE)); // 0xFFFFFFFF
	}

	@Test
	public void testZigZag32RoundTrip() {
		int[] values = {
			0, 1, -1, 2, -2, 42, -42, 127, -127, 128, -128,
			16383, -16383, 16384, -16384,
			2097151, -2097151,
			268435455, -268435455,
			Integer.MAX_VALUE, Integer.MIN_VALUE,
			Integer.MAX_VALUE - 1, Integer.MIN_VALUE + 1
		};
		for (int val : values) {
			int encoded = VarInt.encodeZigZag32(val);
			int decoded = VarInt.decodeZigZag32(encoded);
			assertEquals(val, decoded, "Failed ZigZag32 roundtrip for: " + val);
		}
	}

	@Test
	public void testZigZag64KnownMappings() {
		assertEquals(0L, VarInt.encodeZigZag64(0L));
		assertEquals(1L, VarInt.encodeZigZag64(-1L));
		assertEquals(2L, VarInt.encodeZigZag64(1L));
		assertEquals(3L, VarInt.encodeZigZag64(-2L));
		assertEquals(4L, VarInt.encodeZigZag64(2L));
		assertEquals(-2L, VarInt.encodeZigZag64(Long.MAX_VALUE)); // 0xFFFFFFFFFFFFFFFEL
		assertEquals(-1L, VarInt.encodeZigZag64(Long.MIN_VALUE)); // 0xFFFFFFFFFFFFFFFFL
	}

	@Test
	public void testZigZag64RoundTrip() {
		long[] values = {
			0L, 1L, -1L, 2L, -2L, 42L, -42L, 127L, -127L, 128L, -128L,
			16383L, -16383L, 16384L, -16384L,
			(1L << 21) - 1, -(1L << 21),
			(1L << 28) - 1, -(1L << 28),
			(1L << 35) - 1, -(1L << 35),
			(1L << 42) - 1, -(1L << 42),
			(1L << 49) - 1, -(1L << 49),
			(1L << 56) - 1, -(1L << 56),
			(1L << 62) - 1, -(1L << 62),
			Long.MAX_VALUE, Long.MIN_VALUE,
			Long.MAX_VALUE - 1, Long.MIN_VALUE + 1
		};
		for (long val : values) {
			long encoded = VarInt.encodeZigZag64(val);
			long decoded = VarInt.decodeZigZag64(encoded);
			assertEquals(val, decoded, "Failed ZigZag64 roundtrip for: " + val);
		}
	}

	@Test
	public void testComputeVarIntSize() {
		assertEquals(1, VarInt.computeVarIntSize(0));
		assertEquals(1, VarInt.computeVarIntSize(1));
		assertEquals(1, VarInt.computeVarIntSize(127));
		assertEquals(2, VarInt.computeVarIntSize(128));
		assertEquals(2, VarInt.computeVarIntSize(16383));
		assertEquals(3, VarInt.computeVarIntSize(16384));
		assertEquals(3, VarInt.computeVarIntSize(2097151));
		assertEquals(4, VarInt.computeVarIntSize(2097152));
		assertEquals(4, VarInt.computeVarIntSize(268435455));
		assertEquals(5, VarInt.computeVarIntSize(268435456));
		assertEquals(5, VarInt.computeVarIntSize(Integer.MAX_VALUE));
		assertEquals(5, VarInt.computeVarIntSize(-1));
		assertEquals(5, VarInt.computeVarIntSize(Integer.MIN_VALUE));
	}

	@Test
	public void testComputeVarLongSize() {
		assertEquals(1, VarInt.computeVarLongSize(0L));
		assertEquals(1, VarInt.computeVarLongSize(127L));
		assertEquals(2, VarInt.computeVarLongSize(128L));
		assertEquals(2, VarInt.computeVarLongSize(16383L));
		assertEquals(3, VarInt.computeVarLongSize(16384L));
		assertEquals(4, VarInt.computeVarLongSize(1L << 21));
		assertEquals(5, VarInt.computeVarLongSize(1L << 28));
		assertEquals(6, VarInt.computeVarLongSize(1L << 35));
		assertEquals(7, VarInt.computeVarLongSize(1L << 42));
		assertEquals(8, VarInt.computeVarLongSize(1L << 49));
		assertEquals(9, VarInt.computeVarLongSize(1L << 56));
		assertEquals(9, VarInt.computeVarLongSize(Long.MAX_VALUE)); // 63 bits -> 9 bytes
		assertEquals(10, VarInt.computeVarLongSize(-1L));
		assertEquals(10, VarInt.computeVarLongSize(Long.MIN_VALUE));
	}

	@Test
	public void testKnownProtobufVectors() {
		// Vector 0 -> [0x00]
		byte[] buf = new byte[10];
		int len = VarInt.writeVarInt(0, buf, 0);
		assertEquals(1, len);
		assertEquals((byte) 0x00, buf[0]);

		// Vector 1 -> [0x01]
		len = VarInt.writeVarInt(1, buf, 0);
		assertEquals(1, len);
		assertEquals((byte) 0x01, buf[0]);

		// Vector 127 -> [0x7F]
		len = VarInt.writeVarInt(127, buf, 0);
		assertEquals(1, len);
		assertEquals((byte) 0x7F, buf[0]);

		// Vector 128 -> [0x80, 0x01]
		len = VarInt.writeVarInt(128, buf, 0);
		assertEquals(2, len);
		assertEquals((byte) 0x80, buf[0]);
		assertEquals((byte) 0x01, buf[1]);

		// Vector 300 -> [0xAC, 0x02]
		len = VarInt.writeVarInt(300, buf, 0);
		assertEquals(2, len);
		assertEquals((byte) 0xAC, buf[0]);
		assertEquals((byte) 0x02, buf[1]);

		// Vector 16384 -> [0x80, 0x80, 0x01]
		len = VarInt.writeVarInt(16384, buf, 0);
		assertEquals(3, len);
		assertEquals((byte) 0x80, buf[0]);
		assertEquals((byte) 0x80, buf[1]);
		assertEquals((byte) 0x01, buf[2]);

		// Raw -1 -> [0xFF, 0xFF, 0xFF, 0xFF, 0x0F]
		len = VarInt.writeVarInt(-1, buf, 0);
		assertEquals(5, len);
		assertArrayEquals(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x0F },
				java.util.Arrays.copyOf(buf, 5));

		// ZigZag -1 -> [0x01]
		len = VarInt.writeZigZagVarInt(-1, buf, 0);
		assertEquals(1, len);
		assertEquals((byte) 0x01, buf[0]);

		// ZigZag 1 -> [0x02]
		len = VarInt.writeZigZagVarInt(1, buf, 0);
		assertEquals(1, len);
		assertEquals((byte) 0x02, buf[0]);

		// ZigZag -2 -> [0x03]
		len = VarInt.writeZigZagVarInt(-2, buf, 0);
		assertEquals(1, len);
		assertEquals((byte) 0x03, buf[0]);
	}

	@Test
	public void testWriteZigZagVarLongVectors() {
		byte[] buf = new byte[10];
		int len = VarInt.writeZigZagVarLong(-1L, buf, 0);
		assertEquals(1, len);
		assertEquals((byte) 0x01, buf[0]);

		len = VarInt.writeZigZagVarLong(1L, buf, 0);
		assertEquals(1, len);
		assertEquals((byte) 0x02, buf[0]);

		len = VarInt.writeZigZagVarLong(-2L, buf, 0);
		assertEquals(1, len);
		assertEquals((byte) 0x03, buf[0]);
	}

}

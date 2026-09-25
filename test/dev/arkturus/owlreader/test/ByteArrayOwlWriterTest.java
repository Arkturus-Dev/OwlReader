package dev.arkturus.owlreader.test;

import org.junit.jupiter.api.Test;

import dev.arkturus.owl.Endian;
import dev.arkturus.owl.UnsafeUnsignedException;
import dev.arkturus.owlwriter.ByteArrayOwlWriter;

import java.io.EOFException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ByteArrayOwlWriterTest {

	@Test
	public void testWritePrimitivesBigEndian() throws Exception {
		byte[] data = new byte[32];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data, Endian.BIG);

		writer.writeByte((byte) 0x12);
		writer.writeShort((short) 0x3456);
		writer.writeInt(0x789ABCDE);
		writer.writeLong(0x1122334455667788L);
		writer.writeFloat(1.5f);
		writer.writeDouble(2.5);

		assertEquals((byte) 0x12, data[0]);
		assertEquals((byte) 0x34, data[1]);
		assertEquals((byte) 0x56, data[2]);
		assertEquals((byte) 0x78, data[3]);
		assertEquals((byte) 0x9A, data[4]);
		assertEquals((byte) 0xBC, data[5]);
		assertEquals((byte) 0xDE, data[6]);
	}

	@Test
	public void testWritePrimitivesLittleEndian() throws Exception {
		byte[] data = new byte[32];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data, Endian.LITTLE);

		writer.writeShort((short) 0x1234);
		writer.writeInt(0x01020304);

		assertEquals((byte) 0x34, data[0]);
		assertEquals((byte) 0x12, data[1]);
		assertEquals((byte) 0x04, data[2]);
		assertEquals((byte) 0x03, data[3]);
		assertEquals((byte) 0x02, data[4]);
		assertEquals((byte) 0x01, data[5]);
	}

	@Test
	public void testWriteUnsigned() throws Exception {
		byte[] data = new byte[16];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data, Endian.BIG);

		writer.writeUByte((short) 250);
		writer.writeUShort(60000);
		writer.writeUInt(4000000000L);

		assertEquals((byte) 250, data[0]);
	}

	@Test
	public void testSafeUnsignedExceptions() {
		byte[] data = new byte[16];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data);

		assertThrows(UnsafeUnsignedException.class, () -> writer.writeSUByte((short) 128));
		assertThrows(UnsafeUnsignedException.class, () -> writer.writeSUShort(32768));
		assertThrows(UnsafeUnsignedException.class, () -> writer.writeSUInt(2147483648L));
	}

	@Test
	public void testWriteStrings() throws Exception {
		byte[] data = new byte[128];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data);

		writer.writeString("Hello");
		writer.writeNullString("World");
		writer.writePascalByte("PByte");
		writer.writePascalShort("PShort");
		writer.writePascalInt("PInt");
		writer.writeLine("Line");

		String asStr = writer.asString(StandardCharsets.UTF_8);
		assertTrue(asStr.startsWith("Hello"));
		assertTrue(asStr.contains("World"));
		assertTrue(asStr.contains("Line"));
	}

	@Test
	public void testEOFWhenFull() throws Exception {
		byte[] data = new byte[2];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data);

		writer.writeByte((byte) 1);
		writer.writeByte((byte) 2);
		assertThrows(EOFException.class, () -> writer.writeByte((byte) 3));
	}

	@Test
	public void testInvalidWriteBounds() {
		byte[] data = new byte[10];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data);

		byte[] src = new byte[5];
		assertThrows(IndexOutOfBoundsException.class, () -> writer.write(src, -1, 2));
		assertThrows(IndexOutOfBoundsException.class, () -> writer.write(src, 1, -1));
		assertThrows(IndexOutOfBoundsException.class, () -> writer.write(src, 3, 4));
	}

	@Test
	public void testThreadSafeWritesAndAsString() throws Exception {
		byte[] data = new byte[1000];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data);

		int threadCount = 10;
		int writesPerThread = 100;
		Thread[] threads = new Thread[threadCount];

		for (int t = 0; t < threadCount; t++) {
			threads[t] = new Thread(() -> {
				for (int i = 0; i < writesPerThread; i++) {
					try {
						writer.writeByte((byte) 'A');
					} catch (Exception e) {
						fail(e);
					}
				}
			});
		}

		for (Thread t : threads) t.start();
		for (Thread t : threads) t.join();

		assertEquals(1000, writer.position());
		assertEquals("A".repeat(1000), writer.asString(StandardCharsets.UTF_8));
	}
}

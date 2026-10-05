package dev.arkturus.owlreader.test;

import org.junit.jupiter.api.Test;

import dev.arkturus.owlreader.ByteArrayOwlReader;
import dev.arkturus.owlreader.CRCByteArrayOwlReader;
import dev.arkturus.owlwriter.ByteArrayOwlWriter;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;

public class CRCByteArrayOwlReaderTest {

	@Test
	public void testCRCPrimitives() throws Exception {
		byte[] data = new byte[32];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data);
		writer.writeByte((byte) 0x12);
		writer.writeShort((short) 0x3456);
		writer.writeInt(0x789ABCDE);
		writer.writeLong(0x1122334455667788L);

		CRC32 expectedCrc = new CRC32();
		expectedCrc.update(data, 0, 1 + 2 + 4 + 8);

		ByteArrayOwlReader baseReader = new ByteArrayOwlReader(data);
		CRCByteArrayOwlReader crcReader = new CRCByteArrayOwlReader(baseReader);

		assertEquals((byte) 0x12, crcReader.readByte());
		assertEquals((short) 0x3456, crcReader.readShort());
		assertEquals(0x789ABCDE, crcReader.readInt());
		assertEquals(0x1122334455667788L, crcReader.readLong());

		assertEquals((int) expectedCrc.getValue(), crcReader.getCRC());
		// getCRC resets the crc
		assertEquals(0, crcReader.getCRC());
	}

	@Test
	public void testCRCStringsAndLines() throws Exception {
		byte[] data = new byte[128];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data);
		writer.writeString("Hello");
		writer.writeNullString("World");
		writer.writePascalByte("PByte");
		writer.writePascalShort("PShort");
		writer.writePascalInt("PInt");
		writer.writeLine("Line1");

		int totalBytes = writer.position();
		CRC32 expectedCrc = new CRC32();
		expectedCrc.update(data, 0, totalBytes);

		ByteArrayOwlReader baseReader = new ByteArrayOwlReader(data);
		CRCByteArrayOwlReader crcReader = new CRCByteArrayOwlReader(baseReader);

		assertEquals("Hello", crcReader.readString(5));
		assertEquals("World", crcReader.readNullString());
		assertEquals("PByte", crcReader.readPascalByte());
		assertEquals("PShort", crcReader.readPascalShort());
		assertEquals("PInt", crcReader.readPascalInt());
		assertEquals("Line1", crcReader.readLine());

		assertEquals((int) expectedCrc.getValue(), crcReader.getCRC());
	}

	@Test
	public void testThreadSafeCRCReads() throws Exception {
		byte[] data = new byte[1000];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) (i & 0xFF);
		}

		ByteArrayOwlReader baseReader = new ByteArrayOwlReader(data);
		CRCByteArrayOwlReader crcReader = new CRCByteArrayOwlReader(baseReader);

		int threadCount = 10;
		int readsPerThread = 100;
		Thread[] threads = new Thread[threadCount];

		for (int t = 0; t < threadCount; t++) {
			threads[t] = new Thread(() -> {
				for (int i = 0; i < readsPerThread; i++) {
					try {
						crcReader.readByte();
					} catch (Exception e) {
						fail(e);
					}
				}
			});
		}

		for (Thread t : threads) t.start();
		for (Thread t : threads) t.join();

		assertEquals(1000, crcReader.position());
		CRC32 expectedCrc = new CRC32();
		expectedCrc.update(data, 0, 1000);
		assertEquals((int) expectedCrc.getValue(), crcReader.getCRC());
	}

	@Test
	public void testCRCInt24() throws Exception {
		byte[] data = new byte[16];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data);
		writer.writeInt24(0x123456);
		writer.writeUInt24(0xAABBCC);

		CRC32 expectedCrc = new CRC32();
		expectedCrc.update(data, 0, 6);

		ByteArrayOwlReader baseReader = new ByteArrayOwlReader(data);
		CRCByteArrayOwlReader crcReader = new CRCByteArrayOwlReader(baseReader);

		assertEquals(0x123456, crcReader.readInt24());
		assertEquals(0xAABBCC, crcReader.readUInt24());

		assertEquals((int) expectedCrc.getValue(), crcReader.getCRC());
	}
}

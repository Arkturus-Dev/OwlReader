package dev.arkturus.owlreader.test;

import org.junit.jupiter.api.Test;

import dev.arkturus.owl.Endian;
import dev.arkturus.owl.UnsafeUnsignedException;
import dev.arkturus.owlreader.ByteArrayOwlReader;
import dev.arkturus.owlwriter.ByteArrayOwlWriter;

import java.io.EOFException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ByteArrayOwlReaderTest {

	@Test
	public void testReadPrimitivesBigEndian() throws Exception {
		byte[] data = new byte[32];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data, Endian.BIG);
		writer.writeByte((byte) 0x12);
		writer.writeShort((short) 0x3456);
		writer.writeInt(0x789ABCDE);
		writer.writeLong(0x1122334455667788L);
		writer.writeFloat(3.14f);
		writer.writeDouble(2.71828);

		ByteArrayOwlReader reader = new ByteArrayOwlReader(data, Endian.BIG);
		assertEquals((byte) 0x12, reader.readByte());
		assertEquals((short) 0x3456, reader.readShort());
		assertEquals(0x789ABCDE, reader.readInt());
		assertEquals(0x1122334455667788L, reader.readLong());
		assertEquals(3.14f, reader.readFloat(), 0.0001f);
		assertEquals(2.71828, reader.readDouble(), 0.00001);
	}

	@Test
	public void testReadPrimitivesLittleEndian() throws Exception {
		byte[] data = new byte[16];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data, Endian.LITTLE);
		writer.writeShort((short) 0x1234);
		writer.writeInt(0x01020304);
		writer.writeLong(0x0102030405060708L);

		ByteArrayOwlReader reader = new ByteArrayOwlReader(data, Endian.LITTLE);
		assertEquals((short) 0x1234, reader.readShort());
		assertEquals(0x01020304, reader.readInt());
		assertEquals(0x0102030405060708L, reader.readLong());
	}

	@Test
	public void testReadUnsigned() throws Exception {
		byte[] data = new byte[16];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data);
		writer.writeUByte((short) 200);
		writer.writeUShort(50000);
		writer.writeUInt(4000000000L);

		ByteArrayOwlReader reader = new ByteArrayOwlReader(data);
		assertEquals((short) 200, reader.readUByte());
		assertEquals(50000, reader.readUShort());
		assertEquals(4000000000L, reader.readUInt());
	}

	@Test
	public void testReadSUOverflow() throws Exception {
		byte[] data = new byte[16];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data);
		writer.writeUByte((short) 200);
		writer.writeUShort(50000);
		writer.writeUInt(4000000000L);

		ByteArrayOwlReader reader = new ByteArrayOwlReader(data);
		assertThrows(UnsafeUnsignedException.class, reader::readSUByte);
		assertThrows(UnsafeUnsignedException.class, reader::readSUShort);
		assertThrows(UnsafeUnsignedException.class, reader::readSUInt);
	}

	@Test
	public void testReadStringVariants() throws Exception {
		byte[] data = new byte[128];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data);
		writer.writeString("Fixed");
		writer.writeNullString("NullTerm");
		writer.writePascalByte("PByte");
		writer.writePascalShort("PShort");
		writer.writePascalInt("PInt");

		ByteArrayOwlReader reader = new ByteArrayOwlReader(data);
		assertEquals("Fixed", reader.readString(5));
		assertEquals("NullTerm", reader.readNullString());
		assertEquals("PByte", reader.readPascalByte());
		assertEquals("PShort", reader.readPascalShort());
		assertEquals("PInt", reader.readPascalInt());
	}

	@Test
	public void testReadLine() throws Exception {
		byte[] data = "first line\r\nsecond line\nthird line".getBytes(StandardCharsets.UTF_8);
		ByteArrayOwlReader reader = new ByteArrayOwlReader(data);

		assertEquals("first line", reader.readLine());
		assertEquals("second line", reader.readLine());
		assertEquals("third line", reader.readLine());
	}

	@Test
	public void testEmptyLines() throws Exception {
		byte[] data = "\n\r\n".getBytes(StandardCharsets.UTF_8);
		ByteArrayOwlReader reader = new ByteArrayOwlReader(data);

		assertEquals("", reader.readLine());
		assertEquals("", reader.readLine());
		assertFalse(reader.hasData());
	}

	@Test
	public void testReadBytesWithOffset() throws Exception {
		byte[] data = new byte[] { 10, 20, 30, 40 };
		ByteArrayOwlReader reader = new ByteArrayOwlReader(data);

		byte[] dest = new byte[6];
		int readCount = reader.read(dest, 1, 3);
		assertEquals(3, readCount);
		assertArrayEquals(new byte[] { 0, 10, 20, 30, 0, 0 }, dest);
		assertEquals(3, reader.position());
	}

	@Test
	public void testSkipAndPosition() throws Exception {
		byte[] data = new byte[] { 1, 2, 3, 4, 5 };
		ByteArrayOwlReader reader = new ByteArrayOwlReader(data);

		assertEquals(0, reader.position());
		assertTrue(reader.hasData());
		reader.skipBytes(2);
		assertEquals(2, reader.position());
		assertEquals((byte) 3, reader.readByte());
		assertEquals(3, reader.position());
	}

	@Test
	public void testEOFExceptions() {
		byte[] data = new byte[] { 1 };
		ByteArrayOwlReader reader = new ByteArrayOwlReader(data);

		assertDoesNotThrow(() -> reader.readByte());
		assertThrows(EOFException.class, reader::readByte);
		assertThrows(EOFException.class, reader::readShort);
		assertThrows(EOFException.class, reader::readInt);
		assertThrows(EOFException.class, reader::readLong);
	}

	@Test
	public void testInvalidReadBounds() {
		byte[] data = new byte[] { 1, 2, 3, 4 };
		ByteArrayOwlReader reader = new ByteArrayOwlReader(data);
		byte[] dest = new byte[6];

		assertThrows(IndexOutOfBoundsException.class, () -> reader.read(dest, -1, 3));
		assertThrows(IndexOutOfBoundsException.class, () -> reader.read(dest, 1, -1));
		assertThrows(IndexOutOfBoundsException.class, () -> reader.read(dest, 4, 3));
	}

	@Test
	public void testNegativeSkipBytes() {
		byte[] data = new byte[] { 1, 2, 3 };
		ByteArrayOwlReader reader = new ByteArrayOwlReader(data);
		assertThrows(IndexOutOfBoundsException.class, () -> reader.skipBytes(-1));
	}

	@Test
	public void testNegativeReadString() {
		byte[] data = new byte[] { 1, 2, 3 };
		ByteArrayOwlReader reader = new ByteArrayOwlReader(data);
		assertThrows(IndexOutOfBoundsException.class, () -> reader.readString(-1));
	}
}

package dev.arkturus.owlreader.test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arkturus.owl.Endian;
import dev.arkturus.owl.UnsafeUnsignedException;
import dev.arkturus.owlreader.OwlReader;
import dev.arkturus.owlreader.test.OwlReaderTestFixtures.ReaderFactory;
import dev.arkturus.owlwriter.ByteArrayOwlWriter;

import static org.junit.jupiter.api.Assertions.*;

public class OwlReaderPrimitivesTest {

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testReadPrimitivesBigEndian(ReaderFactory factory) throws Exception {
		byte[] data = new byte[32];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data, Endian.BIG);
		writer.writeByte((byte) 0x12);
		writer.writeShort((short) 0x3456);
		writer.writeInt(0x789ABCDE);
		writer.writeLong(0x1122334455667788L);
		writer.writeFloat(3.14f);
		writer.writeDouble(2.71828);

		OwlReader reader = factory.create(data, Endian.BIG);
		assertEquals((byte) 0x12, reader.readByte());
		assertEquals((short) 0x3456, reader.readShort());
		assertEquals(0x789ABCDE, reader.readInt());
		assertEquals(0x1122334455667788L, reader.readLong());
		assertEquals(3.14f, reader.readFloat(), 0.0001f);
		assertEquals(2.71828, reader.readDouble(), 0.00001);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testReadPrimitivesLittleEndian(ReaderFactory factory) throws Exception {
		byte[] data = new byte[32];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data, Endian.LITTLE);
		writer.writeShort((short) 0x1234);
		writer.writeInt(0x01020304);
		writer.writeLong(0x0102030405060708L);

		OwlReader reader = factory.create(data, Endian.LITTLE);
		assertEquals((short) 0x1234, reader.readShort());
		assertEquals(0x01020304, reader.readInt());
		assertEquals(0x0102030405060708L, reader.readLong());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testReadUnsigned(ReaderFactory factory) throws Exception {
		byte[] data = new byte[16];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data);
		writer.writeUByte((short) 200);
		writer.writeUShort(50000);
		writer.writeUInt(4000000000L);

		OwlReader reader = factory.create(data);
		assertEquals((short) 200, reader.readUByte());
		assertEquals(50000, reader.readUShort());
		assertEquals(4000000000L, reader.readUInt());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testReadSUOverflow(ReaderFactory factory) throws Exception {
		byte[] data = new byte[16];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data);
		writer.writeUByte((short) 200);
		writer.writeUShort(50000);
		writer.writeUInt(4000000000L);

		OwlReader reader = factory.create(data);
		assertThrows(UnsafeUnsignedException.class, reader::readSUByte);
		assertThrows(UnsafeUnsignedException.class, reader::readSUShort);
		assertThrows(UnsafeUnsignedException.class, reader::readSUInt);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testReadInt24BigEndian(ReaderFactory factory) throws Exception {
		byte[] data = new byte[16];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data, Endian.BIG);
		writer.writeInt24(0x123456);
		writer.writeInt24(-42);
		writer.writeInt24(8388607);
		writer.writeInt24(-8388608);

		OwlReader reader = factory.create(data, Endian.BIG);
		assertEquals(0x123456, reader.readInt24());
		assertEquals(-42, reader.readMedium());
		assertEquals(8388607, reader.readInt24());
		assertEquals(-8388608, reader.readMedium());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testReadInt24LittleEndian(ReaderFactory factory) throws Exception {
		byte[] data = new byte[16];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data, Endian.LITTLE);
		writer.writeInt24(0x123456);
		writer.writeInt24(-42);
		writer.writeInt24(8388607);
		writer.writeInt24(-8388608);

		OwlReader reader = factory.create(data, Endian.LITTLE);
		assertEquals(0x123456, reader.readInt24());
		assertEquals(-42, reader.readMedium());
		assertEquals(8388607, reader.readInt24());
		assertEquals(-8388608, reader.readMedium());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testReadUInt24(ReaderFactory factory) throws Exception {
		byte[] data = new byte[16];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data);
		writer.writeUInt24(0);
		writer.writeUMedium(0x800000);
		writer.writeUInt24(0xFFFFFF);

		OwlReader reader = factory.create(data);
		assertEquals(0, reader.readUInt24());
		assertEquals(0x800000, reader.readUMedium());
		assertEquals(0xFFFFFF, reader.readUnsignedInt24());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testReadSUInt24(ReaderFactory factory) throws Exception {
		byte[] data = new byte[16];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data);
		writer.writeSUInt24(8388607);
		writer.writeInt24(-1);

		OwlReader reader = factory.create(data);
		assertEquals(8388607, reader.readSUInt24());
		assertThrows(UnsafeUnsignedException.class, reader::readSUInt24);
	}
}

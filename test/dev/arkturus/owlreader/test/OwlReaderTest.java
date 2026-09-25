package dev.arkturus.owlreader.test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arkturus.owlreader.OwlReader;
import dev.arkturus.owlreader.test.OwlReaderTestFixtures.ReaderFactory;
import dev.arkturus.owlwriter.ByteArrayOwlWriter;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class OwlReaderTest {

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testCheckByte(ReaderFactory factory) throws Exception {
		byte[] data = new byte[] { 42 };
		OwlReader reader = factory.create(data);
		assertTrue(reader.checkByte((byte) 42));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testCheckShort(ReaderFactory factory) throws Exception {
		byte[] data = new byte[2];
		new ByteArrayOwlWriter(data).writeShort((short) 1000);
		OwlReader reader = factory.create(data);
		assertTrue(reader.checkShort((short) 1000));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testCheckInt(ReaderFactory factory) throws Exception {
		byte[] data = new byte[4];
		new ByteArrayOwlWriter(data).writeInt(123456);
		OwlReader reader = factory.create(data);
		assertTrue(reader.checkInt(123456));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testCheckLong(ReaderFactory factory) throws Exception {
		byte[] data = new byte[8];
		new ByteArrayOwlWriter(data).writeLong(9876543210L);
		OwlReader reader = factory.create(data);
		assertTrue(reader.checkLong(9876543210L));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testCheckFloat(ReaderFactory factory) throws Exception {
		byte[] data = new byte[4];
		new ByteArrayOwlWriter(data).writeFloat(1.23f);
		OwlReader reader = factory.create(data);
		assertTrue(reader.checkFloat(1.23f));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testCheckDouble(ReaderFactory factory) throws Exception {
		byte[] data = new byte[8];
		new ByteArrayOwlWriter(data).writeDouble(4.56);
		OwlReader reader = factory.create(data);
		assertTrue(reader.checkDouble(4.56));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testCheckBytes(ReaderFactory factory) throws Exception {
		byte[] data = new byte[] { 1, 2, 3, 4, 5 };
		OwlReader reader = factory.create(data);

		byte[] expected = new byte[] { 1, 2, 3, 4, 5 };
		assertTrue(reader.checkBytes(expected));
		assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, expected);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testCheckUnsigned(ReaderFactory factory) throws Exception {
		byte[] data = new byte[16];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data);
		writer.writeUByte((short) 200);
		writer.writeUShort(50000);
		writer.writeUInt(4000000000L);

		OwlReader reader = factory.create(data);
		assertTrue(reader.checkUByte(200));
		assertTrue(reader.checkUShort(50000));
		assertTrue(reader.checkUInt(4000000000L));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testToInputStream(ReaderFactory factory) throws Exception {
		byte[] data = new byte[] { 1, 2, (byte) 0xFF, 4, 5 };
		OwlReader reader = factory.create(data);
		InputStream in = reader.toInputStream();

		assertEquals(1, in.read());
		assertEquals(2, in.read());
		assertEquals(255, in.read());

		byte[] rest = new byte[2];
		int readCount = in.read(rest, 0, 2);
		assertEquals(2, readCount);
		assertArrayEquals(new byte[] { 4, 5 }, rest);

		assertEquals(-1, in.read());
		assertEquals(-1, in.read(rest, 0, 2));
	}
}

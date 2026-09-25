package dev.arkturus.owlreader.test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arkturus.owlreader.OwlReader;
import dev.arkturus.owlreader.test.OwlReaderTestFixtures.ReaderFactory;

import java.io.EOFException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class OwlReaderOperationsTest {

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testReadBytesWithOffset(ReaderFactory factory) throws Exception {
		byte[] data = new byte[] { 10, 20, 30, 40 };
		OwlReader reader = factory.create(data);

		byte[] dest = new byte[6];
		int readCount = reader.read(dest, 1, 3);
		assertEquals(3, readCount);
		assertArrayEquals(new byte[] { 0, 10, 20, 30, 0, 0 }, dest);
		assertEquals(3, reader.position());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testSkipAndPosition(ReaderFactory factory) throws Exception {
		byte[] data = new byte[] { 1, 2, 3, 4, 5 };
		OwlReader reader = factory.create(data);

		assertEquals(0, reader.position());
		assertTrue(reader.hasData());
		reader.skipBytes(2);
		assertEquals(2, reader.position());
		assertEquals((byte) 3, reader.readByte());
		assertEquals(3, reader.position());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testEOFExceptions(ReaderFactory factory) {
		byte[] data = new byte[] { 1 };
		OwlReader reader = factory.create(data);

		assertDoesNotThrow(() -> reader.readByte());
		assertThrows(EOFException.class, reader::readByte);
		assertThrows(EOFException.class, reader::readShort);
		assertThrows(EOFException.class, reader::readInt);
		assertThrows(EOFException.class, reader::readLong);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testInvalidReadBounds(ReaderFactory factory) {
		byte[] data = new byte[] { 1, 2, 3, 4 };
		OwlReader reader = factory.create(data);
		byte[] dest = new byte[6];

		assertThrows(IndexOutOfBoundsException.class, () -> reader.read(dest, -1, 3));
		assertThrows(IndexOutOfBoundsException.class, () -> reader.read(dest, 1, -1));
		assertThrows(IndexOutOfBoundsException.class, () -> reader.read(dest, 4, 3));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testNegativeSkipBytes(ReaderFactory factory) {
		byte[] data = new byte[] { 1, 2, 3 };
		OwlReader reader = factory.create(data);
		assertThrows(IndexOutOfBoundsException.class, () -> reader.skipBytes(-1));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testNegativeReadString(ReaderFactory factory) {
		byte[] data = new byte[] { 1, 2, 3 };
		OwlReader reader = factory.create(data);
		assertThrows(IndexOutOfBoundsException.class, () -> reader.readString(-1));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testToInputStream(ReaderFactory factory) throws Exception {
		byte[] data = new byte[] { 1, 2, 3, 4, 5 };
		OwlReader reader = factory.create(data);
		InputStream in = reader.toInputStream();

		assertEquals(1, in.read());
		byte[] buf = new byte[3];
		int count = in.read(buf, 0, 3);
		assertEquals(3, count);
		assertArrayEquals(new byte[] { 2, 3, 4 }, buf);
		assertEquals(5, in.read());
		assertEquals(-1, in.read());
	}
}

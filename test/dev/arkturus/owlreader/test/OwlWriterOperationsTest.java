package dev.arkturus.owlreader.test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arkturus.owlreader.test.OwlWriterTestFixtures.WriterFactory;
import dev.arkturus.owlreader.test.OwlWriterTestFixtures.WriterFixture;
import dev.arkturus.owlwriter.OwlWriter;

import static org.junit.jupiter.api.Assertions.*;

public class OwlWriterOperationsTest {

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlWriterTestFixtures#allWriters")
	public void testWriteBytesWithOffset(WriterFactory factory) throws Exception {
		WriterFixture fixture = factory.create(16);
		OwlWriter writer = fixture.writer();

		byte[] src = new byte[] { 10, 20, 30, 40, 50 };
		writer.write(src, 1, 3); // writes 20, 30, 40

		byte[] data = fixture.toByteArray();
		assertTrue(data.length >= 3);
		assertArrayEquals(new byte[] { 20, 30, 40 }, java.util.Arrays.copyOf(data, 3));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlWriterTestFixtures#allWriters")
	public void testInvalidWriteBounds(WriterFactory factory) {
		WriterFixture fixture = factory.create(16);
		OwlWriter writer = fixture.writer();

		byte[] src = new byte[5];
		assertThrows(IndexOutOfBoundsException.class, () -> writer.write(src, -1, 2));
		assertThrows(IndexOutOfBoundsException.class, () -> writer.write(src, 1, -1));
		assertThrows(IndexOutOfBoundsException.class, () -> writer.write(src, 3, 4));
	}
}

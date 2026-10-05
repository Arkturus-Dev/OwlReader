package dev.arkturus.owlreader.test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arkturus.owl.Endian;
import dev.arkturus.owl.UnsafeUnsignedException;
import dev.arkturus.owlreader.test.OwlWriterTestFixtures.WriterFactory;
import dev.arkturus.owlreader.test.OwlWriterTestFixtures.WriterFixture;
import dev.arkturus.owlwriter.OwlWriter;

import static org.junit.jupiter.api.Assertions.*;

public class OwlWriterPrimitivesTest {

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlWriterTestFixtures#allWriters")
	public void testWritePrimitivesBigEndian(WriterFactory factory) throws Exception {
		WriterFixture fixture = factory.create(32, Endian.BIG);
		OwlWriter writer = fixture.writer();

		writer.writeByte((byte) 0x12);
		writer.writeShort((short) 0x3456);
		writer.writeInt(0x789ABCDE);
		writer.writeLong(0x1122334455667788L);
		writer.writeFloat(1.5f);
		writer.writeDouble(2.5);

		byte[] data = fixture.toByteArray();
		assertEquals((byte) 0x12, data[0]);
		assertEquals((byte) 0x34, data[1]);
		assertEquals((byte) 0x56, data[2]);
		assertEquals((byte) 0x78, data[3]);
		assertEquals((byte) 0x9A, data[4]);
		assertEquals((byte) 0xBC, data[5]);
		assertEquals((byte) 0xDE, data[6]);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlWriterTestFixtures#allWriters")
	public void testWritePrimitivesLittleEndian(WriterFactory factory) throws Exception {
		WriterFixture fixture = factory.create(32, Endian.LITTLE);
		OwlWriter writer = fixture.writer();

		writer.writeShort((short) 0x1234);
		writer.writeInt(0x01020304);

		byte[] data = fixture.toByteArray();
		assertEquals((byte) 0x34, data[0]);
		assertEquals((byte) 0x12, data[1]);
		assertEquals((byte) 0x04, data[2]);
		assertEquals((byte) 0x03, data[3]);
		assertEquals((byte) 0x02, data[4]);
		assertEquals((byte) 0x01, data[5]);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlWriterTestFixtures#allWriters")
	public void testWriteUnsigned(WriterFactory factory) throws Exception {
		WriterFixture fixture = factory.create(16, Endian.BIG);
		OwlWriter writer = fixture.writer();

		writer.writeUByte((short) 250);
		writer.writeUShort(60000);
		writer.writeUInt(4000000000L);

		byte[] data = fixture.toByteArray();
		assertEquals((byte) 250, data[0]);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlWriterTestFixtures#allWriters")
	public void testSafeUnsignedExceptions(WriterFactory factory) {
		WriterFixture fixture = factory.create(16);
		OwlWriter writer = fixture.writer();

		assertThrows(UnsafeUnsignedException.class, () -> writer.writeSUByte((short) 128));
		assertThrows(UnsafeUnsignedException.class, () -> writer.writeSUShort(32768));
		assertThrows(UnsafeUnsignedException.class, () -> writer.writeSUInt(2147483648L));
		assertThrows(UnsafeUnsignedException.class, () -> writer.writeSUInt24(8388608));
		assertThrows(UnsafeUnsignedException.class, () -> writer.writeSUMedium(8388608));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlWriterTestFixtures#allWriters")
	public void testWriteInt24BigEndian(WriterFactory factory) throws Exception {
		WriterFixture fixture = factory.create(16, Endian.BIG);
		OwlWriter writer = fixture.writer();

		writer.writeInt24(0x123456);
		writer.writeMedium(0x789ABC);

		byte[] data = fixture.toByteArray();
		assertEquals((byte) 0x12, data[0]);
		assertEquals((byte) 0x34, data[1]);
		assertEquals((byte) 0x56, data[2]);
		assertEquals((byte) 0x78, data[3]);
		assertEquals((byte) 0x9A, data[4]);
		assertEquals((byte) 0xBC, data[5]);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlWriterTestFixtures#allWriters")
	public void testWriteInt24LittleEndian(WriterFactory factory) throws Exception {
		WriterFixture fixture = factory.create(16, Endian.LITTLE);
		OwlWriter writer = fixture.writer();

		writer.writeInt24(0x123456);

		byte[] data = fixture.toByteArray();
		assertEquals((byte) 0x56, data[0]);
		assertEquals((byte) 0x34, data[1]);
		assertEquals((byte) 0x12, data[2]);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlWriterTestFixtures#allWriters")
	public void testWriteUInt24(WriterFactory factory) throws Exception {
		WriterFixture fixture = factory.create(16, Endian.BIG);
		OwlWriter writer = fixture.writer();

		writer.writeUInt24(16777215);
		writer.writeUMedium(0x800000);

		byte[] data = fixture.toByteArray();
		assertEquals((byte) 0xFF, data[0]);
		assertEquals((byte) 0xFF, data[1]);
		assertEquals((byte) 0xFF, data[2]);
		assertEquals((byte) 0x80, data[3]);
		assertEquals((byte) 0x00, data[4]);
		assertEquals((byte) 0x00, data[5]);
	}
}

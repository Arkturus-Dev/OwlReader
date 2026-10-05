package dev.arkturus.owlreader.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.EOFException;
import java.io.IOException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arkturus.owl.Endian;
import dev.arkturus.owl.VarInt;
import dev.arkturus.owlreader.ByteArrayOwlReader;
import dev.arkturus.owlreader.OwlReader;
import dev.arkturus.owlreader.test.OwlWriterTestFixtures.WriterFactory;
import dev.arkturus.owlreader.test.OwlWriterTestFixtures.WriterFixture;
import dev.arkturus.owlwriter.OwlWriter;

public class OwlWriterVarIntTest {

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlWriterTestFixtures#allWriters")
	public void testWriteVarIntRoundTrip(WriterFactory factory) throws Exception {
		int[] values = {
			0, 1, 127, 128, 300, 16383, 16384, 2097151, 2097152,
			268435455, 268435456, Integer.MAX_VALUE, -1, Integer.MIN_VALUE
		};

		WriterFixture fixture = factory.create(128, Endian.BIG);
		try (OwlWriter writer = fixture.writer()) {
			for (int val : values) {
				writer.writeVarInt(val);
			}
		}

		byte[] bytes = fixture.toByteArray();
		try (OwlReader reader = new ByteArrayOwlReader(bytes)) {
			for (int val : values) {
				assertEquals(val, reader.readVarInt(), "Mismatch reading VarInt: " + val);
			}
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlWriterTestFixtures#allWriters")
	public void testWriteVarLongRoundTrip(WriterFactory factory) throws Exception {
		long[] values = {
			0L, 1L, 127L, 128L, 300L, 16383L, 16384L,
			1L << 21, 1L << 28, 1L << 35, 1L << 42, 1L << 49, 1L << 56,
			Long.MAX_VALUE, -1L, Long.MIN_VALUE
		};

		WriterFixture fixture = factory.create(256, Endian.BIG);
		try (OwlWriter writer = fixture.writer()) {
			for (long val : values) {
				writer.writeVarLong(val);
			}
		}

		byte[] bytes = fixture.toByteArray();
		try (OwlReader reader = new ByteArrayOwlReader(bytes)) {
			for (long val : values) {
				assertEquals(val, reader.readVarLong(), "Mismatch reading VarLong: " + val);
			}
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlWriterTestFixtures#allWriters")
	public void testWriteSignedVarIntAndZigZag(WriterFactory factory) throws Exception {
		int[] values = {
			0, -1, 1, -2, 2, -42, 42, -127, 127, -128, 128,
			-16384, 16384, Integer.MIN_VALUE, Integer.MAX_VALUE
		};

		WriterFixture fixture = factory.create(128, Endian.BIG);
		try (OwlWriter writer = fixture.writer()) {
			for (int val : values) {
				writer.writeSignedVarInt(val);
			}
		}

		byte[] bytes = fixture.toByteArray();
		try (OwlReader reader = new ByteArrayOwlReader(bytes)) {
			for (int val : values) {
				assertEquals(val, reader.readSignedVarInt(), "Mismatch reading signed VarInt: " + val);
			}
		}

		// Also test writeZigZagVarInt alias
		WriterFixture fixtureAlias = factory.create(128, Endian.BIG);
		try (OwlWriter writer = fixtureAlias.writer()) {
			for (int val : values) {
				writer.writeZigZagVarInt(val);
			}
		}

		byte[] bytesAlias = fixtureAlias.toByteArray();
		assertArrayEquals(bytes, bytesAlias, "writeZigZagVarInt produced different output than writeSignedVarInt");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlWriterTestFixtures#allWriters")
	public void testWriteSignedVarLongAndZigZag(WriterFactory factory) throws Exception {
		long[] values = {
			0L, -1L, 1L, -2L, 2L, -42L, 42L, -127L, 127L, -128L, 128L,
			-16384L, 16384L, -(1L << 35), 1L << 35, Long.MIN_VALUE, Long.MAX_VALUE
		};

		WriterFixture fixture = factory.create(256, Endian.BIG);
		try (OwlWriter writer = fixture.writer()) {
			for (long val : values) {
				writer.writeSignedVarLong(val);
			}
		}

		byte[] bytes = fixture.toByteArray();
		try (OwlReader reader = new ByteArrayOwlReader(bytes)) {
			for (long val : values) {
				assertEquals(val, reader.readSignedVarLong(), "Mismatch reading signed VarLong: " + val);
			}
		}

		// Also test writeZigZagVarLong alias
		WriterFixture fixtureAlias = factory.create(256, Endian.BIG);
		try (OwlWriter writer = fixtureAlias.writer()) {
			for (long val : values) {
				writer.writeZigZagVarLong(val);
			}
		}

		byte[] bytesAlias = fixtureAlias.toByteArray();
		assertArrayEquals(bytes, bytesAlias, "writeZigZagVarLong produced different output than writeSignedVarLong");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlWriterTestFixtures#allWriters")
	public void testCompactSignedEncodingSizes(WriterFactory factory) throws Exception {
		// Verify ZigZag small negative numbers are exactly 1 byte on the wire
		WriterFixture fixture = factory.create(4, Endian.BIG);
		try (OwlWriter writer = fixture.writer()) {
			writer.writeSignedVarInt(-1);
			writer.writeSignedVarInt(1);
			writer.writeSignedVarInt(-2);
			writer.writeSignedVarInt(2);
		}

		byte[] bytes = fixture.toByteArray();
		assertEquals(4, bytes.length, "Four small signed integers should take exactly 4 bytes in ZigZag encoding");
		assertArrayEquals(new byte[] { 0x01, 0x02, 0x03, 0x04 }, bytes);
	}

}

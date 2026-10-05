package dev.arkturus.owlreader.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arkturus.owl.Endian;
import dev.arkturus.owl.VarInt;
import dev.arkturus.owlreader.OwlReader;
import dev.arkturus.owlreader.test.OwlReaderTestFixtures.ReaderFactory;
import dev.arkturus.owlwriter.ByteArrayOwlWriter;

public class OwlReaderVarIntTest {

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testReadVarInt(ReaderFactory factory) throws Exception {
		int[] values = {
			0, 1, 127, 128, 300, 16383, 16384, 2097151, 2097152,
			268435455, 268435456, Integer.MAX_VALUE, -1, Integer.MIN_VALUE
		};

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (int val : values) {
			byte[] buf = new byte[VarInt.computeVarIntSize(val)];
			VarInt.writeVarInt(val, buf, 0);
			baos.write(buf);
		}

		byte[] data = baos.toByteArray();
		try (OwlReader reader = factory.create(data, Endian.BIG)) {
			for (int val : values) {
				assertEquals(val, reader.readVarInt(), "Mismatch reading VarInt: " + val);
			}
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testReadVarLong(ReaderFactory factory) throws Exception {
		long[] values = {
			0L, 1L, 127L, 128L, 300L, 16383L, 16384L,
			1L << 21, 1L << 28, 1L << 35, 1L << 42, 1L << 49, 1L << 56,
			Long.MAX_VALUE, -1L, Long.MIN_VALUE
		};

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (long val : values) {
			byte[] buf = new byte[VarInt.computeVarLongSize(val)];
			VarInt.writeVarLong(val, buf, 0);
			baos.write(buf);
		}

		byte[] data = baos.toByteArray();
		try (OwlReader reader = factory.create(data, Endian.BIG)) {
			for (long val : values) {
				assertEquals(val, reader.readVarLong(), "Mismatch reading VarLong: " + val);
			}
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testReadSignedVarIntAndZigZag(ReaderFactory factory) throws Exception {
		int[] values = {
			0, -1, 1, -2, 2, -42, 42, -127, 127, -128, 128,
			-16384, 16384, Integer.MIN_VALUE, Integer.MAX_VALUE
		};

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (int val : values) {
			byte[] buf = new byte[5];
			int len = VarInt.writeZigZagVarInt(val, buf, 0);
			baos.write(buf, 0, len);
		}

		byte[] data = baos.toByteArray();
		try (OwlReader reader = factory.create(data, Endian.BIG)) {
			for (int val : values) {
				assertEquals(val, reader.readSignedVarInt(), "Mismatch reading signed VarInt: " + val);
			}
		}

		// Also verify readZigZagVarInt alias
		try (OwlReader reader = factory.create(data, Endian.BIG)) {
			for (int val : values) {
				assertEquals(val, reader.readZigZagVarInt(), "Mismatch reading zigZag VarInt: " + val);
			}
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testReadSignedVarLongAndZigZag(ReaderFactory factory) throws Exception {
		long[] values = {
			0L, -1L, 1L, -2L, 2L, -42L, 42L, -127L, 127L, -128L, 128L,
			-16384L, 16384L, -(1L << 35), 1L << 35, Long.MIN_VALUE, Long.MAX_VALUE
		};

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (long val : values) {
			byte[] buf = new byte[10];
			int len = VarInt.writeZigZagVarLong(val, buf, 0);
			baos.write(buf, 0, len);
		}

		byte[] data = baos.toByteArray();
		try (OwlReader reader = factory.create(data, Endian.BIG)) {
			for (long val : values) {
				assertEquals(val, reader.readSignedVarLong(), "Mismatch reading signed VarLong: " + val);
			}
		}

		// Also verify readZigZagVarLong alias
		try (OwlReader reader = factory.create(data, Endian.BIG)) {
			for (long val : values) {
				assertEquals(val, reader.readZigZagVarLong(), "Mismatch reading zigZag VarLong: " + val);
			}
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testMalformedVarIntExceeds5Bytes(ReaderFactory factory) {
		byte[] invalidVarInt = new byte[] { (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80 };
		OwlReader reader = factory.create(invalidVarInt, Endian.BIG);
		IOException thrown = assertThrows(IOException.class, () -> reader.readVarInt());
		assertTrue(thrown.getMessage().contains("exceeds 5 bytes"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testMalformedVarLongExceeds10Bytes(ReaderFactory factory) {
		byte[] invalidVarLong = new byte[] {
			(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
			(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80
		};
		OwlReader reader = factory.create(invalidVarLong, Endian.BIG);
		IOException thrown = assertThrows(IOException.class, () -> reader.readVarLong());
		assertTrue(thrown.getMessage().contains("exceeds 10 bytes"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testPrematureEOFDuringVarInt(ReaderFactory factory) {
		// Continuation bit is set, but no second byte exists
		byte[] incomplete = new byte[] { (byte) 0x80 };
		OwlReader reader = factory.create(incomplete, Endian.BIG);
		assertThrows(EOFException.class, () -> reader.readVarInt());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testPrematureEOFDuringVarLong(ReaderFactory factory) {
		// Continuation bit is set on 2 bytes, but terminates prematurely
		byte[] incomplete = new byte[] { (byte) 0x80, (byte) 0x80 };
		OwlReader reader = factory.create(incomplete, Endian.BIG);
		assertThrows(EOFException.class, () -> reader.readVarLong());
	}

}

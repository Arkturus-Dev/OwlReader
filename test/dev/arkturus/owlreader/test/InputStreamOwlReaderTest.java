package dev.arkturus.owlreader.test;

import org.junit.jupiter.api.Test;

import dev.arkturus.owl.Endian;
import dev.arkturus.owlreader.InputStreamOwlReader;
import dev.arkturus.owlreader.OwlReader;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class InputStreamOwlReaderTest {

	@Test
	public void testReadPrimitives() throws Exception {
		byte[] data = new byte[] { 10, 0, 20, 0, 0, 0, 30 };
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		InputStreamOwlReader reader = new InputStreamOwlReader(bais, Endian.BIG);

		assertEquals((byte) 10, reader.readByte());
		assertEquals((short) 20, reader.readShort());
		assertEquals(30, reader.readInt());
	}

	@Test
	public void testReadBytesWithOffset() throws Exception {
		byte[] data = new byte[] { 1, 2, 3, 4 };
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		InputStreamOwlReader reader = new InputStreamOwlReader(bais);

		byte[] dest = new byte[6];
		int readCount = reader.read(dest, 2, 3);
		assertEquals(3, readCount);
		assertArrayEquals(new byte[] { 0, 0, 1, 2, 3, 0 }, dest);
	}

	@Test
	public void testReadStrings() throws Exception {
		byte[] data = "Hello\0Pascal\n".getBytes(StandardCharsets.UTF_8);
		// Prefix Pascal with 1 byte length (6)
		byte[] payload = new byte[data.length + 1];
		System.arraycopy(data, 0, payload, 0, 6); // "Hello\0"
		payload[6] = 6; // length of "Pascal"
		System.arraycopy("Pascal\n".getBytes(StandardCharsets.UTF_8), 0, payload, 7, 7);

		ByteArrayInputStream bais = new ByteArrayInputStream(payload);
		InputStreamOwlReader reader = new InputStreamOwlReader(bais);

		assertEquals("Hello", reader.readNullString());
		assertEquals("Pascal", reader.readPascalByte());
		assertEquals("", reader.readLine());
	}

	@Test
	public void testFramedReader() throws Exception {
		byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		InputStreamOwlReader reader = new InputStreamOwlReader(bais);

		OwlReader frame = reader.frame(4);
		assertEquals((byte) 1, frame.readByte());
		assertEquals((byte) 2, frame.readByte());
		frame.close(); // skips remaining 2 bytes of the frame

		assertEquals((byte) 5, reader.readByte());
	}

	@Test
	public void testEOFHandling() {
		ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
		InputStreamOwlReader reader = new InputStreamOwlReader(bais);

		assertThrows(EOFException.class, reader::readByte);
	}

	@Test
	public void testLargeReadExpandsBuffer() throws Exception {
		byte[] data = new byte[2500];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) (i % 127);
		}
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		InputStreamOwlReader reader = new InputStreamOwlReader(bais);

		byte[] readData = reader.read(2500);
		assertArrayEquals(data, readData);
		assertEquals(2500, reader.position());
		assertFalse(reader.hasData());
	}

	@Test
	public void testSkipBytesEOFAndNegative() {
		ByteArrayInputStream bais = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
		InputStreamOwlReader reader = new InputStreamOwlReader(bais);

		assertThrows(IndexOutOfBoundsException.class, () -> reader.skipBytes(-1));
		assertDoesNotThrow(() -> reader.skipBytes(2));
		assertEquals(2, reader.position());
		assertThrows(EOFException.class, () -> reader.skipBytes(5));
	}

	@Test
	public void testFramedPascalStringsLargeLength() throws Exception {
		// Create a Pascal byte string with length 130 (> 127 signed)
		int len = 130;
		byte[] payload = new byte[1 + len];
		payload[0] = (byte) len; // 130 as unsigned byte
		for (int i = 0; i < len; i++) {
			payload[1 + i] = 'x';
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(payload);
		InputStreamOwlReader reader = new InputStreamOwlReader(bais);
		OwlReader frame = reader.frame(1 + len);

		String result = frame.readPascalByte();
		assertEquals(130, result.length());
		assertEquals("x".repeat(130), result);
	}
}

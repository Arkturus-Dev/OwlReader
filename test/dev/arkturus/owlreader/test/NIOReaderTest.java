package dev.arkturus.owlreader.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import dev.arkturus.owl.Endian;
import dev.arkturus.owlreader.NIOReader;

public class NIOReaderTest {

	@Test
	public void testDirectBufferLifecycle() throws IOException {
		ByteBuffer direct = ByteBuffer.allocateDirect(16);
		direct.putInt(0x12345678);
		direct.putDouble(Math.PI);
		direct.flip();

		try (NIOReader reader = new NIOReader(direct)) {
			assertTrue(reader.hasData());
			assertEquals(0, reader.position());
			assertEquals(0x12345678, reader.readInt());
			assertEquals(Math.PI, reader.readDouble());
			assertFalse(reader.hasData());
			assertEquals(12, reader.position());
		}
	}

	@Test
	public void testUnderflowMapsToEOFException() {
		ByteBuffer buffer = ByteBuffer.allocate(2);
		buffer.putShort((short) 42);
		buffer.flip();

		NIOReader reader = new NIOReader(buffer);
		assertThrows(EOFException.class, () -> reader.readInt());
	}

	@Test
	public void testEndianSwitch() throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.order(java.nio.ByteOrder.BIG_ENDIAN);
		buffer.putInt(0x12345678);
		buffer.flip();

		NIOReader reader = new NIOReader(buffer, Endian.LITTLE);
		assertEquals(0x78563412, reader.readInt());

		buffer.clear();
		buffer.order(java.nio.ByteOrder.BIG_ENDIAN);
		buffer.putInt(0x12345678);
		buffer.flip();

		reader = new NIOReader(buffer, Endian.BIG);
		assertEquals(0x12345678, reader.readInt());
	}

	@Test
	public void testGetByteBuffer() {
		ByteBuffer buffer = ByteBuffer.allocate(8);
		NIOReader reader = new NIOReader(buffer);
		assertNotNull(reader.getByteBuffer());
		assertEquals(buffer, reader.getByteBuffer());
	}

	@Test
	public void testSkipBytes() throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(10);
		for (int i = 0; i < 10; i++) buffer.put((byte) i);
		buffer.flip();

		NIOReader reader = new NIOReader(buffer);
		reader.skipBytes(5);
		assertEquals(5, reader.position());
		assertEquals(5, reader.readByte());

		reader.skipBytes(100);
		assertEquals(10, reader.position());
		assertFalse(reader.hasData());
	}

	@Test
	public void testNullStringMissingTerminator() {
		ByteBuffer buffer = ByteBuffer.wrap("NoTerminator".getBytes(StandardCharsets.UTF_8));
		NIOReader reader = new NIOReader(buffer);
		assertThrows(EOFException.class, () -> reader.readNullString());
	}
}

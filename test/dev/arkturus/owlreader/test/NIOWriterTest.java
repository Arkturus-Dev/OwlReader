package dev.arkturus.owlreader.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import dev.arkturus.owl.Endian;
import dev.arkturus.owlwriter.NIOWriter;

public class NIOWriterTest {

	@Test
	public void testDirectBufferWrite() throws IOException {
		ByteBuffer direct = ByteBuffer.allocateDirect(16);
		try (NIOWriter writer = new NIOWriter(direct)) {
			writer.writeInt(0x12345678);
			writer.writeDouble(Math.PI);
			assertEquals(12, writer.position());
			byte[] bytes = writer.toByteArray();
			assertEquals(12, bytes.length);

			direct.flip();
			assertEquals(0x12345678, direct.getInt());
			assertEquals(Math.PI, direct.getDouble());
		}
	}

	@Test
	public void testOverflowThrowsEOFException() throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(3);
		try (NIOWriter writer = new NIOWriter(buffer)) {
			writer.writeByte((byte) 1);
			assertThrows(EOFException.class, () -> writer.writeInt(42));
		}
	}

	@Test
	public void testEndianWrite() throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		try (NIOWriter writer = new NIOWriter(buffer, Endian.LITTLE)) {
			writer.writeInt(0x12345678);
			assertArrayEquals(new byte[] { 0x78, 0x56, 0x34, 0x12 }, writer.toByteArray());
		}
	}

	@Test
	public void testAsString() throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(32);
		try (NIOWriter writer = new NIOWriter(buffer)) {
			writer.writeNullString("Hello World");
			assertEquals("Hello World\0", writer.asString(StandardCharsets.UTF_8));
		}
	}

	@Test
	public void testGetByteBuffer() {
		ByteBuffer buffer = ByteBuffer.allocate(8);
		NIOWriter writer = new NIOWriter(buffer);
		assertNotNull(writer.getByteBuffer());
		assertEquals(buffer, writer.getByteBuffer());
	}
}

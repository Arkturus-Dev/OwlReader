package dev.arkturus.owlreader.test;

import org.junit.jupiter.api.Test;

import dev.arkturus.owl.Endian;
import dev.arkturus.owl.buffer.Buffer;

import java.io.EOFException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BufferTest {

	@Test
	public void testBasicReadWritePrimitives() {
		Buffer b = new Buffer();
		b.write((byte) 42);
		b.writeBytes((short) 1234);
		b.writeBytes(567890);
		b.writeBytes(123456789012345L);

		assertEquals(42, b.read());
		assertEquals((short) 1234, b.readShort());
		assertEquals(567890, b.readInt());
		assertEquals(123456789012345L, b.readLong());
		assertFalse(b.available());
	}

	@Test
	public void testAutoCapacityExpansion() {
		Buffer b = new Buffer(4);
		byte[] large = new byte[100];
		for (int i = 0; i < large.length; i++) {
			large[i] = (byte) i;
		}
		b.write(large);

		byte[] readBack = b.read(large.length);
		assertArrayEquals(large, readBack);
	}

	@Test
	public void testStringsAndDelimiters() {
		Buffer b = new Buffer();
		b.write("Hello, World!");

		assertEquals("Hello", b.readUntil((byte) ','));
		b.advance(1); // skip comma
		assertEquals(' ', (char) b.read());
		assertEquals("World", b.readThrough((byte) '!'));
		assertFalse(b.available());
	}

	@Test
	public void testPreadAndPeek() {
		Buffer b = new Buffer("abc");
		assertEquals('a', (char) b.peek());
		assertFalse(b.pread('x'));
		assertTrue(b.pread('a'));
		assertTrue(b.pread('b'));
		assertTrue(b.pread('c'));
		assertFalse(b.available());
	}

	@Test
	public void testReadEol() {
		Buffer b = new Buffer("line1\r\nline2\nline3");
		assertEquals("line1", b.readUntil((byte) '\r'));
		assertTrue(b.readEol()); // consumes \r\n
		assertEquals("line2", b.readUntil((byte) '\n'));
		assertTrue(b.readEol()); // consumes \n
		assertEquals("line", b.readThrough((byte) '3'));
		assertTrue(b.readEol()); // EOF counts as EOL
	}

	@Test
	public void testBufferReaderReadLine() throws Exception {
		Buffer b = new Buffer();
		b.write("first\n\r\nthird\n");
		var reader = b.reader();
		assertEquals("first", reader.readLine());
		assertEquals("", reader.readLine());
		assertEquals("third", reader.readLine());
	}

	@Test
	public void testWritableBufferIterable() {
		Buffer b = new Buffer();
		b.write(List.of("one", "two", "three"), ", ", "[", "]");
		assertEquals("[one, two, three]", b.toString());
	}

	@Test
	public void testBufferReaderAndWriter() throws Exception {
		Buffer b = new Buffer();
		var writer = b.writer();
		writer.writeString("test-string");
		writer.writeInt(999);

		var reader = b.reader();
		assertEquals("test-string", reader.readString(11));
		assertEquals(999, reader.readInt());
	}

	@Test
	public void testEOFHandling() {
		Buffer b = new Buffer("hi");
		b.read();
		b.read();
		assertThrows(UncheckedIOException.class, b::read);
	}

	@Test
	public void testLocationTrackingConsistency() {
		// Single-byte read
		Buffer b1 = new Buffer("abc\ndef\n");
		assertEquals("[1:0]", b1.location());
		b1.read(); b1.read(); b1.read(); // read "abc"
		assertEquals("[1:3]", b1.location());
		b1.read(); // read '\n'
		assertEquals("[2:0]", b1.location());
		b1.read(); // read 'd'
		assertEquals("[2:1]", b1.location());

		// Advance
		Buffer b2 = new Buffer("abc\ndef\n");
		b2.advance(4); // advance past "abc\n"
		assertEquals("[2:0]", b2.location());
		b2.read(); // read 'd'
		assertEquals("[2:1]", b2.location());

		// Bulk read
		Buffer b3 = new Buffer("abc\ndef\n");
		byte[] dest = new byte[4];
		b3.read(dest, 0, 4);
		assertEquals("[2:0]", b3.location());
		b3.read(); // read 'd'
		assertEquals("[2:1]", b3.location());
	}

	@Test
	public void testReadUntilEOFStateIntegrity() {
		Buffer b = new Buffer("abc\ndef\nghi");
		assertEquals("[1:0]", b.location());
		assertEquals(0, b.position());

		assertThrows(UncheckedIOException.class, () -> b.readUntil((byte) 'z'));
		assertEquals("[1:0]", b.location());
		assertEquals(0, b.position());

		assertThrows(UncheckedIOException.class, () -> b.readThrough((byte) 'z'));
		assertEquals("[1:0]", b.location());
		assertEquals(0, b.position());
	}

	@Test
	public void testBoundsAndNegativeValidation() {
		Buffer b = new Buffer("hello");
		assertThrows(IndexOutOfBoundsException.class, () -> b.advance(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> b.read(-1));

		byte[] buf = new byte[5];
		assertThrows(IndexOutOfBoundsException.class, () -> b.read(buf, 2, 4));
		assertThrows(IndexOutOfBoundsException.class, () -> b.read(buf, -1, 2));
		assertThrows(IndexOutOfBoundsException.class, () -> b.read(buf, 1, -1));

		assertThrows(IndexOutOfBoundsException.class, () -> b.write(buf, 2, 4));
		assertThrows(IndexOutOfBoundsException.class, () -> b.write(buf, -1, 2));
		assertThrows(IndexOutOfBoundsException.class, () -> b.write(buf, 1, -1));

		var reader = b.reader();
		assertThrows(IndexOutOfBoundsException.class, () -> reader.skipBytes(-1));
	}

	@Test
	public void testClearResetsIndent() {
		Buffer b = new Buffer();
		b.indent();
		b.indent();
		assertEquals(2, b.indentLevel());
		b.clear();
		assertEquals(0, b.indentLevel());
		assertEquals("[1:0]", b.location());
		assertEquals(0, b.position());
	}

	@Test
	public void testByteBufferInterop() {
		Buffer b = new Buffer();
		java.nio.ByteBuffer src = java.nio.ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
		b.write(src);
		assertEquals(0, src.remaining());

		java.nio.ByteBuffer view = b.asByteBuffer();
		assertEquals(5, view.remaining());
		assertEquals(1, view.get());

		java.nio.ByteBuffer dst = java.nio.ByteBuffer.allocate(3);
		int read = b.read(dst);
		assertEquals(3, read);
		assertEquals(0, dst.remaining());
		assertArrayEquals(new byte[] { 1, 2, 3 }, dst.array());

		Buffer wrapped = Buffer.wrap(java.nio.ByteBuffer.wrap("wrap-test".getBytes(StandardCharsets.UTF_8)));
		assertEquals("wrap-test", wrapped.toString());
	}

	@Test
	public void testChannelTransfer() throws java.io.IOException {
		Buffer srcBuffer = new Buffer();
		srcBuffer.write("Channel payload data");

		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		java.nio.channels.WritableByteChannel writeChannel = java.nio.channels.Channels.newChannel(baos);
		int written = srcBuffer.writeTo(writeChannel);
		assertEquals("Channel payload data".length(), written);
		assertEquals("Channel payload data", baos.toString(StandardCharsets.UTF_8));

		Buffer dstBuffer = new Buffer();
		java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream("Incoming stream".getBytes(StandardCharsets.UTF_8));
		java.nio.channels.ReadableByteChannel readChannel = java.nio.channels.Channels.newChannel(bais);
		int read = dstBuffer.readFrom(readChannel);
		assertEquals("Incoming stream".length(), read);
		assertEquals("Incoming stream", dstBuffer.toString());
	}

	@Test
	public void testBufferVarIntOperations() {
		Buffer buffer = new Buffer();
		buffer.writeVarInt(300)
			.writeSignedVarInt(-42)
			.writeVarLong(123456789012345L)
			.writeSignedVarLong(-9876543210L);

		assertEquals(300, buffer.readVarInt());
		assertEquals(-42, buffer.readSignedVarInt());
		assertEquals(123456789012345L, buffer.readVarLong());
		assertEquals(-9876543210L, buffer.readSignedVarLong());
	}

	@Test
	public void testBufferMalformedVarIntThrowsUncheckedIOException() {
		Buffer buffer = new Buffer();
		buffer.write(new byte[] { (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80 });
		assertThrows(java.io.UncheckedIOException.class, () -> buffer.readVarInt());
	}

	@Test
	public void testBufferInt24Operations() {
		Buffer buffer = new Buffer();
		buffer.writeBytes24(0x123456)
			.writeInt24(-42)
			.writeUInt24(16777215)
			.writeUMedium(0x800000);

		assertEquals(0x123456, buffer.readInt24());
		assertEquals(-42, buffer.readMedium());
		assertEquals(16777215, buffer.readUInt24());
		assertEquals(0x800000, buffer.readUMedium());
	}
}

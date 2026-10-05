package dev.arkturus.owlreader.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.EOFException;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import dev.arkturus.owl.Endian;
import dev.arkturus.owlwriter.MemorySegmentOwlWriter;

public class MemorySegmentOwlWriterTest {

	@Test
	public void testNativeSegmentWrite() throws IOException {
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment seg = arena.allocate(16);
			try (MemorySegmentOwlWriter writer = new MemorySegmentOwlWriter(seg)) {
				writer.writeInt(0x12345678);
				writer.writeDouble(Math.PI);
				assertEquals(12, writer.position());
				byte[] bytes = writer.toByteArray();
				assertEquals(12, bytes.length);

				assertEquals(0x12345678, seg.get(ValueLayout.JAVA_INT.withOrder(ByteOrder.BIG_ENDIAN), 0));
				assertEquals(Math.PI, seg.get(ValueLayout.JAVA_DOUBLE.withOrder(ByteOrder.BIG_ENDIAN).withByteAlignment(1), 4));
			}
		}
	}

	@Test
	public void testCapacityOverflowThrowsEOFException() throws IOException {
		byte[] arr = new byte[3];
		MemorySegment seg = MemorySegment.ofArray(arr);
		try (MemorySegmentOwlWriter writer = new MemorySegmentOwlWriter(seg)) {
			writer.writeByte((byte) 1);
			assertThrows(EOFException.class, () -> writer.writeInt(42));
		}
	}

	@Test
	public void testEndianWrite() throws IOException {
		byte[] arr = new byte[4];
		MemorySegment seg = MemorySegment.ofArray(arr);
		try (MemorySegmentOwlWriter writer = new MemorySegmentOwlWriter(seg, Endian.LITTLE)) {
			writer.writeInt(0x12345678);
			assertArrayEquals(new byte[] { 0x78, 0x56, 0x34, 0x12 }, writer.toByteArray());
		}
	}

	@Test
	public void testAsString() throws IOException {
		byte[] arr = new byte[32];
		MemorySegment seg = MemorySegment.ofArray(arr);
		try (MemorySegmentOwlWriter writer = new MemorySegmentOwlWriter(seg)) {
			writer.writeNullString("Hello World");
			assertEquals("Hello World\0", writer.asString(StandardCharsets.UTF_8));
		}
	}

	@Test
	public void testGetSegment() {
		byte[] arr = new byte[8];
		MemorySegment seg = MemorySegment.ofArray(arr);
		MemorySegmentOwlWriter writer = new MemorySegmentOwlWriter(seg);
		assertNotNull(writer.getSegment());
		assertEquals(seg, writer.getSegment());
	}
}

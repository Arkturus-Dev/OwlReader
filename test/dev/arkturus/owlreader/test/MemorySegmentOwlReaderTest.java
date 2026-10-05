package dev.arkturus.owlreader.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.EOFException;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import dev.arkturus.owl.Endian;
import dev.arkturus.owlreader.MemorySegmentOwlReader;

public class MemorySegmentOwlReaderTest {

	@Test
	public void testNativeSegmentLifecycle() throws IOException {
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment seg = arena.allocate(16);
			seg.set(ValueLayout.JAVA_INT.withOrder(ByteOrder.BIG_ENDIAN), 0, 0x12345678);
			seg.set(ValueLayout.JAVA_DOUBLE.withOrder(ByteOrder.BIG_ENDIAN).withByteAlignment(1), 4, Math.PI);

			try (MemorySegmentOwlReader reader = new MemorySegmentOwlReader(seg)) {
				assertTrue(reader.hasData());
				assertEquals(0, reader.position());
				assertEquals(0x12345678, reader.readInt());
				assertEquals(Math.PI, reader.readDouble());
				assertEquals(12, reader.position());
			}
		}
	}

	@Test
	public void testHeapSegmentEOF() {
		byte[] data = new byte[] { 1, 2 };
		MemorySegment seg = MemorySegment.ofArray(data);
		MemorySegmentOwlReader reader = new MemorySegmentOwlReader(seg);

		assertThrows(EOFException.class, () -> reader.readInt());
	}

	@Test
	public void testEndianSwitch() throws IOException {
		byte[] data = new byte[] { 0x12, 0x34, 0x56, 0x78 };
		MemorySegment seg = MemorySegment.ofArray(data);

		MemorySegmentOwlReader reader = new MemorySegmentOwlReader(seg, Endian.BIG);
		assertEquals(0x12345678, reader.readInt());

		reader = new MemorySegmentOwlReader(seg, Endian.LITTLE);
		assertEquals(0x78563412, reader.readInt());
	}

	@Test
	public void testSkipBytes() throws IOException {
		byte[] data = new byte[] { 10, 20, 30, 40, 50 };
		MemorySegment seg = MemorySegment.ofArray(data);
		MemorySegmentOwlReader reader = new MemorySegmentOwlReader(seg);

		reader.skipBytes(3);
		assertEquals(3, reader.position());
		assertEquals(40, reader.readByte());

		reader.skipBytes(10);
		assertEquals(5, reader.position());
		assertFalse(reader.hasData());
	}

	@Test
	public void testMissingNullTerminator() {
		byte[] data = "NonTerminated".getBytes(StandardCharsets.UTF_8);
		MemorySegment seg = MemorySegment.ofArray(data);
		MemorySegmentOwlReader reader = new MemorySegmentOwlReader(seg);

		assertThrows(EOFException.class, () -> reader.readNullString());
	}

	@Test
	public void testGetSegment() {
		byte[] data = new byte[8];
		MemorySegment seg = MemorySegment.ofArray(data);
		MemorySegmentOwlReader reader = new MemorySegmentOwlReader(seg);
		assertNotNull(reader.getSegment());
		assertEquals(seg, reader.getSegment());
	}
}

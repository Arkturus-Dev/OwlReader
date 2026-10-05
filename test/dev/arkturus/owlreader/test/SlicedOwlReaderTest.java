package dev.arkturus.owlreader.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arkturus.owl.Endian;
import dev.arkturus.owl.buffer.Buffer;
import dev.arkturus.owlreader.ByteArrayOwlReader;
import dev.arkturus.owlreader.MemorySegmentOwlReader;
import dev.arkturus.owlreader.NIOReader;
import dev.arkturus.owlreader.OwlReader;

import java.io.EOFException;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class SlicedOwlReaderTest {

	static Stream<org.junit.jupiter.api.Named<OwlReaderTestFixtures.ReaderFactory>> allReaders() {
		return OwlReaderTestFixtures.allReaders();
	}

	@ParameterizedTest
	@MethodSource("allReaders")
	@DisplayName("slice(length) advances parent immediately and slice is independent")
	public void testSliceAdvancementAndIndependence(OwlReaderTestFixtures.ReaderFactory factory) throws Exception {
		byte[] data = new byte[] { 10, 20, 30, 40, 50, 60, 70, 80 };
		OwlReader parent = factory.create(data);

		// Read 2 bytes from parent
		assertEquals((byte) 10, parent.readByte());
		assertEquals((byte) 20, parent.readByte());

		// Slice next 4 bytes (30, 40, 50, 60)
		OwlReader slice = parent.slice(4);

		// Parent should have immediately advanced past the slice
		assertEquals((byte) 70, parent.readByte());

		// Slice reads independent data
		assertEquals((byte) 30, slice.readByte());
		assertEquals((byte) 40, slice.readByte());
		assertEquals((byte) 50, slice.readByte());
		assertEquals((byte) 60, slice.readByte());

		// Slice boundary enforcement
		assertFalse(slice.hasData());
		assertThrows(EOFException.class, slice::readByte);

		// Closing slice does not alter parent
		slice.close();

		// Parent can still finish reading
		assertEquals((byte) 80, parent.readByte());
		assertFalse(parent.hasData());
		assertThrows(EOFException.class, parent::readByte);
	}

	@ParameterizedTest
	@MethodSource("allReaders")
	@DisplayName("slice(0) produces an empty reader and does not advance parent")
	public void testSliceZeroLength(OwlReaderTestFixtures.ReaderFactory factory) throws Exception {
		byte[] data = new byte[] { 1, 2, 3 };
		OwlReader parent = factory.create(data);

		OwlReader slice = parent.slice(0);
		assertFalse(slice.hasData());
		assertThrows(EOFException.class, slice::readByte);
		slice.close();

		assertEquals((byte) 1, parent.readByte());
	}

	@ParameterizedTest
	@MethodSource("allReaders")
	@DisplayName("slice(length) with negative length throws IndexOutOfBoundsException")
	public void testSliceNegativeLength(OwlReaderTestFixtures.ReaderFactory factory) {
		byte[] data = new byte[] { 1, 2, 3 };
		OwlReader parent = factory.create(data);

		assertThrows(IndexOutOfBoundsException.class, () -> parent.slice(-1));
	}

	@ParameterizedTest
	@MethodSource("allReaders")
	@DisplayName("slice(length) exceeding available data throws exception")
	public void testSliceExceedingAvailable(OwlReaderTestFixtures.ReaderFactory factory) {
		byte[] data = new byte[] { 1, 2, 3 };
		OwlReader parent = factory.create(data);

		assertThrows(Exception.class, () -> parent.slice(10));
	}

	@Test
	@DisplayName("slice(offset, length) creates detached slice without advancing parent position")
	public void testAbsoluteSliceOnMemoryBackedReaders() throws Exception {
		byte[] data = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };

		// 1. ByteArrayOwlReader
		ByteArrayOwlReader bar = new ByteArrayOwlReader(data);
		bar.readByte(); // pos = 1
		OwlReader barSlice = bar.slice(4, 3); // bytes 4, 5, 6
		assertEquals(1, bar.position());
		assertEquals((byte) 4, barSlice.readByte());
		assertEquals((byte) 5, barSlice.readByte());
		assertEquals((byte) 6, barSlice.readByte());
		assertFalse(barSlice.hasData());
		assertEquals(1, bar.position());

		// 2. NIOReader
		NIOReader nio = new NIOReader(ByteBuffer.wrap(data));
		nio.readByte(); // pos = 1
		OwlReader nioSlice = nio.slice(4, 3);
		assertEquals(1, nio.position());
		assertEquals((byte) 4, nioSlice.readByte());
		assertEquals((byte) 5, nioSlice.readByte());
		assertEquals((byte) 6, nioSlice.readByte());
		assertFalse(nioSlice.hasData());
		assertEquals(1, nio.position());

		// 3. MemorySegmentOwlReader
		MemorySegmentOwlReader msr = new MemorySegmentOwlReader(MemorySegment.ofArray(data));
		msr.readByte(); // pos = 1
		OwlReader msrSlice = msr.slice(4, 3);
		assertEquals(1, msr.position());
		assertEquals((byte) 4, msrSlice.readByte());
		assertEquals((byte) 5, msrSlice.readByte());
		assertEquals((byte) 6, msrSlice.readByte());
		assertFalse(msrSlice.hasData());
		assertEquals(1, msr.position());

		// 4. Buffer
		Buffer buf = new Buffer(data);
		buf.readByte(); // pos = 1
		OwlReader bufSlice = buf.slice(4, 3);
		assertEquals(1, buf.position());
		assertEquals((byte) 4, bufSlice.readByte());
		assertEquals((byte) 5, bufSlice.readByte());
		assertEquals((byte) 6, bufSlice.readByte());
		assertFalse(bufSlice.hasData());
		assertEquals(1, buf.position());
	}

	@Test
	@DisplayName("slice(offset, length) bounds checking")
	public void testAbsoluteSliceBoundsChecking() {
		byte[] data = new byte[] { 1, 2, 3, 4, 5 };
		ByteArrayOwlReader bar = new ByteArrayOwlReader(data);

		assertThrows(IndexOutOfBoundsException.class, () -> bar.slice(-1, 2));
		assertThrows(IndexOutOfBoundsException.class, () -> bar.slice(1, -1));
		assertThrows(IndexOutOfBoundsException.class, () -> bar.slice(3, 3)); // 3+3 > 5
		assertThrows(IndexOutOfBoundsException.class, () -> bar.slice(6, 0)); // offset > length
	}

	@Test
	@DisplayName("Buffer.slice(length) returns an OwlReader instance")
	public void testBufferSliceReturnBuffer() throws Exception {
		byte[] data = new byte[] { 10, 20, 30, 40, 50 };
		Buffer buf = new Buffer(data);
		buf.readByte(); // 10

		OwlReader slice = buf.slice(2);
		assertEquals(3, buf.position());
		assertEquals((byte) 20, slice.readByte());
		assertEquals((byte) 30, slice.readByte());
		assertFalse(slice.hasData());
	}
}

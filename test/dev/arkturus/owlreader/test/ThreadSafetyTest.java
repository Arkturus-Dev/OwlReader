package dev.arkturus.owlreader.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arkturus.owl.Endian;
import dev.arkturus.owl.VarInt;
import dev.arkturus.owlreader.ByteArrayOwlReader;
import dev.arkturus.owlreader.CRCByteArrayOwlReader;
import dev.arkturus.owlreader.InputStreamOwlReader;
import dev.arkturus.owlreader.MemorySegmentOwlReader;
import dev.arkturus.owlreader.NIOReader;
import dev.arkturus.owlreader.OwlReader;
import dev.arkturus.owlwriter.ByteArrayOwlWriter;
import dev.arkturus.owlwriter.OutputStreamOwlWriter;
import dev.arkturus.owlwriter.OwlWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ThreadSafetyTest {

	@FunctionalInterface
	public interface ReaderSupplier {
		OwlReader create(byte[] data) throws Exception;
	}

	public static Stream<org.junit.jupiter.api.Named<ReaderSupplier>> concurrentReaders() {
		return Stream.of(
			org.junit.jupiter.api.Named.of("ByteArrayOwlReader", data -> new ByteArrayOwlReader(data)),
			org.junit.jupiter.api.Named.of("InputStreamOwlReader", data -> new InputStreamOwlReader(new ByteArrayInputStream(data))),
			org.junit.jupiter.api.Named.of("CRCByteArrayOwlReader", data -> new CRCByteArrayOwlReader(new ByteArrayOwlReader(data))),
			org.junit.jupiter.api.Named.of("NIOReader(Heap)", data -> new NIOReader(ByteBuffer.wrap(data))),
			org.junit.jupiter.api.Named.of("MemorySegmentOwlReader", data -> new MemorySegmentOwlReader(MemorySegment.ofArray(data))),
			org.junit.jupiter.api.Named.of("FramedByteArrayOwlReader", data -> new ByteArrayOwlReader(data).frame(data.length)),
			org.junit.jupiter.api.Named.of("FramedInputStreamOwlReader", data -> new InputStreamOwlReader(new ByteArrayInputStream(data)).frame(data.length)),
			org.junit.jupiter.api.Named.of("FramedNIOReader", data -> new NIOReader(ByteBuffer.wrap(data)).frame(data.length)),
			org.junit.jupiter.api.Named.of("FramedMemorySegmentOwlReader", data -> new MemorySegmentOwlReader(MemorySegment.ofArray(data)).frame(data.length))
		);
	}

	@ParameterizedTest
	@MethodSource("concurrentReaders")
	@DisplayName("Concurrent multi-threaded readVarInt and readVarLong atomicity")
	public void testConcurrentReadVarInt(ReaderSupplier supplier) throws Exception {
		int count = 1000;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamOwlWriter writer = new OutputStreamOwlWriter(baos);
		int[] expectedValues = new int[count];
		for (int i = 0; i < count; i++) {
			int val = (i * 1337) & 0x7FFFFFFF;
			expectedValues[i] = val;
			writer.writeVarInt(val);
		}

		byte[] data = baos.toByteArray();
		OwlReader reader = supplier.create(data);

		int threadCount = 8;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		ConcurrentLinkedQueue<Integer> results = new ConcurrentLinkedQueue<>();
		ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

		List<Future<?>> futures = new ArrayList<>();
		for (int t = 0; t < threadCount; t++) {
			futures.add(executor.submit(() -> {
				try {
					startLatch.await();
					while (true) {
						try {
							int val;
							if (!reader.hasData()) {
								break;
							}
							val = reader.readVarInt();
							results.add(val);
						} catch (EOFException e) {
							break;
						}
					}
				} catch (Throwable e) {
					errors.add(e);
				}
			}));
		}

		startLatch.countDown();
		for (Future<?> f : futures) {
			f.get(10, TimeUnit.SECONDS);
		}
		executor.shutdown();

		assertTrue(errors.isEmpty(), "Unexpected errors during concurrent readVarInt: " + errors);
		assertEquals(count, results.size(), "Total read VarInt count mismatch");
	}

	@ParameterizedTest
	@MethodSource("concurrentReaders")
	@DisplayName("Concurrent multi-threaded readPascalShort atomicity")
	public void testConcurrentReadPascalShort(ReaderSupplier supplier) throws Exception {
		int count = 500;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamOwlWriter writer = new OutputStreamOwlWriter(baos);
		for (int i = 0; i < count; i++) {
			writer.writePascalShort("packet-payload-" + i);
		}

		byte[] data = baos.toByteArray();
		OwlReader reader = supplier.create(data);

		int threadCount = 8;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		ConcurrentLinkedQueue<String> results = new ConcurrentLinkedQueue<>();
		ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

		List<Future<?>> futures = new ArrayList<>();
		for (int t = 0; t < threadCount; t++) {
			futures.add(executor.submit(() -> {
				try {
					startLatch.await();
					while (true) {
						try {
							if (!reader.hasData()) {
								break;
							}
							String val = reader.readPascalShort();
							results.add(val);
						} catch (EOFException e) {
							break;
						}
					}
				} catch (Throwable e) {
					errors.add(e);
				}
			}));
		}

		startLatch.countDown();
		for (Future<?> f : futures) {
			f.get(10, TimeUnit.SECONDS);
		}
		executor.shutdown();

		assertTrue(errors.isEmpty(), "Unexpected errors during concurrent readPascalShort: " + errors);
		assertEquals(count, results.size(), "Total read Pascal string count mismatch");
		for (String s : results) {
			assertTrue(s.startsWith("packet-payload-"), "Corrupted string read: " + s);
		}
	}

	@Test
	@DisplayName("Concurrent multi-threaded writeVarInt atomicity in OutputStreamOwlWriter")
	public void testConcurrentOutputStreamOwlWriterVarInt() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamOwlWriter writer = new OutputStreamOwlWriter(baos);

		int threadCount = 8;
		int itemsPerThread = 200;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>();
		ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

		for (int t = 0; t < threadCount; t++) {
			final int threadId = t;
			futures.add(executor.submit(() -> {
				try {
					startLatch.await();
					for (int i = 0; i < itemsPerThread; i++) {
						int value = (threadId << 16) | i;
						writer.writeVarInt(value);
					}
				} catch (Throwable e) {
					errors.add(e);
				}
			}));
		}

		startLatch.countDown();
		for (Future<?> f : futures) {
			f.get(10, TimeUnit.SECONDS);
		}
		executor.shutdown();

		assertTrue(errors.isEmpty(), "Unexpected errors during concurrent writeVarInt: " + errors);

		byte[] bytes = baos.toByteArray();
		ByteArrayOwlReader reader = new ByteArrayOwlReader(bytes);
		int totalRead = 0;
		while (reader.hasData()) {
			int val = reader.readVarInt();
			totalRead++;
		}
		assertEquals(threadCount * itemsPerThread, totalRead, "Total read VarInts must match total written");
	}

	@Test
	@DisplayName("OutputStreamOwlWriter writeInt24 synchronization and accuracy")
	public void testOutputStreamOwlWriterWriteInt24() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamOwlWriter writer = new OutputStreamOwlWriter(baos, Endian.BIG);
		writer.writeInt24(0x123456);
		writer.writeInt24(0xABCDEF);

		ByteArrayOwlReader reader = new ByteArrayOwlReader(baos.toByteArray(), Endian.BIG);
		assertEquals(0x123456, reader.readInt24());
		assertEquals(0xABCDEF, reader.readUInt24());
	}
}

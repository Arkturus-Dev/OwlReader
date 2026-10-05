package dev.arkturus.owlreader.test;

import org.junit.jupiter.api.Named;

import dev.arkturus.owl.Endian;
import dev.arkturus.owl.buffer.Buffer;
import dev.arkturus.owlreader.ByteArrayOwlReader;
import dev.arkturus.owlreader.CRCByteArrayOwlReader;
import dev.arkturus.owlreader.InputStreamOwlReader;
import dev.arkturus.owlreader.OwlReader;

import java.io.ByteArrayInputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.stream.Stream;

import dev.arkturus.owlreader.MemorySegmentOwlReader;
import dev.arkturus.owlreader.NIOReader;

public final class OwlReaderTestFixtures {

	private OwlReaderTestFixtures() {
	}

	@FunctionalInterface
	public interface ReaderFactory {
		OwlReader create(byte[] data, Endian endian);

		default OwlReader create(byte[] data) {
			return create(data, Endian.BIG);
		}
	}

	public static Stream<Named<ReaderFactory>> allReaders() {
		return Stream.of(
			Named.of("ByteArrayOwlReader", (data, endian) -> new ByteArrayOwlReader(data, endian)),
			Named.of("InputStreamOwlReader", (data, endian) -> new InputStreamOwlReader(new ByteArrayInputStream(data), endian)),
			Named.of("BufferReader", (data, endian) -> {
				Buffer b = new Buffer(data, endian);
				return b.reader();
			}),
			Named.of("CRCByteArrayOwlReader", (data, endian) -> new CRCByteArrayOwlReader(new ByteArrayOwlReader(data, endian))),
			Named.of("FramedInputStreamOwlReader", (data, endian) -> {
				var parent = new InputStreamOwlReader(new ByteArrayInputStream(data), endian);
				return parent.frame(data.length);
			}),
			Named.of("NIOReader(Heap)", (data, endian) -> new NIOReader(ByteBuffer.wrap(data.clone()), endian)),
			Named.of("NIOReader(Direct)", (data, endian) -> {
				ByteBuffer bb = ByteBuffer.allocateDirect(data.length);
				bb.put(data);
				bb.flip();
				return new NIOReader(bb, endian);
			}),
			Named.of("MemorySegmentOwlReader(Heap)", (data, endian) -> new MemorySegmentOwlReader(MemorySegment.ofArray(data.clone()), endian)),
			Named.of("MemorySegmentOwlReader(Native)", (data, endian) -> {
				Arena arena = Arena.ofAuto();
				MemorySegment seg = arena.allocate(data.length);
				MemorySegment.copy(MemorySegment.ofArray(data), 0, seg, 0, data.length);
				return new MemorySegmentOwlReader(seg, endian);
			})
		);
	}
}

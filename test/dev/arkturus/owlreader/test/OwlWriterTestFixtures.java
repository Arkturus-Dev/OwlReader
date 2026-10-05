package dev.arkturus.owlreader.test;

import org.junit.jupiter.api.Named;

import dev.arkturus.owl.Endian;
import dev.arkturus.owl.buffer.Buffer;
import dev.arkturus.owlwriter.ByteArrayOwlWriter;
import dev.arkturus.owlwriter.OutputStreamOwlWriter;
import dev.arkturus.owlwriter.OwlWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Stream;

import dev.arkturus.owlwriter.MemorySegmentOwlWriter;
import dev.arkturus.owlwriter.NIOWriter;

public final class OwlWriterTestFixtures {

	private OwlWriterTestFixtures() {
	}

	public interface WriterFixture {
		OwlWriter writer();
		byte[] toByteArray() throws IOException;
	}

	@FunctionalInterface
	public interface WriterFactory {
		WriterFixture create(int capacity, Endian endian);

		default WriterFixture create(int capacity) {
			return create(capacity, Endian.BIG);
		}
	}

	public static Stream<Named<WriterFactory>> allWriters() {
		return Stream.of(
			Named.of("ByteArrayOwlWriter", (capacity, endian) -> {
				byte[] out = new byte[capacity];
				ByteArrayOwlWriter writer = new ByteArrayOwlWriter(out, endian);
				return new WriterFixture() {
					@Override
					public OwlWriter writer() {
						return writer;
					}

					@Override
					public byte[] toByteArray() {
						return Arrays.copyOf(out, writer.position());
					}
				};
			}),
			Named.of("OutputStreamOwlWriter", (capacity, endian) -> {
				ByteArrayOutputStream baos = new ByteArrayOutputStream(capacity);
				OutputStreamOwlWriter writer = new OutputStreamOwlWriter(baos, endian);
				return new WriterFixture() {
					@Override
					public OwlWriter writer() {
						return writer;
					}

					@Override
					public byte[] toByteArray() throws IOException {
						writer.close();
						return baos.toByteArray();
					}
				};
			}),
			Named.of("BufferWriter", (capacity, endian) -> {
				Buffer buffer = new Buffer(capacity, endian);
				OwlWriter writer = buffer.writer();
				return new WriterFixture() {
					@Override
					public OwlWriter writer() {
						return writer;
					}

					@Override
					public byte[] toByteArray() {
						return buffer.toByteArray();
					}
				};
			}),
			Named.of("FramedOutputStreamOwlWriter", (capacity, endian) -> {
				ByteArrayOutputStream baos = new ByteArrayOutputStream(capacity);
				OutputStreamOwlWriter parent = new OutputStreamOwlWriter(baos, endian);
				OwlWriter frame = parent.frame(capacity);
				return new WriterFixture() {
					@Override
					public OwlWriter writer() {
						return frame;
					}

					@Override
					public byte[] toByteArray() throws IOException {
						frame.close();
						return baos.toByteArray();
					}
				};
			}),
			Named.of("NIOWriter(Heap)", (capacity, endian) -> {
				ByteBuffer bb = ByteBuffer.allocate(capacity);
				NIOWriter writer = new NIOWriter(bb, endian);
				return new WriterFixture() {
					@Override
					public OwlWriter writer() {
						return writer;
					}

					@Override
					public byte[] toByteArray() {
						return writer.toByteArray();
					}
				};
			}),
			Named.of("NIOWriter(Direct)", (capacity, endian) -> {
				ByteBuffer bb = ByteBuffer.allocateDirect(capacity);
				NIOWriter writer = new NIOWriter(bb, endian);
				return new WriterFixture() {
					@Override
					public OwlWriter writer() {
						return writer;
					}

					@Override
					public byte[] toByteArray() {
						return writer.toByteArray();
					}
				};
			}),
			Named.of("MemorySegmentOwlWriter(Heap)", (capacity, endian) -> {
				byte[] arr = new byte[capacity];
				MemorySegment seg = MemorySegment.ofArray(arr);
				MemorySegmentOwlWriter writer = new MemorySegmentOwlWriter(seg, endian);
				return new WriterFixture() {
					@Override
					public OwlWriter writer() {
						return writer;
					}

					@Override
					public byte[] toByteArray() {
						return writer.toByteArray();
					}
				};
			}),
			Named.of("MemorySegmentOwlWriter(Native)", (capacity, endian) -> {
				Arena arena = Arena.ofAuto();
				MemorySegment seg = arena.allocate(capacity);
				MemorySegmentOwlWriter writer = new MemorySegmentOwlWriter(seg, endian);
				return new WriterFixture() {
					@Override
					public OwlWriter writer() {
						return writer;
					}

					@Override
					public byte[] toByteArray() {
						return writer.toByteArray();
					}
				};
			})
		);
	}
}

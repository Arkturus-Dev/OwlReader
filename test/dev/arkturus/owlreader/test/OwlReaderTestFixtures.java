package dev.arkturus.owlreader.test;

import org.junit.jupiter.api.Named;

import dev.arkturus.owl.Endian;
import dev.arkturus.owl.buffer.Buffer;
import dev.arkturus.owlreader.ByteArrayOwlReader;
import dev.arkturus.owlreader.CRCByteArrayOwlReader;
import dev.arkturus.owlreader.InputStreamOwlReader;
import dev.arkturus.owlreader.OwlReader;

import java.io.ByteArrayInputStream;
import java.util.stream.Stream;

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
			})
		);
	}
}

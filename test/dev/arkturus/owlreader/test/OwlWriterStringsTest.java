package dev.arkturus.owlreader.test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arkturus.owlreader.test.OwlWriterTestFixtures.WriterFactory;
import dev.arkturus.owlreader.test.OwlWriterTestFixtures.WriterFixture;
import dev.arkturus.owlwriter.OwlWriter;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class OwlWriterStringsTest {

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlWriterTestFixtures#allWriters")
	public void testWriteStrings(WriterFactory factory) throws Exception {
		WriterFixture fixture = factory.create(128);
		OwlWriter writer = fixture.writer();

		writer.writeString("Hello");
		writer.writeNullString("World");
		writer.writePascalByte("PByte");
		writer.writePascalShort("PShort");
		writer.writePascalInt("PInt");
		writer.writeLine("Line");

		byte[] data = fixture.toByteArray();
		String asStr = new String(data, StandardCharsets.UTF_8);
		assertTrue(asStr.startsWith("Hello"));
		assertTrue(asStr.contains("World"));
		assertTrue(asStr.contains("Line"));
	}
}

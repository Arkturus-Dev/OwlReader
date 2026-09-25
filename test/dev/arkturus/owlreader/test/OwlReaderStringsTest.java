package dev.arkturus.owlreader.test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arkturus.owlreader.OwlReader;
import dev.arkturus.owlreader.test.OwlReaderTestFixtures.ReaderFactory;
import dev.arkturus.owlwriter.ByteArrayOwlWriter;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class OwlReaderStringsTest {

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testReadStringVariants(ReaderFactory factory) throws Exception {
		byte[] data = new byte[128];
		ByteArrayOwlWriter writer = new ByteArrayOwlWriter(data);
		writer.writeString("Fixed");
		writer.writeNullString("NullTerm");
		writer.writePascalByte("PByte");
		writer.writePascalShort("PShort");
		writer.writePascalInt("PInt");

		OwlReader reader = factory.create(data);
		assertEquals("Fixed", reader.readString(5));
		assertEquals("NullTerm", reader.readNullString());
		assertEquals("PByte", reader.readPascalByte());
		assertEquals("PShort", reader.readPascalShort());
		assertEquals("PInt", reader.readPascalInt());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testReadLine(ReaderFactory factory) throws Exception {
		byte[] data = "first line\r\nsecond line\nthird line".getBytes(StandardCharsets.UTF_8);
		OwlReader reader = factory.create(data);

		assertEquals("first line", reader.readLine());
		assertEquals("second line", reader.readLine());
		assertEquals("third line", reader.readLine());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testEmptyLines(ReaderFactory factory) throws Exception {
		byte[] data = "\n\r\n".getBytes(StandardCharsets.UTF_8);
		OwlReader reader = factory.create(data);

		assertEquals("", reader.readLine());
		assertEquals("", reader.readLine());
		assertFalse(reader.hasData());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dev.arkturus.owlreader.test.OwlReaderTestFixtures#allReaders")
	public void testLargePascalByte(ReaderFactory factory) throws Exception {
		int len = 130;
		byte[] payload = new byte[1 + len];
		payload[0] = (byte) len; // 130 as unsigned byte
		for (int i = 0; i < len; i++) {
			payload[1 + i] = 'x';
		}

		OwlReader reader = factory.create(payload);
		String result = reader.readPascalByte();
		assertEquals(130, result.length());
		assertEquals("x".repeat(130), result);
	}
}

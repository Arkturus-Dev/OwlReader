package dev.arkturus.owlreader.test;

import org.junit.jupiter.api.Test;

import dev.arkturus.owl.Endian;
import dev.arkturus.owlwriter.OutputStreamOwlWriter;
import dev.arkturus.owlwriter.OwlWriter;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OutputStreamOwlWriterTest {

	@Test
	public void testWritePrimitives() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamOwlWriter writer = new OutputStreamOwlWriter(baos, Endian.BIG);

		writer.writeByte((byte) 0x11);
		writer.writeShort((short) 0x2233);
		writer.writeInt(0x44556677);
		writer.writeLong(0x0102030405060708L);
		writer.writeFloat(1.0f);
		writer.writeDouble(2.0);

		byte[] out = baos.toByteArray();
		assertEquals((byte) 0x11, out[0]);
		assertEquals((byte) 0x22, out[1]);
		assertEquals((byte) 0x33, out[2]);
		assertEquals((byte) 0x44, out[3]);
		assertEquals(27, out.length); // 1 + 2 + 4 + 8 + 4 + 8
	}

	@Test
	public void testWriteStrings() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamOwlWriter writer = new OutputStreamOwlWriter(baos);

		writer.writeString("Hello");
		writer.writeNullString("World");
		writer.writeLine("Line");

		byte[] out = baos.toByteArray();
		String str = new String(out, StandardCharsets.UTF_8);
		assertEquals("HelloWorld\0Line\n", str);
	}

	@Test
	public void testFramedWriter() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamOwlWriter writer = new OutputStreamOwlWriter(baos);

		OwlWriter frame = writer.frame(8);
		frame.writeInt(0x12345678);
		frame.close(); // writes padding up to 8 bytes

		byte[] out = baos.toByteArray();
		assertEquals(8, out.length);
		assertEquals((byte) 0x12, out[0]);
		assertEquals((byte) 0x34, out[1]);
		assertEquals((byte) 0x56, out[2]);
		assertEquals((byte) 0x78, out[3]);
		assertEquals((byte) 0x00, out[4]);
		assertEquals((byte) 0x00, out[5]);
		assertEquals((byte) 0x00, out[6]);
		assertEquals((byte) 0x00, out[7]);
	}
}

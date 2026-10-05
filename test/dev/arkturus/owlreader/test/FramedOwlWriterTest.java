package dev.arkturus.owlreader.test;

import org.junit.jupiter.api.Test;

import dev.arkturus.owlwriter.OutputStreamOwlWriter;
import dev.arkturus.owlwriter.OwlWriter;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;

import static org.junit.jupiter.api.Assertions.*;

public class FramedOwlWriterTest {

	@Test
	public void testFramedWriterCloseZeroPads() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamOwlWriter parent = new OutputStreamOwlWriter(baos);

		OwlWriter frame = parent.frame(4);
		frame.writeByte((byte) 0xAA);
		frame.writeByte((byte) 0xBB);
		frame.close(); // should zero-pad the remaining 2 bytes

		byte[] out = baos.toByteArray();
		assertEquals(4, out.length);
		assertArrayEquals(new byte[] { (byte) 0xAA, (byte) 0xBB, 0, 0 }, out);
	}

	@Test
	public void testFramedWriterBoundaryEnforcement() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamOwlWriter parent = new OutputStreamOwlWriter(baos);

		OwlWriter frame = parent.frame(2);
		frame.writeByte((byte) 1);
		frame.writeByte((byte) 2);
		assertThrows(EOFException.class, () -> frame.writeByte((byte) 3));
	}

	@Test
	public void testFramedWriterInt24() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamOwlWriter parent = new OutputStreamOwlWriter(baos);

		OwlWriter frame = parent.frame(4);
		frame.writeInt24(0x123456);
		frame.close();

		byte[] out = baos.toByteArray();
		assertEquals(4, out.length);
		assertArrayEquals(new byte[] { 0x12, 0x34, 0x56, 0 }, out);
	}
}

package dev.arkturus.owlreader.test;

import org.junit.jupiter.api.Test;

import dev.arkturus.owlreader.InputStreamOwlReader;
import dev.arkturus.owlreader.OwlReader;

import java.io.ByteArrayInputStream;
import java.io.EOFException;

import static org.junit.jupiter.api.Assertions.*;

public class FramedOwlReaderTest {

	@Test
	public void testFrameBoundaryEnforcement() throws Exception {
		byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		InputStreamOwlReader parent = new InputStreamOwlReader(bais);

		OwlReader frame = parent.frame(4);
		assertEquals((byte) 1, frame.readByte());
		assertEquals((byte) 2, frame.readByte());
		assertEquals((byte) 3, frame.readByte());
		assertEquals((byte) 4, frame.readByte());

		assertFalse(frame.hasData());
		assertThrows(EOFException.class, frame::readByte);
	}

	@Test
	public void testFrameCloseSkipsRemaining() throws Exception {
		byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		InputStreamOwlReader parent = new InputStreamOwlReader(bais);

		OwlReader frame = parent.frame(4);
		assertEquals((byte) 1, frame.readByte());
		assertEquals((byte) 2, frame.readByte());
		frame.close(); // skips remaining 2 bytes of the frame

		// Parent should now be at byte 5
		assertEquals((byte) 5, parent.readByte());
		assertEquals((byte) 6, parent.readByte());
	}

	@Test
	public void testSequentialFrames() throws Exception {
		byte[] data = new byte[] { 10, 20, 30, 40, 50, 60 };
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		InputStreamOwlReader parent = new InputStreamOwlReader(bais);

		OwlReader frame1 = parent.frame(2);
		assertEquals((byte) 10, frame1.readByte());
		assertEquals((byte) 20, frame1.readByte());
		frame1.close();

		OwlReader frame2 = parent.frame(3);
		assertEquals((byte) 30, frame2.readByte());
		assertEquals((byte) 40, frame2.readByte());
		assertEquals((byte) 50, frame2.readByte());
		frame2.close();

		assertEquals((byte) 60, parent.readByte());
		assertFalse(parent.hasData());
	}
}

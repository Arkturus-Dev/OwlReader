package dev.arkturus.owlreader.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arkturus.owl.buffer.Buffer;
import dev.arkturus.owlreader.InputStreamOwlReader;
import dev.arkturus.owlreader.OwlReader;

import java.io.EOFException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class FramedOwlReaderTest {

	static Stream<org.junit.jupiter.api.Named<OwlReaderTestFixtures.ReaderFactory>> readers() {
		return OwlReaderTestFixtures.allReaders().filter(named -> !named.getName().startsWith("Framed"));
	}

	@ParameterizedTest
	@MethodSource("readers")
	@DisplayName("Frame boundary enforcement across all reader implementations")
	public void testFrameBoundaryEnforcement(OwlReaderTestFixtures.ReaderFactory factory) throws Exception {
		byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		OwlReader parent = factory.create(data);

		OwlReader frame = parent.frame(4);
		assertEquals((byte) 1, frame.readByte());
		assertEquals((byte) 2, frame.readByte());
		assertEquals((byte) 3, frame.readByte());
		assertEquals((byte) 4, frame.readByte());

		assertFalse(frame.hasData());
		assertThrows(EOFException.class, frame::readByte);
	}

	@ParameterizedTest
	@MethodSource("readers")
	@DisplayName("Closing frame skips remaining frame bytes and aligns parent")
	public void testFrameCloseSkipsRemaining(OwlReaderTestFixtures.ReaderFactory factory) throws Exception {
		byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		OwlReader parent = factory.create(data);

		OwlReader frame = parent.frame(4);
		assertEquals((byte) 1, frame.readByte());
		assertEquals((byte) 2, frame.readByte());
		frame.close(); // skips remaining 2 bytes of the frame

		// Parent should now be at byte 5
		assertEquals((byte) 5, parent.readByte());
		assertEquals((byte) 6, parent.readByte());
	}

	@ParameterizedTest
	@MethodSource("readers")
	@DisplayName("Sequential frames over a single parent reader")
	public void testSequentialFrames(OwlReaderTestFixtures.ReaderFactory factory) throws Exception {
		byte[] data = new byte[] { 10, 20, 30, 40, 50, 60 };
		OwlReader parent = factory.create(data);

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

	@ParameterizedTest
	@MethodSource("readers")
	@DisplayName("Framed reader string and line reading")
	public void testFramedStringsAndLines(OwlReaderTestFixtures.ReaderFactory factory) throws Exception {
		byte[] data = "Hello\nWorld\0Remaining".getBytes(StandardCharsets.UTF_8);
		OwlReader parent = factory.create(data);

		OwlReader frame1 = parent.frame(6); // "Hello\n"
		assertEquals("Hello", frame1.readLine());
		assertFalse(frame1.hasData());
		frame1.close();

		OwlReader frame2 = parent.frame(6); // "World\0"
		assertEquals("World", frame2.readNullString());
		assertFalse(frame2.hasData());
		frame2.close();

		assertEquals("Remaining", parent.readString(9));
	}

	@ParameterizedTest
	@MethodSource("readers")
	@DisplayName("Frame length validation")
	public void testFrameLengthValidation(OwlReaderTestFixtures.ReaderFactory factory) {
		byte[] data = new byte[] { 1, 2, 3 };
		OwlReader parent = factory.create(data);

		assertThrows(IndexOutOfBoundsException.class, () -> parent.frame(-1));
		if (!(parent instanceof InputStreamOwlReader)) {
			assertThrows(IllegalArgumentException.class, () -> parent.frame(10));
		}
	}

	@Test
	@DisplayName("Buffer.frame(length) returns an OwlReader instance")
	public void testBufferFrameReturnsBuffer() throws Exception {
		byte[] data = new byte[] { 1, 2, 3, 4, 5 };
		Buffer buf = new Buffer(data);

		OwlReader framed = buf.frame(3);
		assertEquals((byte) 1, framed.readByte());
		framed.close();

		assertEquals((byte) 4, buf.readByte());
		assertEquals((byte) 5, buf.readByte());
	}
}

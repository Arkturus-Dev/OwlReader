package dev.arkturus.owlwriter;

import java.io.EOFException;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Objects;

import dev.arkturus.owl.Endian;

/**
 * An {@link OwlWriter} implementation backed by a Panama {@link MemorySegment}.
 * Supports on-heap segments and off-heap native memory segments.
 */
public class MemorySegmentOwlWriter extends BaseOwlWriter {

	protected final Object        writeLock = new Object();
	protected final MemorySegment segment;
	protected volatile long       position  = 0;

	private ValueLayout.OfShort  shortLayout;
	private ValueLayout.OfInt    intLayout;
	private ValueLayout.OfLong   longLayout;
	private ValueLayout.OfFloat  floatLayout;
	private ValueLayout.OfDouble doubleLayout;

	public MemorySegmentOwlWriter(MemorySegment segment) {
		this.segment = Objects.requireNonNull(segment);
		updateLayouts(getEndian());
	}

	public MemorySegmentOwlWriter(MemorySegment segment, Endian endian) {
		this.segment = Objects.requireNonNull(segment);
		setEndian(endian);
	}

	@Override
	public void setEndian(Endian endian) {
		super.setEndian(endian);
		synchronized (writeLock) {
			updateLayouts(endian);
		}
	}

	private void updateLayouts(Endian endian) {
		ByteOrder order = (endian == Endian.LITTLE) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
		shortLayout = ValueLayout.JAVA_SHORT.withOrder(order).withByteAlignment(1);
		intLayout = ValueLayout.JAVA_INT.withOrder(order).withByteAlignment(1);
		longLayout = ValueLayout.JAVA_LONG.withOrder(order).withByteAlignment(1);
		floatLayout = ValueLayout.JAVA_FLOAT.withOrder(order).withByteAlignment(1);
		doubleLayout = ValueLayout.JAVA_DOUBLE.withOrder(order).withByteAlignment(1);
	}

	public MemorySegment getSegment() {
		return segment;
	}

	public int position() {
		synchronized (writeLock) {
			return (int) position;
		}
	}

	private void checkCapacity(long bytes) throws EOFException {
		if (position + bytes > segment.byteSize()) {
			position = segment.byteSize();
			throw new EOFException("Capacity exceeded on MemorySegment.");
		}
	}

	@Override
	public void writeByte(byte b) throws IOException {
		synchronized (writeLock) {
			checkCapacity(1);
			segment.set(ValueLayout.JAVA_BYTE, position, b);
			position += 1;
		}
	}

	@Override
	public void writeShort(short s) throws IOException {
		synchronized (writeLock) {
			checkCapacity(2);
			segment.set(shortLayout, position, s);
			position += 2;
		}
	}

	@Override
	public void writeInt(int i) throws IOException {
		synchronized (writeLock) {
			checkCapacity(4);
			segment.set(intLayout, position, i);
			position += 4;
		}
	}

	@Override
	public void writeLong(long l) throws IOException {
		synchronized (writeLock) {
			checkCapacity(8);
			segment.set(longLayout, position, l);
			position += 8;
		}
	}

	@Override
	public void writeFloat(float f) throws IOException {
		synchronized (writeLock) {
			checkCapacity(4);
			segment.set(floatLayout, position, f);
			position += 4;
		}
	}

	@Override
	public void writeDouble(double d) throws IOException {
		synchronized (writeLock) {
			checkCapacity(8);
			segment.set(doubleLayout, position, d);
			position += 8;
		}
	}

	@Override
	public void write(byte[] bytes, int off, int len) throws IOException {
		Objects.checkFromIndexSize(off, len, bytes.length);
		synchronized (writeLock) {
			checkCapacity(len);
			MemorySegment src = MemorySegment.ofArray(bytes);
			MemorySegment.copy(src, off, segment, position, len);
			position += len;
		}
	}

	@Override
	public void writeNullString(String s, Charset encoding) throws IOException {
		synchronized (writeLock) {
			super.writeNullString(s, encoding);
		}
	}

	@Override
	public void writePascalByte(String s, Charset encoding) throws IOException {
		synchronized (writeLock) {
			super.writePascalByte(s, encoding);
		}
	}

	@Override
	public void writePascalShort(String s, Charset encoding) throws IOException {
		synchronized (writeLock) {
			super.writePascalShort(s, encoding);
		}
	}

	@Override
	public void writePascalInt(String s, Charset encoding) throws IOException {
		synchronized (writeLock) {
			super.writePascalInt(s, encoding);
		}
	}

	public byte[] toByteArray() {
		synchronized (writeLock) {
			int pos = (int) position;
			byte[] result = new byte[pos];
			MemorySegment dst = MemorySegment.ofArray(result);
			MemorySegment.copy(segment, 0, dst, 0, pos);
			return result;
		}
	}

	public String asString(Charset encoding) {
		return new String(toByteArray(), encoding);
	}

	@Override
	public void close() throws IOException {
		// No-op for MemorySegment
	}

}

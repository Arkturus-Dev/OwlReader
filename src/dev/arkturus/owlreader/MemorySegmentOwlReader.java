package dev.arkturus.owlreader;

import java.io.EOFException;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Objects;

import dev.arkturus.owl.Endian;

/**
 * An {@link OwlReader} implementation backed by a Panama {@link MemorySegment}.
 * Supports on-heap segments, off-heap native memory segments, and memory-mapped segments.
 */
public class MemorySegmentOwlReader extends BaseOwlReader {

	protected final Object        readLock = new Object();
	protected final MemorySegment segment;
	protected volatile long       position = 0;

	private ValueLayout.OfShort  shortLayout;
	private ValueLayout.OfInt    intLayout;
	private ValueLayout.OfLong   longLayout;
	private ValueLayout.OfFloat  floatLayout;
	private ValueLayout.OfDouble doubleLayout;

	public MemorySegmentOwlReader(MemorySegment segment) {
		this.segment = Objects.requireNonNull(segment);
		updateLayouts(getEndian());
	}

	public MemorySegmentOwlReader(MemorySegment segment, Endian endian) {
		this.segment = Objects.requireNonNull(segment);
		setEndian(endian);
	}

	@Override
	public void setEndian(Endian endian) {
		super.setEndian(endian);
		synchronized (readLock) {
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

	private void checkAvailable(long bytes) throws EOFException {
		if (position + bytes > segment.byteSize()) {
			throw new EOFException("Unexpected EOF.");
		}
	}

	@Override
	public byte readByte() throws IOException {
		synchronized (readLock) {
			checkAvailable(1);
			byte val = segment.get(ValueLayout.JAVA_BYTE, position);
			position += 1;
			return val;
		}
	}

	@Override
	public short readShort() throws IOException {
		synchronized (readLock) {
			checkAvailable(2);
			short val = segment.get(shortLayout, position);
			position += 2;
			return val;
		}
	}

	@Override
	public int readInt() throws IOException {
		synchronized (readLock) {
			checkAvailable(4);
			int val = segment.get(intLayout, position);
			position += 4;
			return val;
		}
	}

	@Override
	public long readLong() throws IOException {
		synchronized (readLock) {
			checkAvailable(8);
			long val = segment.get(longLayout, position);
			position += 8;
			return val;
		}
	}

	@Override
	public float readFloat() throws IOException {
		synchronized (readLock) {
			checkAvailable(4);
			float val = segment.get(floatLayout, position);
			position += 4;
			return val;
		}
	}

	@Override
	public double readDouble() throws IOException {
		synchronized (readLock) {
			checkAvailable(8);
			double val = segment.get(doubleLayout, position);
			position += 8;
			return val;
		}
	}

	@Override
	public int read(byte[] out, int off, int len) throws IOException {
		Objects.checkFromIndexSize(off, len, out.length);
		synchronized (readLock) {
			long remaining = segment.byteSize() - position;
			int toRead = (int) Math.max(0, Math.min((long) len, remaining));
			if (toRead > 0) {
				MemorySegment dst = MemorySegment.ofArray(out);
				MemorySegment.copy(segment, position, dst, off, toRead);
				position += toRead;
			}
			return toRead;
		}
	}

	@Override
	public String readString(int bytes, Charset encoding) throws IOException {
		if (bytes < 0) {
			throw new IndexOutOfBoundsException("bytes cannot be negative: " + bytes);
		}
		synchronized (readLock) {
			checkAvailable(bytes);
			byte[] tmp = new byte[bytes];
			MemorySegment dst = MemorySegment.ofArray(tmp);
			MemorySegment.copy(segment, position, dst, 0, bytes);
			position += bytes;
			return new String(tmp, encoding);
		}
	}

	@Override
	public String readNullString(Charset encoding) throws IOException {
		synchronized (readLock) {
			long size = 0;
			long limit = segment.byteSize();
			while (position + size < limit && segment.get(ValueLayout.JAVA_BYTE, position + size) != 0) {
				size++;
			}
			if (position + size >= limit) {
				throw new EOFException("Unexpected EOF searching for null terminator.");
			}
			byte[] tmp = new byte[(int) size];
			MemorySegment dst = MemorySegment.ofArray(tmp);
			MemorySegment.copy(segment, position, dst, 0, size);
			position += size + 1; // skip null terminator
			return new String(tmp, encoding);
		}
	}

	@Override
	public String readPascalByte(Charset encoding) throws IOException {
		synchronized (readLock) {
			return super.readPascalByte(encoding);
		}
	}

	@Override
	public String readPascalShort(Charset encoding) throws IOException {
		synchronized (readLock) {
			return super.readPascalShort(encoding);
		}
	}

	@Override
	public String readPascalInt(Charset encoding) throws IOException {
		synchronized (readLock) {
			return super.readPascalInt(encoding);
		}
	}

	@Override
	public String readLine(Charset encoding) throws IOException {
		synchronized (readLock) {
			long limit = segment.byteSize();
			if (position >= limit) {
				return "";
			}
			long newlinePos = -1;
			for (long i = position; i < limit; i++) {
				if (segment.get(ValueLayout.JAVA_BYTE, i) == '\n') {
					newlinePos = i;
					break;
				}
			}
			if (newlinePos != -1) {
				int rawLen = (int) (newlinePos - position);
				int len = (rawLen > 0 && segment.get(ValueLayout.JAVA_BYTE, newlinePos - 1) == '\r') ? rawLen - 1 : rawLen;
				byte[] tmp = new byte[rawLen];
				MemorySegment dst = MemorySegment.ofArray(tmp);
				MemorySegment.copy(segment, position, dst, 0, rawLen);
				position = newlinePos + 1; // consume '\n'
				return new String(tmp, 0, len, encoding);
			} else {
				int len = (int) (limit - position);
				int effectiveLen = (len > 0 && segment.get(ValueLayout.JAVA_BYTE, limit - 1) == '\r') ? len - 1 : len;
				byte[] tmp = new byte[len];
				MemorySegment dst = MemorySegment.ofArray(tmp);
				MemorySegment.copy(segment, position, dst, 0, len);
				position = limit;
				return new String(tmp, 0, effectiveLen, encoding);
			}
		}
	}

	@Override
	public void skipBytes(long count) throws IOException {
		if (count < 0) {
			throw new IndexOutOfBoundsException("count cannot be negative: " + count);
		}
		synchronized (readLock) {
			position = Math.min(segment.byteSize(), position + count);
		}
	}

	@Override
	public boolean hasData() {
		synchronized (readLock) {
			return position < segment.byteSize();
		}
	}

	@Override
	public int position() {
		synchronized (readLock) {
			return (int) position;
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (readLock) {
			position = segment.byteSize();
		}
	}

}

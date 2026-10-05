package dev.arkturus.owl.buffer;

import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import dev.arkturus.nullanalysis.annotations.Nullable;
import dev.arkturus.owl.Endian;
import dev.arkturus.owlreader.BaseOwlReader;
import dev.arkturus.owlreader.ByteArrayOwlReader;
import dev.arkturus.owlreader.OwlReader;
import dev.arkturus.owlwriter.BaseOwlWriter;
import dev.arkturus.owlwriter.OwlWriter;

/**
 * Thread Unsafe
 */
public class Buffer implements ReadableBuffer, WritableBuffer {

	public static final int DEFAULT_SIZE = 16;

	private Charset encoding;
	private Endian endian;
	private byte[] buffer;

	private int readIndex = 0;
	private int writeIndex = 0;
	private int line = 1;
	private int lineIndex = 0;
	private int indent = 0;

	public Buffer(byte[] buffer, Charset encoding, Endian endian) {
		this.buffer = buffer;
		this.writeIndex = buffer.length;
		this.encoding = encoding;
		this.endian = endian;
	}

	public Buffer(byte[] buffer, Charset encoding) {
		this(buffer, encoding, Endian.BIG);
	}

	public Buffer(byte[] buffer, Endian endian) {
		this(buffer, StandardCharsets.UTF_8, endian);
	}

	public Buffer(byte[] buffer) {
		this(buffer, StandardCharsets.UTF_8, Endian.BIG);
	}

	public Buffer(int size, Charset encoding, Endian endian) {
		buffer = new byte[size];
		this.encoding = encoding;
		this.endian = endian;
	}

	public Buffer(int size, Charset encoding) {
		this(size, encoding, Endian.BIG);
	}

	public Buffer(int size, Endian endian) {
		this(size, StandardCharsets.UTF_8, endian);
	}

	public Buffer(int size) {
		this(size, StandardCharsets.UTF_8, Endian.BIG);
	}

	public Buffer(Charset encoding, Endian endian) {
		this(DEFAULT_SIZE, encoding, endian);
	}

	public Buffer(Endian endian) {
		this(StandardCharsets.UTF_8, endian);
	}

	public Buffer(Charset encoding) {
		this(encoding, Endian.BIG);
	}

	public Buffer() {
		this(DEFAULT_SIZE);
	}

	public Buffer(String s, Charset encoding, Endian endian) {
		buffer = s.getBytes(encoding);
		writeIndex = buffer.length;
		this.encoding = encoding;
		this.endian = endian;
	}

	public Buffer(String s, Charset encoding) {
		this(s, encoding, Endian.BIG);
	}

	public Buffer(String s, Endian endian) {
		this(s, StandardCharsets.UTF_8, endian);
	}

	public Buffer(String s) {
		this(s, StandardCharsets.UTF_8, Endian.BIG);
	}

	protected void checkEOF(int bytes) {
		if (bytes < 0)
			throw new IndexOutOfBoundsException("bytes cannot be negative: " + bytes);
		if (readIndex + bytes > writeIndex)
			throw new UncheckedIOException(
					new EOFException("Unexpected EOF at " + location(writeIndex - readIndex) + "."));
	}

	protected void checkNewline(int bytes) {
		checkEOF(bytes);
		for (int i = 0; i < bytes; i++) {
			if (buffer[readIndex + i] == '\n')
				newline(i + 1);
		}
	}

	protected void newline(int offset) {
		line++;
		lineIndex = readIndex + offset;
	}

	protected void ensureCapacity(int size) {
		if (writeIndex + size > buffer.length) {
			int newSize = Math.max(writeIndex + size, buffer.length * 2);
			buffer = Arrays.copyOf(buffer, newSize);
		}
	}

	protected String location(int offset) {
		return String.format("[%d:%d]", line, readIndex - lineIndex + offset);
	}

	public String location() {
		return location(0);
	}

	public int position() {
		return readIndex;
	}

	public void clear(){
		readIndex = 0;
		writeIndex = 0;
		line = 1;
		lineIndex = 0;
		indent = 0;
	}

	@Override
	public int indentLevel() {
		return indent;
	}

	@Override
	public void indent() {
		indent++;
	}

	@Override
	public void dedent() {
		indent = Math.max(0, indent - 1);
	}

	// ReadableBuffer

	@Override
	public Endian getEndian() {
		return endian;
	}

	@Override
	public byte peek() {
		checkEOF(1);
		return buffer[readIndex];
	}

	@Override
	public byte read() {
		checkEOF(1);
		byte b = buffer[readIndex++];
		if (b == '\n')
			newline(0);
		return b;
	}

	@Override
	public byte[] read(int len) {
		if (len < 0)
			throw new IndexOutOfBoundsException("len cannot be negative: " + len);
		byte[] b = new byte[len];
		int read = read(b, 0, len);
		if (read != len)
			throw new UncheckedIOException(
					new EOFException("Unexpected EOF at " + location() + "."));
		return b;
	}

	@Override
	public int read(byte[] buffer, int off, int len) {
		Objects.checkFromIndexSize(off, len, buffer.length);
		int size = Math.min(len, this.writeIndex - this.readIndex);
		checkNewline(size);
		System.arraycopy(this.buffer, readIndex, buffer, off, size);
		readIndex += size;
		return size;
	}

	@Override
	public boolean pread(byte b) {
		if (peek() != b)
			return false;
		read();
		return true;
	}

	@Override
	public boolean available() {
		return writeIndex > readIndex;
	}

	@Override
	public void advance(int len) {
		if (len < 0)
			throw new IndexOutOfBoundsException("len cannot be negative: " + len);
		checkNewline(len);
		readIndex += len;
	}

	@Override
	public byte[] readUntilBytes(byte b) {
		int available = writeIndex - readIndex;
		for (int i = 0; i < available; i++) {
			byte current = buffer[readIndex + i];
			if (current == b) {
				byte[] result = Arrays.copyOfRange(buffer, readIndex, readIndex + i);
				checkNewline(i);
				readIndex += i;
				return result;
			}
		}
		throw new UncheckedIOException(
				new EOFException("Unexpected EOF while searching for " + b + " at " + location(available) + "."));
	}

	@Override
	public byte[] readThroughBytes(byte b) {
		int available = writeIndex - readIndex;
		for (int i = 0; i < available; i++) {
			byte current = buffer[readIndex + i];
			if (current == b) {
				byte[] result = Arrays.copyOfRange(buffer, readIndex, readIndex + i);
				checkNewline(i + 1);
				readIndex += i + 1;
				return result;
			}
		}
		throw new UncheckedIOException(
				new EOFException("Unexpected EOF while searching for " + b + " at " + location(available) + "."));
	}

	@Override
	public String readUntil(byte b) {
		return new String(readUntilBytes(b), encoding);
	}

	@Override
	public String readThrough(byte b) {
		return new String(readThroughBytes(b), encoding);
	}

	@Override
	public boolean readEol() {
		if (!available())
			return true;
		checkEOF(1);
		if (pread('\n'))
			return true;
		if (peek() == '\r') {
			checkEOF(2);
			if (buffer[readIndex + 1] == '\n') {
				readIndex += 2;
				newline(0);
				return true;
			}
		}
		return false;
	}

	@Override
	public byte next(byte... bs) {
		int available = writeIndex - readIndex;
		for (int i = 0; i < available; i++) {
			byte current = buffer[readIndex + i];
			for (byte b : bs) {
				if (current == b)
					return current;
			}
		}
		throw new UncheckedIOException(
				new EOFException("Unexpected EOF while searching for " + bs + " at " + location(available) + "."));
	}

	@Override
	public OwlReader reader() {
		return new BufferReader();
	}

	private class BufferReader extends BaseOwlReader {

		@Override
		public Endian getEndian() {
			return endian;
		}

		@Override
		public void setEndian(Endian endian) {
			Buffer.this.endian = endian;
		}

		@Override
		public Charset getDefaultEncoding() {
			return encoding;
		}

		@Override
		public void setDefaultEncoding(Charset charset) {
			Buffer.this.encoding = charset;
		}

		@Override
		public byte readByte() throws IOException {
			try {
				return Buffer.this.read();
			} catch (UncheckedIOException e) {
				if (e.getCause() instanceof IOException)
					throw (IOException) e.getCause();
				throw e;
			}
		}

		@Override
		public String readNullString(Charset encoding) throws IOException {
			try {
				return new String(Buffer.this.readThroughBytes((byte) 0), encoding);
			} catch (UncheckedIOException e) {
				if (e.getCause() instanceof IOException)
					throw (IOException) e.getCause();
				throw e;
			}
		}

		@Override
		public String readLine(Charset encoding) throws IOException {
			if (!available())
				return "";
			int available = writeIndex - readIndex;
			int index = -1;
			for (int i = 0; i < available; i++) {
				if (buffer[readIndex + i] == '\n') {
					index = i;
					break;
				}
			}
			byte[] data;
			if (index != -1) {
				try {
					data = Buffer.this.readThroughBytes('\n');
				} catch (UncheckedIOException e) {
					if (e.getCause() instanceof IOException)
						throw (IOException) e.getCause();
					throw e;
				}
			} else {
				try {
					data = Buffer.this.read(available);
				} catch (UncheckedIOException e) {
					if (e.getCause() instanceof IOException)
						throw (IOException) e.getCause();
					throw e;
				}
			}
			int len = (data.length > 0 && data[data.length - 1] == '\r') ? data.length - 1 : data.length;
			return new String(data, 0, len, encoding);
		}

		@Override
		public byte[] read(int length) throws IOException {
			try {
				return Buffer.this.read(length);
			} catch (UncheckedIOException e) {
				if (e.getCause() instanceof IOException)
					throw (IOException) e.getCause();
				throw e;
			}
		}

		@Override
		public int read(byte[] buffer, int off, int len) throws IOException {
			try {
				return Buffer.this.read(buffer, off, len);
			} catch (UncheckedIOException e) {
				if (e.getCause() instanceof IOException)
					throw (IOException) e.getCause();
				throw e;
			}
		}

		@Override
		public void skipBytes(long count) {
			if (count < 0 || count > Integer.MAX_VALUE)
				throw new IndexOutOfBoundsException("count out of bounds: " + count);
			advance((int) count);
		}

		@Override
		public boolean hasData() {
			return available();
		}

		@Override
		public int position() {
			return readIndex;
		}

		@Override
		public void close() {
			readIndex = writeIndex;
		}

		@Override
		public OwlReader slice(int sliceLength) throws IOException {
			try {
				return Buffer.this.slice(sliceLength);
			} catch (UncheckedIOException e) {
				if (e.getCause() instanceof IOException) {
					throw (IOException) e.getCause();
				}
				throw e;
			}
		}

		@Override
		public OwlReader slice(int sliceOffset, int sliceLength) throws IOException {
			return Buffer.this.slice(sliceOffset, sliceLength);
		}

		@Override
		public OwlReader frame(int frameLength) {
			if (frameLength < 0) {
				throw new IndexOutOfBoundsException("frameLength cannot be negative: " + frameLength);
			}
			if (readIndex + frameLength > writeIndex) {
				throw new IllegalArgumentException("frameLength exceeds available data: " + frameLength + " > " + (writeIndex - readIndex));
			}
			return new FramedBufferReader(frameLength);
		}

		private class FramedBufferReader extends BaseOwlReader {
			private final int frameBound;
			private int       framePos = 0;

			public FramedBufferReader(int frameBound) {
				this.frameBound = frameBound;
				setEndian(Buffer.this.endian);
			}

			private void advanceFrame(int count) throws EOFException {
				if (framePos + count > frameBound) {
					throw new EOFException("End of frame reached.");
				}
				framePos += count;
			}

			@Override
			public byte readByte() throws IOException {
				advanceFrame(1);
				return BufferReader.this.readByte();
			}

			@Override
			public short readShort() throws IOException {
				advanceFrame(2);
				return BufferReader.this.readShort();
			}

			@Override
			public int readInt() throws IOException {
				advanceFrame(4);
				return BufferReader.this.readInt();
			}

			@Override
			public long readLong() throws IOException {
				advanceFrame(8);
				return BufferReader.this.readLong();
			}

			@Override
			public int read(byte[] out, int off, int len) throws IOException {
				Objects.checkFromIndexSize(off, len, out.length);
				int toRead = Math.min(len, frameBound - framePos);
				int read = BufferReader.this.read(out, off, toRead);
				framePos += read;
				return read;
			}

			@Override
			public String readString(int bytes, Charset encoding) throws IOException {
				advanceFrame(bytes);
				return BufferReader.this.readString(bytes, encoding);
			}

			@Override
			public String readNullString(Charset encoding) throws IOException {
				byte[] buf = new byte[frameBound - framePos];
				int size = 0;
				boolean foundNull = false;
				while (framePos < frameBound) {
					advanceFrame(1);
					byte b = BufferReader.this.readByte();
					if (b == 0) {
						foundNull = true;
						break;
					}
					buf[size++] = b;
				}
				if (!foundNull) {
					throw new EOFException("Unexpected EOF searching for null terminator.");
				}
				return new String(buf, 0, size, encoding);
			}

			@Override
			public String readLine(Charset encoding) throws IOException {
				int remaining = frameBound - framePos;
				if (remaining <= 0) {
					return "";
				}
				byte[] lineBuf = new byte[remaining];
				int size = 0;
				boolean foundNewline = false;
				while (framePos < frameBound) {
					advanceFrame(1);
					byte b = BufferReader.this.readByte();
					if (b == '\n') {
						foundNewline = true;
						break;
					}
					lineBuf[size++] = b;
				}
				if (size == 0 && !foundNewline) {
					return "";
				}
				int len = (size > 0 && lineBuf[size - 1] == '\r') ? size - 1 : size;
				return new String(lineBuf, 0, len, encoding);
			}

			@Override
			public void skipBytes(long count) throws IOException {
				long toSkip = Math.min((long) (frameBound - framePos), count);
				advanceFrame((int) toSkip);
				BufferReader.this.skipBytes(toSkip);
			}

			@Override
			public boolean hasData() {
				return framePos < frameBound && BufferReader.this.hasData();
			}

			@Override
			public int position() {
				return framePos;
			}

			@Override
			public void close() throws IOException {
				int unread = frameBound - framePos;
				if (unread > 0) {
					BufferReader.this.skipBytes(unread);
					framePos = frameBound;
				}
			}
		}

	}

	@Override
	public OwlReader slice(int sliceLength) {
		if (sliceLength < 0) {
			throw new IndexOutOfBoundsException("sliceLength cannot be negative: " + sliceLength);
		}
		if (readIndex + sliceLength > writeIndex) {
			throw new UncheckedIOException(new EOFException("Unexpected EOF while slicing: requested " + sliceLength + ", available " + (writeIndex - readIndex)));
		}
		checkNewline(sliceLength);
		int sliceOffset = readIndex;
		readIndex += sliceLength;
		return new ByteArrayOwlReader(buffer, sliceOffset, sliceLength, endian);
	}

	@Override
	public OwlReader slice(int sliceOffset, int sliceLength) {
		if (sliceOffset < 0 || sliceLength < 0 || sliceOffset + sliceLength > writeIndex) {
			throw new IndexOutOfBoundsException("Invalid slice bounds: offset=" + sliceOffset + ", length=" + sliceLength + ", limit=" + writeIndex);
		}
		return new ByteArrayOwlReader(buffer, sliceOffset, sliceLength, endian);
	}

	@Override
	public OwlReader frame(int frameLength) {
		return reader().frame(frameLength);
	}

	// WritableBuffer

	@Override
	public Buffer write(byte b) {
		ensureCapacity(1);
		buffer[writeIndex++] = b;
		return this;
	}

	@Override
	public Buffer write(char c) {
		WritableBuffer.super.write(c);
		return this;
	}

	@Override
	public Buffer write(byte[] buffer) {
		WritableBuffer.super.write(buffer);
		return this;
	}

	@Override
	public Buffer write(byte[] buffer, int off, int len) {
		Objects.checkFromIndexSize(off, len, buffer.length);
		ensureCapacity(len);
		System.arraycopy(buffer, off, Buffer.this.buffer, writeIndex, len);
		writeIndex += len;
		return this;
	}

	@Override
	public Buffer write(String s, Charset encoding) {
		return write(s.getBytes(encoding));
	}

	@Override
	public Buffer write(String s) {
		WritableBuffer.super.write(s);
		return this;
	}

	@Override
	public Buffer write(@Nullable Object o) {
		WritableBuffer.super.write(o);
		return this;
	}

	@Override
	public <T> Buffer write(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn, String delim,
			String prefix, String suffix) {
		WritableBuffer.super.write(iterable, fn, delim, prefix, suffix);
		return this;
	}

	@Override
	public <T> Buffer write(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn, String prefix,
			String suffix) {
		WritableBuffer.super.write(iterable, fn, prefix, suffix);
		return this;
	}

	@Override
	public <T> Buffer write(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn, String delim) {
		WritableBuffer.super.write(iterable, fn, delim);
		return this;
	}

	@Override
	public <T> Buffer write(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn) {
		WritableBuffer.super.write(iterable, fn);
		return this;
	}

	@Override
	public Buffer write(Iterable<@Nullable ?> iterable, String delim, String prefix, String suffix) {
		WritableBuffer.super.write(iterable, delim, prefix, suffix);
		return this;
	}

	@Override
	public Buffer write(Iterable<@Nullable ?> iterable, String prefix, String suffix) {
		WritableBuffer.super.write(iterable, prefix, suffix);
		return this;
	}

	@Override
	public <T> Buffer write(Iterable<@Nullable T> iterable, String delim) {
		WritableBuffer.super.write(iterable, delim);
		return this;
	}

	@Override
	public Buffer write(Iterable<@Nullable ?> iterable) {
		WritableBuffer.super.write(iterable);
		return this;
	}

	@Override
	public Buffer writeln(byte b) {
		WritableBuffer.super.writeln(b);
		return this;
	}

	@Override
	public Buffer writeln(char c) {
		WritableBuffer.super.writeln(c);
		return this;
	}

	@Override
	public Buffer writeln(String s, Charset encoding) {
		WritableBuffer.super.writeln(s, encoding);
		return this;
	}

	@Override
	public Buffer writeln(String s) {
		WritableBuffer.super.writeln(s);
		return this;
	}

	@Override
	public Buffer writeln(Object o) {
		WritableBuffer.super.writeln(o);
		return this;
	}

	@Override
	public Buffer writeln(Iterable<@Nullable ?> iterable, String delim, String prefix, String suffix) {
		WritableBuffer.super.writeln(iterable, delim, prefix, suffix);
		return this;
	}

	@Override
	public Buffer writeln(Iterable<@Nullable ?> iterable, String prefix, String suffix) {
		WritableBuffer.super.writeln(iterable, prefix, suffix);
		return this;
	}

	@Override
	public Buffer writeln(Iterable<@Nullable ?> iterable, String delim) {
		WritableBuffer.super.writeln(iterable, delim);
		return this;
	}

	@Override
	public Buffer writeln(Iterable<@Nullable ?> iterable) {
		WritableBuffer.super.writeln(iterable);
		return this;
	}

	@Override
	public <T> Buffer writeln(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn, String delim,
			String prefix, String suffix) {
		WritableBuffer.super.writeln(iterable, fn, delim, prefix, suffix);
		return this;
	}

	@Override
	public <T> Buffer writeln(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn,
			String prefix, String suffix) {
		WritableBuffer.super.writeln(iterable, fn, prefix, suffix);
		return this;
	}

	@Override
	public <T> Buffer writeln(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn,
			String delim) {
		WritableBuffer.super.writeln(iterable, fn, delim);
		return this;
	}

	@Override
	public <T> Buffer writeln(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn) {
		WritableBuffer.super.writeln(iterable, fn);
		return this;
	}

	@Override
	public Buffer writeln() {
		WritableBuffer.super.writeln();
		return this;
	}

	@Override
	public Buffer writeBytes(short s) {
		ensureCapacity(2);
		endian.writeShort(s, buffer, writeIndex);
		writeIndex += 2;
		return this;
	}

	@Override
	public Buffer writeBytes(int i) {
		ensureCapacity(4);
		endian.writeInt(i, buffer, writeIndex);
		writeIndex += 4;
		return this;
	}

	@Override
	public Buffer writeBytes(long l) {
		ensureCapacity(8);
		endian.writeLong(l, buffer, writeIndex);
		writeIndex += 8;
		return this;
	}

	@Override
	public OwlWriter writer() {
		return new BufferWriter();
	}

	private class BufferWriter extends BaseOwlWriter {

		@Override
		public Endian getEndian() {
			return endian;
		}

		@Override
		public void setEndian(Endian endian) {
			Buffer.this.endian = endian;
		}

		@Override
		public Charset getDefaultEncoding() {
			return encoding;
		}

		@Override
		public void setDefaultEncoding(Charset charset) {
			Buffer.this.encoding = charset;
		}

		@Override
		public void writeByte(byte b) {
			Buffer.this.write(b);
		}

		@Override
		public void writeShort(short s) {
			ensureCapacity(2);
			getEndian().writeShort(s, buffer, writeIndex);
			writeIndex += 2;
		}

		@Override
		public void writeInt(int i) {
			ensureCapacity(4);
			getEndian().writeInt(i, buffer, writeIndex);
			writeIndex += 4;
		}

		@Override
		public void writeLong(long l) {
			ensureCapacity(8);
			getEndian().writeLong(l, buffer, writeIndex);
			writeIndex += 8;
		}

		@Override
		public void write(byte[] bytes, int off, int len) {
			Buffer.this.write(bytes, off, len);
		}

		@Override
		public void close() {
		}

	}

	public static Buffer wrap(ByteBuffer byteBuffer) {
		Buffer b = new Buffer(byteBuffer.remaining());
		b.write(byteBuffer);
		return b;
	}

	@Override
	public ByteBuffer asByteBuffer() {
		return ByteBuffer.wrap(buffer, readIndex, writeIndex - readIndex).slice();
	}

	@Override
	public int read(ByteBuffer dst) {
		int toRead = Math.min(writeIndex - readIndex, dst.remaining());
		if (toRead > 0) {
			checkNewline(toRead);
			dst.put(buffer, readIndex, toRead);
			readIndex += toRead;
		}
		return toRead;
	}

	@Override
	public Buffer write(ByteBuffer src) {
		int len = src.remaining();
		if (len > 0) {
			ensureCapacity(len);
			src.get(buffer, writeIndex, len);
			writeIndex += len;
		}
		return this;
	}

	public int writeTo(WritableByteChannel channel) throws IOException {
		int toWrite = writeIndex - readIndex;
		if (toWrite == 0) {
			return 0;
		}
		ByteBuffer bb = ByteBuffer.wrap(buffer, readIndex, toWrite);
		int written = channel.write(bb);
		if (written > 0) {
			checkNewline(written);
			readIndex += written;
		}
		return written;
	}

	public int readFrom(ReadableByteChannel channel) throws IOException {
		ensureCapacity(1024);
		ByteBuffer bb = ByteBuffer.wrap(buffer, writeIndex, buffer.length - writeIndex);
		int read = channel.read(bb);
		if (read > 0) {
			writeIndex += read;
		}
		return read;
	}

	public void rewind() {
		readIndex = 0;
	}

	public byte[] toByteArray() {
		return Arrays.copyOf(buffer, writeIndex);
	}

	@Override
	public String toString() {
		return new String(buffer, 0, writeIndex, encoding);
	}

}

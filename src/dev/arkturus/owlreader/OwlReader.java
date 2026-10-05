package dev.arkturus.owlreader;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Objects;

import dev.arkturus.nullanalysis.annotations.Nullable;
import dev.arkturus.owl.Endian;

public interface OwlReader extends Closeable, Readable {

	/**
	 * Get the default endianness.
	 *
	 * @return the default ednianness.
	 */
	@Override
	public Endian getEndian();

	/**
	 * Set the endianness.
	 *
	 * @param encoding a String that translates into a {@link Charset} Encoding.
	 * @see {@link #readShort()}
	 * @see {@link #readInt()}
	 * @see {@link #readLong()}
	 * @see {@link #readFloat()}
	 * @see {@link #readDouble()}
	 */
	public void setEndian(Endian endian);

	/**
	 * Get the default encoding.
	 *
	 * @return the default encoding.
	 */
	public Charset getDefaultEncoding();

	/**
	 * Set the default encoding. This encoding is used by default on read String methods that do not specify an
	 * encoding.
	 *
	 * @param encoding a String that translates into a {@link Charset} Encoding.
	 * @throws IllegalCharsetNameException If the given charset name is illegal.
	 * @throws UnsupportedCharsetException If the given charsetName is null.
	 * @see {@link #readString(int)}
	 * @see {@link #readNullString()}
	 * @see {@link #readPascalByte()}
	 * @see {@link #readPascalShort()}
	 * @see {@link #readPascalInt()}
	 * @see {@link #readLine()}
	 */
	public default void setDefaultEncoding(String encoding) {
		setDefaultEncoding(Charset.forName(encoding));
	}

	/**
	 * Set the default encoding. This encoding is used by default on read String methods that do not specify an
	 * encoding.
	 *
	 * @param encoding {@link Charset} Encoding of the String.
	 * @see {@link #readString(int)}
	 * @see {@link #readNullString()}
	 * @see {@link #readPascalByte()}
	 * @see {@link #readPascalShort()}
	 * @see {@link #readPascalInt()}
	 * @see {@link #readLine()}
	 */
	public void setDefaultEncoding(Charset encoding);

	/**
	 * Reads a String with specified length in bytes from the input.
	 *
	 * @param bytes    length of expected String in bytes
	 * @param encoding a String that translates into a {@link Charset} Encoding.
	 * @return a String
	 * @throws IOException
	 * @throws IllegalCharsetNameException If the given charset name is illegal.
	 * @throws UnsupportedCharsetException If the given charsetName is null.
	 * @see {@link #readString(int, Charset)}
	 * @see {@link #readString(int)}
	 */
	public default String readString(int bytes, String encoding) throws IOException {
		return readString(bytes, Charset.forName(encoding));
	}

	/**
	 * Reads a String with specified length in bytes from the input.
	 *
	 * @param bytes    length of expected String in bytes
	 * @param encoding {@link Charset} Encoding of the String.
	 * @return a String
	 * @throws IOException
	 * @see {@link #readString(int, String)}
	 * @see {@link #readString(int)}
	 */
	public default String readString(int bytes, Charset encoding) throws IOException {
		return new String(read(bytes), encoding);
	}

	/**
	 * Reads a String with specified length in bytes from the input using the default encoding.
	 *
	 * @param bytes length of expected String in bytes
	 * @return a String
	 * @throws IOException
	 * @see {@link #readString(int, Charset)}
	 * @see {@link #readString(int, String)}
	 */
	public String readString(int bytes) throws IOException;

	/**
	 * Reads a null terminated String from the input.
	 *
	 * @param encoding a String that translates into a {@link Charset} Encoding.
	 * @return a String
	 * @throws IOException
	 * @throws IllegalCharsetNameException If the given charset name is illegal.
	 * @throws UnsupportedCharsetException If the given charsetName is null.
	 * @see {@link #readNullString(Charset)}
	 * @see {@link #readNullString()}
	 */
	public default String readNullString(String encoding) throws IOException {
		return readNullString(Charset.forName(encoding));
	}

	/**
	 * Reads a null terminated String from the input.
	 *
	 * @param encoding {@link Charset} Encoding of the String.
	 * @return a String
	 * @throws IOException
	 * @see {@link #readNullString(String)}
	 * @see {@link #readNullString()}
	 */
	public String readNullString(Charset encoding) throws IOException;

	/**
	 * Reads a null terminated String from the input using the default encoding.
	 *
	 * @return a String
	 * @throws IOException
	 * @see {@link #readNullString(Charset)}
	 * @see {@link #readNullString(String)}
	 */
	public String readNullString() throws IOException;

	/**
	 * Reads a Pascal String from the input. The string is expected to have a length prefix of 1 byte.
	 *
	 * @param encoding a String that translates into a {@link Charset} Encoding.
	 * @return a String
	 * @throws IOException
	 * @throws IllegalCharsetNameException If the given charset name is illegal.
	 * @throws UnsupportedCharsetException If the given charsetName is null.
	 * @see {@link #readPascalByte(Charset)}
	 * @see {@link #readPascalByte()}
	 */
	public default String readPascalByte(String encoding) throws IOException {
		return readPascalByte(Charset.forName(encoding));
	}

	/**
	 * Reads a Pascal String from the input. The string is expected to have a length prefix of 1 byte.
	 *
	 * @param encoding {@link Charset} Encoding of the String.
	 * @return a String
	 * @throws IOException
	 * @see {@link #readPascalByte(String)}
	 * @see {@link #readPascalByte()}
	 */
	public default String readPascalByte(Charset encoding) throws IOException {
		short b = readUByte();
		return readString(b, encoding);
	}

	/**
	 * Reads a Pascal String from the input using the default encoding. The string is expected to have a length prefix
	 * of 1 byte.
	 *
	 * @return a String
	 * @throws IOException
	 * @see {@link #readPascalByte(Charset)}
	 * @see {@link #readPascalByte(String)}
	 */
	public String readPascalByte() throws IOException;

	/**
	 * Reads a Pascal String from the input. The string is expected to have a length prefix of 2 bytes.
	 *
	 * @param encoding a String that translates into a {@link Charset} Encoding.
	 * @return a String
	 * @throws IOException
	 * @throws IllegalCharsetNameException If the given charset name is illegal.
	 * @throws UnsupportedCharsetException If the given charsetName is null.
	 * @see {@link #readPascalShort(Charset)}
	 * @see {@link #readPascalShort()}
	 */
	public default String readPascalShort(String encoding) throws IOException {
		return readPascalShort(Charset.forName(encoding));
	}

	/**
	 * Reads a Pascal String from the input. The string is expected to have a length prefix of 2 bytes.
	 *
	 * @param encoding {@link Charset} Encoding of the String.
	 * @return a String
	 * @throws IOException
	 * @see {@link #readPascalShort(String)}
	 * @see {@link #readPascalShort()}
	 */
	public default String readPascalShort(Charset encoding) throws IOException {
		int s = readUShort();
		return readString(s, encoding);
	}

	/**
	 * Reads a Pascal String from the input using the default encoding. The string is expected to have a length prefix
	 * of 2 bytes.
	 *
	 * @return a String
	 * @throws IOException
	 * @see {@link #readPascalShort(Charset)}
	 * @see {@link #readPascalShort(String)}
	 */
	public String readPascalShort() throws IOException;

	/**
	 * Reads a Pascal String from the input. The string is expected to have a length prefix of 4 bytes.
	 *
	 * @param encoding a String that translates into a {@link Charset} Encoding.
	 * @return a String
	 * @throws IOException
	 * @throws IllegalCharsetNameException If the given charset name is illegal.
	 * @throws UnsupportedCharsetException If the given charsetName is null.
	 * @see {@link #readPascalInt(Charset)}
	 * @see {@link #readPascalInt()}
	 */
	public default String readPascalInt(String encoding) throws IOException {
		return readPascalInt(Charset.forName(encoding));
	}

	/**
	 * Reads a Pascal String from the input. The string is expected to have a length prefix of 4 bytes.
	 *
	 * @param encoding {@link Charset} Encoding of the String.
	 * @return a String
	 * @throws IOException
	 * @see {@link #readPascalInt(String)}
	 * @see {@link #readPascalInt()}
	 */
	public default String readPascalInt(Charset encoding) throws IOException {
		int i = readSUInt();
		return readString(i, encoding);
	}

	/**
	 * Reads a Pascal String from the input using the default encoding. The string is expected to have a length prefix
	 * of 4 bytes.
	 *
	 * @return a String
	 * @throws IOException
	 * @see {@link #readPascalInt(Charset)}
	 * @see {@link #readPascalInt(String)}
	 */
	public String readPascalInt() throws IOException;

	/**
	 * Reads bytes from the input until a newline character or EOF.
	 *
	 * @param encoding a String that translates into a {@link Charset} Encoding.
	 * @return the line read.
	 * @throws IOException
	 * @throws IllegalCharsetNameException If the given charset name is illegal.
	 * @throws UnsupportedCharsetException If the given charsetName is null.
	 * @see {@link #readLine(Charset)}
	 * @see {@link #readLine()}
	 */
	public default String readLine(String encoding) throws IOException {
		return readLine(Charset.forName(encoding));
	}

	/**
	 * Reads bytes from the input until a newline character or EOF.
	 *
	 * @param encoding {@link Charset} Encoding of the String.
	 * @return the line read.
	 * @throws IOException
	 * @see {@link #readLine(String)}
	 * @see {@link #readLine()}
	 */
	public String readLine(Charset encoding) throws IOException;

	/**
	 * Reads bytes from the input using the default encoding until a newline character or EOF.
	 *
	 * @return the line read.
	 * @throws IOException
	 * @see {@link #readLine(Charset)}
	 * @see {@link #readLine(String)}
	 */
	public String readLine() throws IOException;

	/**
	 * Reads a byte from the input and compares against a value.
	 *
	 * @param b value to compare against.
	 * @return true if values are the same.
	 * @throws IOException
	 */
	public default boolean checkByte(byte b) throws IOException {
		return b == readByte();
	}

	/**
	 * Reads a short from the input and compares against a value.
	 *
	 * @param s value to compare against.
	 * @return true if values are the same.
	 * @throws IOException
	 */
	public default boolean checkShort(short s) throws IOException {
		return s == readShort();
	}

	/**
	 * Reads an int from the input and compares against a value.
	 *
	 * @param i value to compare against.
	 * @return true if values are the same.
	 * @throws IOException
	 */
	public default boolean checkInt(int i) throws IOException {
		return i == readInt();
	}

	/**
	 * Reads a long from the input and compares against a value.
	 *
	 * @param l value to compare against.
	 * @return true if values are the same.
	 * @throws IOException
	 */
	public default boolean checkLong(long l) throws IOException {
		return l == readLong();
	}

	/**
	 * Reads an unsigned byte from the input and compares against a value.
	 *
	 * @param b value to compare against.
	 * @return true if values are the same.
	 * @throws IOException
	 */
	public default boolean checkUByte(int b) throws IOException {
		return b == readUByte();
	}

	/**
	 * Reads an unsigned short from the input and compares against a value.
	 *
	 * @param s value to compare against.
	 * @return true if values are the same.
	 * @throws IOException
	 */
	public default boolean checkUShort(int s) throws IOException {
		return s == readUShort();
	}

	/**
	 * Reads an unsigned int from the input and compares against a value.
	 *
	 * @param i value to compare against.
	 * @return true if values are the same.
	 * @throws IOException
	 */
	public default boolean checkUInt(long i) throws IOException {
		return i == readUInt();
	}

	/**
	 * Reads a float from the input and compares against a value.
	 *
	 * @param f value to compare against.
	 * @return true if values are the same.
	 * @throws IOException
	 */

	public default boolean checkFloat(float f) throws IOException {
		return f == readFloat();
	}

	/**
	 * Reads a double from the input and compares against a value.
	 *
	 * @param d value to compare against.
	 * @return true if values are the same.
	 * @throws IOException
	 */
	public default boolean checkDouble(double d) throws IOException {
		return d == readDouble();
	}

	/**
	 * Reads a bytes from the input and compares against a value. The amount of bytes read is the same as the length of
	 * the provided array.
	 *
	 * @param data values to compare against.
	 * @return true if values are the same.
	 * @throws IOException
	 */
	public default boolean checkBytes(byte[] data) throws IOException {
		byte[] buffer = new byte[data.length];
		if (read(buffer) != buffer.length)
			return false;
		for (int i = 0; i < data.length; i++)
			if (data[i] != buffer[i])
				return false;
		return true;
	}

	/**
	 * Skips ahead <code>count</code> bytes.
	 *
	 * @param count number of bytes to skip.
	 * @throws IOException
	 */
	public void skipBytes(long count) throws IOException;

	/**
	 * Checks if there is at least one byte available to read.
	 *
	 * @throws IOException
	 */
	public boolean hasData() throws IOException;

	/**
	 * Returns the number of bytes read/skipped.
	 *
	 * @throws IOException
	 */
	public int position();

	/**
	 * Creates a sequential bounded viewing window of size <code>length</code> bytes over this reader starting from
	 * {@link #position()}.
	 * <p>
	 * <b>Framing Semantics:</b>
	 * <ul>
	 *   <li><b>Synchronized Cursor:</b> Reading from the returned frame reader advances the parent reader's cursor in lockstep.</li>
	 *   <li><b>Auto-Alignment on Close:</b> When {@link #close()} is called on the framed reader, any unconsumed bytes remaining
	 *       in the frame window are automatically skipped on the parent reader, aligning the parent reader's cursor to the end
	 *       of the frame (i.e. {@code initialPosition + length}).</li>
	 *   <li><b>Boundary Enforcement:</b> Attempting to read beyond {@code length} bytes through the framed reader results in an
	 *       {@link EOFException}, preventing overruns into subsequent frames or payload boundaries.</li>
	 *   <li><b>Usage Pattern:</b> Ideal for sequential parsing of framed network packets, TLV (Type-Length-Value) records, and
	 *       length-prefixed protocol blocks where payloads are consumed sequentially and clean alignment is required even if fields
	 *       are skipped.</li>
	 * </ul>
	 *
	 * @param length number of bytes in the framed window. Must be non-negative.
	 * @return an {@link OwlReader} scoped to read at most <code>length</code> bytes.
	 * @throws IndexOutOfBoundsException if <code>length &lt; 0</code>.
	 * @throws IllegalArgumentException if <code>length</code> exceeds the available data on bounded readers.
	 */
	public default OwlReader frame(int length) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Creates a detached, concurrent-safe sub-reader containing the next <code>length</code> bytes starting from
	 * {@link #position()}.
	 * <p>
	 * <b>Slicing Semantics:</b>
	 * <ul>
	 *   <li><b>Immediate Parent Advancement:</b> Unlike {@link #frame(int)}, invoking {@code slice(length)}
	 *       <b>immediately advances</b> this parent reader's cursor by <code>length</code> bytes upon creation.</li>
	 *   <li><b>Independent Cursor:</b> The returned sub-reader maintains its own isolated position cursor starting at index 0.
	 *       Subsequent operations on the slice have zero effect on the parent reader's position, and operations on the parent
	 *       do not affect the slice.</li>
	 *   <li><b>Zero Side-Effect Close:</b> Closing the slice is a no-op with respect to the parent reader; it does not skip
	 *       bytes or modify the parent's position.</li>
	 *   <li><b>Zero-Copy Performance:</b> For memory-backed readers (such as {@link ByteArrayOwlReader}, {@link NIOReader},
	 *       {@link MemorySegmentOwlReader}, and {@code Buffer}), slicing is a <b>100% zero-copy</b> operation that shares the
	 *       underlying bytes without allocating duplicate arrays or copying data. For streaming readers (such as
	 *       {@link InputStreamOwlReader}), the requested <code>length</code> bytes are eagerly buffered into memory to safely
	 *       decouple the sub-reader from the parent stream and prevent blocking or deadlock.</li>
	 * </ul>
	 *
	 * @param length number of bytes in the slice. Must be non-negative.
	 * @return an independent, detached {@link OwlReader} containing the <code>length</code> bytes.
	 * @throws IndexOutOfBoundsException if <code>length &lt; 0</code>.
	 * @throws EOFException if fewer than <code>length</code> bytes remain before reaching the end of the data.
	 * @throws IOException if an I/O error occurs during slicing.
	 */
	public default OwlReader slice(int length) throws IOException {
		if (length < 0) {
			throw new IndexOutOfBoundsException("length cannot be negative: " + length);
		}
		byte[] data = new byte[length];
		int totalRead = 0;
		while (totalRead < length) {
			int read = this.read(data, totalRead, length - totalRead);
			if (read <= 0) {
				throw new EOFException("Unexpected EOF while slicing: requested " + length + ", read " + totalRead);
			}
			totalRead += read;
		}
		return new ByteArrayOwlReader(data, getEndian());
	}

	/**
	 * Creates a detached, concurrent-safe sub-reader containing <code>length</code> bytes starting at the specified
	 * absolute byte <code>offset</code>.
	 * <p>
	 * <b>Absolute Slicing Semantics:</b>
	 * <ul>
	 *   <li><b>Untouched Parent Cursor:</b> The parent reader's current {@link #position()} is completely unchanged by
	 *       this call, and closing the returned slice has no side-effects on the parent.</li>
	 *   <li><b>Independent Cursor:</b> The returned sub-reader maintains its own isolated position cursor starting at index 0
	 *       relative to the slice.</li>
	 *   <li><b>Zero-Copy:</b> On random-access and memory-backed readers (such as {@link ByteArrayOwlReader},
	 *       {@link NIOReader}, {@link MemorySegmentOwlReader}, and {@code Buffer}), this operation produces a zero-copy
	 *       window over the underlying storage.</li>
	 *   <li><b>Random Access Requirement:</b> Streaming implementations that do not support random seeking throw
	 *       {@link UnsupportedOperationException}.</li>
	 * </ul>
	 *
	 * @param offset the absolute starting byte index within the underlying storage. Must be non-negative.
	 * @param length number of bytes in the slice. Must be non-negative.
	 * @return an independent, detached {@link OwlReader} containing the sliced window.
	 * @throws IndexOutOfBoundsException if <code>offset &lt; 0</code>, <code>length &lt; 0</code>, or
	 *                                   <code>offset + length</code> exceeds the bounds of the reader.
	 * @throws UnsupportedOperationException if this reader does not support absolute offset indexing.
	 * @throws IOException if an I/O error occurs.
	 */
	public default OwlReader slice(int offset, int length) throws IOException {
		throw new UnsupportedOperationException();
	}

	public default InputStream toInputStream() {
		return new InputStream() {

			@Override
			public int read() throws IOException {
				if (!OwlReader.this.hasData())
					return -1;
				return OwlReader.this.readByte() & 0xFF;
			}

			@Override
			public int read(@SuppressWarnings("null") byte @Nullable [] b) throws IOException {
				Objects.requireNonNull(b);
				return read(b, 0, b.length);
			}

			@Override
			public int read(@SuppressWarnings("null") byte @Nullable [] b, int off, int len) throws IOException {
				Objects.requireNonNull(b);
				Objects.checkFromIndexSize(off, len, b.length);
				if (len == 0)
					return 0;
				if (!OwlReader.this.hasData())
					return -1;
				int read = 0;
				while (read < len && OwlReader.this.hasData()) {
					b[off + read++] = OwlReader.this.readByte();
				}
				return read;
			}

			@Override
			public long skip(long n) throws IOException {
				OwlReader.this.skipBytes(n);
				return n;
			}

		};
	}

}

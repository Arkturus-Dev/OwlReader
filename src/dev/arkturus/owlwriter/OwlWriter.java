package dev.arkturus.owlwriter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.function.Function;

import dev.arkturus.nullanalysis.annotations.Nullable;
import dev.arkturus.owl.Endian;
import dev.arkturus.owl.UnsafeUnsignedException;
import dev.arkturus.owl.VarInt;

public interface OwlWriter extends Closeable {

	public void writeByte(byte b) throws IOException;

	public void writeShort(short s) throws IOException;

	/**
	 * Writes a 24-bit medium integer (3 bytes) according to the current endianness.
	 *
	 * @param i the integer value to write (lower 24 bits).
	 * @throws IOException if an I/O error occurs.
	 */
	public default void writeInt24(int i) throws IOException {
		byte[] buffer = new byte[3];
		getEndian().writeInt24(i, buffer, 0);
		write(buffer);
	}

	/**
	 * Writes a 24-bit medium integer (3 bytes) according to the current endianness. Alias for {@link #writeInt24(int)}.
	 *
	 * @param i the integer value to write (lower 24 bits).
	 * @throws IOException if an I/O error occurs.
	 */
	public default void writeMedium(int i) throws IOException {
		writeInt24(i);
	}

	public void writeInt(int i) throws IOException;

	public void writeLong(long l) throws IOException;

	public default void writeFloat(float f) throws IOException {
		writeInt(Float.floatToRawIntBits(f));
	}

	public default void writeDouble(double d) throws IOException {
		writeLong(Double.doubleToRawLongBits(d));
	}

	/**
	 * Writes a variable-length 32-bit integer (VarInt) using 7-bit continuation chunks.
	 *
	 * @param value the integer value to encode.
	 * @throws IOException if an I/O error occurs.
	 */
	public default void writeVarInt(int value) throws IOException {
		while ((value & ~0x7F) != 0) {
			writeByte((byte) ((value & 0x7F) | 0x80));
			value >>>= 7;
		}
		writeByte((byte) (value & 0x7F));
	}

	/**
	 * Writes a variable-length 64-bit long (VarLong) using 7-bit continuation chunks.
	 *
	 * @param value the long value to encode.
	 * @throws IOException if an I/O error occurs.
	 */
	public default void writeVarLong(long value) throws IOException {
		while ((value & ~0x7FL) != 0) {
			writeByte((byte) (((int) value & 0x7F) | 0x80));
			value >>>= 7;
		}
		writeByte((byte) ((int) value & 0x7F));
	}

	/**
	 * Writes a 32-bit signed integer using ZigZag and VarInt encoding.
	 *
	 * @param value the signed integer to encode.
	 * @throws IOException if an I/O error occurs.
	 */
	public default void writeSignedVarInt(int value) throws IOException {
		writeVarInt(VarInt.encodeZigZag32(value));
	}

	/**
	 * Writes a 32-bit signed integer using ZigZag and VarInt encoding. Alias for {@link #writeSignedVarInt(int)}.
	 *
	 * @param value the signed integer to encode.
	 * @throws IOException if an I/O error occurs.
	 */
	public default void writeZigZagVarInt(int value) throws IOException {
		writeSignedVarInt(value);
	}

	/**
	 * Writes a 64-bit signed long using ZigZag and VarLong encoding.
	 *
	 * @param value the signed long to encode.
	 * @throws IOException if an I/O error occurs.
	 */
	public default void writeSignedVarLong(long value) throws IOException {
		writeVarLong(VarInt.encodeZigZag64(value));
	}

	/**
	 * Writes a 64-bit signed long using ZigZag and VarLong encoding. Alias for {@link #writeSignedVarLong(long)}.
	 *
	 * @param value the signed long to encode.
	 * @throws IOException if an I/O error occurs.
	 */
	public default void writeZigZagVarLong(long value) throws IOException {
		writeSignedVarLong(value);
	}

	public default void writeUByte(short s) throws IOException {
		writeByte((byte) s);
	}

	public default void writeSUByte(short s) throws IOException {
		if (s < 0 || s > Byte.MAX_VALUE)
			throw new UnsafeUnsignedException(byte.class);
		writeByte((byte) s);
	}

	public default void writeUShort(int i) throws IOException {
		writeShort((short) i);
	}

	public default void writeSUShort(int i) throws IOException {
		if (i < 0 || i > Short.MAX_VALUE)
			throw new UnsafeUnsignedException(short.class);
		writeShort((short) i);
	}

	public default void writeUInt24(int i) throws IOException {
		writeInt24(i);
	}

	public default void writeUMedium(int i) throws IOException {
		writeInt24(i);
	}

	public default void writeSUInt24(int i) throws IOException {
		if (i < 0 || i > 0x007FFFFF)
			throw new UnsafeUnsignedException(int.class);
		writeInt24(i);
	}

	public default void writeSUMedium(int i) throws IOException {
		writeSUInt24(i);
	}

	public default void writeUInt(long l) throws IOException {
		writeInt((int) l);
	}

	public default void writeSUInt(long l) throws IOException {
		if (l < 0 || l > Integer.MAX_VALUE)
			throw new UnsafeUnsignedException(int.class);
		writeInt((int) l);
	}

	/**
	 * Get the default endianness.
	 *
	 * @return the default ednianness.
	 */
	public Endian getEndian();

	/**
	 * Set the endianness.
	 *
	 * @param endian the {@link Endian} byte order.
	 * @see {@link #writeShort()}
	 * @see {@link #writeInt()}
	 * @see {@link #writeLong()}
	 * @see {@link #writeFloat()}
	 * @see {@link #writeDouble()}
	 */
	public void setEndian(Endian endian);

	/**
	 * Get the current default encoding.
	 *
	 * @return the current default encoding.
	 */
	public Charset getDefaultEncoding();

	/**
	 * Set the default encoding. This encoding is used by default on write String methods that do not specify an
	 * encoding.
	 *
	 * @param encoding a String that translates into a {@link Charset} Encoding.
	 * @throws IllegalCharsetNameException If the given charset name is illegal.
	 * @throws UnsupportedCharsetException If the given charsetName is null.
	 * @see {@link #writeString(String)}
	 * @see {@link #writeNullString(String)}
	 * @see {@link #writePascalByte(String)}
	 * @see {@link #writePascalShort(String)}
	 * @see {@link #writePascalInt(String)}
	 * @see {@link #writeLine(String)}
	 */
	public default void setDefaultEncoding(String encoding) {
		setDefaultEncoding(Charset.forName(encoding));
	}

	/**
	 * Set the default encoding. This encoding is used by default on write String methods that do not specify an
	 * encoding.
	 *
	 * @param encoding {@link Charset} Encoding of the String.
	 * @see {@link #writeString(String)}
	 * @see {@link #writeNullString(String)}
	 * @see {@link #writePascalByte(String)}
	 * @see {@link #writePascalShort(String)}
	 * @see {@link #writePascalInt(String)}
	 * @see {@link #writeLine(String)}
	 */
	public void setDefaultEncoding(Charset encoding);

	/**
	 * Writes a String.
	 *
	 * @param s        the String to write.
	 * @param encoding a String that translates into a {@link Charset} Encoding.
	 * @throws IOException
	 * @throws IllegalCharsetNameException If the given charset name is illegal.
	 * @throws UnsupportedCharsetException If the given charset name is null.
	 * @see {@link #writeString(String, Charset)}
	 * @see {@link #writeString(String)}
	 */
	public default void writeString(String s, String encoding) throws IOException {
		writeString(s, Charset.forName(encoding));
	}

	/**
	 * Writes a String.
	 *
	 * @param s        the String to write.
	 * @param encoding {@link Charset} Encoding of the String.
	 * @throws IOException
	 * @see {@link #writeString(String, String)}
	 * @see {@link #writeString(String)}
	 */
	public default void writeString(String s, Charset encoding) throws IOException {
		ByteBuffer bytes = encoding.encode(s);
		write(bytes.array(), 0, bytes.limit());
	}

	/**
	 * Writes a String using the default encoding.
	 *
	 * @param s the String to write.
	 * @throws IOException
	 * @see {@link #writeString(String, Charset)}
	 * @see {@link #writeString(String, String)}
	 */
	public void writeString(String s) throws IOException;

	/**
	 * Writes a null terminated String.
	 *
	 * @param s        the String to write.
	 * @param encoding a String that translates into a {@link Charset} Encoding.
	 * @throws IOException
	 * @throws IllegalCharsetNameException If the given charset name is illegal.
	 * @throws UnsupportedCharsetException If the given charset name is null.
	 * @see {@link #writeNullString(String, Charset)}
	 * @see {@link #writeNullString(String)}
	 */
	public default void writeNullString(String s, String encoding) throws IOException {
		writeNullString(s, Charset.forName(encoding));
	}

	/**
	 * Writes a null terminated String.
	 *
	 * @param s        the String to write.
	 * @param encoding {@link Charset} Encoding of the String.
	 * @throws IOException
	 * @see {@link #writeNullString(String, String)}
	 * @see {@link #writeNullString(String)}
	 */
	public default void writeNullString(String s, Charset encoding) throws IOException {
		ByteBuffer bytes = encoding.encode(s);
		write(bytes.array(), 0, bytes.limit());
		writeByte((byte) 0);
	}

	/**
	 * Writes a null terminated String using the default encoding.
	 *
	 * @param s the String to write.
	 * @throws IOException
	 * @see {@link #writeNullString(String, Charset)}
	 * @see {@link #writeNullString(String, String)}
	 */
	public void writeNullString(String s) throws IOException;

	/**
	 * Writes a Pascal String with a length prefix of 1 byte.
	 *
	 * @param s        the String to write.
	 * @param encoding a String that translates into a {@link Charset} Encoding.
	 * @throws IOException
	 * @throws IllegalCharsetNameException If the given charset name is illegal.
	 * @throws UnsupportedCharsetException If the given charset name is null.
	 * @see {@link #writePascalByte(Charset)}
	 * @see {@link #writePascalByte()}
	 */
	public default void writePascalByte(String s, String encoding) throws IOException {
		writePascalByte(s, Charset.forName(encoding));
	}

	/**
	 * Writes a Pascal String with a length prefix of 1 byte.
	 *
	 * @param s        the String to write.
	 * @param encoding {@link Charset} Encoding of the String.
	 * @throws IOException
	 * @see {@link #writePascalByte(String, String)}
	 * @see {@link #writePascalByte(String)}
	 */
	public default void writePascalByte(String s, Charset encoding) throws IOException {
		ByteBuffer bytes = encoding.encode(s);
		writeUByte((short) bytes.limit());
		write(bytes.array(), 0, bytes.limit());
	}

	/**
	 * Writes a Pascal String using the default encoding with a length prefix of 1 byte.
	 *
	 * @param s the String to write.
	 * @throws IOException
	 * @see {@link #writePascalByte(String, Charset)}
	 * @see {@link #writePascalByte(String, String)}
	 */
	public void writePascalByte(String s) throws IOException;

	/**
	 * Writes a Pascal String with a length prefix of 2 bytes.
	 *
	 * @param s        the String to write.
	 * @param encoding a String that translates into a {@link Charset} Encoding.
	 * @throws IOException
	 * @throws IllegalCharsetNameException If the given charset name is illegal.
	 * @throws UnsupportedCharsetException If the given charset name is null.
	 * @see {@link #writePascalShort(String, Charset)}
	 * @see {@link #writePascalShort(String)}
	 */
	public default void writePascalShort(String s, String encoding) throws IOException {
		writePascalShort(s, Charset.forName(encoding));
	}

	/**
	 * Writes a Pascal String with a length prefix of 2 bytes.
	 *
	 * @param s        the String to write.
	 * @param encoding {@link Charset} Encoding of the String.
	 * @throws IOException
	 * @see {@link #writePascalShort(String, String)}
	 * @see {@link #writePascalShort(String)}
	 */
	public default void writePascalShort(String s, Charset encoding) throws IOException {
		ByteBuffer bytes = encoding.encode(s);
		writeUShort(bytes.limit());
		write(bytes.array(), 0, bytes.limit());
	}

	/**
	 * Writes a Pascal String using the default encoding with a length prefix of 2 bytes.
	 *
	 * @param s the String to write.
	 * @throws IOException
	 * @see {@link #writePascalShort(String, Charset)}
	 * @see {@link #writePascalShort(String, String)}
	 */
	public void writePascalShort(String s) throws IOException;

	/**
	 * Writes a Pascal String with a length prefix of 4 bytes.
	 *
	 * @param s        the String to write.
	 * @param encoding a String that translates into a {@link Charset} Encoding.
	 * @throws IOException
	 * @throws IllegalCharsetNameException If the given charset name is illegal.
	 * @throws UnsupportedCharsetException If the given charset name is null.
	 * @see {@link #writePascalInt(String, Charset)}
	 * @see {@link #writePascalInt(String)}
	 */
	public default void writePascalInt(String s, String encoding) throws IOException {
		writePascalInt(s, Charset.forName(encoding));
	}

	/**
	 * Writes a Pascal String with a length prefix of 4 bytes.
	 *
	 * @param s        the String to write.
	 * @param encoding {@link Charset} Encoding of the String.
	 * @throws IOException
	 * @see {@link #writePascalInt(String, String)}
	 * @see {@link #writePascalInt(String)}
	 */
	public default void writePascalInt(String s, Charset encoding) throws IOException {
		ByteBuffer bytes = encoding.encode(s);
		writeSUInt(bytes.limit());
		write(bytes.array(), 0, bytes.limit());
	}

	/**
	 * Writes a Pascal String using the default encoding with a length prefix of 4 bytes.
	 *
	 * @param s the String to write.
	 * @throws IOException
	 * @see {@link #writePascalInt(String, Charset)}
	 * @see {@link #writePascalInt(String, String)}
	 */
	public void writePascalInt(String s) throws IOException;

	/**
	 * Writes a String terminating with a newline.
	 *
	 * @param s        the String to write.
	 * @param encoding a String that translates into a {@link Charset} Encoding.
	 * @throws IOException
	 * @throws IllegalCharsetNameException If the given charset name is illegal.
	 * @throws UnsupportedCharsetException If the given charset name is null.
	 * @see {@link #writeLine(String, Charset)}
	 * @see {@link #writeLine(String)}
	 */
	public default void writeLine(String s, String encoding) throws IOException {
		writeLine(s, Charset.forName(encoding));
	}

	/**
	 * Writes a String terminating with a newline.
	 *
	 * @param s        the String to write.
	 * @param encoding {@link Charset} Encoding of the String.
	 * @throws IOException
	 * @see {@link #writeLine(String, String)}
	 * @see {@link #writeLine(String)}
	 */
	public default void writeLine(String s, Charset encoding) throws IOException {
		writeString(s, encoding);
		writeByte((byte) '\n');
	}

	/**
	 * Writes a String terminating with a newline using the default encoding.
	 *
	 * @param s the String to write.
	 * @throws IOException
	 * @see {@link #writeLine(String, Charset)}
	 * @see {@link #writeLine(String, String)}
	 */
	public void writeLine(String s) throws IOException;

	public default void write(byte[] bytes) throws IOException {
		write(bytes, 0, bytes.length);
	}

	public void write(byte[] bytes, int off, int len) throws IOException;

	public default void writeObject(@Nullable Object o) throws IOException {
		if (o == null)
			writeString("null");
		else
			writeString(o.toString());
	}

	public default void writeIterable(Iterable<@Nullable ?> iterable, String delim, String prefix, String suffix)
			throws IOException {
		writeString(prefix);
		writeIterable(iterable, delim, "", "");
		writeString(suffix);
	}

	public default void writeIterable(Iterable<@Nullable ?> iterable, String prefix, String suffix) throws IOException {
		writeIterable(iterable, ", ", prefix, suffix);
	}

	public default void writeIterable(Iterable<@Nullable ?> iterable, String delim) throws IOException {
		int i = 0;
		for (Object item : iterable) {
			if (i++ > 0)
				writeString(delim);
			writeObject(item);
		}
	}

	public default void writeIterable(Iterable<@Nullable ?> iterable) throws IOException {
		writeIterable(iterable, ", ");
	}

	public default <T> void writeIterable(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn, String delim,
			String prefix, String suffix) throws IOException {
		writeString(prefix);
		writeIterable(iterable, fn, delim, "", "");
		writeString(suffix);
	}

	public default <T> void writeIterable(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn, String prefix,
			String suffix) throws IOException {
		writeIterable(iterable, fn, ", ", prefix, suffix);
	}

	public default <T> void writeIterable(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn, String delim)
			throws IOException {
		int i = 0;
		for (@Nullable T item : iterable) {
			if (i++ > 0)
				writeString(delim);
			if (item == null)
				writeObject(null);
			else
				writeObject(fn.apply(item));
		}
	}

	public default <T> void writeIterable(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn)
			throws IOException {
		writeIterable(iterable, fn, ", ");
	}

	public default OwlWriter frame(int length) {
		throw new UnsupportedOperationException();
	}

}

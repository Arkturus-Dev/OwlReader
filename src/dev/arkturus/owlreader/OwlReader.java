package dev.arkturus.owlreader;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

public interface OwlReader extends Readable {

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
		if (read(data) != buffer.length)
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

}

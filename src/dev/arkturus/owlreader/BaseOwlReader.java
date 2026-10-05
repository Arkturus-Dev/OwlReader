package dev.arkturus.owlreader;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import dev.arkturus.owl.Endian;

public abstract class BaseOwlReader implements OwlReader {

	private volatile Endian endian = Endian.BIG;
	private volatile Charset encoding = StandardCharsets.UTF_8;

	@Override
	public Endian getEndian() {
		return endian;
	}

	@Override
	public void setEndian(Endian endian) {
		this.endian = endian;
	}

	@Override
	public Charset getDefaultEncoding() {
		return encoding;
	}

	@Override
	public void setDefaultEncoding(Charset encoding) {
		this.encoding = encoding;
	}

	@Override
	public short readShort() throws IOException {
		return getEndian().readShort(read(2), 0);
	}

	@Override
	public int readInt24() throws IOException {
		return getEndian().readInt24(read(3), 0);
	}

	@Override
	public int readUInt24() throws IOException {
		return getEndian().readUInt24(read(3), 0);
	}

	@Override
	public int readInt() throws IOException {
		return getEndian().readInt(read(4), 0);
	}

	@Override
	public long readLong() throws IOException {
		return getEndian().readLong(read(8), 0);
	}

	@Override
	public String readString(int bytes) throws IOException {
		return readString(bytes, getDefaultEncoding());
	}

	@Override
	public String readNullString() throws IOException {
		return readNullString(getDefaultEncoding());
	}

	@Override
	public String readPascalByte() throws IOException {
		return readPascalByte(getDefaultEncoding());
	}

	@Override
	public String readPascalShort() throws IOException {
		return readPascalShort(getDefaultEncoding());
	}

	@Override
	public String readPascalInt() throws IOException {
		return readPascalInt(getDefaultEncoding());
	}

	@Override
	public String readLine() throws IOException {
		return readLine(getDefaultEncoding());
	}

}
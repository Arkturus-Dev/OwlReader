package dev.arkturus.owlreader;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import dev.arkturus.owl.Endian;

public abstract class BaseOwlReader implements OwlReader {

	private Endian endian = Endian.BIG;
	private Charset encoding = StandardCharsets.UTF_8;

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
		return endian.readShort(read(2), 0);
	}

	@Override
	public int readInt() throws IOException {
		return endian.readInt(read(4), 0);
	}

	@Override
	public long readLong() throws IOException {
		return endian.readLong(read(8), 0);
	}

	@Override
	public String readString(int bytes) throws IOException {
		return readString(bytes, encoding);
	}

	@Override
	public String readNullString() throws IOException {
		return readNullString(encoding);
	}

	@Override
	public String readPascalByte() throws IOException {
		return readPascalByte(encoding);
	}

	@Override
	public String readPascalShort() throws IOException {
		return readPascalShort(encoding);
	}

	@Override
	public String readPascalInt() throws IOException {
		return readPascalInt(encoding);
	}

	@Override
	public String readLine() throws IOException {
		return readLine(encoding);
	}

}
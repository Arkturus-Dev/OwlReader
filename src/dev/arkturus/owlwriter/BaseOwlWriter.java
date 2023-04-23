package dev.arkturus.owlwriter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import dev.arkturus.owl.Endian;

public abstract class BaseOwlWriter implements OwlWriter {

	private volatile Endian endian = Endian.BIG;
	protected volatile Charset encoding = StandardCharsets.UTF_8;

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
	public void writeString(String s) throws IOException {
		writeString(s, encoding);
	}

	@Override
	public void writeNullString(String s) throws IOException {
		writeNullString(s, encoding);
	}

	@Override
	public void writePascalByte(String s) throws IOException {
		writePascalByte(s, encoding);
	}

	@Override
	public void writePascalShort(String s) throws IOException {
		writePascalShort(s, encoding);
	}

	@Override
	public void writePascalInt(String s) throws IOException {
		writePascalInt(s, encoding);
	}

	@Override
	public void writeLine(String s) throws IOException {
		writeLine(s, encoding);
	}

}
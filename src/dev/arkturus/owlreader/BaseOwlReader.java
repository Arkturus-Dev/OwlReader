package dev.arkturus.owlreader;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class BaseOwlReader implements OwlReader {

	private Charset encoding = StandardCharsets.UTF_8;

	@Override
	public Charset getDefaultEncoding() {
		return encoding;
	}

	@Override
	public void setDefaultEncoding(Charset encoding) {
		this.encoding = encoding;
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
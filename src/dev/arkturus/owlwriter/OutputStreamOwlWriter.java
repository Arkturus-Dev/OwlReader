package dev.arkturus.owlwriter;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import dev.arkturus.owl.Endian;

public class OutputStreamOwlWriter extends BaseOwlWriter {

	private OutputStream out;
	private byte[]       buffer = new byte[8];

	public OutputStreamOwlWriter(OutputStream out) {
		this.out = out;
	}

	public OutputStreamOwlWriter(OutputStream out, Endian endian) {
		this(out);
		setEndian(endian);
	}

	@Override
	public synchronized void writeByte(byte b) throws IOException {
		out.write(b);
	}

	@Override
	public synchronized void writeShort(short s) throws IOException {
		getEndian().writeShort(s, buffer, 0);
		out.write(buffer, 0, 2);
	}

	@Override
	public synchronized void writeInt(int i) throws IOException {
		getEndian().writeInt(i, buffer, 0);
		out.write(buffer, 0, 4);
	}

	@Override
	public synchronized void writeLong(long l) throws IOException {
		getEndian().writeLong(l, buffer, 0);
		out.write(buffer, 0, 8);
	}

	@Override
	public synchronized void write(byte[] bytes, int off, int len) throws IOException {
		out.write(bytes, off, len);
	}

	@Override
	public synchronized void writeNullString(String s, Charset encoding) throws IOException {
		super.writeNullString(s, encoding);
	}

	@Override
	public synchronized void writePascalByte(String s, Charset encoding) throws IOException {
		super.writePascalByte(s, encoding);
	}

	@Override
	public synchronized void writePascalShort(String s, Charset encoding) throws IOException {
		super.writePascalShort(s, encoding);
	}

	@Override
	public synchronized void writePascalInt(String s, Charset encoding) throws IOException {
		super.writePascalInt(s, encoding);
	}

	@Override
	public synchronized void close() throws IOException {
		out.close();
	}

	@Override
	public OwlWriter frame(int length) {
		return new FramedOutputStreamOwlWriter(length);
	}

	private class FramedOutputStreamOwlWriter extends BaseOwlWriter {

		private int position;
		private int length;

		public FramedOutputStreamOwlWriter(int length) {
			this.length = length;
		}

		@Override
		public void writeByte(byte b) throws IOException {
			advance(1);
			OutputStreamOwlWriter.this.writeByte(b);
		}

		@Override
		public void writeShort(short s) throws IOException {
			advance(2);
			OutputStreamOwlWriter.this.writeShort(s);
		}

		@Override
		public void writeInt(int i) throws IOException {
			advance(4);
			OutputStreamOwlWriter.this.writeInt(i);
		}

		@Override
		public void writeLong(long l) throws IOException {
			advance(8);
			OutputStreamOwlWriter.this.writeLong(l);
		}

		@Override
		public void write(byte[] bytes, int off, int len) throws IOException {
			advance(len);
			OutputStreamOwlWriter.this.write(bytes, off, len);
		}

		@Override
		public void close() throws IOException {
			for (int i = 0; i < length - position; i++)
				writeByte((byte) 0);
		}

		private void advance(long count) throws IOException {
			int remaining = length - position;
			if (count > remaining)
				throw new EOFException("Unexpected EOF.");
			position += count;
		}

	}

}

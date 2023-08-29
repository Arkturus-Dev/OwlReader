package dev.arkturus.owlreader;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.zip.CRC32;

public class CRCByteArrayOwlReader extends BaseOwlReader implements CRCReader {

	private ByteArrayOwlReader reader;
	private CRC32              crc = new CRC32();

	public CRCByteArrayOwlReader(ByteArrayOwlReader reader) {
		this.reader = reader;
	}

	private void addCRC(int length) {
		crc.update(reader.in, reader.position - length, length);
	}

	@Override
	public int getCRC() {
		synchronized (reader.readLock) {
			long value = crc.getValue();
			crc.reset();
			return (int) value;
		}
	}

	@Override
	public byte readByte() throws IOException {
		synchronized (reader.readLock) {
			byte b = reader.readByte();
			addCRC(1);
			return b;
		}
	}

	@Override
	public short readShort() throws IOException {
		synchronized (reader.readLock) {
			short s = reader.readShort();
			addCRC(2);
			return s;
		}
	}

	@Override
	public int readInt() throws IOException {
		synchronized (reader.readLock) {
			int i = reader.readInt();
			addCRC(4);
			return i;
		}
	}

	@Override
	public long readLong() throws IOException {
		synchronized (reader.readLock) {
			long l = reader.readLong();
			addCRC(8);
			return l;
		}
	}

	@Override
	public int read(byte[] buffer, int off, int len) throws IOException {
		synchronized (reader.readLock) {
			int read = reader.read(buffer, off, len);
			addCRC(read);
			return read;
		}
	}

	@Override
	public String readString(int bytes, Charset encoding) throws IOException {
		synchronized (reader.readLock) {
			String s = reader.readString(bytes, encoding);
			addCRC(bytes);
			return s;
		}
	}

	@Override
	public String readNullString(Charset encoding) throws IOException {
		synchronized (reader.readLock) {
			int start = reader.position();
			String s = reader.readNullString(encoding);
			addCRC(reader.position() - start);
			return s;
		}
	}

	@Override
	public String readPascalByte(Charset encoding) throws IOException {
		synchronized (reader.readLock) {
			int start = reader.position();
			String s = reader.readPascalByte(encoding);
			addCRC(reader.position() - start);
			return s;
		}
	}

	@Override
	public String readPascalShort(Charset encoding) throws IOException {
		synchronized (reader.readLock) {
			int start = reader.position();
			String s = reader.readPascalShort(encoding);
			addCRC(reader.position() - start);
			return s;
		}
	}

	@Override
	public String readPascalInt(Charset encoding) throws IOException {
		synchronized (reader.readLock) {
			int start = reader.position();
			String s = reader.readPascalInt(encoding);
			addCRC(reader.position() - start);
			return s;
		}
	}

	@Override
	public String readLine(Charset encoding) throws IOException {
		synchronized (reader.readLock) {
			int start = reader.position();
			String s = reader.readLine(encoding);
			addCRC(reader.position() - start);
			return s;
		}
	}

	@Override
	public void skipBytes(long count) throws IOException {
		synchronized (reader.readLock) {
			reader.skipBytes(count);
		}
	}

	@Override
	public boolean hasData() {
		synchronized (reader.readLock) {
			return reader.hasData();
		}
	}

	@Override
	public int position() {
		synchronized (reader.readLock) {
			return reader.position();
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (reader.readLock) {
			reader.close();
		}
	}

}

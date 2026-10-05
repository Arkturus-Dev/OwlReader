package dev.arkturus.owl;

public enum Endian {

	BIG, LITTLE;

	public short readShort(byte[] data, int off) {
		switch (this) {
			case LITTLE:
				return (short) (((data[off + 1] & 0xFF) << 8) | (data[off] & 0xFF));
			case BIG:
				return (short) (((data[off] & 0xFF) << 8) | (data[off + 1] & 0xFF));
			default:
				throw new IllegalArgumentException();
		}
	}

	public void writeShort(short s, byte[] buffer, int off) {
		switch (this) {
			case LITTLE:
				buffer[off] = (byte) (s & 0xFF);
				buffer[off + 1] = (byte) ((s >>> 8) & 0xFF);
				break;
			case BIG:
				buffer[off] = (byte) ((s >>> 8) & 0xFF);
				buffer[off + 1] = (byte) (s & 0xFF);
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	public int readInt24(byte[] data, int off) {
		return (readUInt24(data, off) << 8) >> 8;
	}

	public int readUInt24(byte[] data, int off) {
		switch (this) {
			case LITTLE:
				return ((data[off + 2] & 0xFF) << 16) | ((data[off + 1] & 0xFF) << 8) | (data[off] & 0xFF);
			case BIG:
				return ((data[off] & 0xFF) << 16) | ((data[off + 1] & 0xFF) << 8) | (data[off + 2] & 0xFF);
			default:
				throw new IllegalArgumentException();
		}
	}

	public void writeInt24(int i, byte[] buffer, int off) {
		switch (this) {
			case LITTLE:
				buffer[off] = (byte) (i & 0xFF);
				buffer[off + 1] = (byte) ((i >>> 8) & 0xFF);
				buffer[off + 2] = (byte) ((i >>> 16) & 0xFF);
				break;
			case BIG:
				buffer[off] = (byte) ((i >>> 16) & 0xFF);
				buffer[off + 1] = (byte) ((i >>> 8) & 0xFF);
				buffer[off + 2] = (byte) (i & 0xFF);
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	public int readInt(byte[] data, int off) {
		switch (this) {
			case LITTLE:
				return (((data[off + 3] & 0xFF) << 24) | ((data[off + 2] & 0xFF) << 16) | ((data[off + 1] & 0xFF) << 8)
						| (data[off] & 0xFF));
			case BIG:
				return (((data[off] & 0xFF) << 24) | ((data[off + 1] & 0xFF) << 16) | ((data[off + 2] & 0xFF) << 8)
						| (data[off + 3] & 0xFF));
			default:
				throw new IllegalArgumentException();
		}
	}

	public void writeInt(int i, byte[] buffer, int off) {
		switch (this) {
			case LITTLE:
				buffer[off] = (byte) (i & 0xFF);
				buffer[off + 1] = (byte) ((i >>> 8) & 0xFF);
				buffer[off + 2] = (byte) ((i >>> 16) & 0xFF);
				buffer[off + 3] = (byte) ((i >>> 24) & 0xFF);
				break;
			case BIG:
				buffer[off] = (byte) ((i >>> 24) & 0xFF);
				buffer[off + 1] = (byte) ((i >>> 16) & 0xFF);
				buffer[off + 2] = (byte) ((i >>> 8) & 0xFF);
				buffer[off + 3] = (byte) (i & 0xFF);
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	public long readLong(byte[] data, int off) {
		switch (this) {
			case LITTLE:
				return (((long) (data[off + 7] & 0xFF)) << 56) | (((long) (data[off + 6] & 0xFF)) << 48)
						| (((long) (data[off + 5] & 0xFF)) << 40) | (((long) (data[off + 4] & 0xFF)) << 32)
						| (((long) (data[off + 3] & 0xFF)) << 24) | (((long) (data[off + 2] & 0xFF)) << 16)
						| (((long) (data[off + 1] & 0xFF)) << 8) | ((long) (data[off] & 0xFF));
			case BIG:
				return (((long) (data[off] & 0xFF)) << 56) | (((long) (data[off + 1] & 0xFF)) << 48)
						| (((long) (data[off + 2] & 0xFF)) << 40) | (((long) (data[off + 3] & 0xFF)) << 32)
						| (((long) (data[off + 4] & 0xFF)) << 24) | (((long) (data[off + 5] & 0xFF)) << 16)
						| (((long) (data[off + 6] & 0xFF)) << 8) | ((long) (data[off + 7] & 0xFF));
			default:
				throw new IllegalArgumentException();
		}
	}

	public void writeLong(long l, byte[] buffer, int off) {
		switch (this) {
			case LITTLE:
				buffer[off] = (byte) (l & 0xFF);
				buffer[off + 1] = (byte) ((l >>> 8) & 0xFF);
				buffer[off + 2] = (byte) ((l >>> 16) & 0xFF);
				buffer[off + 3] = (byte) ((l >>> 24) & 0xFF);
				buffer[off + 4] = (byte) ((l >>> 32) & 0xFF);
				buffer[off + 5] = (byte) ((l >>> 40) & 0xFF);
				buffer[off + 6] = (byte) ((l >>> 48) & 0xFF);
				buffer[off + 7] = (byte) ((l >>> 56) & 0xFF);
				break;
			case BIG:
				buffer[off] = (byte) ((l >>> 56) & 0xFF);
				buffer[off + 1] = (byte) ((l >>> 48) & 0xFF);
				buffer[off + 2] = (byte) ((l >>> 40) & 0xFF);
				buffer[off + 3] = (byte) ((l >>> 32) & 0xFF);
				buffer[off + 4] = (byte) ((l >>> 24) & 0xFF);
				buffer[off + 5] = (byte) ((l >>> 16) & 0xFF);
				buffer[off + 6] = (byte) ((l >>> 8) & 0xFF);
				buffer[off + 7] = (byte) (l & 0xFF);
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

}
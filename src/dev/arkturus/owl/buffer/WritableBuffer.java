package dev.arkturus.owl.buffer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import dev.arkturus.nullanalysis.annotations.Nullable;
import dev.arkturus.owlwriter.OwlWriter;

public interface WritableBuffer {

	public WritableBuffer write(byte b);

	public default WritableBuffer write(char c) {
		return write((byte) c);
	}

	public default WritableBuffer write(byte[] buffer) {
		return write(buffer, 0, buffer.length);
	}

	public WritableBuffer write(byte[] buffer, int off, int len);

	public WritableBuffer write(String s, Charset encoding);

	public default WritableBuffer write(String s) {
		return write(s, StandardCharsets.UTF_8);
	}

	public default WritableBuffer write(@Nullable Object o) {
		if (o == null)
			write("null");
		else if (Bufferable.class.isAssignableFrom(o.getClass()))
			((Bufferable) o).toBuffer(this);
		else
			write(o.toString());
		return this;
	}

	public default <T> WritableBuffer write(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn, String delim,
			String prefix, String suffix) {
		return write(prefix).write(iterable, fn, delim).write(suffix);
	}

	public default <T> WritableBuffer write(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn, String prefix,
			String suffix) {
		return write(iterable, fn, ", ", prefix, suffix);
	}

	public default <T> WritableBuffer write(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn, String delim) {
		int i = 0;
		for (@Nullable T item : iterable) {
			if (i > 0)
				write(delim);
			if (item == null)
				write("null");
			else
				write(fn.apply(item));
			i++;
		}
		return this;
	}

	public default <T> WritableBuffer write(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn) {
		return write(iterable, fn, ", ");
	}

	public default WritableBuffer write(Iterable<@Nullable ?> iterable, String delim, String prefix, String suffix) {
		return write(prefix).write(iterable, delim).write(suffix);
	}

	public default WritableBuffer write(Iterable<@Nullable ?> iterable, String prefix, String suffix) {
		return write(iterable, ", ", prefix, suffix);
	}

	public default <T> WritableBuffer write(Iterable<@Nullable T> iterable, String delim) {
		int i = 0;
		for (@Nullable T item : iterable) {
			if (i > 0)
				write(delim);
			if (item == null)
				write("null");
			else
				write(item);
			i++;
		}
		return this;
	}

	public default WritableBuffer write(Iterable<@Nullable ?> iterable) {
		return write(iterable, ", ");
	}

	public default WritableBuffer writeln(byte b) {
		return write(b).writeln();
	}

	public default WritableBuffer writeln(char c) {
		return write((byte) c).writeln();
	}

	public default WritableBuffer writeln(String s, Charset encoding) {
		return write(s, encoding).writeln();
	}

	public default WritableBuffer writeln(String s) {
		return write(s, StandardCharsets.UTF_8).writeln();
	}

	public default WritableBuffer writeln(Object o) {
		return write(o).writeln();
	}

	public default WritableBuffer writeln(Iterable<@Nullable ?> iterable, String delim, String prefix, String suffix) {
		return write(iterable, delim, prefix, suffix).writeln();
	}

	public default WritableBuffer writeln(Iterable<@Nullable ?> iterable, String prefix, String suffix) {
		return write(iterable, prefix, suffix).writeln();
	}

	public default WritableBuffer writeln(Iterable<@Nullable ?> iterable, String delim) {
		return write(iterable, delim).writeln();
	}

	public default WritableBuffer writeln(Iterable<@Nullable ?> iterable) {
		return write(iterable).writeln();
	}

	public default <T> WritableBuffer writeln(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn, String delim,
			String prefix,
			String suffix) {
		return write(iterable, fn, delim, prefix, suffix).writeln();
	}

	public default <T> WritableBuffer writeln(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn,
			String prefix, String suffix) {
		return write(iterable, fn, prefix, suffix).writeln();
	}

	public default <T> WritableBuffer writeln(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn,
			String delim) {
		return write(iterable, fn, delim).writeln();
	}

	public default <T> WritableBuffer writeln(Iterable<@Nullable T> iterable, Function<T, @Nullable ?> fn) {
		return write(iterable, fn).writeln();
	}

	public default WritableBuffer writeln() {
		write('\n');
		for (int i = 0; i < indentLevel(); i++)
			write('\t');
		return this;
	}

	public int indentLevel();

	public void indent();

	public void dedent();

	public WritableBuffer writeBytes(short s);

	public WritableBuffer writeBytes(int i);

	public WritableBuffer writeBytes(long l);

	public OwlWriter writer();

	public default WritableBuffer write(java.nio.ByteBuffer src) {
		throw new UnsupportedOperationException();
	}

}

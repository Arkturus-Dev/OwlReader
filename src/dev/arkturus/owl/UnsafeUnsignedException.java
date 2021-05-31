package dev.arkturus.owl;

public class UnsafeUnsignedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public UnsafeUnsignedException(Class<?> clazz) {
		super("Unsafe unsigned casting occured when attempting to cast to " + clazz.getSimpleName());
	}
}
module dev.arkturus.owlreader {
	exports dev.arkturus.owl;
	exports dev.arkturus.owl.buffer;
	exports dev.arkturus.owlreader;
	exports dev.arkturus.owlwriter;

	requires transitive dev.arkturus.nullanalysis;
	requires java.base;
}
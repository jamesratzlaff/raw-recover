package com.jamesratzlaff.rawrecover.io.file.type.spi;

import java.io.IOException;
import java.util.function.Predicate;

import com.jamesratzlaff.rawrecover.RawDisk;

public interface FindsEndOffset extends Predicate<String> {

	String getType();
	
	default long calculateSize(long startOffset, RawDisk rd, long offsetLimit) throws IOException {
		return -1;
	}
	
	@Override
	default boolean test(String type) {
		return getType().equalsIgnoreCase(type);
	}
	
	long getEndOffset(long startOffset, RawDisk rd, long offsetLimit) throws IOException;
	
	default boolean calculatesSize() {
		return false;
	}
	
	
	
	
	
}

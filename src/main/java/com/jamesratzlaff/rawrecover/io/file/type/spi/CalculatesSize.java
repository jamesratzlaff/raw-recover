package com.jamesratzlaff.rawrecover.io.file.type.spi;

import java.io.IOException;

import com.jamesratzlaff.rawrecover.RawDisk;

public interface CalculatesSize extends FindsEndOffset {

	@Override
	long calculateSize(long startOffset, RawDisk rd, long offsetLimit) throws IOException;

	

	@Override
	default long getEndOffset(long startOffset, RawDisk rd, long offsetLimit) throws IOException{
		long calculatedSize = calculateSize(startOffset, rd, offsetLimit);
		if(calculatedSize<0) {
			return -1;
		}
		return calculatedSize+startOffset;
	}

	@Override
	default boolean calculatesSize() {
		return true;
	}
	
	
}

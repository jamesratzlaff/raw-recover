package com.jamesratzlaff.rawrecover.io;

public interface HasBytesPerSector {
	default long getBytesPerSector() {
		return 512;
	}
}

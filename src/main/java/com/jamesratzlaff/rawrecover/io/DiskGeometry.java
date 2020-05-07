package com.jamesratzlaff.rawrecover.io;

public interface DiskGeometry extends HasBytesPerSector{
	long getTotalCylinders();
	long getTotalHeads();
	long getTotalSectors();
	long getTotalTracks();
	long getTracksPerCylinder();
	@Override
	int hashCode();
	@Override
	boolean equals(Object o);
}

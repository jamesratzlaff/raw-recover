package com.jamesratzlaff.rawrecover;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;

public interface RawFileLocation extends Comparable<RawFileLocation>, Serializable{

	String getType();
	
	long getStartOffset();
	
	long getEndOffset();
	default long getLength() {
		if(getEndOffset()<0) {
			return 0;
		}
		return getEndOffset()-getStartOffset();
	}
	void setEndOffset(long endOffset);
	
	default int compareTo(RawFileLocation other) {
		int cmp = 0;
		if(this.equals(other)) {
			return cmp;
		}
		if(other==null) {
			return -1;
		}
		return Long.compareUnsigned(this.getStartOffset(), this.getEndOffset());
	}
	
	default boolean endsInPaddedCluster() {
		return false;
	}
	
	void endsInPaddedCluster(boolean endInPaddedCluster);
	
	private ByteBuffer getLastCluster(RawDisk rd) throws IOException {
		long endOffset = getEndOffset();
		ByteBuffer bb = null;
		int sectorSize = RawDisk.DEFAULT_SECTOR_SIZE;
		int sectorsPerCluster = 8;
		int clusterSize = sectorSize*sectorsPerCluster;
		if(endOffset!=-1) {
			bb = ByteBuffer.wrap(new byte[clusterSize]);
			long lastClusterAddress = (endOffset/clusterSize)*clusterSize;
			try {
				rd.read(bb,lastClusterAddress);
			} catch(IOException ioe) {
				System.err.println("Got I/O Exception "+ioe.getMessage()+". Trying small reads");
				bb.rewind();
				for(int i=0;i<sectorsPerCluster;i++) {
					int pos = i*sectorSize;
					bb.position(pos);
					ByteBuffer mini = ByteBuffer.wrap(new byte[sectorSize]);
					try {
						rd.read(mini, lastClusterAddress+pos);
						bb.put(mini);
					}catch(IOException ioe2) {
						System.err.println("Got I/O Exception '"+ioe.getMessage()+"' reading "+(lastClusterAddress+pos)+" assuming zeroed sector");
					}
				}
			}
			bb.position((int)(endOffset%(long)clusterSize));
		}
		return bb;
	}
	
	default boolean endsInPaddedCluster(RawDisk rd) throws IOException {
		boolean truth = false;
		ByteBuffer bb = getLastCluster(rd);
		if(bb!=null) {
			ByteBuffer zeroBuff = ByteBuffer.allocateDirect(bb.remaining());
			if(zeroBuff.equals(bb)) {
				truth = true;
			} 
		}
		return truth;
	}
	
	
	
	default boolean containsLocation(long location) {
		return getStartOffset()<=location&&location<getEndOffset();
	}
	/*
	 * TODO: add methods:
	 * default boolean overlapsWith(RawFileLocation other)
	 */
	
	
}

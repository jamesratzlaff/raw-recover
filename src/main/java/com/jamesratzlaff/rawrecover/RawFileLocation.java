package com.jamesratzlaff.rawrecover;

import java.io.Serializable;

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
	
	
	
}

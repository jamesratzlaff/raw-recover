package com.jamesratzlaff.rawrecover;

import java.io.Serializable;

public interface LongRange extends Serializable, Comparable<LongRange> {
	default long getEnd() {
		return getStart() + getLength();
	}

	long getLength();

	void setLength(long len);

	default void deltaLength(long delta) {
		setLength(getLength() + delta);
	}

	default void setEnd(long end) {
		setLength(end - getStart());
	}

	default boolean contains(long value) {
		return getStart()<=value&&value<getEnd();
	}
	
	long getStart();

	default int compareTo(LongRange other) {
		int cmp = 0;
		if (this.equals(other)) {
			return cmp;
		}
		if (other == null) {
			cmp = -1;
		}
		if (cmp == 0) {
			cmp = Long.compareUnsigned(this.getStart(), other.getStart());
		}
		if (cmp == 0) {
			cmp = Long.compareUnsigned(this.getLength(), other.getLength());
		}
		return cmp;

	}
}

package com.jamesratzlaff.rawrecover;

import java.io.Serializable;
import java.util.function.BiPredicate;

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

	default boolean hasSameStartAs(LongRange other) {
		if(other==null) {
			return false;
		}
		return getStart()==other.getStart();
	}
	
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
	
	default boolean contains(LongRange other) {
		boolean truth = other!=null;
		if(truth) {
			if(!this.equals(other)) {
				truth=(this.getStart()<=other.getStart())&&this.getLength()>=other.getLength();
			}
		}
		return truth;
	}
	
	default int overlaps(LongRange other) {
		if(other==null) {
			return 0;
		}
		if(this.getEnd()>other.getStart()&&this.getStart()<other.getStart()) {
			return -1;
		}
		if(other.getStart()>this.getEnd()&&other.getStart()<this.getStart()) {
			return 1;
		}
		return 0;
	}
	
	default int isAdjacentTo(LongRange other) {
		if(other==null) {
			return 0;
		}
		if(this.getStart()<other.getStart()&&this.getEnd()==other.getStart()) {
			return -1;
		}
		if(other.getEnd()==this.getStart()&&this.getEnd()>other.getEnd()) {
			return 1;
		}
		return 0;
	}
	
	default boolean startsBefore(LongRange other) {
		if(other==null) {
			return false;
		}
		return this.getStart()<other.getStart();
	}
	
	default boolean startsAfter(LongRange other) {
		if(other==null) {
			return false;
		}
		return this.getStart()>=other.getEnd();
	}
	
	default boolean endsAfter(LongRange other) {
		return this.getEnd()>other.getEnd();
	}
	default boolean endsBefore(LongRange other) {
		return getEnd()<=other.getStart();
	}
	default boolean isBeforeAndOverlaps(LongRange other) {
		return getStart()<other.getStart()&&getEnd()>other.getStart()&&getEnd()<other.getEnd();
	}
	
	default boolean isAfterAndOverlaps(LongRange other) {
		return other.isBeforeAndOverlaps(this);
	}
	default boolean isBeforeAndAdjacentTo(LongRange other) {
		return this.getStart()<other.getStart()&&this.getEnd()==other.getStart();
	}
	
	default boolean isAfterAndAdjacentTo(LongRange other) {
		return other.isBeforeAndAdjacentTo(this);
	}
	default boolean isCompletelyBefore(LongRange other) {
		return this.getEnd()<other.getStart();
	}
	default boolean isCompletelyAfter(LongRange other) {
		return other.isCompletelyBefore(this);
	}
	
	
	public static boolean aEndsIsBeforeBEnds(LongRange a, LongRange b) {
		if(a==null||b==null) {
			return false;
		}
		return Long.compareUnsigned(a.getEnd(), b.getEnd())<0;
	}
	
	public static boolean aStartsIsBeforeBStarts(LongRange a, LongRange b) {
		if(a==null||b==null) {
			return false;
		}
		return Long.compareUnsigned(a.getStart(), b.getStart())<0;
	}
	
}

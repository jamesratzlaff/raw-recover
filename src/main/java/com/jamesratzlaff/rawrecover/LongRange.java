package com.jamesratzlaff.rawrecover;

import java.beans.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface LongRange extends Serializable, Comparable<LongRange> {
	default long getEnd() {
		return getStart() + getLength();
	}

	@Transient
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
	
	
	public static LongRange min(LongRange a, LongRange b) {
		if(a.compareTo(b)<0) {
			return a;
		}
		return b;
	}
	
	public static LongRange max(LongRange a, LongRange b) {
		if(a.compareTo(b)>0) {
			return a;
		}
		return b;
	}
	
	public static LongRange mergeIfAdjacent(LongRange a, LongRange b) {
		int adjacency = a!=null?a.isAdjacentTo(b):0;
		if(adjacency==0) {
			return null;
		}
		long len = a.getLength()+b.getLength();
		if(adjacency<0) {
			return new ArbitraryRange(a.getStart(), len);
		}
		return new ArbitraryRange(b.getStart(), len);
	}
	
	public static LongRange arbitraryMerge(LongRange a, LongRange b) {
		
		LongRange min = min(a,b);
		LongRange max = max(a,b);
		long start = min.getStart();
		long end = max.getEnd();
		return new ArbitraryRange(start, end-start);
	}
	
	private static int getMaxAdjacentIndex(List<? extends LongRange> sortedRanges, int startIndex) {
		int result = -1;
		LongRange a = sortedRanges.get(startIndex);
		for(int i=startIndex+1;i<sortedRanges.size();i++) {
			LongRange b=sortedRanges.get(i);
			if(a.isAdjacentTo(b)!=0) {
				result=i;
				a=b;
			} else {
				break;
			}
		}
		return result;
	}
	
	public static List<LongRange> mergeAdjacents(List<? extends LongRange> ranges) {
		List<LongRange> result = new ArrayList<LongRange>();
		Collections.sort(ranges);
		for(int i=0;i<ranges.size();i++) {
			int endAdj = getMaxAdjacentIndex(ranges, i);
			LongRange current = ranges.get(i);
			if(endAdj!=-1) {
				LongRange toMerge = ranges.get(endAdj);
				current = arbitraryMerge(current, toMerge);
				i=endAdj;
			}
			result.add(current);
		}
		return result;
	}
	
	public static void main(String[] args) {
		ArbitraryRange a = new ArbitraryRange(0, 4);
		ArbitraryRange b = new ArbitraryRange(4, 1);
		ArbitraryRange c = new ArbitraryRange(5, 1);
		ArbitraryRange d = new ArbitraryRange(6, 1);
		ArbitraryRange e = new ArbitraryRange(7, 1);
		ArbitraryRange f = new ArbitraryRange(8, 1);
		
		List<ArbitraryRange> all = Arrays.asList(a,b,c,d,e,f);
		for(int i=0;i<all.size();i++) {
			ArbitraryRange current = all.get(i);
			int maxAdj = getMaxAdjacentIndex(all, i);
			System.out.println("("+i+")"+current+" : "+maxAdj);
		}
		System.out.println(mergeAdjacents(all));
		
		
		
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

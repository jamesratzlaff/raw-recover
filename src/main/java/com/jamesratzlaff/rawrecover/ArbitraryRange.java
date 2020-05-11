package com.jamesratzlaff.rawrecover;

public class ArbitraryRange extends OffsetRange {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2531801873037877765L;

	public ArbitraryRange(long startOffset, long length) {
		super(startOffset, length);
	}

	public ArbitraryRange(long startOffset) {
		super(startOffset);
	}

	public ArbitraryRange(LongRange other) {
		super(other);
	}

}

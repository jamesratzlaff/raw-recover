package com.jamesratzlaff.rawrecover;

import java.util.Objects;



public abstract class OffsetRange implements LongRange{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8038125801660689256L;
	private final long start;
	private long length = 0;

	
	public OffsetRange(LongRange other) {
		this(other.getStart(),other.getLength());
	}
	
	public OffsetRange(long startOffset, long length) {
		this(startOffset);
		setLength(length);
	}
	
	public OffsetRange(long startOffset) {
		this.start = startOffset;
	}

	

	public long getLength() {
		return this.length;
	}

	public void setLength(long len) {
		if (len < 0) {
			System.err.println("can't set length (currently " + getLength() + ") to a negative value of " + len
					+ ". Length values must be positive. ignoring...");
		}
		this.length = len;
	}


	/**
	 * 
	 * @return the inclusive start offset of this range
	 */
	public long getStart() {
		return this.start;
	}

	

	@Override
	public int hashCode() {
		return Objects.hash(length, start);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof OffsetRange))
			return false;
		OffsetRange other = (OffsetRange) obj;
		return length == other.length && start == other.start;
	}

	protected String toString(String classSimpleName) {
		StringBuilder builder = new StringBuilder();
		builder.append(classSimpleName!=null?classSimpleName:"/*OffsetRange*/").append(" {'start':");
		builder.append(getStart()).append(" /*0x").append(Long.toHexString(getStart())).append("*/");
		builder.append(",'end':");
		builder.append(getEnd()).append(" /*0x").append(Long.toHexString(getEnd())).append("*/");
		builder.append(",'length':");
		builder.append(getLength());
		builder.append("}");
		return builder.toString();
	}
	
	@Override
	public String toString() {
		return toString(getClass().getSimpleName());
	}


}

package com.jamesratzlaff.rawrecover;

public class ZeroedOutOffsetRange extends OffsetRange{

	/**
	 * 
	 */
	private static final long serialVersionUID = 312856742598876248L;

	public ZeroedOutOffsetRange(long startOffset, long length) {
		super(startOffset, length);
	}

	public ZeroedOutOffsetRange(long startOffset) {
		super(startOffset);
	}
	
	@Override
	public String toString() {
		return super.toString(getClass().getSimpleName());
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ZeroedOutOffsetRange))
			return false;
		return true;
	}

}

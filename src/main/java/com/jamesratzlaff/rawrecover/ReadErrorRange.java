package com.jamesratzlaff.rawrecover;

import java.util.Objects;

public class ReadErrorRange extends OffsetRange implements HasMessage{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7756363528338569656L;
	private final String message;
	
	public ReadErrorRange(long startOffset, long length, String message) {
		super(startOffset, length);
		this.message=message;
	}
	public ReadErrorRange(long startOffset, String message) {
		this(startOffset,RawDisk.DEFAULT_SECTOR_SIZE,message);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(message);
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ReadErrorRange))
			return false;
		ReadErrorRange other = (ReadErrorRange) obj;
		return Objects.equals(message, other.message);
	}
	
	@Override
	public String getMessage() {
		return message;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("/*ReadErrorRange*/ {'start':");
		builder.append(getStart()).append(" /*0x").append(Long.toHexString(getStart())).append("*/");
		builder.append(",'end':");
		builder.append(getEnd()).append(" /*0x").append(Long.toHexString(getEnd())).append("*/");
		builder.append(",'length':");
		builder.append(getLength());
		builder.append(", 'message':");
		builder.append(getMessage()!=null?"'"+getMessage()+"'":null);
		builder.append("}");
		return builder.toString();
	}
	
	
	
	
	
	
	
	
	
	
	
	
}

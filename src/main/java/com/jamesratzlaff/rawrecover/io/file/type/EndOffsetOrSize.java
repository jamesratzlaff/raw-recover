package com.jamesratzlaff.rawrecover.io.file.type;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

import com.jamesratzlaff.rawrecover.RawDisk;
import com.jamesratzlaff.rawrecover.io.file.type.spi.FindsEndOffset;

public class EndOffsetOrSize implements Serializable {
	public static boolean autoInitWithToString = true;
	/**
	 * 
	 */
	private static final long serialVersionUID = -3356299562075404394L;
	private final long startOffset;
	private final String type;
	private long endOffsetOrSize = Long.MIN_VALUE;

	private final long readLimit;
	private final RawDisk rd;
	private boolean isValid = false;
	private boolean isCalculatedSize = true;

	public EndOffsetOrSize(long startOffset, String type, long readLimit, RawDisk rd) {
		this.startOffset = startOffset;
		this.type = type;
		this.readLimit = readLimit;
		this.rd = rd;
	}

	public long getEndOffsetOrSize() {
		if (endOffsetOrSize == Long.MIN_VALUE) {
			FindsEndOffset feo = FileEndOffsetServiceImpl.Instance.get().getEndFinder(getType());
			if (feo == null) {
				return this.endOffsetOrSize;
			}
			this.isCalculatedSize = feo.calculatesSize();
			try {
				if (isCalculatedSize) {
					this.endOffsetOrSize = feo.calculateSize(startOffset, rd, readLimit);
				} else {
					this.endOffsetOrSize = feo.getEndOffset(startOffset, rd, readLimit);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			if (this.getEndOffsetOrSize() > -1) {
				long end = this.endOffsetOrSize;
				if (this.isCalculatedSize) {
					end += this.startOffset;
				}
				if (end <= readLimit) {
					this.isValid = true;
				}
			}
		}
		return this.endOffsetOrSize;
	}

	public boolean isValid() {
		return this.isValid;
	}

	private void initIfNecessary() {
		if (endOffsetOrSize == Long.MIN_VALUE) {
			getEndOffsetOrSize();
		}
	}

	public long getSize() {
		initIfNecessary();
		long reso = getEndOffsetOrSize();
		if (!isCalculatedSize() && reso > 0) {
			reso -= this.startOffset;
		}
		return reso;
	}

	public long getEndOffset() {
		initIfNecessary();
		long reso = getEndOffsetOrSize();
		if (isCalculatedSize() && reso > 0) {
			return reso += this.startOffset;
		}
		return reso;
	}

	public boolean isCalculatedSize() {
		initIfNecessary();
		return this.isCalculatedSize;
	}

	public String getType() {
		return this.type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(endOffsetOrSize, isCalculatedSize, isValid, readLimit, startOffset, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof EndOffsetOrSize)) {
			return false;
		}
		EndOffsetOrSize other = (EndOffsetOrSize) obj;
		return endOffsetOrSize == other.endOffsetOrSize && isCalculatedSize == other.isCalculatedSize
				&& isValid == other.isValid && readLimit == other.readLimit && startOffset == other.startOffset
				&& Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		if (autoInitWithToString) {
			initIfNecessary();
		}
		StringBuilder builder = new StringBuilder();
		builder.append("EndOffsetOrSize [startOffset=");
		builder.append(startOffset);
		builder.append(", type=");
		builder.append(type);
		builder.append(", endOffsetOrSize=");
		builder.append(endOffsetOrSize);
		builder.append(", readLimit=");
		builder.append(readLimit);
		builder.append(", isValid=");
		builder.append(isValid);
		builder.append(", isCalculatedSize=");
		builder.append(isCalculatedSize);
		builder.append("]");
		return builder.toString();
	}

}

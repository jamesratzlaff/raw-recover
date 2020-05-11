package com.jamesratzlaff.rawrecover.io.file.type;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Objects;

public class HiveBin {

	public static final byte[] HBIN_SIGNATURE = "hbin".getBytes(StandardCharsets.US_ASCII);
	
	
	private final byte[] signature = Arrays.copyOf(HBIN_SIGNATURE, HBIN_SIGNATURE.length);
	/**
	 * Offset of a current hive bin in bytes, relative from the start of the hive bins data.
	 * 
	 * This is 4 bytes, but since java doesn't have uint32 we're using a 
	 */
	private long offset;
	/**
	 * Size of a current hive bin in bytes
	 * </br>
	 * A hive bin size is multiple of 4096 bytes.
	 */
	private int size;
	
	private final byte[] reserved = new byte[8];
	
	/**
	 * FILETIME (UTC), defined for the first hive bin only.
	 * </br>
	 * A <i>Timestamp</i> in the header of the first hive bin acts as a backup copy of a <i>Last written timestamp</i> in the base block.
	 */
	private FileTime timeStamp;
	
	/**
	 * This field has no meaning on a disk
	 * </br>
	 * The Spare field is used when shifting hive bins and cells in memory. In Windows 2000, the same field is called MemAlloc, it is used to track memory allocations for hive bins.
	 */
	private int spareOrMemAlloc;
	
	
	public HiveBin() {
		reset();
		
	}
	
	private void reset() {
		size=-1;
		spareOrMemAlloc=0;
		timeStamp=null;
		offset=-1;
	}


	/**
	 * @return the offset
	 */
	public long getOffset() {
		return offset;
	}


	/**
	 * @param offset the offset to set
	 */
	public void setOffset(long offset) {
		this.offset = offset;
	}


	/**
	 * @return the size
	 */
	public int getSize() {
		return size;
	}


	/**
	 * @param size the size to set
	 */
	public void setSize(int size) {
		this.size = size;
	}


	/**
	 * @return the timeStamp
	 */
	public FileTime getTimeStamp() {
		return timeStamp;
	}


	/**
	 * @param timeStamp the timeStamp to set
	 */
	public void setTimeStamp(FileTime timeStamp) {
		this.timeStamp = timeStamp;
	}


	/**
	 * @return the spareOrMemAlloc
	 */
	public int getSpareOrMemAlloc() {
		return spareOrMemAlloc;
	}


	/**
	 * @param spareOrMemAlloc the spareOrMemAlloc to set
	 */
	public void setSpareOrMemAlloc(int spareOrMemAlloc) {
		this.spareOrMemAlloc = spareOrMemAlloc;
	}


	/**
	 * @return the signature
	 */
	public byte[] getSignature() {
		return signature;
	}


	/**
	 * @return the reserved
	 */
	public byte[] getReserved() {
		return reserved;
	}
	public HiveBin apply(ByteBuffer bb) {
		return apply(bb,0);
	}
	public HiveBin apply(ByteBuffer bb, int pos) {
		if(bb==null) {
			reset();
		} else {
			int ogPos = bb.position();
			if((bb.remaining()-pos)>=32) {
				bb.position(pos);
				byte[] sig = new byte[getSignature().length];
				bb.get(sig);
				if(Arrays.equals(sig, getSignature())) {
					this.setOffset(bb.order(ByteOrder.LITTLE_ENDIAN).getInt());
					this.setSize(bb.order(ByteOrder.LITTLE_ENDIAN).getInt());
					long tstamp=bb.order(ByteOrder.LITTLE_ENDIAN).getLong();
					bb.get(this.reserved);
					this.setTimeStamp(FileTime.fromMillis(tstamp));
					this.setSpareOrMemAlloc(bb.order(ByteOrder.LITTLE_ENDIAN).getInt());
					
					
				}
				
			}
			bb.position(ogPos);
		}
		return this;
	}
	
	public HiveBin apply(byte[] bytes, int pos) {
		if(bytes==null) {
			reset();
		} else {
			ByteBuffer bb = ByteBuffer.wrap(bytes);
			apply(bb,pos);
		}
		return this;
		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(reserved);
		result = prime * result + Arrays.hashCode(signature);
		result = prime * result + Objects.hash(offset, size, spareOrMemAlloc, timeStamp);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof HiveBin)) {
			return false;
		}
		HiveBin other = (HiveBin) obj;
		return offset == other.offset && Arrays.equals(reserved, other.reserved)
				&& Arrays.equals(signature, other.signature) && size == other.size
				&& spareOrMemAlloc == other.spareOrMemAlloc && Objects.equals(timeStamp, other.timeStamp);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HiveBin [signature=");
		builder.append(Arrays.toString(signature));
		builder.append(", offset=");
		builder.append(offset);
		builder.append(", size=");
		builder.append(size);
		builder.append(", reserved=");
		builder.append(Arrays.toString(reserved));
		builder.append(", timeStamp=");
		builder.append(timeStamp);
		builder.append(", spareOrMemAlloc=");
		builder.append(spareOrMemAlloc);
		builder.append("]");
		return builder.toString();
	}
	
	
}

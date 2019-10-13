package com.jamesratzlaff.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.jamesratzlaff.rawrecover.RawDisk;

public class EndOfFileGetter {

	public static long getMp4Size(int startOffset, RawDisk rd) throws IOException {
		long result = -1;
		int initialBufferSize = 8192;
		
		
		if(rd!=null) {
			ByteBuffer bb = ByteBuffer.wrap(new byte[initialBufferSize]).order(ByteOrder.BIG_ENDIAN);
			rd.read(bb, startOffset);
			int firstHeaderLen =  bb.getInt(0);
			int bbOffset = populateByteBufferWithDataFromLocation(startOffset, firstHeaderLen, bb, rd);
			int secondHeaderLen = bb.getInt(bbOffset);
			bbOffset=populateByteBufferWithDataFromLocation(startOffset, firstHeaderLen+secondHeaderLen, bb, rd);
			int thirdHeaderLen = bb.getInt(bbOffset);
			bbOffset = populateByteBufferWithDataFromLocation(startOffset, firstHeaderLen+secondHeaderLen+(Integer.BYTES<<1), bb, rd);
			result = Integer.toUnsignedLong(bb.getInt(bbOffset))+firstHeaderLen+secondHeaderLen+thirdHeaderLen;
			
		}
		return result;
	}
	
	public static long getRIFFSize(int startOffset, RawDisk rd) throws IOException {
		long result = -1;
		int initialBufferSize = 8192;
		if(rd!=null) {
			ByteBuffer bb = ByteBuffer.wrap(new byte[initialBufferSize]).order(ByteOrder.LITTLE_ENDIAN);
			rd.read(bb, startOffset);
			result=bb.getInt(1);
			
		}
		return result;
		
	}
	
	/**
	 * 
	 * @param startOffset
	 * @param lenToSeek
	 * @param bb the byteBuffer to be populated with data
	 * @param rd
	 * @return the offset within the ByteBuffer to seek to
	 * @throws IOException
	 */
	public static int populateByteBufferWithDataFromLocation(int diskStartOffset, int lenToSeek, ByteBuffer bb, RawDisk rd) throws IOException{
		int actualOffset = diskStartOffset+lenToSeek;
		if(lenToSeek<bb.capacity()) {
			return lenToSeek;
		}
		int seekOffset = lenToSeek%bb.capacity();
		bb=rd.read(bb, (actualOffset/bb.capacity())*bb.capacity());
		return seekOffset;
	
	}
	
	
}

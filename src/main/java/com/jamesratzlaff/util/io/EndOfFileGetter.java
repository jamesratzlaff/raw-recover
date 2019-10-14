package com.jamesratzlaff.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.jamesratzlaff.rawrecover.RawDisk;

public class EndOfFileGetter {
	private static final byte[] ASF_FILE_PROP_GUID = toByteArray(0xA1, 0xDC, 0xAB, 0x8C, 0x47, 0xA9, 0xCF, 0x11, 0x8E, 0xE4, 0x00, 0xC0, 0x0C, 0x20, 0x53, 0x65);

	
	
	public static long getEndOffset(long startOffset, RawDisk rd, long endOffset, String type) throws IOException{
		long result = -1;
		if("MP4".equals(type)) {
			long size = getMp4Size(startOffset, rd);
			if(size!=-1) {
				result=size+startOffset;
			}
		} else if("ASF".equals(type)) {
			long size = getASFSize(startOffset, rd, endOffset);
			if(size!=-1) {
				result=startOffset+size;
			}
		} else if("RIFF".equals(type)) {
			long size = getRIFFSize(startOffset, rd);
			if(size!=-1) {
				result = size+startOffset;
			}
		} else if("JPEG".equals(type)) {
			result = searchFor(startOffset, rd, endOffset, new byte[] {(byte)0xFF,(byte)0xD9});
		} else if("PNG".equals(type)) {
			result = searchFor(startOffset, rd, endOffset, "IEND".getBytes(StandardCharsets.US_ASCII));
			if(result!=-1) {
				result+=4;
			}
		}
		
		return result;
	}
	
	public static long getMp4Size(long startOffset, RawDisk rd) throws IOException {
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
	
	public static long getASFSize(long startOffset, RawDisk rd, long endOffset) throws IOException {
		long result = -1;
		int initialBufferSize = 512;
		if(rd!=null) {
			long filePropertiesGuidLocation = searchFor(startOffset, rd, endOffset, ASF_FILE_PROP_GUID);
			if(filePropertiesGuidLocation!=-1) {
				ByteBuffer bb = ByteBuffer.wrap(new byte[initialBufferSize]).order(ByteOrder.LITTLE_ENDIAN);
				long sizeDataOffset = filePropertiesGuidLocation+ASF_FILE_PROP_GUID.length+8+ASF_FILE_PROP_GUID.length;
				long addressContainingSizeData = RawDisk.getContainingSectorOffset(sizeDataOffset);
				long diffOffset=sizeDataOffset-addressContainingSizeData;
				rd.read(bb, addressContainingSizeData);
				result = bb.getLong((int)diffOffset);
			}
		}
		return result;
	}
	
	public static long getRIFFSize(long startOffset, RawDisk rd) throws IOException {
		long result = -1;
		int initialBufferSize = 8192;
		if(rd!=null) {
			ByteBuffer bb = ByteBuffer.wrap(new byte[initialBufferSize]).order(ByteOrder.LITTLE_ENDIAN);
			rd.read(bb, startOffset);
			result=bb.getInt(1);
			
		}
		return result;
	}
	
	private static byte[] toByteArray(int...ints) {
		byte[] asBytes = new byte[ints.length];
		for(int i=0;i<ints.length;i++) {
			asBytes[i]=(byte)ints[i];
		}
		return asBytes;
	}
	
	public static long searchFor(long startOffset, RawDisk rd, long endOffset, byte[] toSearchFor) throws IOException {
		long result = -1;
		int initialBufferSize = 8192;
		if(rd!=null) {
			ByteBuffer bb = ByteBuffer.wrap(new byte[initialBufferSize]);
			rd.read(bb,startOffset);
			bb.flip();
			byte[] compareArray = new byte[toSearchFor.length];
			for(int i=0;result==-1&&((i+startOffset)-toSearchFor.length)<endOffset;) {
				if(bb.remaining()<toSearchFor.length) {
					bb=rebuffer(bb,bb.position(),rd,(i+startOffset)+bb.remaining());
				}
				compareArray = get(bb, compareArray);
				if(Arrays.equals(toSearchFor, compareArray)) {
					result = startOffset+i;
				}
				i+=1;
			}
		}
		return result;
	}
	private static ByteBuffer rebuffer(ByteBuffer bb, int bbStartOffset, RawDisk rd, long readOffset) throws IOException {
		return rebuffer(bb,bbStartOffset,rd,readOffset,512);
	}
	private static ByteBuffer rebuffer(ByteBuffer bb, int bbStartOffset, RawDisk rd, long readOffset, int readLen) throws IOException {
		ByteBuffer reBuffed = ByteBuffer.wrap(new byte[bb.remaining()+readLen]);
		reBuffed.put(bb);
		rd.read(reBuffed, readOffset);
		reBuffed.flip();
		reBuffed.position(0);
		return reBuffed;
	}
	
	private static byte[] get(ByteBuffer bb, byte[] dst) {
		bb.get(dst);
		bb.position(bb.position()-(dst.length-1));
		return dst;
	}
	
	private static byte[] get(ByteBuffer bb, byte[] dst, int index) {
		return get(bb,dst,index,0,dst.length);
	}
	private static byte[] get(ByteBuffer bb, byte[] dst, int index, int dstOffset, int dstLen) {
		for(int i=0;i<dstLen;i++) {
			dst[i+dstOffset]=bb.get(index+i);
		}
		return dst;
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
	private static int populateByteBufferWithDataFromLocation(long diskStartOffset, int lenToSeek, ByteBuffer bb, RawDisk rd) throws IOException{
		long actualOffset = diskStartOffset+lenToSeek;
		if(lenToSeek<bb.capacity()) {
			return lenToSeek;
		}
		int seekOffset = lenToSeek%bb.capacity();
		bb=rd.read(bb, (actualOffset/bb.capacity())*bb.capacity());
		return seekOffset;
	
	}
	
	
}

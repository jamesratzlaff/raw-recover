/**
 *
 */
package com.jamesratzlaff.util.io;

import java.nio.ByteBuffer;
import java.util.function.Predicate;

/**
 * @author
 *
 */
public class ByteBufferPredicates {
	public static final byte[] JFIF = {0x4A,0x46,0x49,0x46,0x00};
	public static final Predicate<ByteBuffer> EXT4_SUPER_BLOCK = bb -> {
		byte current = bb.get(0x38);

		boolean truth = (current == 0x53);
		if (truth) {
			current = bb.get(0x39);
			truth = current == (byte)0xef;
		}
		return truth;
	};
	//example @ 27AA:0000
	public static final Predicate<ByteBuffer> SQLITE = bb->{
		byte[] toMatch = "SQLite format ".getBytes();
		boolean matches = byteBufferMatches(bb, toMatch);
		return matches;
	};

	public static final Predicate<ByteBuffer> ELF_7F = bb->{
		byte[] toMatch = {0x7F,0x45,0x4C,0x46};
		boolean matches = byteBufferMatches(bb, toMatch);
		return matches;
	};
	public static final Predicate<ByteBuffer> IDA_DB = bb->{
		byte[] toMatch = "IDA".getBytes();
		boolean matches = byteBufferMatches(bb, toMatch);
		return matches;
	};
	public static final Predicate<ByteBuffer> GZIP_FILE = bb->{
		byte[] toMatch = {0x1F,(byte)0x8B,0x08};
		boolean matches = byteBufferMatches(bb, toMatch);
		return matches;
	};

	public static final Predicate<ByteBuffer> JPG_FF = bb->{
		byte[] toMatch = {(byte)0xFF,(byte)0xD8,(byte)0xFF};
		boolean matches = byteBufferMatches(bb, toMatch);
		if(matches) {
			matches = byteBufferMatches(6,bb, JFIF);
		}
		return matches;
	};

	public static final Predicate<ByteBuffer> WEBM = bb->{
		byte[] toMatch = {(byte)0x1A, 0x45, (byte)0xDF, (byte)0xA3};
		boolean matches = byteBufferMatches(bb, toMatch);
		
		return matches;
	};

	public static final Predicate<ByteBuffer> MP4 = bb->{

		byte[] toMatch1 = {0x66, 0x74, 0x79, 0x70};
		byte[] toMatch2 = {0x6D, 0x6F, 0x6F, 0x76};
		byte[] toMatch3 = {0x6D, 0x64, 0x61, 0x74};
		boolean matches =byteBufferMatches(4, bb, toMatch1);
		if(!matches) {
			matches =byteBufferMatches(4, bb, toMatch2);
		}
		if(!matches) {
			matches =byteBufferMatches(4, bb, toMatch3);
		}
		return matches;

	};

	public static final Predicate<ByteBuffer> FLV = bb->{
		byte[] toMatch={0x46, 0x4C, 0x56};
		boolean matches = byteBufferMatches(bb, toMatch);
		return matches;
	};


	public static final Predicate<ByteBuffer> PNG_IHDR = bb-> {
		byte[] toMatch = {(byte)0x49,(byte)0x48,(byte)0x44,(byte)0x52};
		boolean matches=byteBufferMatches(12,bb, toMatch);
		return matches;
	};

	public static final Predicate<ByteBuffer> AVI = bb->{
		final byte[] toMatch0= {0x52, 0x49, 0x46, 0x46};
		final byte[] toMatch1= {0x41, 0x56, 0x49, 0x20};
		boolean matches=byteBufferMatches(bb, toMatch0);
		if(matches) {
			matches=byteBufferMatches(4, bb, toMatch1);
		}
		return matches;
	};

	public static final Predicate<ByteBuffer> DOS_PE = bb->{
		byte[] toMatch = {0x4D,(byte)0x5A,(byte)0xD0,0x00};
		boolean matches = byteBufferAndMatches(bb, toMatch);
		return matches;
	};
	private static boolean byteBufferAndMatches(ByteBuffer bb, byte[] toAnd) {
		boolean matches = byteBufferAndMatches(0, bb, toAnd);
		return matches;
	}
	private static boolean byteBufferMatches(ByteBuffer bb, byte[] toMatch) {
		boolean matches = byteBufferMatches(0, bb, toMatch);
		return matches;
	}

	private static boolean byteBufferAndMatches(int offset, ByteBuffer bb, byte[] toAnd) {
		boolean matches=true;
		for(int i=0;matches&&i<toAnd.length;i++) {
			byte current=toAnd[i];
			try {
				byte fromBB = bb.get(i+offset);
				matches=(fromBB==(current&fromBB));
			}catch(Exception e) {
				e.printStackTrace();
				matches=false;
				break;
			}
		}
		return matches;
	}
	public static boolean byteBufferMatches(int offset, ByteBuffer bb, byte[] toMatch) {
		boolean matches=true;
		for(int i=0;matches&&i<toMatch.length;i++) {
			byte current=toMatch[i];
			try {
				matches=current==bb.get(i+offset);
			}catch(Exception e) {
				e.printStackTrace();
				matches=false;
				break;
			}
		}
		return matches;
	}


}

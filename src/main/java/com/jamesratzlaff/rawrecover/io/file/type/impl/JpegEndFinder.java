package com.jamesratzlaff.rawrecover.io.file.type.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.jamesratzlaff.rawrecover.RawDisk;
import com.jamesratzlaff.rawrecover.io.file.type.EndOffsetOrSize;
import com.jamesratzlaff.rawrecover.io.file.type.spi.FindsEndOffset;
import com.jamesratzlaff.util.io.EndOfFileGetter;
import com.jamesratzlaff.util.io.db.Database;

public class JpegEndFinder implements FindsEndOffset {
	private static final byte[] JPEG_END = new byte[] { (byte) 0xFF, (byte) 0xD9 };

	@Override
	public String getType() {
		return "JPEG";
	}

	@Override
	public long getEndOffset(long startOffset, RawDisk rd, long offsetLimit) {
		long result = -1;
		try {
			result = EndOfFileGetter.searchFor(startOffset, rd, offsetLimit, JPEG_END);
			ByteBuffer bb = ByteBuffer.wrap(new byte[8192]).order(ByteOrder.BIG_ENDIAN);
			bb = rd.read(bb, startOffset).order(ByteOrder.BIG_ENDIAN);
			if (bb.getInt(6) == 0x45786966 && bb.get(11) == 0) {
				if (result > -1 && result < offsetLimit) {
					long altResult = EndOfFileGetter.searchFor(
							((result % 4096) != 0 ? 4096 : 0) + ((result / 4096) * 4096), rd, offsetLimit, JPEG_END);
					if (altResult < offsetLimit) {
						result = altResult;
					}
				}
			}
			if (result != -1) {
				result += JPEG_END.length;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static void main(String[] args) {
		long start = 2190475264l;
		String type = "mp4";
		RawDisk r=new RawDisk();
		
		Database d = new Database();
		long readLimit = d.getStartOffsetAfter(start);
		readLimit = d.getStartOffsetAfter(start);
		EndOffsetOrSize eoos = new EndOffsetOrSize(start, type, readLimit, r);
		System.out.println(eoos);
	}

}

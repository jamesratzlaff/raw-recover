package com.jamesratzlaff.rawrecover.io.file.type.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.jamesratzlaff.rawrecover.RawDisk;
import com.jamesratzlaff.rawrecover.io.file.type.spi.CalculatesSize;
import com.jamesratzlaff.util.io.EndOfFileGetter;

public class Mp4Sizer implements CalculatesSize {

	@Override
	public String getType() {
		return "MP4";
	}

	@Override
	public long calculateSize(long startOffset, RawDisk rd, long offsetLimit) {
		long result = -1;
		int initialBufferSize = 8192;
		try {
			if (rd != null) {
				ByteBuffer bb = ByteBuffer.wrap(new byte[initialBufferSize]).order(ByteOrder.BIG_ENDIAN);
				rd.read(bb, startOffset);
				int firstHeaderLen = bb.getInt(0);
				int bbOffset = EndOfFileGetter.populateByteBufferWithDataFromLocation(startOffset,
						firstHeaderLen + Integer.BYTES, bb, rd);
				if (bbOffset > Integer.BYTES - 1) {
					bbOffset -= Integer.BYTES;
				}
				int secondHeaderLen = bb.getInt(bbOffset);
				bbOffset = EndOfFileGetter.populateByteBufferWithDataFromLocation(startOffset,
						firstHeaderLen + secondHeaderLen + Integer.BYTES, bb, rd);
				if (bbOffset > Integer.BYTES - 1) {
					bbOffset -= Integer.BYTES;
				}
				int thirdHeaderLen = 0;
				try {
					thirdHeaderLen = bb.getInt(bbOffset);
				} catch (IndexOutOfBoundsException e) {
					System.err.println("#getMp4Size:Could not get index " + bbOffset + " in byte buffer " + bb
							+ " (startoffset:" + startOffset + ")");
					return result;
				}
				bbOffset = EndOfFileGetter.populateByteBufferWithDataFromLocation(startOffset,
						firstHeaderLen + secondHeaderLen + (Integer.BYTES << 1) + Integer.BYTES, bb, rd);
				if (bbOffset > Integer.BYTES - 1) {
					bbOffset -= Integer.BYTES;
				}
				result = Integer.toUnsignedLong(bb.getInt(bbOffset)) + firstHeaderLen + secondHeaderLen
						+ thirdHeaderLen;

			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return result;
	}

}

package com.jamesratzlaff.rawrecover.io.file.type.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.jamesratzlaff.rawrecover.RawDisk;
import com.jamesratzlaff.rawrecover.io.file.type.spi.FindsEndOffset;
import com.jamesratzlaff.util.io.EndOfFileGetter;

public class PngEndFinder implements FindsEndOffset{
	private static final byte[] IEND_BYTES = "IEND".getBytes(StandardCharsets.US_ASCII);
	@Override
	public String getType() {
		return "PNG";
	}

	@Override
	public long getEndOffset(long startOffset, RawDisk rd, long offsetLimit) throws IOException {
		long result = -1;
		result = EndOfFileGetter.searchFor(startOffset, rd, offsetLimit, IEND_BYTES);
		if (result != -1) {
			// adding length of IEND_BYTES and the proceeding CRC-32
			result += IEND_BYTES.length + Integer.BYTES;
		}
		return result;
	}

}

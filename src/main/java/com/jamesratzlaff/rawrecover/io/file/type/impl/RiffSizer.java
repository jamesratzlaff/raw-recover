package com.jamesratzlaff.rawrecover.io.file.type.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.jamesratzlaff.rawrecover.RawDisk;
import com.jamesratzlaff.rawrecover.io.file.type.spi.CalculatesSize;

public class RiffSizer implements CalculatesSize{

	@Override
	public String getType() {
		return "RIFF";
	}

	@Override
	public long calculateSize(long startOffset, RawDisk rd, long offsetLimit) throws IOException {
		long result = -1;
		int initialBufferSize = 8192;
		if (rd != null) {
			ByteBuffer bb = ByteBuffer.wrap(new byte[initialBufferSize]).order(ByteOrder.LITTLE_ENDIAN);
			rd.read(bb, startOffset);
			result = bb.getInt(1);

		}
		return result;
	}
	

}

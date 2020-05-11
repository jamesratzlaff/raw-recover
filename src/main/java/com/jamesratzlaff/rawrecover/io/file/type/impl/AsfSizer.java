package com.jamesratzlaff.rawrecover.io.file.type.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.jamesratzlaff.rawrecover.RawDisk;
import com.jamesratzlaff.rawrecover.io.file.type.spi.CalculatesSize;
import com.jamesratzlaff.util.io.EndOfFileGetter;

public class AsfSizer implements CalculatesSize{
	private static final byte[] ASF_FILE_PROP_GUID = EndOfFileGetter.toByteArray(0xA1, 0xDC, 0xAB, 0x8C, 0x47, 0xA9, 0xCF, 0x11, 0x8E,
			0xE4, 0x00, 0xC0, 0x0C, 0x20, 0x53, 0x65);
	@Override
	public String getType() {
		return "ASF";
	}

	@Override
	public long calculateSize(long startOffset, RawDisk rd, long offsetLimit) throws IOException {
		long result = -1;
		int initialBufferSize = 512;
		if (rd != null) {
			long filePropertiesGuidLocation = EndOfFileGetter.searchFor(startOffset, rd, offsetLimit, ASF_FILE_PROP_GUID);
			if (filePropertiesGuidLocation != -1) {
				ByteBuffer bb = ByteBuffer.wrap(new byte[initialBufferSize]).order(ByteOrder.LITTLE_ENDIAN);
				long sizeDataOffset = filePropertiesGuidLocation + ASF_FILE_PROP_GUID.length + 8
						+ ASF_FILE_PROP_GUID.length;
				long addressContainingSizeData = rd.getContainingSectorOffset(sizeDataOffset);
				long diffOffset = sizeDataOffset - addressContainingSizeData;
				rd.read(bb, addressContainingSizeData);
				result = bb.getLong((int) diffOffset);
			}
		}
		return result;
	}

}

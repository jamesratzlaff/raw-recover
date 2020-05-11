package com.jamesratzlaff.rawrecover.io.file.type.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.jamesratzlaff.rawrecover.RawDisk;
import com.jamesratzlaff.rawrecover.io.file.type.EndOffsetOrSize;
import com.jamesratzlaff.rawrecover.io.file.type.HiveBin;
import com.jamesratzlaff.rawrecover.io.file.type.spi.CalculatesSize;
import com.jamesratzlaff.util.io.db.Database;

public class HiveBinSizer implements CalculatesSize{

	@Override
	public String getType() {
		return "HBIN";
	}

	@Override
	public long calculateSize(long startOffset, RawDisk rd, long offsetLimit) throws IOException {
		long result = -1;
		int initialBufferSize = 8192;
			if (rd != null) {
				ByteBuffer bb = ByteBuffer.wrap(new byte[initialBufferSize]).order(ByteOrder.BIG_ENDIAN);
				rd.read(bb, startOffset);
				bb.flip();
				HiveBin hb = new HiveBin();
				result=hb.apply(bb).getSize();
				
			}
		return result;
	}
	
	
	public static void main(String[] args) {
		long start = 0xD84F10000l;
		String type = "HBIN";
		RawDisk r=new RawDisk();
		
		Database d = new Database();
		long readLimit = d.getStartOffsetAfter(start);
		readLimit = d.getStartOffsetAfter(start);
		EndOffsetOrSize eoos = new EndOffsetOrSize(start, type, readLimit, r);
		System.out.println(eoos);
		
	}
	

	
	
}

package com.jamesratzlaff.rawrecover.io.file.type.impl;

import java.io.IOException;

import com.jamesratzlaff.rawrecover.RawDisk;
import com.jamesratzlaff.rawrecover.io.file.type.spi.FindsEndOffset;
import com.jamesratzlaff.util.io.EndOfFileGetter;

public class XmlEndFinder implements FindsEndOffset{

	@Override
	public String getType() {
		return "XML";
	}

	@Override
	public long getEndOffset(long startOffset, RawDisk rd, long offsetLimit) throws IOException {
		long result = -1;
		byte[] gt = EndOfFileGetter.getAsASCIIBytes(">");
		byte[] lt = EndOfFileGetter.getAsASCIIBytes("<");
		long close = EndOfFileGetter.searchFor(startOffset, rd, offsetLimit, gt);
		long open = -1;
		int maxVal = 0x80;
		String elementTag = null;

		if (close != -1) {
			open = EndOfFileGetter.searchFor(maxVal, close, rd, offsetLimit, lt);
		}
		while (result == -1 && open != -1 && elementTag == null) {
			close = EndOfFileGetter.searchFor(maxVal, open, rd, offsetLimit, gt);
			if (close != -1) {
				close += 1;
				int len = (int) (close - open);// 131_697_172_480
				String asStr = EndOfFileGetter.getASCIIString(open, len, rd.getFileChannel());
				if (asStr == null) {
					break;
				}
				if (asStr.startsWith("<!") || asStr.startsWith("<?") || asStr.startsWith("<[")
						|| asStr.length() < 2) {
					open = EndOfFileGetter.searchFor(maxVal, close, rd, offsetLimit, lt);
					continue;
				} else {
					if (asStr.endsWith("/>")) {
						result = close;
					} else {
						asStr = asStr.substring(1, asStr.length() - 1);
						String[] splitUp = asStr.split("\\s+");
						if (splitUp.length > 0) {
							elementTag = splitUp[0];
						}
					}
				}
			} else {
				break;
			}
		}
		if (result == -1 && elementTag != null) {
			if (elementTag.equals("map")) {
				System.out.println("poop");
			}
			String taggedElement = "</" + elementTag + ">";
//			System.out.println(taggedElement);
			byte[] toSearchFor = EndOfFileGetter.getAsASCIIBytes(taggedElement);
			result = EndOfFileGetter.searchFor(maxVal, startOffset, rd, offsetLimit, toSearchFor);
			if (result != -1) {
				result += toSearchFor.length;
			}
		}
		return result;
	}

}

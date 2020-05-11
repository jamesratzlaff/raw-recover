package com.jamesratzlaff.rawrecover.io.file.type.impl;

import java.io.IOException;

import com.jamesratzlaff.rawrecover.RawDisk;
import com.jamesratzlaff.rawrecover.io.file.type.spi.FindsEndOffset;
import com.jamesratzlaff.util.io.EndOfFileGetter;

public class HtmlEndFinder implements FindsEndOffset{

	@Override
	public String getType() {
		return "HTML";
	}

	@Override
	public long getEndOffset(long startOffset, RawDisk rd, long offsetLimit) throws IOException {
		long result = -1;
		byte[] slashhtml = EndOfFileGetter.getAsASCIIBytes("</html>");
		byte[] slashHTML = EndOfFileGetter.getAsASCIIBytes("</HTML>");
		result = EndOfFileGetter.searchFor(0x80,startOffset, rd, offsetLimit, slashhtml);
		if (result != -1) {
			result += slashhtml.length;
		} else {
			result = EndOfFileGetter.searchFor(0x80,startOffset, rd, offsetLimit, slashHTML);
			if (result != -1) {
				result += slashHTML.length;
			}
		}
		return result;
		/*
		 * TODO: this may need to do a stack-like approach to find the outermost
		 * element. Also for both XML and HTML do a buffer check to see that all
		 * characters are in the ASCII range (basically bitwise & all bytes with 7F and
		 * be sure that they are equal to their original value);
		 * 
		 */
	}

}

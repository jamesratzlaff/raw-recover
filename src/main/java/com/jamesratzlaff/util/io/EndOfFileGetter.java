package com.jamesratzlaff.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Predicate;

import com.jamesratzlaff.rawrecover.RawDisk;

public class EndOfFileGetter {
	private static final byte[] ASF_FILE_PROP_GUID = toByteArray(0xA1, 0xDC, 0xAB, 0x8C, 0x47, 0xA9, 0xCF, 0x11, 0x8E,
			0xE4, 0x00, 0xC0, 0x0C, 0x20, 0x53, 0x65);
	private static final byte[] IEND_BYTES = "IEND".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] JPEG_END = new byte[] { (byte) 0xFF, (byte) 0xD9 };

	/*
	 * TODO: This is obviously an unsustainable way of doing things. Figure out a
	 * pattern that allows this to be conf file configurable, or consider using
	 * something like yara
	 */
	public static long getEndOffset(long startOffset, RawDisk rd, long offsetLimit, String type) throws IOException {
		long result = -1;

		if ("MP4".equals(type)) {
			long size = getMp4Size(startOffset, rd);
			if (size != -1) {
				result = size + startOffset;
			}
		} else if ("ASF".equals(type)) {
			long size = getASFSize(startOffset, rd, offsetLimit);
			if (size != -1) {
				result = startOffset + size;
			}
		} else if ("RIFF".equals(type)) {
			long size = getRIFFSize(startOffset, rd);
			if (size != -1) {
				result = size + startOffset;
			}
		} else if ("JPEG".equals(type)) {
			result = searchFor(startOffset, rd, offsetLimit, JPEG_END);
			ByteBuffer bb = ByteBuffer.wrap(new byte[8192]).order(ByteOrder.BIG_ENDIAN);
			bb = rd.read(bb, startOffset).order(ByteOrder.BIG_ENDIAN);
			if (bb.getInt(6) == 0x45786966 && bb.get(11) == 0) {
				if (result > -1 && result < offsetLimit) {
					long altResult = searchFor(((result % 4096) != 0 ? 4096 : 0) + ((result / 4096) * 4096), rd,
							offsetLimit, JPEG_END);
					if (altResult < offsetLimit) {
						result = altResult;
					}
				}
			}
			if (result != -1) {
				result += JPEG_END.length;
			}
		} else if ("PNG".equals(type)) {

			result = searchFor(startOffset, rd, offsetLimit, IEND_BYTES);
			if (result != -1) {
				// adding length of IEND_BYTES and the proceeding CRC-32
				result += IEND_BYTES.length + Integer.BYTES;
			}
		} else if ("HTML".equals(type)) {
			byte[] slashhtml = getAsASCIIBytes("</html>");
			byte[] slashHTML = getAsASCIIBytes("</HTML>");
			result = searchFor(0x80,startOffset, rd, offsetLimit, slashhtml);
			if (result != -1) {
				result += slashhtml.length;
			} else {
				result = searchFor(0x80,startOffset, rd, offsetLimit, slashHTML);
				if (result != -1) {
					result += slashHTML.length;
				}
			}
			/*
			 * TODO: this may need to do a stack-like approach to find the outermost
			 * element. Also for both XML and HTML do a buffer check to see that all
			 * characters are in the ASCII range (basically bitwise & all bytes with 7F and
			 * be sure that they are equal to their original value);
			 * 
			 */
		} else if ("XML".equals(type)) {
			byte[] gt = getAsASCIIBytes(">");
			byte[] lt = getAsASCIIBytes("<");
			long close = searchFor(startOffset, rd, offsetLimit, gt);
			long open = -1;
			int maxVal = 0x80;
			String elementTag = null;
			if (startOffset == 63254028288l) {
				System.out.println("hoop");
			}
			if (close != -1) {
				open = searchFor(maxVal, close, rd, offsetLimit, lt);
			}
			while (result == -1 && open != -1 && elementTag == null) {
				close = searchFor(maxVal, open, rd, offsetLimit, gt);
				if (close != -1) {
					close += 1;
					int len = (int) (close - open);// 131_697_172_480
					String asStr = getASCIIString(open, len, rd.getFileChannel());
					if (asStr == null) {
						break;
					}
					if (asStr.startsWith("<!") || asStr.startsWith("<?") || asStr.startsWith("<[")
							|| asStr.length() < 2) {
						open = searchFor(maxVal, close, rd, offsetLimit, lt);
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
//				System.out.println(taggedElement);
				byte[] toSearchFor = getAsASCIIBytes(taggedElement);
				result = searchFor(maxVal, startOffset, rd, offsetLimit, toSearchFor);
				if (result != -1) {
					result += toSearchFor.length;
				}
			}
		}
		if (result > offsetLimit) {
			System.out.println("the " + type + "\tstartoffset " + startOffset + " has the discovered endoffset of "
					+ result + " which exceeds the offsetLimit of " + offsetLimit + "\n(overflow of "
					+ (result - offsetLimit) + " bytes and size of " + (result - startOffset)
					+ " bytes [size to offsetLimit " + (offsetLimit - startOffset) + " bytes]). Resetting to -1.");
			result = -1;
		}

		return result;
	}

	public static String getASCIIString(long startOffset, int len, FileChannel fc) throws IOException {
		return getString(startOffset, len, fc, StandardCharsets.US_ASCII);
	}

	public static String getString(long startOffset, int len, FileChannel fc, Charset charset) throws IOException {
		String str = null;
		byte[] bytes = getBytes(startOffset, len, fc);
		if (bytes != null) {
			if (charset != null) {
				str = new String(bytes, charset);
			} else {
				str = new String(bytes);
			}
		}
		return str;
	}

	public static byte[] getBytes(long startOffset, int len, FileChannel fc) throws IOException {
		long readStartLoc = (startOffset / RawDisk.DEFAULT_SECTOR_SIZE) * RawDisk.DEFAULT_SECTOR_SIZE;
		int bbStartLoc = (int) (startOffset % RawDisk.DEFAULT_SECTOR_SIZE);
		long readEndLoc = ((len + startOffset) / RawDisk.DEFAULT_SECTOR_SIZE) * RawDisk.DEFAULT_SECTOR_SIZE;
		if (((len + startOffset) % RawDisk.DEFAULT_SECTOR_SIZE) != 0) {
			readEndLoc += RawDisk.DEFAULT_SECTOR_SIZE;
		}
		int size = (int) (readEndLoc - readStartLoc);
		byte[] buffer = new byte[size];
		ByteBuffer bb = ByteBuffer.wrap(buffer);
		fc.read(bb, readStartLoc);
		bb.rewind();
		bb.position(bbStartLoc);
		byte[] toRead = new byte[len];
		bb.get(toRead);
		return toRead;
	}

	private static byte[] getAsASCIIBytes(String str) {
		return str.getBytes(StandardCharsets.US_ASCII);
	}

	public static void main(String[] args) throws Exception {
		RawDisk rd = new RawDisk("\\\\.\\PhysicalDrive0");
		long readOffset = 2994774016l;
		long limit = 0xB2810000l;
		long end = getEndOffset(readOffset, rd, limit, "XML");

//		try(FileChannel fc = FileChannel.open(Paths.get(args[0]), StandardOpenOption.READ)){
//			System.out.println(getMp4Size(0, fc));
//		}catch(IOException ioe) {
//			ioe.printStackTrace();
//		}
	}

	public static long getMp4Size(long startOffset, FileChannel rd) throws IOException {
		long result = -1;
		int initialBufferSize = 8192;
		if (rd != null) {
			ByteBuffer bb = ByteBuffer.wrap(new byte[initialBufferSize]).order(ByteOrder.BIG_ENDIAN);
			rd.read(bb, startOffset);
			int firstHeaderLen = bb.getInt(0);
//			System.out.println("first header len "+firstHeaderLen);
			int bbOffset = populateByteBufferWithDataFromLocation(startOffset, firstHeaderLen + Integer.BYTES, bb, rd);
			if (bbOffset > Integer.BYTES - 1) {
				bbOffset -= Integer.BYTES;
			}
			int secondHeaderLen = bb.getInt(bbOffset);
//			System.out.println("second header len "+secondHeaderLen);
			bbOffset = populateByteBufferWithDataFromLocation(startOffset,
					firstHeaderLen + secondHeaderLen + Integer.BYTES, bb, rd);
			if (bbOffset > Integer.BYTES - 1) {
				bbOffset -= Integer.BYTES;
			}
			int thirdHeaderLen = 0;
			try {
//				HexView.print(bb);
//				System.out.println("bbOfset:"+bbOffset);
				thirdHeaderLen = bb.getInt(bbOffset);
			} catch (IndexOutOfBoundsException e) {
				System.err.println("#getMp4Size:Could not get index " + bbOffset + " in byte buffer " + bb
						+ " (startoffset:" + startOffset + ")");
				return result;
			}
//			System.out.println("third header len "+thirdHeaderLen);
			bbOffset = populateByteBufferWithDataFromLocation(startOffset,
					firstHeaderLen + secondHeaderLen + (Integer.BYTES << 1) + Integer.BYTES, bb, rd);
			if (bbOffset > Integer.BYTES - 1) {
				bbOffset -= Integer.BYTES;
			}
			result = Integer.toUnsignedLong(bb.getInt(bbOffset)) + firstHeaderLen + secondHeaderLen + thirdHeaderLen;

		}
		return result;
	}

	public static long getMp4Size(long startOffset, RawDisk rd) throws IOException {
		long result = -1;
		int initialBufferSize = 8192;
		if (rd != null) {
			ByteBuffer bb = ByteBuffer.wrap(new byte[initialBufferSize]).order(ByteOrder.BIG_ENDIAN);
			rd.read(bb, startOffset);
			int firstHeaderLen = bb.getInt(0);
			int bbOffset = populateByteBufferWithDataFromLocation(startOffset, firstHeaderLen + Integer.BYTES, bb, rd);
			if (bbOffset > Integer.BYTES - 1) {
				bbOffset -= Integer.BYTES;
			}
			int secondHeaderLen = bb.getInt(bbOffset);
			bbOffset = populateByteBufferWithDataFromLocation(startOffset,
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
			bbOffset = populateByteBufferWithDataFromLocation(startOffset,
					firstHeaderLen + secondHeaderLen + (Integer.BYTES << 1) + Integer.BYTES, bb, rd);
			if (bbOffset > Integer.BYTES - 1) {
				bbOffset -= Integer.BYTES;
			}
			result = Integer.toUnsignedLong(bb.getInt(bbOffset)) + firstHeaderLen + secondHeaderLen + thirdHeaderLen;

		}
		return result;
	}

	public static long getASFSize(long startOffset, RawDisk rd, long endOffset) throws IOException {
		long result = -1;
		int initialBufferSize = 512;
		if (rd != null) {
			long filePropertiesGuidLocation = searchFor(startOffset, rd, endOffset, ASF_FILE_PROP_GUID);
			if (filePropertiesGuidLocation != -1) {
				ByteBuffer bb = ByteBuffer.wrap(new byte[initialBufferSize]).order(ByteOrder.LITTLE_ENDIAN);
				long sizeDataOffset = filePropertiesGuidLocation + ASF_FILE_PROP_GUID.length + 8
						+ ASF_FILE_PROP_GUID.length;
				long addressContainingSizeData = RawDisk.getContainingSectorOffset(sizeDataOffset);
				long diffOffset = sizeDataOffset - addressContainingSizeData;
				rd.read(bb, addressContainingSizeData);
				result = bb.getLong((int) diffOffset);
			}
		}
		return result;
	}

	public static long getRIFFSize(long startOffset, RawDisk rd) throws IOException {
		long result = -1;
		int initialBufferSize = 8192;
		if (rd != null) {
			ByteBuffer bb = ByteBuffer.wrap(new byte[initialBufferSize]).order(ByteOrder.LITTLE_ENDIAN);
			rd.read(bb, startOffset);
			result = bb.getInt(1);

		}
		return result;
	}

	private static byte[] toByteArray(int... ints) {
		byte[] asBytes = new byte[ints.length];
		for (int i = 0; i < ints.length; i++) {
			asBytes[i] = (byte) ints[i];
		}
		return asBytes;
	}

	public static long searchFor(int maxValue, long startOffset, RawDisk rd, long endOffset, byte[] toSearchFor)
			throws IOException {
		return searchFor(maxValue, startOffset, rd.getFileChannel(), endOffset, toSearchFor);
	}

	public static long searchFor(long startOffset, RawDisk rd, long endOffset, byte[] toSearchFor) throws IOException {
		return searchFor(startOffset, rd.getFileChannel(), endOffset, toSearchFor);
	}

	public static long searchFor(long startOffset, FileChannel rd, long endOffset, byte[] toSearchFor)
			throws IOException {
		return searchFor(-1, startOffset, rd, endOffset, toSearchFor);
	}

	public static long searchFor(int maxValue,long startOffset, FileChannel rd, long endOffset, byte[] toSearchFor)
			throws IOException {
		long result = -1;
		int initialBufferSize = 8192;
		if (rd != null) {
			ByteBuffer bb = ByteBuffer.wrap(new byte[initialBufferSize]);
			try {
				rd.read(bb, (startOffset / 512) * 512);
			} catch (IOException ioe) {
				return -1;
			}
			bb.rewind();
			bb.position((int) (startOffset % 512));
			int maxRead = getOffsetOfByteGreaterThanOrEqualTo(maxValue, bb);
			
			byte[] compareArray = new byte[toSearchFor.length];
			for (int i = 0; (maxRead>-1?i<maxRead:true)&&result == -1 && ((i + startOffset) - toSearchFor.length) < endOffset;) {
				if (bb.remaining() < toSearchFor.length) {
					bb = rebuffer(bb, bb.position(), rd, (i + startOffset) + bb.remaining());
					maxRead = getOffsetOfByteGreaterThanOrEqualTo(maxValue, bb);
//					HexView.print(bb);
				}
				compareArray = get(bb, compareArray);
				boolean ans = Arrays.equals(toSearchFor, compareArray);
//				System.out.println(HexView.toHexString(compareArray)+" == "+HexView.toHexString(toSearchFor)+"?"+ ans);

				if (ans) {
					result = startOffset + i;
				}
				i += 1;
			}
		}
		return result;
	}

	private static int getOffsetOfByteGreaterThanOrEqualTo(int maxValue, ByteBuffer bb) {
		if (maxValue > -1) {
			for (int i = bb.position(); i < bb.capacity(); i++) {
				int byteAsInt = Byte.toUnsignedInt(bb.get(i));
				if (byteAsInt >= maxValue) {
					return i;
				}
			}
		}
		return -1;

	}

	private static boolean allBytesAreLessThan(int maxValue, ByteBuffer bb) {
		boolean truth = true;
		for (int i = bb.position(); truth && i < bb.capacity(); i++) {
			int byteAsInt = Byte.toUnsignedInt(bb.get(i));
			truth = byteAsInt < maxValue;
		}
		return truth;
	}

	private static ByteBuffer rebuffer(ByteBuffer bb, int bbStartOffset, FileChannel rd, long readOffset)
			throws IOException {
		return rebuffer(bb, bbStartOffset, rd, readOffset, 512);
	}

	private static ByteBuffer rebuffer(ByteBuffer bb, int bbStartOffset, FileChannel rd, long readOffset, int readLen)
			throws IOException {
		ByteBuffer reBuffed = ByteBuffer.wrap(new byte[bb.remaining() + readLen]);
		reBuffed.put(bb);
		rd.read(reBuffed, readOffset);
		reBuffed.flip();
		reBuffed.position(0);
		return reBuffed;
	}

	private static byte[] get(ByteBuffer bb, byte[] dst) {
		bb.get(dst);
		bb.position(bb.position() - (dst.length - 1));
		return dst;
	}

	private static byte[] get(ByteBuffer bb, byte[] dst, int index) {
		return get(bb, dst, index, 0, dst.length);
	}

	private static byte[] get(ByteBuffer bb, byte[] dst, int index, int dstOffset, int dstLen) {
		for (int i = 0; i < dstLen; i++) {
			dst[i + dstOffset] = bb.get(index + i);
		}
		return dst;
	}

	/**
	 * 
	 * @param startOffset
	 * @param lenToSeek
	 * @param bb          the byteBuffer to be populated with data
	 * @param rd
	 * @return the offset within the ByteBuffer to seek to
	 * @throws IOException
	 */
	public static int populateByteBufferWithDataFromLocation(long diskStartOffset, int lenToSeek, ByteBuffer bb,
			RawDisk rd) throws IOException {
		long actualOffset = diskStartOffset + lenToSeek;
		if (lenToSeek < 0) {
			System.out.println("wtf");
		}
		if (lenToSeek < bb.capacity()) {
			return lenToSeek;
		}
		int seekOffset = (int) (actualOffset % bb.capacity());
		bb.rewind();
		bb = rd.read(bb, (actualOffset / bb.capacity()) * bb.capacity());
		bb.rewind();
		return seekOffset;

	}
	
//	public static long getPotentialEndOffset(long zeroedOutStartOffsetOrClusterStartOffset, Predicate<ByteBuffer> predicate, RawDisk rd) {
//		
//	}

	private static int populateByteBufferWithDataFromLocation(long diskStartOffset, int lenToSeek, ByteBuffer bb,
			FileChannel rd) throws IOException {
		long actualOffset = diskStartOffset + lenToSeek;
		if (lenToSeek < 0) {
			System.out.println("wtf");
		}
		if (lenToSeek < bb.capacity()) {
			return lenToSeek;
		}
		int seekOffset = (int) (actualOffset % bb.capacity());
		long readOffset = (actualOffset / bb.capacity()) * bb.capacity();
		bb.rewind();
		rd.read(bb, readOffset);
		bb.rewind();
		return seekOffset;

	}

}

/**
 *
 */
package com.jamesratzlaff.util.io;

import java.nio.ByteBuffer;

/**
 * @author
 *
 */
public class HexView {

	public static final HexView HEX_VIEW=new HexView();
	
	private int bytesPerLine;
	private long startOffset;
	private int length;


	public HexView() {
		bytesPerLine=16;
		startOffset=0;
		length=-1;
	}

	public String toString(ByteBuffer bb) {
		
		StringBuilder sb = new StringBuilder();
		int pos = bb.position();
		int lineNo=0;
		int maxNum = bb.capacity();
		int maxNumLen = String.valueOf(maxNum).length();
		while(bb.hasRemaining()) {
			sb.append(String.format("%0"+maxNumLen+"d", bytesPerLine*lineNo));
			sb.append("|");
			byte[] line = getLine(bb,lineNo);
			sb.append(toHexString(line));
			sb.append("|");
			sb.append(toText(line));
			sb.append('\n');
			lineNo+=1;
		}
		bb.position(pos);
		return sb.toString();
	}

	private static boolean xIsInY(byte x, byte[] y) {
		boolean result = false;
		for(int i=0;!result&&i<y.length;i++) {
			result=y[i]==x;
		}
		return result;
	}

	private static void replaceAll(byte[] bytes, byte replacement,byte...toReplace) {
		for(int i=0;i<bytes.length;i++) {
			byte toUse = bytes[i];
			if(xIsInY(toUse,toReplace)) {
				bytes[i]=replacement;
			}
		}
	}

	public static String toText(byte[] bytes) {
		replaceAll(bytes,(byte)'.',(byte)0x0,(byte)0x9,(byte)0xa,(byte)0xd);
		String asString=new String(bytes);

		return asString;
	}

	public static void printCharacterTable() {
		System.out.println("===START===CHARACTER_TABLE===========");
		System.out.println(getCharacterTable());
		System.out.println("-----------CHARACTER_TABLE----END----");
	}

	public static String getCharacterTable() {
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<256;i++) {
			if(i>0) {
				sb.append('\n');
			}
			sb.append(String.format("0x%02x", i)).append(":'").appendCodePoint(i).append('\'');;
		}
		return sb.toString();
	}

	public byte[] getLine(ByteBuffer bb, int lineNo) {
		int offset=lineNo*bytesPerLine;
		byte[] result = new byte[bytesPerLine];
	
		bb.get(result,0,Math.min(bb.remaining(), bytesPerLine));
		return result;
	}

	public static String toHexString(byte[] bytes) {
		StringBuilder sb=new StringBuilder();
		for(int i=0;i<bytes.length;i++) {
			if(i!=0) {
				sb.append(' ');
			}
			sb.append(toHexString(bytes[i]));
		}
		return sb.toString();
	}
	
	public static void print(ByteBuffer bb) {
		String hexViewString = HEX_VIEW.toString(bb);
		System.out.println(hexViewString);
	}

	private static String toHexString(byte b) {
		return String.format("%02x", b);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "HexView [bytesPerLine=" + bytesPerLine + ", startOffset=" + startOffset + ", length=" + length + "]";
	}




}

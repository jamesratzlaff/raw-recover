package com.jamesratzlaff.rawrecover;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.jamesratzlaff.util.io.EndOfFileGetter;

public interface RawFileLocation  extends LongRange {

	String getType();

	@Override
	long getStart();

	@Override
	long getEnd();

	@Override
	default long getLength() {
		if (getEnd() < 0) {
			return 0;
		}
		return getEnd() - getStart();
	}
	@Override
	default void deltaLength(long delta) {
		setLength(getLength()+delta);
	}
	@Override
	default void setLength(long len) {
		setEnd(getStart()+len);
	}

	@Override
	void setEnd(long endOffset);

	
	default boolean getEndsInPaddedCluster() {
		return endsInPaddedCluster();
	}
	
	default boolean endsInPaddedCluster() {
		return false;
	}

	String getMozillaCacheUrl();

	void setMozillaCacheUrl(String cacheUrl);

	void endsInPaddedCluster(boolean endInPaddedCluster);
	
	private int getTargetAddress(ByteBuffer bb, RawDisk rd, long offset) throws IOException {

		int sectorSize = RawDisk.DEFAULT_SECTOR_SIZE;
		int sectorsPerCluster = 8;
		int clusterSize = sectorSize * sectorsPerCluster;
		if (offset != -1 && bb != null) {
			long lastClusterAddress = (offset / clusterSize) * clusterSize;
			try {
				rd.read(bb, lastClusterAddress);
			} catch (IOException ioe) {
				System.err.println("Got I/O Exception " + ioe.getMessage() + ". Trying small reads");
				bb.rewind();
				for (int i = 0; i < sectorsPerCluster; i++) {
					int pos = i * sectorSize;
					bb.position(pos);
					ByteBuffer mini = ByteBuffer.wrap(new byte[sectorSize]);
					try {
						rd.read(mini, lastClusterAddress + pos);
						bb.put(mini);
					} catch (IOException ioe2) {
						System.err.println("Got I/O Exception '" + ioe.getMessage() + "' reading "
								+ (lastClusterAddress + pos) + " assuming zeroed sector");
					}
				}
			}
			return (int) (offset % bb.capacity());
		}
		return -1;
	}
	
	@Override
	default int compareTo(LongRange other) {
		int cmp=0;
		if(this.equals(other)) {
			return cmp;
		}
		if(other==null) {
			cmp=-1;
		}
		if(cmp!=0) {
			cmp=Long.compareUnsigned(this.getStart(), other.getEnd());
		}
		if(cmp!=0) {
			cmp=Long.compareUnsigned(this.getEnd(), other.getEnd());
		}
		return cmp;
	}

	private ByteBuffer getLastCluster(RawDisk rd) throws IOException {
		long endOffset = getEnd();
		if (endOffset != -1) {
			int sectorSize = RawDisk.DEFAULT_SECTOR_SIZE;
			int sectorsPerCluster = 8;
			int clusterSize = sectorSize * sectorsPerCluster;
			ByteBuffer bb = ByteBuffer.wrap(new byte[clusterSize]);

			getTargetAddress(bb, rd, endOffset);
			bb.rewind();
			bb.position(0);
			bb.position((int) (endOffset % (long) clusterSize));
			return bb;
		}
		return null;
	}

	default void writeToDisk(RawDisk in) throws IOException {
		writeToDisk(in, null);
	}

	default void writeToDisk(RawDisk in, Path out) throws IOException {
		if (this.getEnd() != -1) {
			if (out == null) {
				String hexStr = Long.toHexString(getStart());
				while (hexStr.length() < 16) {
					hexStr = "0" + hexStr;
				}
				out = Paths.get(System.getProperty("user.home"), ".rawrecover", "output",
						hexStr + "_" + getLength() + "." + getType().toLowerCase());
				if (!Files.exists(out.getParent())) {
					Files.createDirectories(out.getParent());
				}
			}
			if (Files.exists(out)) {
				System.err.println("The file " + out + " already exists.");
			} else {
				long bytesWritten = 0;
				try (FileChannel outputFile = FileChannel.open(out, StandardOpenOption.CREATE,
						StandardOpenOption.WRITE)) {

					ByteBuffer bb = ByteBuffer.allocate(RawDisk.DEFAULT_SECTOR_SIZE);
					int fullSectors = (int) (getLength() / RawDisk.DEFAULT_SECTOR_SIZE);
					int leftOverBytes = (int) (getLength() % RawDisk.DEFAULT_SECTOR_SIZE);
					for (int i = 0; i < fullSectors; i++) {
						bb.clear();
						try {
							in.read(bb, getStart() + (i * RawDisk.DEFAULT_SECTOR_SIZE));
							bb.flip();
							bytesWritten += outputFile.write(bb);
						} catch (IOException ioe) {
							System.err
									.println("Could not read " + getStart() + (i * RawDisk.DEFAULT_SECTOR_SIZE));
							outputFile.write(bb);
						}
					}
					if (leftOverBytes != 0) {
						in.read(bb, getStart() + (fullSectors * RawDisk.DEFAULT_SECTOR_SIZE));
						bb.rewind();
						bb.limit(leftOverBytes);
						outputFile.write(bb);
					}

				} finally {
					System.out.println("Wrote " + bytesWritten + " bytes to " + out);
				}
			}
		}
	}

	default boolean endsInPaddedCluster(RawDisk rd) throws IOException {
		boolean truth = false;
		ByteBuffer bb = getLastCluster(rd);
		if (bb != null) {
			ByteBuffer zeroBuff = ByteBuffer.allocateDirect(bb.remaining());
			if (zeroBuff.equals(bb)) {
				truth = true;
			
			} else {
				//If a file doesn't end in a 0 padded sector it may be part of the data in firefox's cache files, so we need to check for that
				bb.rewind();
				byte[] toMatch = new byte[] { 0x3A, 0x68, 0x74, 0x74, 0x70 };

				long addy = EndOfFileGetter.searchFor(getEnd(), rd, getEnd() + 8192, toMatch);
				if (addy != -1) {
					bb.rewind();
					int readOffset = getTargetAddress(bb, rd, addy);
					ByteArrayOutputStream bs = new ByteArrayOutputStream();
					boolean matches = true;
					bb.position(readOffset);
					if(bb.remaining()<toMatch.length+1024) {
						ByteBuffer bigger = ByteBuffer.allocate(bb.remaining()+bb.capacity());
						int remaining = bb.remaining();
						
						bigger.put(bb);
						readOffset = getTargetAddress(bigger,rd,addy+bb.capacity()+readOffset+remaining);
						bigger.position(readOffset);
						bb=bigger;
					}
					for (int i = 0; matches && i < toMatch.length; i++) {
						byte b = 0;
						try {
						b = bb.get();//Long.toHexString((((int)bb.get(291))&0xFF))
						} catch(BufferUnderflowException bu) {
							break;
						}
						if (b == toMatch[i]) {
							if (i > 0) {
								bs.write(b);
							}
						} else {
							matches = false;
						}
					}
					if (matches) {
						while (bb.hasRemaining()) {
							byte nxtByte = bb.get();
							if (nxtByte == 0) {
								break;
							}
							bs.write(nxtByte);
						}
					}
					if (matches) {
						truth = true;
						setMozillaCacheUrl(new String(bs.toByteArray(), StandardCharsets.US_ASCII));
					}
				}
			}

		}
		return truth;
	}

	
	/*
	 * TODO: add methods: default boolean overlapsWith(RawFileLocation other)
	 */

}

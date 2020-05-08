/**
 *
 */
package com.jamesratzlaff.rawrecover;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ObjLongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.jamesratzlaff.rawrecover.io.DiskDriveInfo;
import com.jamesratzlaff.rawrecover.io.OSPhysicalDriveInfo;
import com.jamesratzlaff.rawrecover.io.internal.WMIDiskDriveInfo;
import com.jamesratzlaff.util.function.ObjIntBiPredicate;
import com.jamesratzlaff.util.io.EndOfFileGetter;
import com.jamesratzlaff.util.io.db.Database;

/**
 * @author
 *
 */
public class RawDisk {
	public static final int DEFAULT_SECTOR_SIZE = 512;
	public static final int maxReadAmount = 65536 - (16384 + (8192 - (2048 + 1024)));
	public static final int clusterSize = 4096;
	public static final int sectors_per_track = 63;
	public static final int tracks_per_cylinder = 255;

	private final String resource;
	
	private OSPhysicalDriveInfo physicalDriveInfo;
	private transient RandomAccessFile raf;
	private transient FileChannel fc;
	private transient int blockSize = 4096;
	private long size = Long.MIN_VALUE;
	private List<ReadErrorRange> errorRanges;

	
	public RawDisk() {
		this("\\\\.\\PhysicalDrive0");
	}
	
	public RawDisk(String resourceName) {
		this.resource = resourceName;
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			this.close();
		}));
	}
	
	/**
	 * TODO: Add OS detection to determine which supplier to use
	 * @return the Supplier ctor base on detected OS
	 */
	private static Supplier<DiskDriveInfo> getDriveInfoImplementation(){
		return WMIDiskDriveInfo::new;
	}
	
	public OSPhysicalDriveInfo getPhysicalDriveInfo() {
		if(this.physicalDriveInfo==null) {
			DiskDriveInfo driveInfo=getDriveInfoImplementation().get();
			if(driveInfo!=null) {
				this.physicalDriveInfo=driveInfo.getByDevicePath(resource);
			}
		}
		return this.physicalDriveInfo;
	}
	
	
	
	
	
	public int getBlockSize() {
		return blockSize;
	}

	
	public long writeTo(OutputStream os, LongRange lr) throws IOException {
		return writeTo(os, lr.getStart(),lr.getEnd());
	}
	
	public long writeTo(OutputStream os, long startOffset, long endOffset) throws IOException{
		int readSize = 8192;
		int size = (int)(endOffset-startOffset);
		int parts = (size/readSize)+(size%readSize>0?1:0);
		long read = 0;
		byte[] buffer = new byte[readSize];
		ByteBuffer bb = ByteBuffer.wrap(buffer);
		List<ReadErrorRange> errorRanges = getErrorRangesIn(startOffset, endOffset, getErrorRanges());
		
		for(int i=0;i<parts;i++) {
			bb.rewind();
			long pos = (readSize*i)+startOffset;
			List<ReadErrorRange> currentReadErrorRanges = getErrorRangesIn(pos, pos+readSize, errorRanges);
			errorRanges.removeAll(currentReadErrorRanges);
			ReadErrorRange current = null;
			if(!currentReadErrorRanges.isEmpty()) {
				current=currentReadErrorRanges.remove(0);
			}
			while(current!=null) {
				
				if(current.contains(pos)) {
					long writes = Math.min(current.getLength()-(pos-current.getStart()), bb.remaining());
					for(long j=0;j<writes;j++) {
						bb.put((byte)0);
						
					}
					pos+=writes;
					read+=writes;
					if(!currentReadErrorRanges.isEmpty()) {
						current=currentReadErrorRanges.remove(0);
					} else {
						current=null;
					}
				} else {
					int currentLimit = bb.limit();
					long lim = current.getStart()-pos;
					bb.limit((int)lim);
					int currentlyRead=getFileChannel().read(bb, pos);
					pos+=currentlyRead;
					read+=currentlyRead;
					bb.limit(currentLimit);
				}
				
			}
			if(bb.remaining()>0) {
				read += getFileChannel().read(bb, pos);
			}
			bb.rewind();
			
			int off = (i==0?(int)(startOffset%bb.capacity()):0);
			int len = (i==parts-1?(int)(endOffset%buffer.length):buffer.length-off);
			os.write(buffer, off, len);
		}
		return read;
		
	}
	
	public List<ReadErrorRange> getErrorRanges(){
		if(this.errorRanges==null) {
			Database db = new Database();
			this.errorRanges=db.getReadErrorRanges();
			db.close();
		}
		return this.errorRanges;
	}
	
	public static List<ReadErrorRange> getErrorRangesIn(long startOffset, long endOffset, List<ReadErrorRange> ranges){
		if(ranges==null) {
			ranges=Collections.emptyList();
		}
		return ranges.stream().filter(range->range.getStart()>=startOffset&&range.getEnd()<endOffset).sorted().collect(Collectors.toList());
	}
	
	
	
	

	public RandomAccessFile getRandomAccessFile() {
		if (raf == null) {
			try {
				raf = new RandomAccessFile(resource, "r");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		return raf;
	}

	private static void close(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void close() {
		if (fc != null) {
			close(fc);
		}
		if (raf != null) {
			close(raf);
		}
	}

	public long size() {
		if (this.size == Long.MIN_VALUE) {
//			try {
			this.size = getPhysicalDriveInfo().getSize();//80_026_361_856l;//500_107_862_016l;// getFileChannel().size();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
		return this.size;
	}

	public void seekInRandomAccessFile(long offset) {
		try {
			getRandomAccessFile().seek(offset);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public FileChannel seekInFileChannel(long offset) {
		try {
			getFileChannel().position(offset);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return getFileChannel();
	}

	public FileChannel getFileChannel() {
		if (fc == null) {
			fc = getRandomAccessFile().getChannel();
			try {
				System.out.println(fc.position());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return fc;
	}

	public void nextCluster() {
		seekInFileChannel(getPosition() + clusterSize);
	}

	public ByteBuffer read(byte[] bytes, long offset) throws IOException {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		int read = getFileChannel().read(bb, offset);

		return bb;
	}

	public ByteBuffer read(byte[] bytes) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		try {
			int read = getFileChannel().read(bb);
			System.out.println(read);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bb;
	}
	
	private int getBytesPerSector() {
		return (int)getPhysicalDriveInfo().getBytesPerSector();
	}

	public ByteBuffer[] readSafely(int numberOfBytes) {
		if (numberOfBytes > (size() - getPosition())) {
			numberOfBytes = (int) (size() - getPosition());
		}
		int numberOfBuffers = numberOfBytes / getBytesPerSector();
		if (numberOfBytes % getBytesPerSector() != 0) {
			numberOfBuffers += 1;
		}
		ByteBuffer[] buffers = new ByteBuffer[numberOfBuffers];
		long bytesRead = 0;
		for (int i = 0; i < buffers.length; i++) {
			buffers[i] = ByteBuffer.wrap(new byte[getBytesPerSector()]);
			try {
				bytesRead += getFileChannel().read(buffers[i]);
				buffers[i].rewind();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("read " + bytesRead + " bytes");

		return buffers;
	}

	public ByteBuffer readSafelyIntoBuffer(int numberOfBytes) {
		ByteBuffer[] buffers = readSafely(numberOfBytes);
		ByteBuffer bb = ByteBuffer.wrap(new byte[buffers.length * getBytesPerSector()]);
		for (int i = 0; i < buffers.length; i++) {
			bb.put(buffers[i]);
		}
		bb.rewind();
		return bb;
	}

	public long getPosition() {
		try {
			return getFileChannel().position();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Long.MAX_VALUE;
	}

	public static long getContainingSectorOffset(long offset) {
		return ((offset / DEFAULT_SECTOR_SIZE) * DEFAULT_SECTOR_SIZE);
	}

	public ByteBuffer read(ByteBuffer bb, long offset) throws IOException {

		if (bb == null) {
			byte[] bs = new byte[getBytesPerSector()];
			bb = ByteBuffer.wrap(bs);
		}

		int read = getFileChannel().read(bb, offset);

		return bb;
	}

	public void seekToNextCluster(ByteBuffer bb) {

		long asClusters = (bb.limit() / clusterSize) + (bb.limit() % clusterSize != 0 ? 1 : 0);
		long toJump = asClusters * clusterSize;
		seekInFileChannel(toJump + getPosition());
	}

	public ByteBuffer read(ByteBuffer bb) {

		if (bb == null) {
			byte[] bs = new byte[getBytesPerSector()];
			bb = ByteBuffer.wrap(bs);
		}
		try {
			int read = getFileChannel().read(bb);
		} catch (IOException e) {

			e.printStackTrace();
		}
		return bb;
	}

	@Override
	public int hashCode() {
		return Objects.hash(resource);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RawDisk other = (RawDisk) obj;
		return Objects.equals(resource, other.resource);
	}

	public ByteBuffer read(int bytes) {
		return read(new byte[bytes]);
	}

	public ByteBuffer read(int bytes, long offset) throws IOException {
		return read(new byte[bytes], offset);
	}

	public long getRemaining() {
		return (size() - 1) - getPosition();
	}

	public void doSearches(List<PredicateTracker> trackers) {
//		ByteBuffer bb = ByteBuffer.wrap(new byte[512]);
//		long GB = 1 << 30;
//		long startTime = System.currentTimeMillis();
//		while (getPosition() < size()) {
//			long pos = getPosition();
//
//			if (pos % (GB) == 0) {
//				long currentTime = System.currentTimeMillis();
//				long total = currentTime - startTime;
//				long totalInSeconds = total / 1000;
//				long speedInMBps = 1024 / totalInSeconds;
//				long leftToReadInMB = (size() - pos) / (1 << 20);
//
//				System.out.println(System.currentTimeMillis() + " - " + (pos / GB) + "GB read - "
//						+ Long.toHexString(pos) + "@ " + (1024 / totalInSeconds) + "MB/s - "
//						+ leftToReadInMB / speedInMBps + " seconds to go");
//				startTime = currentTime;
//			}
//			read(bb, pos);
//			bb.rewind();
//			trackers.forEach(tracker -> tracker.accept(bb));
//			if (pos + clusterSize < size()) {
//				nextCluster();
//			} else {
//				break;
//			}
//		}
	}

	public static class DiskInfoCollector implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4908947041017938907L;
		private final List<PredicateTracker> trackers;
		private final ReadErrorTracker errorTracker;
		private final ArrayList<Long> skipSectors;
		private long currentSkipSector = -1l;
		private transient long startTime = -1;
		private transient long lastOp = -1;
		private final RawDisk disk;
		private transient long totalOpTime = 0;
		private transient long endTime = -1;
		private ByteBuffer bb;
		private int readAmount = 8192;
		private long totalRead;
		private long lastPosition;
		private transient long perGigOpTime;
		private boolean hasMore = true;
		private boolean enableTrackers = true;
		private boolean enableEmptyDataIndexing = false;
		private BlankRangeTracker brt;
		private boolean errorAggregationEnabled = false;
		private ReadErrorRangeTracker errorRangeTracker;
		private long limit=-1;

		public boolean isErrorAggregationEnabled() {
			return errorAggregationEnabled;
		}

		public DiskInfoCollector setErrorAggregationEnabled(boolean errorAggregationEnabled) {
			this.errorAggregationEnabled = errorAggregationEnabled;
			return this;
		}

		public DiskInfoCollector(RawDisk rd, List<PredicateTracker> trackers) {
			this(rd, trackers, new ArrayList<Long>(0));
		}

		public DiskInfoCollector(RawDisk rd, List<PredicateTracker> trackers, List<Long> skipSectors) {
			this(rd, 64, trackers, skipSectors);
		}

		public DiskInfoCollector(RawDisk rd, int duplicateMessageThreshold, List<PredicateTracker> trackers,
				List<Long> skipSectors) {
			this.trackers = trackers != null ? trackers : new ArrayList<PredicateTracker>(0);
			this.disk = rd;
			this.errorTracker = new ReadErrorTracker(duplicateMessageThreshold);
			this.lastPosition = this.disk != null ? this.disk.getPosition() : 0;
			if (skipSectors == null) {
				skipSectors = new ArrayList<Long>(0);
			}
			this.skipSectors = new ArrayList<Long>(skipSectors);
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				if (this.hasMore) {
					System.out.println(this.toString());
				}
			}));
		}

		public BlankRangeTracker getBlankRangeTracker() {
			if (this.brt == null) {
				this.brt = new BlankRangeTracker();
			}
			return this.brt;
		}

		public DiskInfoCollector setReadAmount(int readAmount) {
			int modAmount = readAmount % disk.getBytesPerSector();
			if (modAmount != 0) {
				readAmount = ((readAmount / disk.getBytesPerSector()) + 1) * disk.getBytesPerSector();
			}
			if (readAmount < disk.getBytesPerSector()) {
				readAmount = disk.getBytesPerSector();
			}
			if (this.readAmount != readAmount) {
				this.readAmount = readAmount;
				this.bb = ByteBuffer.wrap(new byte[this.readAmount]);
			}
			return this;
		}

		public ReadErrorRangeTracker getErrorRangeTracker() {
			if (errorRangeTracker == null) {
				this.errorRangeTracker = new ReadErrorRangeTracker();
			}
			return this.errorRangeTracker;
		}

		public long getPosition() {
			return this.getDisk().getPosition();
		}

		public DiskInfoCollector reduce() {
			if (this.skipSectors != null) {
				this.skipSectors.trimToSize();
			}
			if (this.trackers != null) {
				this.trackers.forEach(PredicateTracker::reduce);
			}
			return this;
		}

		public DiskInfoCollector enableTracker(boolean enable) {
			this.enableTrackers = enable;
			return this;
		}

		public boolean trackersEnabled() {
			return this.enableTrackers;
		}

		public DiskInfoCollector enableEmptyDataIndexing(boolean enable) {
			this.enableEmptyDataIndexing = enable;
			return this;
		}

		public boolean emptyDataIndexingEnabled() {
			return this.enableEmptyDataIndexing;
		}

		public List<RawFileLocation> getFileLocations() {
			reduce();
			List<RawFileLocation> all = getTrackers().stream()
					.flatMap(tracker -> tracker.getOffsets().stream()
							.map(off -> new SimpleRawFileLocation(tracker.getName(), off.longValue())))
					.collect(Collectors.toList());
			return all;
		}
		
		public long limit() {
			if(this.disk!=null&&(this.limit<0||this.limit>this.disk.size())) {
				return this.disk.size();
			}
			return this.limit;
		}
		
		public DiskInfoCollector limit(long limit) {
			this.limit=limit;
			return this;
		}

		public void search() {
			
			
			long GB = 1 << 30;
			while (nextSearch()) {
				if (getLastPosition() % GB == 0) {
					long totalTimeSeconds = (System.currentTimeMillis() - getStartTime()) / 1000;
					long opTimeInSeconds = perGigOpTime / 1000;
					long MBs = opTimeInSeconds > 0 ? (1024l / opTimeInSeconds) : Long.MAX_VALUE;
					long avgMBs = (((getLastPosition() >>> 20) / totalTimeSeconds));
					long avgTtg = (getDisk().getRemaining() >>> 20) / avgMBs;
					System.out
							.println(
									System.currentTimeMillis() + " - " + (getLastPosition() / GB) + "GB read - "
											+ (MBs > 0 ? (Long.toHexString(getLastPosition()) + "@ " + MBs + "MB/s - "
													+ ((this.getDisk().getRemaining() >>> 20) / MBs)
													+ "  seconds to go - Avg: " + avgMBs + " MBs (" + avgTtg + ") ttg")
													: ""));
					perGigOpTime = 0;
				}
			}
		}

		
		
		public boolean hasMore() {
			return this.hasMore;
		}

		public List<ReadErrorRange> getErrors() {
			if (isErrorAggregationEnabled()&&(getErrorRangeTracker().getOffsetRanges()==null||getErrorRangeTracker().getOffsetRanges().isEmpty())) {
				getErrorRangeTracker().check(skipSectors, getDisk());
			}
			return getErrorRangeTracker().getOffsetRanges();
		}

		public boolean nextSearch() {
			if (startTime < 0) {
				startTime = System.currentTimeMillis();
			}
			if (this.currentSkipSector == -1 && !this.skipSectors.isEmpty()) {
				this.currentSkipSector = this.skipSectors.remove(0);
			}
			getBb();
			if (this.disk != null && this.disk.getRemaining() > 0) {
				long offset = this.disk.getPosition();
				if (this.currentSkipSector > -1 && offset <= this.currentSkipSector
						&& this.currentSkipSector <= (offset + bb.limit())) {
					this.disk.seekToNextCluster(bb);
					this.lastPosition = this.disk.getPosition();
					bb.limit(bb.capacity());
					this.currentSkipSector = -1;
					this.hasMore = true;
					return this.hasMore;
				}
				boolean exceptionOccured = false;
				long opStart = System.currentTimeMillis();
				try {

					this.disk.read(bb, offset);
					bb.flip();
					totalRead += bb.limit();
					int clusters = (bb.limit() / clusterSize) + (bb.limit() % clusterSize != 0 ? 1 : 0);
					if (emptyDataIndexingEnabled()) {
						getBlankRangeTracker().check(offset, bb);
					}
					for (int i = 0; i < clusters; i++) {
						int bb_offset = i * clusterSize;
						if (trackersEnabled()) {
							trackers.forEach(tracker -> tracker.addIfAcceptable(bb, bb_offset, offset));
						}
					}

					if (offset + clusterSize < this.disk.size()) {
						this.disk.seekToNextCluster(bb);
						this.lastPosition = this.disk.getPosition();
						bb.limit(bb.capacity());
					} else {
						this.endTime = System.currentTimeMillis();
						this.hasMore = false;
						return this.hasMore;
					}
				} catch (IOException e) {
					errorTracker.accept(e, offset);
					exceptionOccured = true;
				}
				long opEnd = System.currentTimeMillis();
				long opTime = opEnd - opStart;
				this.perGigOpTime += opTime;
				this.totalOpTime += opTime;
				this.lastOp = System.currentTimeMillis();
				if (exceptionOccured) {
					if (bb.limit() <= clusterSize) {
						this.disk.seekToNextCluster(bb);
						bb.limit(bb.capacity());
						this.lastPosition = this.getPosition();
					} else {
						bb.limit(bb.limit() - 512);
					}
				}
				this.hasMore = true;
				return this.hasMore;
			}
			this.hasMore = false;
			return this.hasMore;
		}

		public List<PredicateTracker> getTrackers() {
			return trackers;
		}

		public ReadErrorTracker getErrorTracker() {
			return errorTracker;
		}

		public long getStartTime() {
			return startTime;
		}

		public long getLastOp() {
			return lastOp;
		}

		public RawDisk getDisk() {
			return disk;
		}

		public long getTotalOpTime() {
			return totalOpTime;
		}

		public long getEndTime() {
			return endTime;
		}

		public ByteBuffer getBb() {
			if (bb == null) {
				bb = ByteBuffer.wrap(new byte[this.readAmount]);
			}
			return bb;
		}

		public long getTotalRead() {
			return totalRead;
		}

		public long getLastPosition() {
			return lastPosition;
		}

		@Override
		public int hashCode() {
			return Objects.hash(disk, errorTracker, lastPosition, totalRead, trackers);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DiskInfoCollector other = (DiskInfoCollector) obj;
			return Objects.equals(disk, other.disk) && Objects.equals(errorTracker, other.errorTracker)
					&& lastPosition == other.lastPosition && totalRead == other.totalRead
					&& Objects.equals(trackers, other.trackers);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("DiskInfoCollector [trackers=");
			builder.append(trackers);
			builder.append(", errorTracker=");
			builder.append(errorTracker);
			builder.append(", disk=");
			builder.append(disk);
			builder.append(", totalRead=");
			builder.append(totalRead);
			builder.append(", lastPosition=");
			builder.append(lastPosition);
			builder.append(", start time=");
			builder.append(new Date(startTime));
			builder.append(", lastOpTime=");
			builder.append(new Date(lastOp));
			builder.append(", runtime=");
			builder.append(", emptyDataIndex=");
			builder.append(this.brt);
			LocalDate start = LocalDate.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault());
			LocalDate end = LocalDate.ofInstant(Instant.ofEpochMilli(lastOp), ZoneId.systemDefault());
			Period p = Period.between(start, end);
			builder.append(p.toString());

			builder.append("]");
			return builder.toString();
		}

	}

	public static class BlankRangeTracker implements Serializable {
		private static final long serialVersionUID = 111028776892660930L;
		private final List<ZeroedOutOffsetRange> offsetRanges;
		private ZeroedOutOffsetRange current;
		private final transient ByteBuffer blankSector;
		private int endsWithBlank = -1;

		public void check(long offset, ByteBuffer bb) {
			if (bb != null) {
				int bbPos = bb.position();
				if (offset > -1) {
					bb.position(0);
					byte[] buffer = new byte[blankSector.capacity()];
					ByteBuffer temp = ByteBuffer.wrap(buffer);
					int iterations = bb.capacity() / blankSector.capacity();
					if (bb.capacity() % blankSector.capacity() != 0) {
						iterations += 1;
					}
					int readOffset = bb.position();

					for (int i = 0; i < iterations; i++) {
						bb.get(buffer);

						int mm = this.blankSector.mismatch(temp);
						if (mm == -1) {
							if (this.current == null) {

								this.current = new ZeroedOutOffsetRange(
										offset + readOffset + (endsWithBlank > 0 ? -endsWithBlank : 0));
								this.offsetRanges.add(this.current);
								if (endsWithBlank > 0) {
									this.current.deltaLength(endsWithBlank);
								}
							}
							endsWithBlank = -1;
							this.current.deltaLength(temp.capacity());
						} else {
							if (this.current != null) {
								this.current.deltaLength(mm);
								this.current = null;
							}
							int zeros = endsWithBlank(temp);
							if (zeros > 0) {
								endsWithBlank = zeros;
							} else {
								endsWithBlank = -1;
							}
						}
						readOffset += temp.capacity();
					}
				}
				bb.position(bbPos);
			}
		}

		private int endsWithBlank(ByteBuffer temp) {
			int zeros = 0;
			for (int i = temp.capacity() - 1; i > -1; i--) {
				if (temp.get(i) == 0) {
					zeros += 1;
				} else {
					break;
				}
			}
			return zeros;
		}

		public BlankRangeTracker() {
			this.offsetRanges = new ArrayList<ZeroedOutOffsetRange>();
			this.blankSector = ByteBuffer.allocateDirect(DEFAULT_SECTOR_SIZE);
		}

		public List<ZeroedOutOffsetRange> getOffsetRanges() {
			return offsetRanges;
		}

		@Override
		public int hashCode() {
			return Objects.hash(current, offsetRanges);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			BlankRangeTracker other = (BlankRangeTracker) obj;
			return Objects.equals(current, other.current) && Objects.equals(offsetRanges, other.offsetRanges);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("/*BlankRangeTracker*/ {'offsetRanges':[");
			boolean empty = offsetRanges.isEmpty();
			if (!empty) {
				builder.append("\n\t");
			}
			builder.append(String.join(",\n\t",
					offsetRanges.stream().sorted().map(OffsetRange::toString).collect(Collectors.toList())));
			if (!empty) {
				builder.append("\n");
			}
			builder.append("]}");
			return builder.toString();
		}

	}

	public static List<Long> getBadClusters(RawDisk rd){
		List<Long> badClusters = new ArrayList<Long>(8192);
		try {
			long originalPosition = rd.getFileChannel().position();
			rd.getFileChannel().position(0);
			long currentOffset=0;
			int clusterSize=4096;
			ByteBuffer bb = ByteBuffer.allocateDirect(clusterSize);
			int errorCounter=0;
			int errorCounterThreshold=128;
			boolean stop=false;
			long start = System.currentTimeMillis();
			long ONE_GB=1<<30;
			while(currentOffset<rd.size()&&!stop) {
				if(currentOffset%ONE_GB==0) {
					long avgSpeed = getAvgMBPerSecond(start, currentOffset);
					System.out.println((currentOffset>>>30)+"GB read, "+avgSpeed+"MB/sec");
				}
				try {
					rd.read(bb, currentOffset);
				}catch(IOException ioe) {
					if(!"Data error (cyclic redundancy check)".equalsIgnoreCase(ioe.getMessage())) {
						System.out.println(ioe.getMessage());
						errorCounter+=1;
						if(errorCounter>errorCounterThreshold) {
							stop=true;
						}
					} else {
						badClusters.add(currentOffset);
					}
				}
				currentOffset+=bb.capacity();
				bb.rewind();
			}
			rd.getFileChannel().position(originalPosition);
		}catch(IOException e) {
			e.printStackTrace();
		}
		return badClusters;
		
	}
	
	private static long getElapsedTime(long startTime) {
		return System.currentTimeMillis()-startTime;
	}
	
	private static long getAvgMBPerSecond(long startTime, long offset) {
		long totalTime = getElapsedTime(startTime);
		long timeInSeconds = totalTime/1000;
		long offsetInMB = offset>>>20;
		if(timeInSeconds<1) {
			return 0;
		}
		return offsetInMB/timeInSeconds;
	}
	
	public static class ReadErrorRangeTracker implements Serializable {
		private static final long serialVersionUID = 111028776892660930L;
		private final List<ReadErrorRange> offsetRanges;
		private ReadErrorRange current;
		private final transient ByteBuffer sectorBuffer;

		public void check(List<Long> offsets, RawDisk rd) {
			offsets.forEach(offset -> {
				System.out.println("Checking "+offset);
				check(offset, rd);
			});
		}

		public void check(long offset, RawDisk rd) {
			
			if (rd != null) {
				if(this.current!=null&&this.current.getEnd()!=offset) {
					this.current=null;
				}
				if (offset > -1) {
					int iterations = 4096 / sectorBuffer.capacity();

					for (int i = 0; i < iterations; i++) {
						String message = null;
						long internalOffset = (i * sectorBuffer.capacity());
						long readOffset = offset+internalOffset;
						try {
							sectorBuffer.position(0);
							rd.read(sectorBuffer, readOffset);
							sectorBuffer.rewind();
						} catch (IOException ioe) {
							message = ioe.getMessage();
						}
						if (message != null) {
							if (this.current == null) {
								this.current = new ReadErrorRange(readOffset, message);
								this.offsetRanges.add(this.current);
							}
							if (this.current != null) {
								if (!this.current.getMessage().equalsIgnoreCase(message)) {
									this.current = new ReadErrorRange(readOffset, message);
									this.offsetRanges.add(this.current);
								}
								this.current.deltaLength(sectorBuffer.capacity());
							}
						} else {
							this.current = null;
						}
					}
				}
			}
		}

		public ReadErrorRangeTracker() {
			this.offsetRanges = new ArrayList<ReadErrorRange>();
			this.sectorBuffer = ByteBuffer.allocateDirect(DEFAULT_SECTOR_SIZE);
		}

		public List<ReadErrorRange> getOffsetRanges() {
			return offsetRanges;
		}

		@Override
		public int hashCode() {
			return Objects.hash(current, offsetRanges);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ReadErrorRangeTracker other = (ReadErrorRangeTracker) obj;
			return Objects.equals(current, other.current) && Objects.equals(offsetRanges, other.offsetRanges);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("/*ReadErrorRangeTracker*/ {'offsetRanges':[");
			boolean empty = offsetRanges.isEmpty();
			if (!empty) {
				builder.append("\n\t");
			}
			builder.append(String.join(",\n\t",
					offsetRanges.stream().sorted().map(OffsetRange::toString).collect(Collectors.toList())));
			if (!empty) {
				builder.append("\n");
			}
			builder.append("]}");
			return builder.toString();
		}

	}

	public static class PredicateTracker implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = -6399180409738218646L;
		private final String name;
		private List<Long> offsets;
		private transient final Predicate<ByteBuffer> predicate;
		private transient final ObjIntBiPredicate<ByteBuffer> biPred;

		public PredicateTracker(String name) {
			this.name = name;
			this.biPred = PredicateMaker.getBiPredicate(name);
			this.predicate = PredicateMaker.getPredicate(name);

		}

		public String getName() {
			return this.name;
		}

		public List<Long> getOffsets() {
			if (this.offsets == null) {
				this.offsets = new ArrayList<Long>();
			}
			return this.offsets;
		}

		public PredicateTracker reduce() {
			List<Long> all = getOffsets();
			if (all instanceof ArrayList) {
				((ArrayList<Long>) all).trimToSize();
				all = new ArrayList<Long>(all.stream().distinct().sorted().collect(Collectors.toList()));
				this.offsets = all;
			}
			return this;
		}

		private synchronized void addOffset(long diskOffset) {
			getOffsets().add(diskOffset);
		}

		public void addIfAcceptable(ByteBuffer bb, int predicateOffset, long diskOffset) {
			if (this.biPred.test(bb, predicateOffset)) {
				addOffset(diskOffset + predicateOffset);
			}
		}

		@Deprecated
		public void accept(ByteBuffer bb, int offset) {
			if (bb != null) {
				try {
					if (this.biPred.test(bb, offset)) {
//						occurances += 1;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		@Deprecated
		public void accept(ByteBuffer bb) {
			if (bb != null) {
				try {
					if (this.predicate.test(bb)) {
//						occurances += 1;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, getOffsets());
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("PredicateTracker ").append('(').append(getOffsets().size()).append(" elements) [name=");
			builder.append(name);
			builder.append(", offsets=");
			builder.append(
					String.join(",\n", getOffsets().stream().map(l -> l.toString()).collect(Collectors.toList())));
			builder.append("]");
			return builder.toString();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PredicateTracker other = (PredicateTracker) obj;
			return Objects.equals(name, other.name) && Objects.equals(getOffsets(), other.getOffsets());
		}

	}

	public static class ReadErrorTracker implements Serializable, ObjLongConsumer<Exception> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4331535358771531995L;
		private final Map<String, List<Long>> errorOffsets;
		private long maxErrorOffset = -1;
		private Exception lastException;
		private int duplicateMessages;
		private final int duplicateMessageThreshold;

		public ReadErrorTracker(int duplicateMessageThreshold) {
			this.errorOffsets = new ConcurrentHashMap<String, List<Long>>(16);
			this.maxErrorOffset = -1;
			this.duplicateMessageThreshold = duplicateMessageThreshold;
		}

		public ReadErrorTracker() {
			this(64);
		}

		private boolean isDuplicate(Exception e, long offset) {
			if (Objects.equals(e.getClass(), getLastExceptionClass())
					&& Objects.equals(getLastExceptionMessage(), e.getMessage())) {
				return offset <= maxErrorOffset;
			}
			return false;
		}

		public synchronized void accept(Exception e, long offset) {
			if (isDuplicate(e, offset)) {
				duplicateMessages += 1;
				if (duplicateMessages >= duplicateMessageThreshold) {
					throw new RuntimeException(
							"Duplicate Exception threshold of " + duplicateMessageThreshold + " exceeded", e);
				}
			} else {
				duplicateMessages = 0;
				String message = e.getMessage();
				if (message == null) {
					message = "";
				}
				List<Long> offsetsForMessage = errorOffsets.get(message);
				if (offsetsForMessage == null) {
					offsetsForMessage = new ArrayList<Long>();
					errorOffsets.put(message, offsetsForMessage);
				}
				if (this.maxErrorOffset < offset) {
					this.maxErrorOffset = offset;

				} else {
					if (this.maxErrorOffset > offset) {
						System.err.println("Potential wonkiness");
					}
				}
				if (offsetsForMessage.isEmpty() || offset > offsetsForMessage.get(offsetsForMessage.size() - 1)) {
					offsetsForMessage.add(offset);
					System.err.println(e.getMessage() + "@" + Long.toHexString(offset));
				}
			}
		}

		private String getLastExceptionMessage() {
			return this.lastException != null ? this.lastException.getMessage() : null;
		}

		private Class<? extends Exception> getLastExceptionClass() {
			return this.lastException != null ? this.lastException.getClass() : null;
		}

		private String getLastExceptionClassName() {
			Class<? extends Exception> lastExceptionClass = getLastExceptionClass();
			return lastExceptionClass != null ? lastExceptionClass.getCanonicalName() : null;
		}

		@Override
		public int hashCode() {
			return Objects.hash(duplicateMessageThreshold, duplicateMessages, errorOffsets, getLastExceptionClassName(),
					getLastExceptionMessage(), maxErrorOffset);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ReadErrorTracker other = (ReadErrorTracker) obj;
			return duplicateMessageThreshold == other.duplicateMessageThreshold
					&& duplicateMessages == other.duplicateMessages && Objects.equals(errorOffsets, other.errorOffsets)
					&& Objects.equals(getLastExceptionClassName(), other.getLastExceptionClassName())
					&& Objects.equals(getLastExceptionMessage(), other.getLastExceptionMessage());
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("/*ReadErrorTracker*/ {errorOffsets:");
			builder.append("{");
			errorOffsets.keySet().forEach(key -> {
				builder.append('"').append(key).append("\":[\n")
						.append(String.join(",\n",
								errorOffsets.get(key).stream().map(Long::toHexString).collect(Collectors.toList())))
						.append("],");
			});
			builder.append("{");
//			builder.append(errorOffsets);
			builder.append(", maxErrorOffset:");
			builder.append(maxErrorOffset);
			String lastExceptionClassName = getLastExceptionClassName();
			String lastExceptionMessage = getLastExceptionMessage();
			builder.append(", lastExceptionClassname:")
					.append(String.format(lastExceptionClassName != null ? "\"%s\"" : "%s", lastExceptionClassName));
			builder.append(", lastExceptionMessage:")
					.append(String.format(lastExceptionMessage != null ? "\"%s\"" : "%s", lastExceptionMessage));
			builder.append(", duplicateMessages:" + duplicateMessages);
			builder.append(duplicateMessages);
			builder.append(", duplicateMessageThreshold:");
			builder.append(duplicateMessageThreshold);
			builder.append("}");
			return builder.toString();
		}

		public Map<String, List<Long>> getErrorOffsets() {
			return errorOffsets;
		}

	}

	public static void scanDisk(DiskInfoCollector collector) {
		try {
			collector.search();
			collector.reduce();

		} catch (RuntimeException re) {
			System.err.println("Exiting due to runtime exception (it could just be we reached the end of the drive)");
			re.printStackTrace();
		} finally {
			System.out.println(collector);
		}

	}

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			args = new String[] { "\\\\.\\PhysicalDrive3" };
		}
		RawDisk rd = new RawDisk(args[0]);
		rd.seekInRandomAccessFile(0x0l);// 0x1F600400l);
		long startTime = System.currentTimeMillis();
//		doBadClusterSearch(rd);
//		doQuerying(rd);
		doScanning(rd);
		doEndSearch(rd, "MP4","HTML","XML");
		long endTime = System.currentTimeMillis();
		System.out.println("Total run time: " + (endTime - startTime) + " ms");

	}
	
	private static void doBadClusterSearch(RawDisk rd) {
		List<Long> badClusters = getBadClusters(rd);
		System.out.println("========Bad Clusters======");
		badClusters.forEach(System.out::println);
		System.out.println("--------Bad Clusters------");
	}

	private static void doQuerying(RawDisk rd) {
		Database db = new Database();
		List<RawFileLocation> locations = db.getRawOffsets().stream().filter(location -> location.getEnd() != -1
				&& !location.endsInPaddedCluster() && location.getType().equalsIgnoreCase("JPEG"))
				.collect(Collectors.toList());
		doEndClusterSearch(db, rd, locations);
		locations.forEach(System.out::println);
	}
	
	private static void doEndSearch(RawDisk rd, String...types) {
		Database db = new Database();
		Set<String> typeSet = Arrays.asList(types).stream().map(type->type.toUpperCase()).collect(Collectors.toSet()); 
		List<RawFileLocation> locations = db.getRawOffsets().stream().filter(location -> location.getEnd() == -1
				&& !location.endsInPaddedCluster() && (typeSet.isEmpty()?true:typeSet.contains(location.getType())))
				.sorted().collect(Collectors.toList());
		findEnds(locations, rd);
		locations = locations.stream().filter(location->location.getEnd()!=-1).collect(Collectors.toList());
		db.merge(locations);
		doEndClusterSearch(db, rd, locations);
	}

	private static void doEndClusterSearch(Database db, RawDisk rd, List<RawFileLocation> locations) {
		locations.stream().filter(location -> location.getEnd() != -1 && !location.endsInPaddedCluster()).forEach(l -> {

			try {
				l.endsInPaddedCluster(rd);
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		});
		db.merge(locations.stream().filter(location -> location.endsInPaddedCluster()).collect(Collectors.toList()));
	}

	private static void doScanning(RawDisk rd) {
		List<String> preds = PredicateMaker.config.getStringList("app.use-predicates");
		List<PredicateTracker> trackers = preds.stream().map(PredicateTracker::new).collect(Collectors.toList());
		List<Long> skips = PredicateMaker.config.getLongList("app.skip.values");

		DiskInfoCollector collector = new DiskInfoCollector(rd, trackers, skips).setReadAmount(8192);
		collector.enableTracker(true).enableEmptyDataIndexing(true);
		scanDisk(collector);
		
		Database db = new Database();
		if(collector.emptyDataIndexingEnabled()) {
			List<ZeroedOutOffsetRange> zeroedOut = collector.getBlankRangeTracker().getOffsetRanges();
			db.mergeZeroedOffsets(zeroedOut);
		}
		List<SimpleRawFileLocation> current = db.getRawOffsets();
		List<RawFileLocation> updates = collector.getFileLocations().stream().filter(fl->!offsetListContains(current, fl)).collect(Collectors.toList());
		try {
			findEnds(updates, rd);
		}catch(Exception e) {
			e.printStackTrace();
		}
		db.merge(updates);
		
		
//		db.resetAll(locations);
//		List<RawFileLocation> locations = loadLocationsFromConfig("MP4","JPEG","RIFF","PNG","SQLITE","WEBM","ASF");

//		findEnds(locations,rd);
//		printStats(locations);
//		db.merge(locations);
//		

	}
	
	private static boolean offsetListContains(List<? extends LongRange> list, LongRange other) {
		if(list==null||other==null) {
			return false;
		}
		return list.stream().anyMatch(i->i.hasSameStartAs(other));
	}

	public static void printStats(List<RawFileLocation> locations) {
		System.out.println("Total file locations: " + locations.size());
		System.out.println("Total size of file with known end locations: "
				+ locations.stream().filter(l -> l.getEnd() != -1).mapToLong(l -> l.getLength()).sum());
		System.out.println("Largest file size: "
				+ locations.stream().filter(l -> l.getEnd() != -1).mapToLong(l -> l.getLength()).max().orElse(0));
		System.out.println("Total files where end could not be found: "
				+ locations.stream().filter(l -> l.getEnd() == -1).count());
	}

	public static void findEnds(List<RawFileLocation> rfls, RawDisk rd) {
		for (int i = 0; i < rfls.size(); i++) {
			RawFileLocation rfl = rfls.get(i);
			if (rfl.getEnd() == -1) {
				System.out.println("finding "+rfl);
				long start = rfl.getStart();
				String type = rfl.getType();
				long maxOffset = i < rfls.size() - 1 ? rfls.get(i + 1).getStart() : rd.size();
				long endOffset = -1;
				try {
					endOffset = EndOfFileGetter.getEndOffset(start, rd, maxOffset, type);
				} catch (Exception ioe) {
					System.err.println("[ERROR]Could not get end offset for " + rfl);
				}
				if (endOffset != -1) {
					rfl.setEnd(endOffset);
				}
			}
		}
	}

	private static List<RawFileLocation> create(String type, List<Long> startOffsets) {
		ArrayList<RawFileLocation> reso = new ArrayList<RawFileLocation>(startOffsets.size());
		for (int i = 0; i < startOffsets.size(); i++) {
			long startOffset = startOffsets.get(i);
			RawFileLocation rfl = new SimpleRawFileLocation(type, startOffset);
			reso.add(rfl);
		}
		Collections.sort(reso);
		return reso;
	}
	
	public static List<RawFileLocation> loadLocationsFromConfig(String... types) {
		ArrayList<RawFileLocation> l = new ArrayList<RawFileLocation>();
		for (int i = 0; i < types.length; i++) {
			String type = types[i];
			List<Long> startOffsets = PredicateMaker.config.getLongList("app.startOffsets." + type);
			List<RawFileLocation> asList = create(type, startOffsets);
			l.addAll(asList);
		}
		l.trimToSize();
		Collections.sort(l);
		return l;
	}

//	public static void oldMain(String[] args) {
//		if (args.length < 1) {
//			args = new String[] { "\\\\.\\PhysicalDrive0" };
//		}
//
//		RawDisk rd = new RawDisk(args[0]);
//		rd.seekInRandomAccessFile(0x1F600400l);// 0x1F600400l);
//
//		ByteBuffer bb = rd.read(1 << 20);
//
//		HexView hv = new HexView();
//		System.out.println(hv.toString(bb));
//		System.out.println(Long.toHexString(rd.getPosition()));
//		try {
//			System.out.println(rd.getFileChannel().position());
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		rd.close();
//	}

//
//
//	ByteBuffer buf = ByteBuffer.allocate(512);
//
//	try (RandomAccessFile raf = new RandomAccessFile("\\\\.\\PhysicalDrive0", "r");
//	     FileChannel fc = raf.getChannel()) {
//	    fc.read(buf);
//	    System.out.println("It worked! Read bytes: " + buf.position());
//	} catch (Exception e) {
//	    e.printStackTrace();
//	}

}

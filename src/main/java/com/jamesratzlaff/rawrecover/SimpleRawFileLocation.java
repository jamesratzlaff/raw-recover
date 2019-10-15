package com.jamesratzlaff.rawrecover;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.jamesratzlaff.rawrecover.RawDisk.PredicateTracker;

/**
 * 
 * @author jamesratzlaff
 *
 */
public class SimpleRawFileLocation implements RawFileLocation {

	private static final long serialVersionUID = -3135942373708404445L;
	private final String type;
	private long startOffset;
	private long endOffset = -1;
	private boolean endsInPaddedCluster = false;

	public static List<SimpleRawFileLocation> create(List<PredicateTracker> trackers) {
		List<SimpleRawFileLocation> result = trackers.stream().flatMap(tracker -> {
			List<SimpleRawFileLocation> asList = create(tracker);
			return asList.stream();
		}).sorted().collect(Collectors.toList());
		return result;
	}

	public static List<SimpleRawFileLocation> create(Map<String, ? extends Collection<Long>> map) {
		int totalLen = map.values().stream().mapToInt(list -> list.size()).sum();
		ArrayList<SimpleRawFileLocation> result = new ArrayList<SimpleRawFileLocation>(totalLen);
		map.keySet().forEach(key -> {
			Collection<Long> locations = map.get(key);
			List<SimpleRawFileLocation> local = create(key, locations);
			result.addAll(local);
		});
		result.trimToSize();
		Collections.sort(result);
		return result;
	}

	public static List<SimpleRawFileLocation> create(PredicateTracker pt) {
		List<SimpleRawFileLocation> result = create(pt.getName(), pt.getOffsets());
		return result;
	}

	public static List<SimpleRawFileLocation> create(String type, Collection<Long> startLocations) {
		List<SimpleRawFileLocation> result = new ArrayList<SimpleRawFileLocation>(startLocations.size());
		startLocations.stream().map(location -> new SimpleRawFileLocation(type, location)).forEach(result::add);
		Collections.sort(result);
		return result;
	}

	public SimpleRawFileLocation(String type, long start, long end, boolean endsInPaddedCluster) {
		this.type = type;
		this.startOffset = start;
		this.endOffset = end;
		this.endsInPaddedCluster = endsInPaddedCluster;
	}

	public SimpleRawFileLocation(String type, long start, long end) {
		this(type, start, end, false);
	}

	public SimpleRawFileLocation(String type, long start) {
		this(type, start, -1);
	}

	public SimpleRawFileLocation(String type) {
		this(type, -1);
	}

	public long getStartOffset() {
		return startOffset;
	}

	public void setStartOffset(long startOffset) {
		this.startOffset = startOffset;
	}

	public void setLength(long sizeInBytes) {
		this.endOffset = getStartOffset() + sizeInBytes;
	}

	public long getEndOffset() {
		return endOffset;
	}

	public void setEndOffset(long endOffset) {

		this.endOffset = endOffset;
		if (endOffset <= getStartOffset()) {
			System.err.println(getType() + "\tThe given end offset " + endOffset
					+ " is less than or equal to the start offset " + getStartOffset() + ".");
		}
	}

	public String getType() {
		return type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(endOffset, startOffset, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimpleRawFileLocation other = (SimpleRawFileLocation) obj;
		return endOffset == other.endOffset && startOffset == other.startOffset && Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SimpleRawFileLocation [type=");
		builder.append(type);
		builder.append(", startOffset=");
		builder.append(startOffset);
		builder.append(", endOffset=");
		builder.append(endOffset);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public boolean endsInPaddedCluster() {
		return endsInPaddedCluster;
	}

	public void endsInPaddedCluster(boolean endsInPaddedCluster) {
		this.endsInPaddedCluster = endsInPaddedCluster;
	}

	@Override
	public boolean endsInPaddedCluster(RawDisk rd) {
		try {
			endsInPaddedCluster(RawFileLocation.super.endsInPaddedCluster(rd));
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return endsInPaddedCluster();
	}

}

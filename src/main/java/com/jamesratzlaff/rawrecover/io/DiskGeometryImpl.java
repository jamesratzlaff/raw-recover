package com.jamesratzlaff.rawrecover.io;

import java.util.Map;
import java.util.Objects;

import com.jamesratzlaff.rawrecover.util.StringTransforms;

public class DiskGeometryImpl implements DiskGeometry {
	private final long bytesPerSector;
	private final long totalCylinders;
	private final long totalHeads;
	private final long totalSectors;
	private final long totalTracks;
	private final long tracksPerCylinder;

	public DiskGeometryImpl() {
		this(512);
	}

	public DiskGeometryImpl(long bytesPerSector) {
		this(bytesPerSector, -1, -1, -1, -1, -1);
	}

	public DiskGeometryImpl(String delimiterRegex, Iterable<String> keyValuePairs, String bytesPerSectorKey,
			String totalCylindersKey, String totalHeadsKey, String totalSectorsKey, String totalTracksKey,
			String tracksPerCylinderKey) {
		this(StringTransforms.transformKVPStringList(keyValuePairs, delimiterRegex), bytesPerSectorKey,
				totalCylindersKey, totalHeadsKey, totalSectorsKey, totalTracksKey, tracksPerCylinderKey);
	}

	public DiskGeometryImpl(Map<String,String> map, String bytesPerSectorKey, String totalCylindersKey, String totalHeadsKey, String totalSectorsKey, String totalTracksKey, String tracksPerCylinderKey) {
		this(getFromMapUsingKey(map,bytesPerSectorKey,"512"),getFromMapUsingKey(map,totalCylindersKey),getFromMapUsingKey(map,totalHeadsKey),getFromMapUsingKey(map,totalSectorsKey),getFromMapUsingKey(map,totalTracksKey),getFromMapUsingKey(map,tracksPerCylinderKey));
	}
	public DiskGeometryImpl(Map<String,String> map, String bytesPerSectorKey) {
		this(bytesPerSectorKey,null,null,null,null,null);
	}
	public DiskGeometryImpl(String bytesPerSector) {
		this(getIfNotNullOrBlank(bytesPerSector,512));
	}
	public DiskGeometryImpl(String bytesPerSector, String totalCylinders, String totalHeads, String totalSectors,
			String totalTracks, String tracksPerCylinder) {
		this(getIfNotNullOrBlank(bytesPerSector,512), getIfNotNullOrBlank(totalCylinders),getIfNotNullOrBlank(totalHeads),getIfNotNullOrBlank(totalSectors),getIfNotNullOrBlank(totalTracks),getIfNotNullOrBlank(tracksPerCylinder));
	}

	public DiskGeometryImpl(Iterable<String> keyValuePairs, String bytesPerSectorKey, String totalCylindersKey, String totalHeadsKey, String totalSectorsKey, String totalTracksKey, String tracksPerCylinderKey) {
		this(StringTransforms.transformKVPStringList(keyValuePairs),bytesPerSectorKey, totalCylindersKey, totalHeadsKey, totalSectorsKey, totalTracksKey, tracksPerCylinderKey);
	}

	public DiskGeometryImpl(long bytesPerSector, long totalCylinders, long totalHeads, long totalSectors,
			long totalTracks, long tracksPerCylinder) {
		super();
		this.bytesPerSector = bytesPerSector;
		this.totalCylinders = totalCylinders;
		this.totalHeads = totalHeads;
		this.totalSectors = totalSectors;
		this.totalTracks = totalTracks;
		this.tracksPerCylinder = tracksPerCylinder;
	}

	public long getBytesPerSector() {
		return bytesPerSector;
	}

	public long getTotalCylinders() {
		return totalCylinders;
	}

	public long getTotalHeads() {
		return totalHeads;
	}

	public long getTotalSectors() {
		return totalSectors;
	}

	public long getTotalTracks() {
		return totalTracks;
	}

	public long getTracksPerCylinder() {
		return tracksPerCylinder;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DiskGeometryImpl [bytesPerSector=");
		builder.append(bytesPerSector);
		builder.append(", totalCylinders=");
		builder.append(totalCylinders);
		builder.append(", totalHeads=");
		builder.append(totalHeads);
		builder.append(", totalSectors=");
		builder.append(totalSectors);
		builder.append(", totalTracks=");
		builder.append(totalTracks);
		builder.append(", tracksPerCylinder=");
		builder.append(tracksPerCylinder);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(bytesPerSector, totalCylinders, totalHeads, totalSectors, totalTracks, tracksPerCylinder);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof DiskGeometryImpl))
			return false;
		DiskGeometryImpl other = (DiskGeometryImpl) obj;
		return bytesPerSector == other.bytesPerSector && totalCylinders == other.totalCylinders
				&& totalHeads == other.totalHeads && totalSectors == other.totalSectors
				&& totalTracks == other.totalTracks && tracksPerCylinder == other.tracksPerCylinder;
	}
	private static <K,V> V getFromMapUsingKey(Map<K,V> map, K key, V defVal) {
		V reso=defVal;
		if(map!=null&&key!=null) {
			reso=map.get(key);
		}
		return reso;
		
	}
	private static <K,V> V getFromMapUsingKey(Map<K,V> map, K key) {
		return getFromMapUsingKey(map, key, null);
	}
	
	private static final long getIfNotNullOrBlank(String number) {
		return getIfNotNullOrBlank(number, -1);
	}

	private static final long getIfNotNullOrBlank(String number, long retVal) {
		long toRet = retVal;
		if (number != null) {
			number = number.trim();
			if (!number.isBlank()) {
				toRet = Long.parseUnsignedLong(number);
			}
		}
		return toRet;
	}
}

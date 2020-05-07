package com.jamesratzlaff.rawrecover.io;

import java.util.Map;
import java.util.Objects;

public class OSPhysicalDriveInfoImpl extends AbstractOSPhysicalDriveInfo {

	private final DiskGeometry geometry;

	public OSPhysicalDriveInfoImpl(Iterable<String> keyValuePairs, String pathKey, String sizeKey, String nameKey,
			String bytesPerSectorKey, String totalCylindersKey, String totalHeadsKey, String totalSectorsKey,
			String totalTracksKey, String tracksPerCylinderKey) {
		super(keyValuePairs, pathKey, sizeKey, nameKey);
		this.geometry = new DiskGeometryImpl(keyValuePairs, bytesPerSectorKey, totalCylindersKey, totalHeadsKey,
				totalSectorsKey, totalTracksKey, tracksPerCylinderKey);
		//
	}

	public OSPhysicalDriveInfoImpl(Iterable<String> keyValuePairs, String pathKey, String sizeKey, String nameKey) {
		this(keyValuePairs, pathKey, sizeKey, nameKey, null, null, null, null, null, null);
	}

	public OSPhysicalDriveInfoImpl(Map<String, String> map, String pathKey, String sizeKey, String nameKey) {
		this(map, pathKey, sizeKey, nameKey, null, null, null, null, null, null);

	}

	public OSPhysicalDriveInfoImpl(Map<String, String> map, String pathKey, String sizeKey, String nameKey,
			String bytesPerSectorKey, String totalCylindersKey, String totalHeadsKey, String totalSectorsKey,
			String totalTracksKey, String tracksPerCylinderKey) {
		super(map, pathKey, sizeKey, nameKey);
		this.geometry = new DiskGeometryImpl(map, bytesPerSectorKey, totalCylindersKey, totalHeadsKey, totalSectorsKey,
				totalTracksKey, tracksPerCylinderKey);

	}

	public OSPhysicalDriveInfoImpl(String delimiterRegex, Iterable<String> keyValuePairs, String pathKey,
			String sizeKey, String nameKey, String bytesPerSectorKey, String totalCylindersKey, String totalHeadsKey,
			String totalSectorsKey, String totalTracksKey, String tracksPerCylinderKey) {
		super(delimiterRegex, keyValuePairs, pathKey, sizeKey, nameKey);
		this.geometry = new DiskGeometryImpl(delimiterRegex, keyValuePairs, bytesPerSectorKey, totalCylindersKey,
				totalHeadsKey, totalSectorsKey, totalTracksKey, tracksPerCylinderKey);
	}

	public OSPhysicalDriveInfoImpl(String delimiterRegex, Iterable<String> keyValuePairs, String pathKey,
			String sizeKey, String nameKey) {
		this(delimiterRegex, keyValuePairs, pathKey, sizeKey, nameKey, null, null, null, null, null, null);
	}

	public OSPhysicalDriveInfoImpl(String drvPath, long size, String name, long bytesPerSector, long totalCylinders,
			long totalHeads, long totalSectors, long totalTracks, long tracksPerCylinder) {
		this(drvPath, size, name, new DiskGeometryImpl(bytesPerSector, totalCylinders, totalHeads, totalSectors,
				totalTracks, tracksPerCylinder));
	}

	public OSPhysicalDriveInfoImpl(String drvPath, long size, String name, DiskGeometry geometry) {
		super(drvPath, size, name);
		this.geometry = geometry == null ? new DiskGeometryImpl() : geometry;
	}

	public OSPhysicalDriveInfoImpl(String drvPath, String size, String name, DiskGeometry geometry) {
		super(drvPath, size, name);
		this.geometry = geometry == null ? new DiskGeometryImpl() : geometry;
	}

	public OSPhysicalDriveInfoImpl(String drvPath, String size, String name) {
		this(drvPath, size, name, (DiskGeometry) null);
	}

	public DiskGeometry getGeometry() {
		return this.geometry;
	}

	@Override
	public long getBytesPerSector() {
		DiskGeometry geom = getGeometry();
		if (geom != null) {
			return geom.getBytesPerSector();
		}
		return super.getBytesPerSector();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(geometry);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof OSPhysicalDriveInfoImpl)) {
			return false;
		}
		OSPhysicalDriveInfoImpl other = (OSPhysicalDriveInfoImpl) obj;
		return Objects.equals(geometry, other.geometry);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("OSPhysicalDriveInfoImpl [devicePath=");
		builder.append(getDevicePath());
		builder.append(", size=");
		builder.append(getSize());
		builder.append(", name()=");
		builder.append(getName());
		builder.append(", geometry=");
		builder.append(geometry);
		builder.append("]");
		return builder.toString();
	}

	

}

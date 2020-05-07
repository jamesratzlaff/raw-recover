/**
 *
 */
package com.jamesratzlaff.rawrecover.io;

import java.util.Map;
import java.util.Objects;

import com.jamesratzlaff.rawrecover.util.StringTransforms;

/**
 * @author
 *
 */
public abstract class AbstractOSPhysicalDriveInfo implements OSPhysicalDriveInfo {

	private final String drvPath;
	private final long size;
	private final String name;

	public AbstractOSPhysicalDriveInfo(String drvPath, long size, String name) {
		this.drvPath = drvPath;
		this.size = size;
		this.name = name;
	}

	public AbstractOSPhysicalDriveInfo(Iterable<String> keyValuePairs, String pathKey, String sizeKey, String nameKey) {
		this(StringTransforms.transformKVPStringList(keyValuePairs), pathKey, sizeKey, nameKey);
	}

	public AbstractOSPhysicalDriveInfo(String delimiterRegex, Iterable<String> keyValuePairs, String pathKey,
			String sizeKey, String nameKey) {
		this(StringTransforms.transformKVPStringList(keyValuePairs, delimiterRegex), pathKey, sizeKey, nameKey);
	}

	public AbstractOSPhysicalDriveInfo(Map<String, String> map, String pathKey, String sizeKey, String nameKey) {
		this(pathKey != null && map != null ? map.get(pathKey) : null,
				sizeKey != null && map != null ? map.get(sizeKey) : null,
				nameKey != null && map != null ? map.get(nameKey) : null);
	}

	public AbstractOSPhysicalDriveInfo(String drvPath, String size, String name) {
		this(drvPath.trim(), size != null && !size.isBlank() ? Long.parseUnsignedLong(size.trim()) : 0,
				name != null ? name.trim() : null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ratzlaff.james.io.OSPhysicalDriveInfo#getDevicePath()
	 */
	@Override
	public String getDevicePath() {
		return drvPath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ratzlaff.james.io.OSPhysicalDriveInfo#getSize()
	 */
	@Override
	public long getSize() {
		return size;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(drvPath, name, size);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof AbstractOSPhysicalDriveInfo)) {
			return false;
		}
		AbstractOSPhysicalDriveInfo other = (AbstractOSPhysicalDriveInfo) obj;
		return Objects.equals(drvPath, other.drvPath) && Objects.equals(name, other.name) && size == other.size;
	}

}

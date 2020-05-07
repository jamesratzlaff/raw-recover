/**
 *
 */
package com.jamesratzlaff.rawrecover.io;

/**
 * @author
 *
 */
public interface OSPhysicalDriveInfo extends HasBytesPerSector {

	String getDevicePath();
	long getSize();
	String getName();


}

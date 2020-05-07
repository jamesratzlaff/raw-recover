/**
 *
 */
package com.jamesratzlaff.rawrecover.io;

import java.util.Map;
import java.util.Set;

/**
 * @author
 *
 */
public interface DiskDriveInfo 	 {


	
	Set<String> getDevicePaths();
	
	Map<String, OSPhysicalDriveInfo> getDevices();
	
	OSPhysicalDriveInfo getByDevicePath(String devicePath);

}

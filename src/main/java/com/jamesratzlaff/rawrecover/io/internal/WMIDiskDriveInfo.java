/**
 *
 */
package com.jamesratzlaff.rawrecover.io.internal;

import static com.jamesratzlaff.rawrecover.io.internal.WMIDiskInfoFields.BytesPerSector;
import static com.jamesratzlaff.rawrecover.io.internal.WMIDiskInfoFields.Model;
import static com.jamesratzlaff.rawrecover.io.internal.WMIDiskInfoFields.Name;
import static com.jamesratzlaff.rawrecover.io.internal.WMIDiskInfoFields.Size;
import static com.jamesratzlaff.rawrecover.io.internal.WMIDiskInfoFields.TotalCylinders;
import static com.jamesratzlaff.rawrecover.io.internal.WMIDiskInfoFields.TotalHeads;
import static com.jamesratzlaff.rawrecover.io.internal.WMIDiskInfoFields.TotalSectors;
import static com.jamesratzlaff.rawrecover.io.internal.WMIDiskInfoFields.TotalTracks;
import static com.jamesratzlaff.rawrecover.io.internal.WMIDiskInfoFields.TracksPerCylinder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.jamesratzlaff.rawrecover.io.DiskDriveInfo;
import com.jamesratzlaff.rawrecover.io.OSPhysicalDriveInfo;
import com.jamesratzlaff.rawrecover.io.OSPhysicalDriveInfoImpl;
import com.jamesratzlaff.rawrecover.util.StringTransforms;

/**
 * @author
 *
 */
public class WMIDiskDriveInfo implements DiskDriveInfo {

	public static final String WMICOMMAND = "wmic";
	public static final List<String> args = Arrays.asList("diskdrive", "get");
	private Map<String, OSPhysicalDriveInfo> devices;

	private static final List<String> fieldsToUse = Arrays.asList(Name, Size, Model, BytesPerSector, TotalCylinders,
			TotalHeads, TotalSectors, TotalTracks, TracksPerCylinder).stream().map(WMIDiskInfoFields::name)
			.collect(Collectors.toList());

	private static void runCommand(WMIDiskInfoFields... fields) {
		runCommand(Arrays.asList(fields).stream().map(WMIDiskInfoFields::name).collect(Collectors.toList()));
	}

	private static ProcessOutputCapture runCommand(List<String> fields) {
		if (fields == null || fields.isEmpty()) {
			fields = new ArrayList<String>(fieldsToUse);
		}
		List<String> rgs = new ArrayList<String>(3);
		rgs.add(WMICOMMAND);
		rgs.addAll(args);
		rgs.add(String.join(",", fieldsToUse));
		ProcessBuilder pb = new ProcessBuilder(rgs);
		Process p = null;
		ProcessOutputCapture poc = null;

		try {
			p = pb.start();
			poc = new ProcessOutputCapture(p, true).withFilter(s -> !s.isBlank());
			p.waitFor();
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
		if (poc != null) {
			poc.close();
		}
		return poc;
	}

	private static OSPhysicalDriveInfo fromWMIMap(Map<String, String> strMap) {
		OSPhysicalDriveInfo reso = strMap != null
				? new OSPhysicalDriveInfoImpl(strMap, Name.name(), Size.name(), Model.name(), BytesPerSector.name(),
						TotalCylinders.name(), TotalHeads.name(), TotalSectors.name(), TotalTracks.name(),
						TracksPerCylinder.name())
				: null;
		return reso;
	}

	private static Map<String, OSPhysicalDriveInfo> toNamedPhysicalDeviceMap(List<Map<String, String>> strMaps) {
		Map<String, OSPhysicalDriveInfo> result = new HashMap<String, OSPhysicalDriveInfo>(strMaps.size());
		for (int i = 0; i < strMaps.size(); i++) {
			Map<String, String> currentMap = strMaps.get(i);
			String devName = WMIDiskInfoFields.Name.fromMap(currentMap);
			OSPhysicalDriveInfo driveInfo = fromWMIMap(currentMap);
			result.put(devName, driveInfo);
		}
		return result;
	}

	

	private static List<String> getDiskInformationCommandOutput() {
		ProcessOutputCapture poc = runCommand((List<String>) null);
		return poc.getOutLines();
	}

	private static List<Map<String, String>> getDeviceInfoStringMap() {
		List<String> diskInfoOutput = getDiskInformationCommandOutput();
		List<Map<String, String>> strMap = StringTransforms.transformSpaceDelimitedTableStrings(diskInfoOutput, null);
		return strMap;
	}

	private static Map<String, OSPhysicalDriveInfo> getDefaultDeviceInfo() {
		List<Map<String, String>> strMaps = getDeviceInfoStringMap();
		Map<String, OSPhysicalDriveInfo> result = toNamedPhysicalDeviceMap(strMaps);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ratzlaff.james.io.DiskDriveInfo#getByDevicePath(java.lang.String)
	 */
	@Override
	public OSPhysicalDriveInfo getByDevicePath(String devicePath) {
		OSPhysicalDriveInfo result = null;
		if (devicePath != null) {
			String devicePathToUse = devicePath.toUpperCase();
			Map<String, OSPhysicalDriveInfo> devices = getDevices();
			if (devices != null) {
				result = devices.get(devicePathToUse);
			}
		}
		return result;
	}

	@Override
	public Set<String> getDevicePaths() {
		Map<String, OSPhysicalDriveInfo> devices = getDevices();
		if (devices != null) {
			return devices.keySet();
		}
		return Collections.emptySet();
	}

	@Override
	public Map<String, OSPhysicalDriveInfo> getDevices() {
		if (devices == null) {
			devices = new HashMap<String, OSPhysicalDriveInfo>();
			Map<String, OSPhysicalDriveInfo> defMap = getDefaultDeviceInfo();
			if (defMap != null) {
				devices = defMap;
			}
		}
		return this.devices;
	}
	public static void main(String[] args) throws Exception {
		WMIDiskDriveInfo infos = new WMIDiskDriveInfo();
		infos.getDevices().entrySet().stream().forEach(System.out::println);
	}
}

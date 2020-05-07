package com.jamesratzlaff.rawrecover.io.internal;

import java.util.Map;

public enum WMIDiskInfoFields {
	
		Availability,
		BytesPerSector,
		Capabilities,
		CapabilityDescriptions,
		CompressionMethod,
		ConfigManagerErrorCode,
		ConfigManagerUserConfig,
		DefaultBlockSize,
		Description,
		DeviceID,
		ErrorCleared,
		ErrorDescription,
		ErrorMethodology,
		Index,
		InstallDate,
		InterfaceType,
		LastErrorCode,
		Manufacturer,
		MaxBlockSize,
		MaxMediaSize,
		MediaLoaded,
		MediaType,
		MinBlockSize,
		Model,
		Name,
		NeedsCleaning,
		NumberOfMediaSupported,
		PNPDeviceID,
		Partitions,
		PowerManagementCapabilities,
		PowerManagementSupported,
		SCSIBus,
		SCSILogicalUnit,
		SCSIPort,
		SCSITargetId,
		SectorsPerTrack,
		Signature,
		Size,
		Status,
		StatusInfo,
		SystemName,
		TotalCylinders,
		TotalHeads,
		TotalSectors,
		TotalTracks,
		TracksPerCylinder;
	
	
	public <V> V fromMap(Map<String,V> map) {
		V val = null;
		if(map!=null) {
			val=map.get(this.name());
		}
		return val;
	}
	
}

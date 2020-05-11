package com.jamesratzlaff.util.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.jamesratzlaff.rawrecover.RawDisk;
import com.jamesratzlaff.rawrecover.RawFileLocation;

public class FileDump {

	public static void dumpFilesOfType(RawDisk rd,List<RawFileLocation> locations, String typeName, String outputDir) throws Exception {
		Files.createDirectories(Paths.get(outputDir));
		locations.stream().filter(loc->typeName.equalsIgnoreCase(loc.getType())).forEach(loc->{
			long start = loc.getStart();
			long end = loc.getLength()>0?loc.getLength():start+4096;
			String filename = start+"."+typeName.toLowerCase();
			try {
				FileOutputStream fos = new FileOutputStream(Paths.get(outputDir, filename).toFile());
				rd.writeTo(fos, start, end);
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		});
	}
	
	
	
}

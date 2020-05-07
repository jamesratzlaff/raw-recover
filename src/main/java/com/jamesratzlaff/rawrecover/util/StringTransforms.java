/**
 *
 */
package com.jamesratzlaff.rawrecover.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author
 *
 */
public class StringTransforms {

	private static final Pattern spacerDelimited = Pattern.compile("(\\S+)\\s*");
	
	static class TextLocation implements Comparable<TextLocation> {
		private final int startIndex;
		private final int len;
		
		public TextLocation(int startIndex, int len)  {
			this.startIndex=startIndex;
			this.len=len;
		}

		public String toString(CharSequence cs) {
			String result = null;
			if(cs!=null) {
				if(cs.length()>startIndex) {
					int end = Math.min(cs.length(), startIndex+len);
					result = cs.subSequence(startIndex, end).toString();
				}
			}
			return result;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("TextLocation [startIndex=");
			builder.append(startIndex);
			builder.append(", len=");
			builder.append(len);
			builder.append("]");
			return builder.toString();
		}

		@Override
		public int hashCode() {
			return Objects.hash(len, startIndex);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof TextLocation)) {
				return false;
			}
			TextLocation other = (TextLocation) obj;
			return len == other.len && startIndex == other.startIndex;
		}
		
		private static Comparator<TextLocation> tlComp = (a,b)->{
			return Integer.compare(a.startIndex, b.startIndex);
		};
		
		public int compareTo(TextLocation other) {
			return tlComp.compare(this, other);
		}
		
		
		
	}
	
	
	public static Map<String,String> transformKVPStringList(Iterable<String> keyValuePairs){
		return transformKVPStringList(keyValuePairs, "=");
	}


	
	
	public static List<Map<String,String>> transformSpaceDelimitedTable(String results,Collection<String> headerOrdering){
		List<String> asLines = new ArrayList<String>(Arrays.asList(results.split("[\\r\\n]+")));
		return transformSpaceDelimitedTableStrings(asLines, headerOrdering);
		
		
	}
	
	public static List<Map<String,String>> transformSpaceDelimitedTableStrings(List<String> asLines, Collection<String> headerOrdering){
		String header = asLines.remove(0);
		Map<String,TextLocation> headerIndices = getHeaderIndices(header);
		List<Map<String,String>> reso = convertRowsIntoKeyValueMaps(headerIndices, asLines);
		return reso;
	}
	
	private static Map<String,TextLocation> getHeaderIndices(String headerString){
		Map<String,TextLocation> indices = new HashMap<String,TextLocation>();
		Matcher m = spacerDelimited.matcher(headerString);
		while(m.find()) {
			String name = m.group(1).trim();
			int index = m.start();
			indices.put(name, new TextLocation(index, m.end()-index));
		}
		return indices;
	}
	private static List<Map<String,String>> convertRowsIntoKeyValueMaps(Map<String,TextLocation> headerIndices, Iterable<String> rows){
		ArrayList<Map<String,String>> kvpMaps = new ArrayList<Map<String,String>>();
		rows.forEach(row->{
			Map<String,String> mapped = convertRowIntoKeyValueMap(headerIndices, row);
			kvpMaps.add(mapped);
		});
		kvpMaps.trimToSize();
		return kvpMaps;
	}
	private static Map<String,String> convertRowIntoKeyValueMap(Map<String,TextLocation> headerIndices, String row){
		List<String> asKVPStrings = convertRowIntoKeyValuePairs(headerIndices, row);
		Map<String,String> asMap = transformKVPStringList(asKVPStrings);
		return asMap;
	}
	
	private static List<String> convertRowIntoKeyValuePairs(Map<String, TextLocation> headerIndices, String row) {
		List<String> asStrings = new ArrayList<String>(headerIndices.size());
		for(String key : headerIndices.keySet()) {
			TextLocation location = headerIndices.get(key);
			StringBuilder kvp = new StringBuilder();
			kvp.append(key).append("=");
			String val = location.toString(row);
			kvp.append(val!=null?val.trim():null);
			asStrings.add(kvp.toString());
		}
		return asStrings;
	}
	
	
	
	

	@SuppressWarnings("rawtypes")
	public static Map<String,String> transformKVPStringList(Iterable<String> keyValuePairs, String delimiterRegexStr){
		String delimiterToUse = delimiterRegexStr==null?"=":delimiterRegexStr;
		Map<String,String> kvpMap = (keyValuePairs instanceof Collection)?new HashMap<String,String>(((Collection)keyValuePairs).size()):new HashMap<String,String>();
		keyValuePairs.forEach(kvpStr->{
			String[] kvpStrArr = kvpStr.split(delimiterToUse,2);
			String key=null;
			String value=null;
			if(kvpStrArr.length>0) {
				key=kvpStrArr[0].trim();
				if(kvpStrArr.length>1) {
					value=kvpStrArr[1].trim();
				}
			}
			kvpMap.put(key, value);
		});

		return kvpMap;
	}
	
	public static void main(String[] args) {
		String[] lines = { 
		"BytesPerSector  Model                                     Name                TotalCylinders  TotalHeads  TotalSectors  TotalTracks  TracksPerCylinder",
		"512             WDC WD20 03FZEX-00Z4SA0 SCSI Disk Device  \\\\.\\PHYSICALDRIVE2  243201          255         3907024065    62016255     255",
		"512             TOSHIBA MK8034GSX SCSI Disk Device        \\\\.\\PHYSICALDRIVE3  9729            255         156296385     2480895      255",
		"512             Samsung SSD 960 PRO 512GB                 \\\\.\\PHYSICALDRIVE1  62260           255         1000206900    15876300     255",
		"512             Samsung SSD 960 EVO 500GB                 \\\\.\\PHYSICALDRIVE0  60801           255         976768065     15504255     255"};
		String line = String.join("\n", lines);
		
		List<Map<String,String>> transformed = transformSpaceDelimitedTable(line, null);
		transformed.forEach(System.out::println);
	}

}

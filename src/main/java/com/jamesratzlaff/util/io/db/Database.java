package com.jamesratzlaff.util.io.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.jamesratzlaff.rawrecover.PredicateMaker;
import com.jamesratzlaff.rawrecover.RawFileLocation;
import com.jamesratzlaff.rawrecover.ReadErrorRange;
import com.jamesratzlaff.rawrecover.SimpleRawFileLocation;
import com.jamesratzlaff.rawrecover.ZeroedOutOffsetRange;

public class Database {

	private Connection conn;

	public Database() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			this.close();
		}));
	}

	private Connection getConnection() {
		try {
			if (this.conn == null || conn.isClosed()) {
				try {

					conn = DriverManager.getConnection("jdbc:h2:~/.rawrecover/rawrecover3", "rawrecover", "recoverme!");

				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this.conn;
	}

	public List<SimpleRawFileLocation> getRawOffsets() {
		List<SimpleRawFileLocation> list = new ArrayList<SimpleRawFileLocation>();
		Map<Long, String> urls = getURLs();
		try {
			String n8tive = getConnection().nativeSQL("SELECT OFFSETS.START_OFFSET AS startOffset, OFFSETS.END_OFFSET AS endOffset, OFFSETS.ENDS_IN_PADDED_CLUSTER AS endsInPaddedCluster, TYPES.NAME AS TYPE FROM OFFSETS, PUBLIC.TYPES WHERE TYPES.ID = OFFSETS.TYPE_ID ORDER BY startOffset");
			Statement s = getConnection().createStatement();
			
			ResultSet rs = s.executeQuery(n8tive);
			while (rs.next()) {
				long startOffset = rs.getLong("startOffset");
				
				SimpleRawFileLocation loc = new SimpleRawFileLocation(rs.getString("type"), startOffset,
						rs.getLong("endOffset"), rs.getBoolean("endsInPaddedCluster"));
				String url = urls.get(startOffset);
				if (url != null) {
					loc.setMozillaCacheUrl(url);
				}
				list.add(loc);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}

	public int[] mergeZeroedOffsets(List<ZeroedOutOffsetRange> values) {
		int[] result = null;

		PreparedStatement ps;
		try {
			ps = getConnection().prepareStatement("MERGE INTO ZEROED_OFFSET_RANGES KEY(START_OFFSET) VALUES(?,?)");

			for (int i = 0; i < values.size(); i++) {
				ZeroedOutOffsetRange rfl = values.get(i);
				ps.setLong(1, rfl.getStart());
				ps.setLong(2, rfl.getLength());
				ps.addBatch();
			}
			result = ps.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public List<ZeroedOutOffsetRange> getZeroedOutOffsetRanges(){
		List<ZeroedOutOffsetRange> list = new ArrayList<ZeroedOutOffsetRange>();
		try {
			Statement s = getConnection().createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM ZEROED_OFFSET_RANGES ORDER BY START_OFFSET");
			while(rs.next()) {
				long start = rs.getLong("START_OFFSET");
				long len = rs.getLong("LENGTH");
				list.add(new ZeroedOutOffsetRange(start, len));
			}
			
		}catch(SQLException e) {
			e.printStackTrace();
		}
		return list;
	}

//	private void getMozCacheUrls(List<RawFileLocation> offsets) {
//		Map<Long, String> urls=new ConcurrentHashMap<Long,String>();
//		try {
//			ResultSet rs = getConnection().createStatement().executeQuery(
//					"SELECT OFFSETS.START_OFFSET AS startOffset, OFFSETS.END_OFFSET AS endOffset, OFFSETS.ENDS_IN_PADDED_CLUSTER AS endsInPaddedCluster,  TYPES.NAME AS type, FROM OFFSETS, TYPES WHERE TYPES.ID = OFFSETS.TYPE_ID");//"SELECT OFFSETS.START_OFFSET AS startOffset, OFFSETS.END_OFFSET AS endOffset, OFFSETS.ENDS_IN_PADDED_CLUSTER AS endsInPaddedCluster,  TYPES.NAME AS type,MOZ_CACHE_URLS.URL AS URL, FROM OFFSETS, TYPES,MOZ_CACHE_URLS,MOZ_CACHE_ASSOC  WHERE TYPES.ID = OFFSETS.TYPE_ID");// AND MOZ_CACHE_ASSOC.OFFSET_ID = OFFSETS.START_OFFSET  AND MOZ_CACHE_ASSOC.MOZ_CACHE_URL_ID = MOZ_CACHE_URLS.ID ORDER BY startOffset ASC");
//			while (rs.next()) {
//				SimpleRawFileLocation loc = new SimpleRawFileLocation(rs.getString("type"), rs.getLong("startOffset"),
//						rs.getLong("endOffset"), rs.getBoolean("endsInPaddedCluster"));
//				list.add(loc);
//			}
//	}

	private Map<Long, String> getURLs() {
		Map<Long, String> reso = new HashMap<Long, String>();
		try {
			ResultSet rs = getConnection().createStatement().executeQuery(
					"SELECT MOZ_CACHE_ASSOC.OFFSET_ID AS OFFSET_ID,MOZ_CACHE_URLS.URL AS url FROM MOZ_CACHE_URLS,MOZ_CACHE_ASSOC WHERE MOZ_CACHE_URLS.ID = MOZ_CACHE_ASSOC.MOZ_CACHE_URL_ID");
			while (rs.next()) {

				reso.put(rs.getLong("OFFSET_ID"), rs.getString("url"));

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return reso;
	}

	public void resetAll(List<RawFileLocation> offsets) {
		offsets.forEach(offset -> offset.setEnd(-1));
		merge(offsets);
	}

	public static Predicate<RawFileLocation> whereTypesEqual(String... types) {
		Predicate<RawFileLocation> typePredicate = (p) -> true;
		for (int i = 0; i < types.length; i++) {
			String type = types[i];
			if (i == 0) {
				typePredicate = typePredicate.and(p -> type.equalsIgnoreCase(p.getType()));
			} else {
				typePredicate = typePredicate.or(p -> type.equalsIgnoreCase(p.getType()));
			}
		}
		return typePredicate;
	}

	public int[] mergeReadErrors(List<ReadErrorRange> values) {

		int[] result = null;

		PreparedStatement ps;
		try {
			ps = getConnection().prepareStatement("MERGE INTO READ_ERROR_RANGES KEY(START_OFFSET) VALUES(?,?)");

			for (int i = 0; i < values.size(); i++) {
				ReadErrorRange rfl = values.get(i);
				ps.setLong(1, rfl.getStart());
				ps.setLong(2, rfl.getLength());
				ps.addBatch();
			}
			result = ps.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		mergeReadErrorAssociations(values);
		return result;
	}

	public int[] mergeReadErrorAssociations(List<ReadErrorRange> values) {

		mergeErrorMessages(values.stream().map(value -> value.getMessage()).distinct().collect(Collectors.toList()));

		int[] result = null;
		PreparedStatement ps;
		try {

			ps = getConnection().prepareStatement(
					"MERGE INTO READ_ERROR_RANGES_MESSAGES KEY(READ_ERROR_RANGES_ID) VALUES(?,(SELECT ID FROM READ_ERROR_MESSAGES WHERE MESSAGE=?))");
			for (int i = 0; i < values.size(); i++) {
				ReadErrorRange rfl = values.get(i);
				ps.setString(2, rfl.getMessage());
				ps.setLong(1, rfl.getStart());
				ps.addBatch();
			}
			result = ps.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;

	}

	public List<ReadErrorRange> getReadErrorRanges() {
		String query = PredicateMaker.config.getString("queries.read-error-ranges");
		ResultSet rs = null;
		ArrayList<ReadErrorRange> reso = new ArrayList<ReadErrorRange>();
		try {
			rs = getConnection().createStatement().executeQuery(query);
			while (rs.next()) {
				long start = rs.getLong("START");
				long len = rs.getLong("LEN");
				String msg = rs.getString("MESSAGE");
				ReadErrorRange rer = new ReadErrorRange(start, len, msg);
				reso.add(rer);
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		reso.trimToSize();
		return reso;
		// "SELECT READ_ERROR_RANGES.START_OFFSET AS START, READ_ERROR_RANGES.LENGTH AS
		// LEN, READ_ERROR_MESSAGES.MESSAGE AS MESSAGE FROM READ_ERROR_RANGES,
		// READ_ERROR_MESSAGES, READ_ERROR_RANGES_MESSAGES WHERE
		// READ_ERROR_RANGES_MESSAGES.READ_ERROR_RANGES_ID =
		// READ_ERROR_RANGES.START_OFFSET AND
		// READ_ERROR_RANGES_MESSAGES.READ_ERROR_MESSAGE_ID=READ_ERROR_MESSAGES.ID ORDER
		// BY START ASC")
	}

	private int[] mergeErrorMessages(List<String> values) {
		int[] result = null;
		PreparedStatement ps;
		try {
			ps = getConnection().prepareStatement(
					"MERGE INTO READ_ERROR_MESSAGES KEY(ID) VALUES((SELECT ID FROM READ_ERROR_MESSAGES WHERE MESSAGE=?),?)");

			for (int i = 0; i < values.size(); i++) {
				String rfl = values.get(i);
				ps.setString(1, rfl);
				ps.setString(2, rfl);
				ps.addBatch();
			}
			result = ps.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;

	}

	private int[] mergeURLS(List<String> urls) {
		int[] result = null;
		PreparedStatement ps;
		try {
			ps = getConnection().prepareStatement(
					"MERGE INTO MOZ_CACHE_URLS KEY(ID) VALUES((SELECT ID FROM MOZ_CACHE_URLS WHERE URL=?),?)");

			for (int i = 0; i < urls.size(); i++) {
				String rfl = urls.get(i);
				ps.setString(1, rfl);
				ps.setString(2, rfl);
				ps.addBatch();
			}
			result = ps.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;

	}

	public int[] mergeURLAssociations(List<RawFileLocation> locations) {
		List<RawFileLocation> offsets = locations.stream().filter(location -> location.getMozillaCacheUrl() != null)
				.collect(Collectors.toList());
		mergeURLS(offsets.stream().map(location -> location.getMozillaCacheUrl()).collect(Collectors.toList()));
		int[] result = null;
		PreparedStatement ps;
		try {

			ps = getConnection().prepareStatement(
					"MERGE INTO MOZ_CACHE_ASSOC KEY(OFFSET_ID) VALUES(?,(SELECT ID FROM MOZ_CACHE_URLS WHERE URL=?))");
			for (int i = 0; i < offsets.size(); i++) {
				RawFileLocation rfl = offsets.get(i);
				ps.setString(2, rfl.getMozillaCacheUrl());
				ps.setLong(1, rfl.getStart());
				ps.addBatch();
			}
			result = ps.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;

	}

//SELECT OFFSETS.START_OFFSET AS startOffset, OFFSETS.END_OFFSET AS endOffset, TYPES.NAME AS type, (OFFSETS.END_OFFSET-OFFSETS.START_OFFSET) AS SIZE FROM OFFSETS, TYPES WHERE TYPES.ID = OFFSETS.TYPE_ID AND OFFSETS.END_OFFSET>OFFSETS.START_OFFSET ORDER BY SIZE DESC
	public int[] merge(List<RawFileLocation> offsets) {
		int[] result = null;
		mergeTypesFromLocations(offsets);
		PreparedStatement ps;
		try {
			ps = getConnection().prepareStatement(
					"MERGE INTO OFFSETS KEY(START_OFFSET) VALUES(?,?,(SELECT ID FROM TYPES WHERE NAME=?),?)");

			for (int i = 0; i < offsets.size(); i++) {
				RawFileLocation rfl = offsets.get(i);
				ps.setLong(1, rfl.getStart());
				ps.setLong(2, rfl.getEnd());
				ps.setString(3, rfl.getType());
				ps.setBoolean(4, rfl.endsInPaddedCluster());
				ps.addBatch();
			}
			result = ps.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		mergeURLAssociations(offsets);
		return result;
	}

	public void close() {
		if (this.conn != null) {
			try {
				if (!this.conn.isClosed()) {
					this.conn.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private int[] mergeTypesFromLocations(List<RawFileLocation> values) {
		return mergeTypes(values.stream().map(value -> value.getType()).distinct().collect(Collectors.toList()));
	}

	public int[] mergeTypes(List<String> values) {
		int[] result = null;

		PreparedStatement ps;
		try {
			ps = getConnection()
					.prepareStatement("MERGE INTO TYPES KEY(ID) VALUES((SELECT ID FROM TYPES WHERE NAME=?),?)");

			for (int i = 0; i < values.size(); i++) {
				String str = values.get(i);
				ps.setString(1, str);
				ps.setString(2, str);
				ps.addBatch();
			}
			result = ps.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static void main(String[] args) throws Exception {
		Database db = new Database();
		List<SimpleRawFileLocation> locations = db.getRawOffsets();
		SimpleRawFileLocation srfl = new SimpleRawFileLocation("ASF", 0, -1);
		srfl.setMozillaCacheUrl("https://example.com/?a.jpg");
		db.merge(Arrays.asList(srfl));
//		locations.get(0).setEndOffset(4);
//		int[] reso = db.merge(locations);
//		System.out.println(Arrays.toString(reso));
//		System.out.println(locations.size());
	}

}

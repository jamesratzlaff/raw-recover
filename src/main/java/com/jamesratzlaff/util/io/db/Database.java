package com.jamesratzlaff.util.io.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.jamesratzlaff.rawrecover.RawFileLocation;
import com.jamesratzlaff.rawrecover.SimpleRawFileLocation;

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

					conn = DriverManager.getConnection("jdbc:h2:~/.rawrecover/rawrecover2", "rawrecover", "recoverme!");

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

	public List<RawFileLocation> getRawOffsets() {
		List<RawFileLocation> list = new ArrayList<RawFileLocation>();
		try {
			ResultSet rs = getConnection().createStatement().executeQuery(
					"SELECT OFFSETS.START_OFFSET AS startOffset, OFFSETS.END_OFFSET AS endOffset, OFFSETS.ENDS_IN_PADDED_CLUSTER AS endsInPaddedCluster, TYPES.NAME AS type FROM OFFSETS, TYPES WHERE TYPES.ID = OFFSETS.TYPE_ID ORDER BY startOffset ASC");
			while (rs.next()) {
				SimpleRawFileLocation loc = new SimpleRawFileLocation(rs.getString("type"), rs.getLong("startOffset"),
						rs.getLong("endOffset"), rs.getBoolean("endsInPaddedCluster"));
				list.add(loc);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}
	
	public void resetAll(List<RawFileLocation> offsets) {
		offsets.forEach(offset->offset.setEndOffset(-1));
		merge(offsets);
	}
	
	public static Predicate<RawFileLocation> whereTypesEqual(String...types){
		Predicate<RawFileLocation> typePredicate = (p)->true;
		for(int i=0;i<types.length;i++) {
			String type = types[i];
			if(i==0) {
				typePredicate=typePredicate.and(p->type.equalsIgnoreCase(p.getType()));
			} else {
				typePredicate=typePredicate.or(p->type.equalsIgnoreCase(p.getType()));
			}
		}
		return typePredicate;
	}
	
	
	
	
	
	
//SELECT OFFSETS.START_OFFSET AS startOffset, OFFSETS.END_OFFSET AS endOffset, TYPES.NAME AS type, (OFFSETS.END_OFFSET-OFFSETS.START_OFFSET) AS SIZE FROM OFFSETS, TYPES WHERE TYPES.ID = OFFSETS.TYPE_ID AND OFFSETS.END_OFFSET>OFFSETS.START_OFFSET ORDER BY SIZE DESC
	public int[] merge(List<RawFileLocation> offsets) {
		int[] result = null;
		PreparedStatement ps;
		try {
			ps = getConnection().prepareStatement(
					"MERGE INTO OFFSETS KEY(START_OFFSET) VALUES(?,?,(SELECT ID FROM TYPES WHERE NAME=?),?)");

			for (int i = 0; i < offsets.size(); i++) {
				RawFileLocation rfl = offsets.get(i);
				ps.setLong(1, rfl.getStartOffset());
				ps.setLong(2, rfl.getEndOffset());
				ps.setString(3, rfl.getType());
				ps.setBoolean(4, rfl.endsInPaddedCluster());
				ps.addBatch();
			}
			result = ps.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
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

	
	
	public static void main(String[] args) throws Exception {
		Database db = new Database();
		List<RawFileLocation> locations = db.getRawOffsets();
		locations.stream().filter(whereTypesEqual("SQLITE")).forEach(System.out::println);;
//		locations.get(0).setEndOffset(4);
//		int[] reso = db.merge(locations);
//		System.out.println(Arrays.toString(reso));
//		System.out.println(locations.size());
	}

}

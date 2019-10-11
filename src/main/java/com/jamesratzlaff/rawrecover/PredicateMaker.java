package com.jamesratzlaff.rawrecover;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.jamesratzlaff.util.function.ObjIntBiPredicate;
import com.jamesratzlaff.util.io.ByteBufferPredicates;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class PredicateMaker {

	private static final Map<String, byte[]> data = new LinkedHashMap<String, byte[]>();
	private static final Map<String, ObjIntBiPredicate<ByteBuffer>> biPredicates = new LinkedHashMap<String, ObjIntBiPredicate<ByteBuffer>>();
	private static final Map<String, Predicate<ByteBuffer>> predicates = new LinkedHashMap<String, Predicate<ByteBuffer>>();
	public static final Config config = ConfigFactory.load();
	
	public static byte[] getDataBytes(String dataKey) {
		byte[] dataBytes = data.get(dataKey);
		if (dataBytes == null) {
			Config byteStrs = config.getConfig("data");
			Config dataformats = config.getConfig("data-format");
			String strData = byteStrs.getString(dataKey);
			if (strData != null) {
				if (dataformats.hasPath(dataKey)) {
					String charSetName = dataformats.getString(dataKey);
					try {
						dataBytes = strData.getBytes(charSetName);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
				if (dataBytes == null) {
					dataBytes = toByteArray(strData);
				}
				if (dataBytes != null) {
					data.put(dataKey, dataBytes);
				}
			}
		}
		return dataBytes;
	}

	private static String[] toPairs(String str) {
		String[] reso = null;
		if (str != null) {
			str = str.replaceAll("\\s+", "");
			if ((str.length() & 1) == 1) {
				str = "0" + str;
			}
			str = str.replaceAll("([\\S]{2})", "$1 ");
			reso = str.split("[\\s]+");

		}
		return reso;
	}

	public static byte[] toByteArray(String str) {
		byte[] reso = null;
		String[] strs = toPairs(str);
		if (strs != null) {
			reso = new byte[strs.length];
			for (int i = 0; i < strs.length; i++) {
				byte asByte = 0;
				String current = strs[i];
				try {
					asByte = (byte) Integer.parseInt(current, 16);
				} catch (NumberFormatException nfe) {
					nfe.printStackTrace();
				}
				reso[i] = asByte;
			}
		}
		return reso;
	}

//	public static class BufferPredicate implements ObjIntBiPredicate<ByteBuffer>, Predicate<ByteBuffer>, Serializable{
//
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = -28215673846115834L;
//		private final int offset;
//		private final BufferPredicate AND_OR;
//		private final byte[] data;
//		
//		
//		
//		@Override
//		public boolean test(ByteBuffer t) {
//			// TODO Auto-generated method stub
//			return false;
//		}
//
//		@Override
//		public boolean test(ByteBuffer t, int u) {
//			// TODO Auto-generated method stub
//			return false;
//		}
//		
//	}

	public static Predicate<ByteBuffer> getPredicate(String key) {
		Predicate<ByteBuffer> result = predicates.get(key);
		if (result == null) {
			getBiPredicate(key);
			result = predicates.get(key);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static ObjIntBiPredicate<ByteBuffer> getBiPredicate(String key) {
		ObjIntBiPredicate<ByteBuffer> result = biPredicates.get(key);
		if (result == null) {
			Config preds = config.getConfig("predicates");
			if (preds.hasPath(key)) {
				Config pred = preds.getConfig(key);
				Object data = pred.getAnyRef("data");
				int offset = pred.hasPath("offset") ? pred.getInt("offset") : 0;

				List<String> datas = new ArrayList<String>(1);
				if (data instanceof String) {
					datas.add((String) data);
				} else if (data instanceof List) {
					datas = new ArrayList<String>((List<String>) data);
				}
				ObjIntBiPredicate<ByteBuffer> andThen = null;
				if (pred.hasPath("then")) {
					List<String> thenNames = new ArrayList<String>(1);
					Object thenNameObj = pred.getAnyRef("then");
					if (thenNameObj instanceof String) {
						thenNames = (List<String>) thenNameObj;
					}
					for (int i = 0; i < thenNames.size(); i++) {
						String thenName = thenNames.get(i);
						ObjIntBiPredicate<ByteBuffer> thn = getBiPredicate(thenName);
						if (thn != null) {
							if (andThen == null) {
								andThen = thn;
							} else {
								andThen = andThen.or(thn);
							}
						}
					}
				}
				for (int i = 0; i < datas.size(); i++) {
					String dataname = datas.get(i);
					byte[] bytes = getDataBytes(dataname);
					if (bytes != null) {
						if (result == null) {
							result = (bb, oset) -> {
								boolean truth = ByteBufferPredicates.byteBufferMatches(oset + offset, bb, bytes);
								return truth;
							};
						} else {
							result = result.or((bb, oset) -> {
								boolean truth = ByteBufferPredicates.byteBufferMatches(oset + offset, bb, bytes);
								return truth;
							});
						}
					}
					if (andThen != null) {
						if (result == null) {
							result = andThen;
						} else {
							result = result.and(andThen);
						}
					}

				}
			}
			if (result != null) {
				ObjIntBiPredicate<ByteBuffer> asFinVar = result;
				biPredicates.put(key, asFinVar);
				predicates.put(key, (b) -> {
					return asFinVar.test(b, 0);
				});
				result = asFinVar;
			}
		}
		return result;
	}

	public static void main(String[] args) {
		ByteBuffer bb = ByteBuffer
				.wrap(new byte[] { (byte) 137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82 });
		ObjIntBiPredicate<ByteBuffer> pred1 = getBiPredicate("PNG");
		ObjIntBiPredicate<ByteBuffer> pred2 = getBiPredicate("MP4");
		ObjIntBiPredicate<ByteBuffer> pred3 = getBiPredicate("JPEG");
		System.out.println(pred3.test(bb, 0));
		System.out.println(pred2.test(bb, 0));
		System.out.println(pred1.test(bb, 0));
		System.out.println(pred1.test(bb, 1));
	}

}

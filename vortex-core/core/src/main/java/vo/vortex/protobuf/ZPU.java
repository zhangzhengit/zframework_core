//package vo.zframework.protobuf;
//
//import java.util.Arrays;
//import java.util.Map;
//import java.util.Objects;
//import java.util.concurrent.ConcurrentHashMap;
//
//import io.protostuff.LinkedBuffer;
//import io.protostuff.ProtobufIOUtil;
//import io.protostuff.Schema;
//import io.protostuff.runtime.RuntimeSchema;
//
///**
// * Protobuf工具
// *
// * @author zhangzhen
// * @data Jul 8, 2020
// *
// */
//@SuppressWarnings("unchecked")
//public class ZPU {
//
//	public static final int L_LENGTH = 4;
//	private static Map<Class<?>, Schema<?>> schemaCache = new ConcurrentHashMap<>();
//
//	public static <T> byte[] serialize(final T t) {
//		final Class<T> cls = (Class<T>) t.getClass();
//		final Schema<T> s = getSchema(cls);
//		final byte[] bs = io.protostuff.ProtobufIOUtil.toByteArray(t, s, LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
//		return bs;
//	}
//
//	public static <T> T deserialize(final byte[] bs, final Class<T> cls) {
//		final Schema<T> s = getSchema(cls);
//		final T t = s.newMessage();
//		ProtobufIOUtil.mergeFrom(bs, t, s);
//		return t;
//	}
//
//	public static <T> T deserialize(final byte[] data, final int f, final int t, final Class<T> clazz) {
//		final Schema<T> schema = getSchema(clazz);
//		final T obj = schema.newMessage();
//		final byte[] copyOfRange = Arrays.copyOfRange(data, f, t);
//		ProtobufIOUtil.mergeFrom(copyOfRange, obj, schema);
//		return obj;
//	}
//
//	private static <T> Schema<T> getSchema(final Class<T> clazz) {
//		Schema<T> schema = (Schema<T>) schemaCache.get(clazz);
//		if (Objects.isNull(schema)) {
//			schema = RuntimeSchema.getSchema(clazz);
//			if (Objects.nonNull(schema)) {
//				schemaCache.put(clazz, schema);
//			}
//		}
//
//		return schema;
//	}
//
//	public static byte[] toZPbs(final Object object) {
//		final byte[] serialize = serialize(object);
//		final byte[] wanzhangbytearray = wanzhangbytearray(serialize);
//		return wanzhangbytearray;
//	}
//
//	public static byte[] wanzhangbytearray(final byte[] bs) {
//		if (bs == null) {
//			return new byte[4];
//		}
//		final int length = bs.length;
//		final byte[] copyOf = Arrays.copyOf(bs, length + L_LENGTH);
//
//		final byte[] lba = intToByteArray(length);
//		copyOf[0] = lba[0];
//		copyOf[1] = lba[1];
//		copyOf[2] = lba[2];
//		copyOf[3] = lba[3];
//
//		for (int i = L_LENGTH; i < copyOf.length; i++) {
//			copyOf[i] = bs[i - L_LENGTH];
//		}
//
//		return copyOf;
//	}
//
//	public static byte[] intToByteArray(final int i) {
//		final byte[] b = new byte[4];
//		b[0] = (byte) (i >> 24 & 0xFF);
//		b[1] = (byte) (i >> 16 & 0xFF);
//		b[2] = (byte) (i >> 8 & 0xFF);
//		b[3] = (byte) (i & 0xFF);
//		return b;
//	}
//
//	public static int byteArrayToInt(final byte[] bs) {
//		int b = 0;
//		for (int i = 0; i < 4; i++) {
//			final int s = (4 - 1 - i) * 8;
//			b += (bs[i] & 0x000000FF) << s;
//		}
//		return b;
//	}
//
//
//}
//package vo;
//package vo;





package vo.vortex.core;

/**
 * 读取http请求时的请求行的 METHOD 结果
 *
 * @author zhangzhen
 * @date 2024年12月20日 下午3:40:38
 *
 */
public class MR {

	private int readLength;
	private String methodName;

	private byte[] array;

	public MR(final int readLength, final String methodName, final byte[] array) {
		this.readLength = readLength;
		this.methodName = methodName;
		this.array = array;
	}

	public int getReadLength() {
		return this.readLength;
	}

	public void setReadLength(final int readLength) {
		this.readLength = readLength;
	}

	public String getMethodName() {
		return this.methodName;
	}

	public void setMethodName(final String methodName) {
		this.methodName = methodName;
	}

	public byte[] getArray() {
		return this.array;
	}

	public void setArray(final byte[] array) {
		this.array = array;
	}

}

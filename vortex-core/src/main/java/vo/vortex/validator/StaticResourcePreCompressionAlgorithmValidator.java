package vo.vortex.validator;

import java.lang.reflect.Field;
import java.util.Set;

import vo.vortex.common.CU;
import vo.vortex.common.RU;
import vo.vortex.enums.AcceptEncodingEnum;
import vo.vortex.exception.AlgorithmNotSupportedException;

/**
 *
 * 静态文件预压缩算法配置项校验
 *
 * @author zhangzhen
 * @date 2026年6月26日 20:42:37
 */
public class StaticResourcePreCompressionAlgorithmValidator implements ZCustomValidator {

	@Override
	public void validated(final Object object, final Field field) throws Exception {
		final Object value = RU.getFiledValue(object, field);

		if (value == null) {
			return;
		}

		 final Set<String> sae = CU.ofSet(AcceptEncodingEnum.BR.getValue(), AcceptEncodingEnum.ZSTD.getValue(),
					AcceptEncodingEnum.GZIP.getValue(), AcceptEncodingEnum.DEFLATE.getValue());

		final Set<String> set = (Set<String>) value;

		final boolean allMatch = set.stream().allMatch(ae -> sae.contains(ae));
		if (!allMatch) {
			throw new AlgorithmNotSupportedException(set + "，支持的选项为：" + sae);
		}

	}

}

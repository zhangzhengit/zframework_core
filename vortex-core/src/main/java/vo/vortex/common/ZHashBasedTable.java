package vo.vortex.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <行, 列, 值> 结构
 *
 * @param <R>
 * @param <C>
 * @param <V>
 *
 * @author zhangzhen
 * @date 2026年7月4日 05:41:52
 */
public class ZHashBasedTable<R, C, V> {

	private final Map<R, Map<C, V>> rows = new HashMap<>(4, 1F);

	public V get(final R rowKey, final C columnKey) {
		final Map<C, V> row = this.rows.get(rowKey);
		return row == null ? null : row.get(columnKey);
	}

	public void put(final R rowKey, final C columnKey, final V value) {
		this.rows.computeIfAbsent(rowKey, k -> new HashMap<>(4, 1F)).put(columnKey, value);
	}

	public Map<C, V> row(final R rowKey) {
		return this.rows.getOrDefault(rowKey, Collections.emptyMap());
	}

	public Set<R> rowKeySet() {
		return this.rows.keySet();
	}

}

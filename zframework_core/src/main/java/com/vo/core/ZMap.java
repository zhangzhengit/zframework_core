package com.vo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.hutool.core.collection.CollUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 带最大容量并且自动淘汰旧值的Map实现，最大容量由 public ZMap(final int groups, final int numberOfGroup)  两个参数相乘而得出，
 * 如果put时个数已超过最大容量，则丢弃旧的然后放入本次put新增的。
 *
 * 注意：丢弃旧的是按照组进行的，不是按照所有的数据进行的，分组的key按key.hashCode来取特征，
 * 所以即使放进来的key全都不重复并且也没到到最大容量，也有可能size方法返回值小于放进来的总数。
 *
 * 默认最大容量为  DEFAULT_GROUPS * DEFAULT_NUMBER_OF_GROUP
 *
 * @author zhangzhen
 * @date 2023年11月26日
 *
 */
public class ZMap<K, V> implements Map<K, V> {

	/**
	 * 默认最大组数
	 */
	public static final int DEFAULT_GROUPS = 10000;

	/**
	 * 默认每组存放的最大个数
	 */
	public static final int DEFAULT_NUMBER_OF_GROUP = 100;

	/**
	 * 分组数，本类实例化时就初始化的组数
	 */
	private final int groups;

	/**
	 * 每组存放的最大个数，每个组的最大容量，如果此组个数超过此值，则丢弃最旧的数据放入本次新增的
	 */
	private final int numberOfGroup;

	private final ArrayList<LinkedList<ZNode>> data;

	public ZMap() {
		this.groups = DEFAULT_GROUPS;
		this.numberOfGroup = DEFAULT_NUMBER_OF_GROUP;
		this.data = new ArrayList<>(this.groups);
		this.initData();
	}

	public ZMap(final int groups, final int numberOfGroup) {
		this.groups = groups <= 0 ? DEFAULT_GROUPS : groups;
		this.numberOfGroup = numberOfGroup <= 0 ? DEFAULT_NUMBER_OF_GROUP : numberOfGroup;
		this.data = new ArrayList<>(this.groups);
		this.initData();
	}

	private synchronized void initData() {
		for (int i = 0; i < this.groups; i++) {
			this.data.add(new LinkedList<>());
		}
	}

	@Override
	public synchronized int size() {
		int c = 0;
		for (int i = 0; i < this.data.size(); i++) {
			c += this.data.get(i).size();
		}
		return c;
	}

	@Override
	public synchronized boolean isEmpty() {
		if (this.data.isEmpty()) {
			return true;
		}

		for (int i = 0; i < this.data.size(); i++) {
			final int size = this.data.get(i).size();
			if (size > 0) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean containsKey(final Object key) {

		// 不用 get(key)，因为 V 可能为null，无法判断

		if (this.data.isEmpty()) {
			return false;
		}

		final int n = this.hash(key);

		final String keyword = lock(n);
		synchronized (keyword.intern()) {

			final LinkedList<ZNode> nl = this.data.get(n);
			if (CollUtil.isEmpty(nl)) {
				return false;
			}

			for (final ZNode zNode : nl) {
				if (Objects.equals(zNode.getK(), key)) {
					return true;
				}
			}

			return false;
		}

	}

	@Override
	public synchronized boolean containsValue(final Object value) {

		for (int i = 0; i < this.data.size(); i++) {
			final LinkedList<ZNode> nl = this.data.get(i);
			for (final ZNode zNode : nl) {
				if (Objects.equals(zNode.getV(), value)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public V get(final Object key) {

		if (this.data.isEmpty()) {
			return null;
		}

		final int n = this.hash(key);

		final String keyword = lock(n);
		synchronized (keyword.intern()) {

			final LinkedList<ZNode> nl = this.data.get(n);

			if (nl.isEmpty()) {
				return null;
			}

			for (final ZNode zNode : nl) {
				if (Objects.equals(zNode.getK(), key)) {
					return (V) zNode.getV();
				}
			}

			return null;

		}
	}

	/**
	 * 按照key所在的分组放入新的数据，如果分组数据已满(达到numberOfGroup)则丢弃此组最旧的数据然后放入本次新增的
	 */
	@Override
	public V put(final K key, final V value) {

		final int n = this.hash(key);

		final String keyword = ZMap.lock(n);
		synchronized (keyword.intern()) {
			final LinkedList<ZNode> nl = this.data.get(n);
			if (nl.size() >= this.numberOfGroup) {
				nl.removeFirst();
				nl.add(new ZNode<>(key, value));
				return value;
			}

			final Iterator<ZNode> iterator = nl.iterator();
			while (iterator.hasNext()) {
				final ZNode zNode = iterator.next();
				if (Objects.equals(zNode.getK(), key)) {
					iterator.remove();
					break;
				}
			}

			nl.add(new ZNode<>(key, value));

			return value;
		}
	}

	private static String lock(final int n) {
		final String keyword = ("zmaplock-" + n);
		return keyword;
	}

	@Override
	public V remove(final Object key) {
		final int n = this.hash(key);
		final String keyword = ZMap.lock(n);
		synchronized (keyword.intern()) {

			final LinkedList<ZNode> nl = this.data.get(n);
			if (nl.isEmpty()) {
				return null;
			}

			final Iterator<ZNode> it = nl.iterator();
			while (it.hasNext()) {
				final ZNode zNode = it.next();
				if (Objects.equals(zNode.getK(), key)) {
					it.remove();
					return (V) zNode.getV();
				}
			}

			return null;
		}
	}

	@Override
	public void putAll(final Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException(ZMap.class.getSimpleName() + " 不支持 putAll " + "操作");
	}

	/**
	 * 清空所有数据
	 */
	@Override
	public synchronized void clear() {
		this.data.clear();
		this.initData();
	}

	@Override
	public synchronized Set<K> keySet() {

		if (this.data.isEmpty()) {
			return Collections.emptySet();
		}

		final Set<K> s = new HashSet<>();
		for (int i = 0; i < this.groups; i++) {
			final LinkedList<ZNode> nl = this.data.get(i);
			if (nl.isEmpty()) {
				continue;
			}
			for (final ZNode zNode : nl) {
				s.add((K) zNode.getK());
			}
		}

		return s;
	}

	@Override
	public Collection<V> values() {
		throw new UnsupportedOperationException(ZMap.class.getSimpleName() + " 不支持 values " + "操作");
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException(ZMap.class.getSimpleName() + " 不支持 entrySet " + "操作");
	}

	private int hash(final Object k) {
		if (k == null) {
			return 0;
		}

		final int hashCode = Math.abs(k.hashCode());
		return hashCode % this.groups;
	}


	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("[");
		for (int i = 0; i < this.groups; i++) {
			final LinkedList<ZNode> nl = this.data.get(i);
			if (nl == null || nl.isEmpty()) {
				continue;
			}
			builder.append(nl);
		}

		builder.append("]");
		return builder.toString();
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ZNode<K, V>{
		private K k;
		private V v;
		// FIXME 2023年11月26日 下午10:42:29 zhanghen: TODO 加个Data 新增时间和超时时间秒数，每次调用本类方法则自动执行过期操作
	}

}

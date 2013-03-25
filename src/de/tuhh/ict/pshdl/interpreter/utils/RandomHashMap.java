package de.tuhh.ict.pshdl.interpreter.utils;

import java.util.*;

public class RandomHashMap<K, V> extends HashMap<K, V> {

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return new RandomIterHashSet<>(super.entrySet());
	}

	@Override
	public Set<K> keySet() {
		return new RandomIterHashSet<>(super.keySet());
	}

	@Override
	public Set<V> values() {
		return new RandomIterHashSet<>(super.values());
	}
}

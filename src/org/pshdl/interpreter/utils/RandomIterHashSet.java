package org.pshdl.interpreter.utils;

import java.util.*;

public class RandomIterHashSet<E> extends HashSet<E> {

	public RandomIterHashSet() {
		super();
	}

	public RandomIterHashSet(Collection<E> c) {
		super(c);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -1327582812164498879L;

	/**
	 * Returns an iterator over the elements in this set. The elements are
	 * returned in no particular order.
	 * 
	 * @return an Iterator over the elements in this set
	 * @see ConcurrentModificationException
	 */
	@Override
	public Iterator<E> iterator() {
		List<E> temp = new ArrayList<>(super.size());
		Iterator<E> iterator = super.iterator();
		while (iterator.hasNext()) {
			E s = iterator.next();
			temp.add(s);
		}
		Collections.shuffle(temp);
		return temp.iterator();
	}

}

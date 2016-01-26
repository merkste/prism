package common.iterable.collections;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.PrimitiveIterator.OfInt;
import java.util.Set;

import common.iterable.IterableBitSet;
import common.iterable.IterableInt;

/**
 * Wrapper to connect the Set-interface to instances of the class BitSet.
 * @author Marcus Daum (mdaum@tcs.inf.tu-dresden.de)
 */
public class BitSetWrapper implements Set<Integer>, IterableInt
{
	private BitSet bitSet;

	public BitSetWrapper(final BitSet set)
	{
		bitSet = set;
	}

	@Override
	public OfInt iterator()
	{
		return IterableBitSet.getSetBits(bitSet).iterator();
	}

	@Override
	public int size()
	{
		return bitSet.cardinality();
	}

	@Override
	public boolean isEmpty()
	{
		return bitSet.isEmpty();
	}

	@Override
	public boolean contains(final Object o)
	{
		return (o instanceof Integer) && bitSet.get((Integer) o);
	}

	@Override
	public Object[] toArray()
	{
		final Integer[] objects = new Integer[size()];
		final Iterator<Integer> it = iterator();
		for (int index = 0; index < objects.length; index++)
			objects[index] = it.next();
		return objects;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(final T[] a)
	{
		final T[] result;
		final int size = size();
		if (a.length >= size) {
			result = a;
		} else {
			result = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size());
		}

		final Iterator<Integer> it = iterator();
		for (int index = 0; index < size; index++) {
			result[index] = (T) (it.hasNext() ? it.next() : null);
		}
		return result;
	}

	@Override
	public boolean add(final Integer i)
	{
		return add((int) i);
	}

	public boolean add(final int i)
	{
		if (bitSet.get(i)) {
			return false;
		}
		bitSet.set(i);
		return true;
	}

	@Override
	public boolean remove(final Object o)
	{
		if (!(o instanceof Integer)) {
			return false;
		}
		final int i = (Integer) o;
		if (bitSet.get(i)) {
			bitSet.clear(i);
			return true;
		}
		return false;
	}

	@Override
	public boolean containsAll(final Collection<?> c)
	{
		for (Iterator<?> it = c.iterator(); it.hasNext();) {
			if (!contains(it.next())) {
				return false;
			}
		}
		return true;
	}

	public boolean addAll(final BitSet set)
	{
		if (bitSet.equals(set)) {
			return false;
		}
		bitSet.or(set);
		return true;
	}

	@Override
	public boolean addAll(final Collection<? extends Integer> c)
	{
		if (c instanceof BitSetWrapper) {
			return addAll(((BitSetWrapper) c).bitSet);
		}
		boolean modified = false;
		for (int i : c) {
			modified |= add(i);
		}
		return modified;
	}

	public boolean removeAll(final BitSet set)
	{
		final int size = size();
		bitSet.andNot(set);
		return (size() != size);
	}

	@Override
	public boolean removeAll(final Collection<?> c)
	{
		if (c instanceof BitSetWrapper) {
			return removeAll(((BitSetWrapper) c).bitSet);
		}
		boolean modified = false;
		//first determine the smaller collection, and iterate over this one
		//this will save some time
		if (size() > c.size()) {
			//iterate over the parameter-collection
			for (Iterator<?> it = c.iterator(); it.hasNext();) {
				modified |= remove(it.next());
			}
		} else {
			//iterate over the current collection
			for (Iterator<?> it = iterator(); it.hasNext();) {
				if (c.contains(it.next())) {
					it.remove();
					modified = true;
				}
			}
		}
		return modified;
	}

	public boolean retainAll(final BitSet set)
	{
		final int size = size();
		bitSet.and(set);
		return (size() != size);
	}

	@Override
	public boolean retainAll(final Collection<?> c)
	{
		if (c instanceof BitSetWrapper) {
			return retainAll(((BitSetWrapper) c).bitSet);
		}
		boolean modified = false;
		for (Iterator<Integer> it = iterator(); it.hasNext();) {
			if (!c.contains(it.next())) {
				it.remove();
				modified = true;
			}
		}
		return modified;
	}

	@Override
	public void clear()
	{
		bitSet.clear();
	}

	@Override
	public String toString()
	{
		return bitSet.toString();
	}

	@Override
	public boolean equals(final Object o)
	{
		return (o instanceof BitSetWrapper) && bitSet.equals(((BitSetWrapper) o).bitSet);
	}

	@Override
	public int hashCode()
	{
		return bitSet.hashCode();
	}

	/**
	 * Simple test method.
	 * 
	 * @param args ignored
	 */
	public static void main(String[] args)
	{
		BitSet test = new BitSet();
		test.set(1);
		test.set(2);
		test.set(3);
		test.set(5);
		test.set(8);
		test.set(13);
		test.set(21);
	
		System.out.println("\n" + test + " - set bits:");
		for (Integer index : new BitSetWrapper(test)) {
			System.out.println(index);
		}
	}

	@Override
	public BitSetWrapper clone()
	{
		return new BitSetWrapper((BitSet) bitSet.clone());
	}
}
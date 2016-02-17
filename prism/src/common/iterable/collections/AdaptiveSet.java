package common.iterable.collections;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * A set that chooses dynamically an appropriate class to store its data.
 * If there is a need to store more elements then the threshold specified by <code>elementsInSet</code>, it makes use of <code>BitSetWrapper</code>.
 * @author Marcus Daum (mdaum@tcs.inf.tu-dresden.de)
 */
public class AdaptiveSet implements Set<Integer>
{
	private static final int OPTIMAL_THRESHOLD = 10;

	private BitSetWrapper bitSet = null;
	private Set<Integer> set = null;
	private final int elementsInSet;

	public AdaptiveSet()
	{
		this(OPTIMAL_THRESHOLD);
	}

	public AdaptiveSet(final int theElementsInSet)
	{
		elementsInSet = theElementsInSet;
		set = new SortedFixedCapacityIntegerSet(elementsInSet);
	}

	public AdaptiveSet(final BitSet aBitSet)
	{
		this(OPTIMAL_THRESHOLD, aBitSet);
	}

	public AdaptiveSet(final int theElementsInSet, final BitSet aBitSet)
	{
		this(theElementsInSet, (aBitSet == null) ? null : new BitSetWrapper(aBitSet));
	}

	public AdaptiveSet(final BitSetWrapper aBitSetWrapper)
	{
		this(OPTIMAL_THRESHOLD, aBitSetWrapper);
	}

	public AdaptiveSet(final int theElementsInSet, final BitSetWrapper aBitSetWrapper)
	{
		if (aBitSetWrapper == null) {
			throw new NullPointerException("The argument for the constructor must not be null.");
		}
		elementsInSet = theElementsInSet;
		if (aBitSetWrapper.size() > elementsInSet) {
			bitSet = aBitSetWrapper;
			return;
		}
		set = new SortedFixedCapacityIntegerSet(elementsInSet);
		set.addAll(aBitSetWrapper);
	}

	public AdaptiveSet(final Set<Integer> aSet)
	{
		this(OPTIMAL_THRESHOLD, aSet);
	}

	public AdaptiveSet(final int theElementsInSet, final Set<Integer> aSet)
	{
		if (aSet == null) {
			throw new NullPointerException("The argument for the constructor must not be null.");
		}
		elementsInSet = theElementsInSet;
		if (aSet.size() > elementsInSet) {
			bitSet = new BitSetWrapper(new BitSet());
			bitSet.addAll(aSet);
			return;
		}
		set = aSet;
	}

	@Override
	public int size()
	{
		return (set == null) ? bitSet.size() : set.size();
	}

	@Override
	public boolean isEmpty()
	{
		return (set == null) ? bitSet.isEmpty() : set.isEmpty();
	}

	@Override
	public boolean contains(final Object o)
	{
		return (set == null) ? bitSet.contains(o) : set.contains(o);
	}

	@Override
	public Iterator<Integer> iterator()
	{
		return (set == null) ? bitSet.iterator() : set.iterator();
	}

	@Override
	public Object[] toArray()
	{
		return (set == null) ? bitSet.toArray() : set.toArray();
	}

	@Override
	public <T> T[] toArray(final T[] a)
	{
		return (set == null) ? bitSet.toArray(a) : set.toArray(a);
	}

	private void convertSetToBitSet()
	{
		bitSet = new BitSetWrapper(new BitSet());
		bitSet.addAll(set);
		set = null;
	}

	@Override
	public boolean add(final Integer i)
	{
		if (bitSet != null) {
			return bitSet.add(i);
		}
		try {
			return set.add(i);
		} catch (IllegalArgumentException e){
			convertSetToBitSet();
			return bitSet.add(i);
		}
	}

	private void convertBitSetToSet()
	{
		assert (bitSet.size() <= elementsInSet) : "Call only when there are fewer elements in the BitSet then allowed.";
		set = new SortedFixedCapacityIntegerSet(elementsInSet);
		set.addAll(bitSet);
		bitSet = null;
	}

	@Override
	public boolean remove(final Object o)
	{
		if (!(o instanceof Integer)) {
			return false;
		}
		if (set != null) {
			return set.remove(o);
		}
		boolean modified = bitSet.remove(o);
		if (bitSet.size() <= elementsInSet) {
			convertBitSetToSet();
		}
		return modified;
	}

	@Override
	public boolean containsAll(final Collection<?> c)
	{
		return (set == null) ? bitSet.containsAll(c) : set.containsAll(c);
	}

	@Override
	public boolean addAll(final Collection<? extends Integer> c)
	{
		if (bitSet != null)
			return bitSet.addAll(c);
		try {
			return set.addAll(c);
		} catch (IllegalArgumentException e){
			convertSetToBitSet();
			return bitSet.addAll(c);
		}
	}

	@Override
	public boolean retainAll(final Collection<?> c)
	{
		if (set != null) {
			return set.retainAll(c);
		}
		boolean modified = bitSet.retainAll(c);
		if (bitSet.size() < elementsInSet) {
			convertBitSetToSet();
		}
		return modified;
	}

	@Override
	public boolean removeAll(final Collection<?> c)
	{
		if (set != null) {
			return set.removeAll(c);
		}
		boolean modified = bitSet.removeAll(c);
		if (bitSet.size() < elementsInSet) {
			convertBitSetToSet();
		}
		return modified;
	}

	@Override
	public void clear()
	{
		set = new SortedFixedCapacityIntegerSet(elementsInSet);
		bitSet = null;
	}

	@Override
	public String toString()
	{
		return (set == null) ? bitSet.toString() : set.toString();
	}

	@Override
	public boolean equals(final Object o)
	{
		if (! (o instanceof AdaptiveSet)) {
			return false;
		}
		return (set == null) ? bitSet.equals(((AdaptiveSet) o).bitSet) : set.equals(((AdaptiveSet) o).set);
	}

	@Override
	public int hashCode()
	{
		return (set == null) ? bitSet.hashCode() : set.hashCode();
	}
}
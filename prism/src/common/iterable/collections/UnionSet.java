package common.iterable.collections;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

import common.IteratorTools;
import common.functions.AbstractPredicate;
import common.functions.Predicate;
import common.iterable.ChainedIterator;
import common.iterable.FilteringIterator;

public class UnionSet<T> extends AbstractSet<T>
{
	private final Set<T> set1;
	private final Set<T> set2;
	private final Predicate<T> notInSet1;
	private int size = -1;

	public UnionSet(final Set<T> set1, final Set<T> set2)
	{
		this.set1 = set1;
		this.set2 = set2;
		notInSet1 = new Excludes<T>(set1);
	}

	@Override
	public boolean contains(final Object element)
	{
		return set1.contains(element) || set2.contains(element);
	}

	@Override
	public Iterator<T> iterator()
	{
		return new ChainedIterator<T>(set1.iterator(), new FilteringIterator<T>(set2.iterator(), notInSet1));
	}

	@Override
	public int size()
	{
		if (size < 0) {
			size = IteratorTools.count(this);
		}
		return size;
	}

	private final class Excludes<E> extends AbstractPredicate<E>
	{
		private final Set<E> elements;

		public Excludes(final Set<E> elements)
		{
			this.elements = elements;
		}

		@Override
		public final boolean getBoolean(final E element)
		{
			return !elements.contains(element);
		}
	}
}
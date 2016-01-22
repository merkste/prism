package common.iterable.collections;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;


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
		notInSet1 = ((Predicate<T>) set1::contains).negate();
	}

	@Override
	public boolean contains(final Object element)
	{
		return set1.contains(element) || set2.contains(element);
	}

	@Override
	public Iterator<T> iterator()
	{
		return stream().iterator();
	}

	@Override
	public Stream<T> stream()
	{
		Stream<T> stream1 = set1.stream();
		Stream<T> stream2 = set2.stream().filter(notInSet1);
		return Stream.concat(stream1, stream2);
	}

	@Override
	public int size()
	{
		if (size < 0) {
			size = Math.toIntExact(stream().count());
		}
		return size;
	}
}
package common.iterable.collections;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import common.iterable.FunctionalIterator;
import common.iterable.IterableArray;


public class UnionSet<T> extends AbstractSet<T>
{
	protected final Set<T> set1;
	protected final Set<T> set2;
	protected final Predicate<T> notInSet1;
	protected int size = -1;



	@SafeVarargs
	public static <T> Set<T> of(Set<T> ... sets)
	{
		return new IterableArray.Of<>(sets).reduce(UnionSet<T>::new).orElse(Collections.emptySet());
	}

	public UnionSet(Set<T> set1, Set<T> set2)
	{
		Objects.requireNonNull(set1);
		Objects.requireNonNull(set2);
		this.set1 = set1;
		this.set2 = set2;
		notInSet1 = ((Predicate<T>) set1::contains).negate();
	}

	@Override
	public boolean contains(Object element)
	{
		return set1.contains(element) || set2.contains(element);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<T> iterator()
	{
		FunctionalIterator<T> iterable1 = FunctionalIterator.extend(set1);
		FunctionalIterator<T> iterable2 = FunctionalIterator.extend(set2).filter(notInSet1);
		return iterable1.chain(iterable2);
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
			int size1 = set1.size();
			int size2 = FunctionalIterator.extend(set2).filter(notInSet1).count();
			size = size1 + size2;
		}
		return size;
	}
}
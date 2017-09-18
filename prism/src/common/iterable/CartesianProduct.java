package common.iterable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;

import common.IteratorTools;

public class CartesianProduct
{
	private static final Function<Object, Object[]> AS_TUPLE = element -> new Object[] {element};



	@SafeVarargs
	public static FunctionalIterable<Object[]> of(final Iterable<?> ... iterables)
	{
		return of(new IterableArray.Of<>(iterables));
	}

	public static FunctionalIterable<Object[]> of(final Iterable<Iterable<?>> iterables)
	{
		return of(iterables.iterator());
	}

	public static FunctionalIterable<Object[]> of(final Iterator<Iterable<?>> iterables)
	{
		if (!iterables.hasNext()) {
			return EmptyIterable.Of();
		}
		FunctionalIterable<?> elements = FunctionalIterable.extend(iterables.next());
		if (!iterables.hasNext()) {
			return elements.map(asTuple());
		}
		// recurse
		FunctionalIterable<Object[]> tuples = CartesianProduct.of(iterables);
		// elements x tuples
		return elements.flatMap(element -> tuples.map((Function<Object[],Object[]>) tuple -> prepend(element, tuple)));
	}

	public static <T> T[] prepend(T element, T[] tuple)
	{
		@SuppressWarnings("unchecked")
		T[] newTupel = (T[]) new Object[tuple.length + 1];
		newTupel[0] = element;
		System.arraycopy(tuple, 0, newTupel, 1, tuple.length);
		return newTupel;
	}

	@SuppressWarnings("unchecked")
	public static <T> Function<T, Object[]> asTuple() {
		return (Function<T, Object[]>) AS_TUPLE;
	}

	public static void main(final String[] args)
	{
		// declare type of function explicitly to circumvent class format error due to faulty type inference
		Function<Object[], String> toString = Arrays::toString;

		Iterator<? extends Object[]> product = of().iterator();
		IteratorTools.printIterator("empty product  ", product);
		System.out.println();

		product = of(Arrays.asList(1, 2, 3)).iterator();
		IteratorTools.printIterator("single product ", new MappingIterator.From<>(product, toString));
		System.out.println();

		product = of(Arrays.asList(1, 2, 3), Arrays.asList('a', 'b')).iterator();
		IteratorTools.printIterator("binary product ", new MappingIterator.From<>(product, toString));
		System.out.println();

		product = of(Arrays.asList(1, 2, 3), Arrays.asList('a', 'b'), Arrays.asList(0.5)).iterator();
		IteratorTools.printIterator("ternary product", new MappingIterator.From<>(product, toString));
		System.out.println();
	}
}

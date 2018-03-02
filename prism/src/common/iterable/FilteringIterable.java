package common.iterable;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

import common.iterable.FunctionalPrimitiveIterable.IterableDouble;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.FunctionalPrimitiveIterable.IterableLong;

public abstract class FilteringIterable<E, I extends Iterable<E>> implements FunctionalIterable<E>
{
	protected final I iterable;

	public FilteringIterable(I iterable)
	{
		Objects.requireNonNull(iterable);
		this.iterable = iterable;
	}

	@SuppressWarnings("unchecked")
	public static <T> FunctionalIterable<T> dedupe(Iterable<T> iterable)
	{
		if (iterable instanceof IterableDouble) {
			return (FunctionalIterable<T>) dedupe((IterableDouble) iterable);
		}
		if (iterable instanceof IterableInt) {
			return (FunctionalIterable<T>) dedupe((IterableInt) iterable);
		}
		if (iterable instanceof IterableLong) {
			return (FunctionalIterable<T>) dedupe((IterableLong) iterable);
		}
		return new DedupedIterable.Of<>(iterable);
	}

	public static IterableDouble dedupe(IterableDouble iterable)
	{
		return new DedupedIterable.Of<>(iterable).mapToDouble(Double::doubleValue);
	}

	public static IterableInt dedupe(IterableInt iterable)
	{
		return new DedupedIterable.OfInt(iterable);
	}

	public static IterableLong dedupe(IterableLong iterable)
	{
		return new DedupedIterable.Of<>(iterable).mapToLong(Long::longValue);
	}

	@SuppressWarnings("unchecked")
	public static <T> Iterable<T> isNull(Iterable<T> iterable)
	{
		if (iterable instanceof PrimitiveIterator.OfDouble) {
			return (Iterable<T>) EmptyIterable.OfDouble();
		} else if (iterable instanceof PrimitiveIterator.OfInt) {
			return (Iterable<T>) EmptyIterable.OfInt();
		} else if (iterable instanceof PrimitiveIterator.OfLong) {
			return(Iterable<T>) EmptyIterable.OfLong();
		}
		return new FilteringIterable.Of<>(iterable, Objects::isNull);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> FunctionalIterable<T> nonNull(Iterable<T> iterable)
	{
		if (iterable instanceof FunctionalPrimitiveIterable) {
			return (FunctionalPrimitiveIterable) iterable;
		}
		return new FilteringIterable.Of<>(iterable, Objects::nonNull);
	}



	public static abstract class DedupedIterable<E, I extends FunctionalIterable<E>> implements FunctionalIterable<E>
	{
		protected I source;
		protected I deduped;
	
		@SuppressWarnings("unchecked")
		public DedupedIterable(Iterable<E> source)
		{
			this.source  = (I) FunctionalIterable.extend(source);
			this.deduped = null;
		}
	
	
	
		public static class Of<E> extends DedupedIterable<E, FunctionalIterable<E>>
		{
			public Of(Iterable<E> source)
			{
				super(source);
			}
	
			@Override
			public FunctionalIterator<E> iterator()
			{
				if (source == null) {
					return deduped.iterator();
				}
				Set<E> set                 = new HashSet<E>();
				FunctionalIterable<E> iter = source.filter(set::add);
				deduped                    = FunctionalIterable.extend(set);
				source                     = null;
				return iter.iterator();
			}
		}
	
	
	
		public static class OfInt extends DedupedIterable<Integer, IterableInt> implements IterableInt
		{
			public OfInt(IterableInt source)
			{
				super(source);
			}
	
			@Override
			public FunctionalPrimitiveIterator.OfInt iterator()
			{
				if (source == null) {
					deduped.iterator();
				}
				BitSet bits          = new BitSet();
				IntPredicate set     = (int i) -> {if (bits.get(i)) return false; else bits.set(i); return true;};
				IterableInt filtered = source.filter(set);
				deduped              = IterableBitSet.getSetBits(bits);
				source               = null;
				return FunctionalIterator.extend(filtered.iterator());
			}
		}
	}



	public static class Of<E> extends FilteringIterable<E, Iterable<E>>
	{
		protected final Predicate<? super E> predicate;

		public Of(Iterable<E> iterable, Predicate<? super E> predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			return new FilteringIterator.Of<>(iterable.iterator(), predicate);
		}
	}



	public static class OfDouble extends FilteringIterable<Double, IterableDouble> implements IterableDouble
	{
		protected final DoublePredicate predicate;
	
		public OfDouble(IterableDouble iterable, DoublePredicate predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}
	
		@Override
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return new FilteringIterator.OfDouble(iterable.iterator(), predicate);
		}
	}



	public static class OfInt extends FilteringIterable<Integer, IterableInt> implements IterableInt
	{
		protected final IntPredicate predicate;

		public OfInt(IterableInt iterable, IntPredicate predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return new FilteringIterator.OfInt(iterable.iterator(), predicate);
		}
	}



	public static class OfLong extends FilteringIterable<Long, IterableLong> implements IterableLong
	{
		protected final LongPredicate predicate;

		public OfLong(IterableLong iterable, LongPredicate predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}

		@Override
		public FunctionalPrimitiveIterator.OfLong iterator()
		{
			return new FilteringIterator.OfLong(iterable.iterator(), predicate);
		}
	}
}

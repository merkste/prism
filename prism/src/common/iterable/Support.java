package common.iterable;

import java.util.BitSet;
import java.util.PrimitiveIterator.OfInt;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import common.BitSetTools;
import common.IteratorTools;
import common.functions.Relation;
import common.functions.primitive.PredicateDouble;
import common.functions.primitive.PredicateInt;

// FIXME ALG: consider using e.g. Support(values) is equal to
// support = Relation.LEQ.curry(0)
//				.and(Relation.GEQ.curry(values.length))
//				.and(Relation.GEQ.curry(0).compose(values::get)); 
// new FilteredIterable(values, support)
public class Support implements PredicateInt, IterableInt
{
	private final double[] values;
	private final DoublePredicate predicate;

	// FIXME ALG: check common mathematical definition/terminology
	public Support(final double[] values)
	{
		this(values, 0);
	}

	public Support(final double[] values, final double threshold)
	{
		this(values, Relation.GT, threshold);
	}

	public Support(final double[] values, final Relation relation, final double threshold)
	{
		this(values, relation.inverse().curry(threshold));
	}

	public Support(final double[] values, final PredicateDouble predicate)
	{
		this(values, (DoublePredicate) predicate::test);
	}

	public Support(final double[] values, final DoublePredicate predicate)
	{
		this.values = values;
		this.predicate = predicate;
	}

	@Override
	public final boolean test(final int index)
	{
		return 0 <= index && index < values.length && predicate.test(values[index]);
	}

	public BitSet asBitSet()
	{
		return BitSetTools.asBitSet(this);
	}

	@Override
	public OfInt iterator()
	{
		return new FilteringIterator.OfInt(new Interval(0, values.length), this);
	}

	public IntStream stream()
	{
		return new Interval(0, values.length).stream().filter(this);
	}

	public static void main(final String[] args)
	{
		final Support support = new Support(new double[] { 0.98, 0.8, 1.0, 0.0 }, Relation.GEQ, 1.0);
		IteratorTools.printIterator("support", support.iterator());

		final Support support1 = new Support(new double[] { 1, 0, 1 });
		final Support support2 = new Support(new double[] { 0, 1, 1 });
		System.out.println(support1.and((IntPredicate) support2).test(0));
		System.out.println(support1.and((IntPredicate) support2).test(1));
		System.out.println(support1.and((IntPredicate) support2).test(2));
	}
}
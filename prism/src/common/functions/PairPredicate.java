package common.functions;

import java.util.function.BiPredicate;

public interface PairPredicate<R, S> extends PairMapping<R, S, Boolean>, BiPredicate<R, S>
{
	public boolean test(R element1, S element2);

	@Override
	public Predicate<S> curry(R element1);

	@Override
	public PairPredicate<R, S> negate();

	@Override
	public PairPredicate<R, S> and(BiPredicate<? super R, ? super S> predicate);

	@Override
	public PairPredicate<R, S> or(BiPredicate<? super R, ? super S> predicate);

	public PairPredicate<R, S> implies(BiPredicate<? super R, ? super S> predicate);

	public PairPredicate<S, R> inverse();
}
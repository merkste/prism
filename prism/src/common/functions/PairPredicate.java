package common.functions;

import java.util.function.BiPredicate;

public interface PairPredicate<R, S> extends PairMapping<R, S, Boolean>, BiPredicate<R, S>
{
	public boolean test(R element1, S element2);

	@Override
	public Predicate<S> curry(R element1);

	public PairPredicate<R, S> not();

	public PairPredicate<R, S> and(PairPredicate<? super R, ? super S> predicate);

	public PairPredicate<R, S> or(PairPredicate<? super R, ? super S> predicate);

	public PairPredicate<R, S> implies(PairPredicate<? super R, ? super S> predicate);

	public PairPredicate<S, R> inverse();
}
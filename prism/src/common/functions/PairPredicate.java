package common.functions;

public interface PairPredicate<R, S> extends PairMapping<R, S, Boolean>
{
	public boolean getBoolean(final R element1, final S element2);

	@Override
	public Predicate<S> curry(final R element1);

	public PairPredicate<R, S> not();

	public PairPredicate<R, S> and(final PairPredicate<? super R, ? super S> predicate);

	public PairPredicate<R, S> or(final PairPredicate<? super R, ? super S> predicate);

	public PairPredicate<R, S> implies(final PairPredicate<? super R, ? super S> predicate);

	public PairPredicate<S, R> inverse();
}
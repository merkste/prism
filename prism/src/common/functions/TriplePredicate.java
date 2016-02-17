package common.functions;

public interface TriplePredicate<Q, R, S> extends TripleMapping<Q, R, S, Boolean>
{
	public boolean getBoolean(final Q element1, final R element2, final S element3);

	@Override
	public PairPredicate<R, S> curry(final Q element1);

	@Override
	public Predicate<S> curry(final Q element1, final R element2);

	public TriplePredicate<Q, R, S> not();

	public TriplePredicate<Q, R, S> and(final TriplePredicate<? super Q, ? super R, ? super S> predicate);

	public TriplePredicate<Q, R, S> or(final TriplePredicate<? super Q, ? super R, ? super S> predicate);

	public TriplePredicate<Q, R, S> implies(final TriplePredicate<? super Q, ? super R, ? super S> predicate);
}
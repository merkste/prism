package common.functions;

public interface Predicate<T> extends Mapping<T, Boolean>
{
	public boolean getBoolean(final T element);

	public Predicate<T> not();

	public Predicate<T> and(final Predicate<? super T> predicate);

	public Predicate<T> or(final Predicate<? super T> predicate);

	public Predicate<T> implies(final Predicate<? super T> predicate);

	@Override
	public <S> Predicate<S> compose(final Mapping<S, ? extends T> mapping);
}
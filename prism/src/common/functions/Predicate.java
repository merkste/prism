package common.functions;

public interface Predicate<T> extends Mapping<T, Boolean>, java.util.function.Predicate<T>
{
	public boolean test(T element);

	@Override
	public Predicate<T> negate();

	@Override
	public Predicate<T> and(java.util.function.Predicate<? super T> predicate);

	@Override
	public Predicate<T> or(java.util.function.Predicate<? super T> predicate);

	public Predicate<T> implies(java.util.function.Predicate<? super T> predicate);

	@Override
	public <S> Predicate<S> compose(Mapping<S, ? extends T> mapping);
}
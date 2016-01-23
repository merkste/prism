package common.functions;

public interface Predicate<T> extends Mapping<T, Boolean>
{
	public boolean test(T element);

	public Predicate<T> not();

	public Predicate<T> and(Predicate<? super T> predicate);

	public Predicate<T> or(Predicate<? super T> predicate);

	public Predicate<T> implies(Predicate<? super T> predicate);

	@Override
	public <S> Predicate<S> compose(Mapping<S, ? extends T> mapping);
}
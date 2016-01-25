package common.functions;

public abstract class AbstractPredicate<T> extends AbstractMapping<T, Boolean>implements Predicate<T>
{
	public static final <T> Predicate<T> True()
	{
		return Predicate.True();
	}

	public static final <T> Predicate<T> False()
	{
		return Predicate.False();
	}
}
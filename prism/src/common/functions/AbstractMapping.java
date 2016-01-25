package common.functions;

public abstract class AbstractMapping<S, T> implements Mapping<S, T>
{
	public static <S, T> Mapping<S, T> constant(final T value)
	{
		return Mapping.constant(value);
	}

	public static <T> Mapping<T, T> identity()
	{
		return Mapping.identity();
	}
}
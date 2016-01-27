package common.functions;

import java.util.Objects;
import java.util.function.Function;

public abstract class MemoizedMapping<S, T> implements Mapping<S, T>
{
	protected final Function<S, T> function;

	public MemoizedMapping(Function<S, T> function)
	{
		Objects.requireNonNull(function);
		this.function = function;
	}

	@Override
	public T apply(S element)
	{
		T value = lookup(element);
		if (value == null) {
			value = store(element, function.apply(element));
		}
		return value;
	}

	@Override
	public Mapping<S, T> memoize()
	{
		return this;
	}

	protected abstract T lookup(S element);

	protected abstract T store(S element, T value);
}
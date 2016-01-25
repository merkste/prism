package common.functions;

public abstract class MemoizedMapping<S, T> implements Mapping<S, T>
{
	final Mapping<S, T> mapping;

	public MemoizedMapping(final Mapping<S, T> mapping)
	{
		this.mapping = mapping;
	}

	@Override
	public T apply(final S element)
	{
		T value = lookup(element);
		if (value == null) {
			value = store(element, mapping.apply(element));
		}
		return value;
	}

	@Override
	public Mapping<S, T> memoize()
	{
		return this;
	}

	protected abstract T lookup(final S element);

	protected abstract T store(final S element, T value);
}
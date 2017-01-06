package jdd;

public interface Clearable extends AutoCloseable
{
	public void clear();

	default void close()
	{
		clear();
	}
}

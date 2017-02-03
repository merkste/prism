package common.iterable;

import java.util.Objects;
import java.util.function.Function;

public interface ObjIntFunction<T, R>
{
	R apply(T t, int i);

	default <V> ObjIntFunction<T, V> andThen(Function<? super R, ? extends V> after) {
		Objects.requireNonNull(after);
		return (T t, int i) -> after.apply(apply(t, i));
	}
}

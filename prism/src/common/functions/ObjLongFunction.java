package common.functions;

import java.util.Objects;
import java.util.function.Function;

public interface ObjLongFunction<T, R>
{
	R apply(T t, long i);

	default <V> ObjLongFunction<T, V> andThen(Function<? super R, ? extends V> after) {
		Objects.requireNonNull(after);
		return (T t, long i) -> after.apply(apply(t, i));
	}
}

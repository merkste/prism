package common.iterable;

import java.util.Objects;
import java.util.function.Function;

public interface ObjDoubleFunction<T, R>
{
	R apply(T t, double i);

	default <V> ObjDoubleFunction<T, V> andThen(Function<? super R, ? extends V> after) {
		Objects.requireNonNull(after);
		return (T t, double i) -> after.apply(apply(t, i));
	}
}

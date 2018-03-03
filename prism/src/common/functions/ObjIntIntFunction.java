package common.functions;

import java.util.Objects;
import java.util.function.Function;

public interface ObjIntIntFunction<T, R>
{
	R apply(T t, int i, int j);

	default <V> ObjIntIntFunction<T, V> andThen(Function<? super R, ? extends V> after) {
		Objects.requireNonNull(after);
		return (T t, int i, int j) -> after.apply(apply(t, i, j));
	}
}

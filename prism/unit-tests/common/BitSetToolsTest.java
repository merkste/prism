package common;

import common.iterable.FunctionalIterable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.BitSet;
import java.util.stream.Stream;

import static common.iterable.Assertions.assertIterableEquals;

public class BitSetToolsTest
{
    public static Stream<int[]> getIndices()
    {
        return Stream.of(new int[0], new int[] {1, 2, 3, Integer.MAX_VALUE});
    }
    @ParameterizedTest
    @MethodSource("getIndices")
    public void testAsBitSetIntArray(int[] indices)
    {
        BitSet set = BitSetTools.asBitSet(indices);
        assertIterableEquals(new IterableBitSet(set), FunctionalIterable.ofInt(indices));
    }

    @ParameterizedTest
    public void testAsBitSetIterator(int[] indices)
    {

    }

    @ParameterizedTest
    public void testAsBitSetStream(int[] indices)
    {

    }
}

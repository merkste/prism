package common;

import common.iterable.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.BitSet;
import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static common.iterable.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class BitSetToolsTest
{
    public static Stream<int[]> getIndices()
    {
        return Stream.of(new int[0], new int[] {0, 1, 2, 3, 5, 8, Integer.MAX_VALUE});
    }
    @ParameterizedTest
    @MethodSource("getIndices")
    public void testAsBitSetIntArray(int[] indices)
    {
        BitSet set = BitSetTools.asBitSet(indices);
        assertIterableEquals(new IterableBitSet(set), FunctionalIterable.ofInt(indices));
    }

    @ParameterizedTest
    @MethodSource("getIndices")
    public void testAsBitSetIterator(int[] indices)
    {
        FunctionalPrimitiveIterator.OfInt iterator = FunctionalIterator.ofInt(indices);
        BitSet set = BitSetTools.asBitSet(iterator.map((int i) -> Integer.valueOf(i)));
        assertIterableEquals(new IterableBitSet(set), FunctionalIterable.ofInt(indices));
    }

    @ParameterizedTest
    @MethodSource("getIndices")
    public void testAsBitSetOfInt(int[] indices)
    {
        BitSet set = BitSetTools.asBitSet((Iterator<Integer>) FunctionalIterator.ofInt(indices));
        assertIterableEquals(new IterableBitSet(set), FunctionalIterable.ofInt(indices));
    }

    @ParameterizedTest
    @MethodSource("getIndices")
    public void testAsBitSetStream(int[] indices)
    {
        BitSet set = BitSetTools.asBitSet(IntStream.of(indices));
        assertIterableEquals(new IterableBitSet(set), FunctionalIterable.ofInt(indices));
    }

    @ParameterizedTest
    @MethodSource("getIndices")
    public void testShiftDown(int[] indices)
    {
        // use a separate Iterable to generated the expected set
        FunctionalPrimitiveIterable.OfInt iterable = FunctionalIterable.ofInt(indices);
        BitSet set = BitSetTools.asBitSet(indices);
        for (int j=0; j<3; j++) {
            int offset = j;
            BitSet expected = iterable.mapToInt((int i) -> i - offset).filter((int i) -> i >= 0).collect(new BitSet());
            BitSet actual = BitSetTools.shiftDown(set, offset);
            assertNotSame(set, actual);
            assertEquals(expected, actual);
        }
    }

    @ParameterizedTest
    @MethodSource("getIndices")
    public void testShiftUp(int[] indices)
    {
        // use a separate Iterable to generated the expected set
        FunctionalPrimitiveIterable.OfInt iterable = FunctionalIterable.ofInt(indices);
        BitSet set = BitSetTools.asBitSet(indices);
        for (int j=0; j<3; j++) {
            int offset = j;
            BitSet expected = iterable.mapToInt((int i) -> i + offset).filter((int i) -> i >= 0).collect(new BitSet());
            BitSet actual = BitSetTools.shiftUp(set, offset);
            assertNotSame(set, actual);
            assertEquals(expected, actual);
        }
    }

    @ParameterizedTest
    @MethodSource("getIndices")
    public void testComplementTo(int[] indices)
    {
        BitSet set = BitSetTools.asBitSet(indices);
        int length = indices.length;
        int toIndex = 5;
        IterableBitSet zeros = IterableBitSet.getClearBits(set, toIndex - 1);
        FunctionalPrimitiveIterable.OfInt ones = new IterableBitSet(set).filter((int i) -> i >= toIndex);
        BitSet expected = zeros.concat(ones).collect(new BitSet());
        BitSet actual = BitSetTools.complement(set, toIndex);
        assertNotSame(set, actual);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("getIndices")
    public void testComplementFromTo(int[] indices)
    {
        BitSet set = BitSetTools.asBitSet(indices);
        int length = indices.length;
        int toIndex = 5;
        IterableBitSet zeros = IterableBitSet.getClearBits(set, toIndex - 1);
        FunctionalPrimitiveIterable.OfInt ones = new IterableBitSet(set).filter((int i) -> i >= toIndex);
        BitSet expected = zeros.concat(ones).collect(new BitSet());
        BitSet actual = BitSetTools.complement(set, toIndex);
        assertNotSame(set, actual);
        assertEquals(expected, actual);
    }

    public BitSet complement(BitSet set, int fromIndex, int toIndex)
    {

    }
}

package common.methods;

import java.util.BitSet;

import common.functions.Mapping;
import common.functions.PairMapping;
import common.functions.primitive.MappingFromInteger;

/**
 * @deprecated
 * Use J8 Functions instead.
 */
@Deprecated
public class CallBitSet
{
	private static final NextSetBit NEXT_SET_BIT = new NextSetBit();

	public static NextSetBit nextSetBit()
	{
		return NEXT_SET_BIT;
	}

	public static Mapping<BitSet, Integer> nextSetBit(final int fromIndex)
	{
		return new Mapping<BitSet, Integer>()
		{
			@Override
			public Integer apply(final BitSet indices)
			{
				return indices.nextSetBit(fromIndex);
			}
		};
	}

	public static final class NextSetBit implements PairMapping<BitSet, Integer, Integer>, UnaryMethod<BitSet, Integer, Integer>
	{
		@Override
		public MappingFromInteger<Integer> curry(final BitSet indices)
		{
			return new MappingFromInteger<Integer>()
			{
				@Override
				public Integer apply(final int fromIndex)
				{
					return indices.nextSetBit(fromIndex);
				}
			};
		}

		@Override
		public Integer apply(final BitSet indices, final Integer fromIndex)
		{
			return indices.nextSetBit(fromIndex);
		}
	}
}
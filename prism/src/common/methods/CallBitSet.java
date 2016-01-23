package common.methods;

import java.util.BitSet;

import common.functions.AbstractMapping;
import common.functions.AbstractPairMapping;
import common.functions.Mapping;
import common.functions.primitive.AbstractMappingFromInteger;
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
		return new AbstractMapping<BitSet, Integer>()
		{
			@Override
			public Integer apply(final BitSet indices)
			{
				return indices.nextSetBit(fromIndex);
			}
		};
	}

	public static final class NextSetBit extends AbstractPairMapping<BitSet, Integer, Integer>implements UnaryMethod<BitSet, Integer, Integer>
	{
		@Override
		public MappingFromInteger<Integer> curry(final BitSet indices)
		{
			return new AbstractMappingFromInteger<Integer>()
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
package explicit.modelviews.methods;

import java.util.Map.Entry;

import common.functions.PairMapping;
import common.functions.primitive.MappingFromInteger;
import common.methods.UnaryMethod;
import explicit.modelviews.DTMCRestricted;

/**
 * @deprecated
 * Use J8 Functions instead.
 */
public class CallDTMCRestricted
{
	private static final MapStateToOriginalModel MAP_STATE_TO_ORIGINAL_MODEL = new MapStateToOriginalModel();
	private static final MapStateToRestrictedModel MAP_STATE_TO_RESTRICTED_MODEL = new MapStateToRestrictedModel();
	private static final MapTransitionToRestrictedModel MAP_TRANSITION_TO_RESTRICTED_MODEL = new MapTransitionToRestrictedModel();

	public static MapStateToOriginalModel mapStateToOriginalModel()
	{
		return MAP_STATE_TO_ORIGINAL_MODEL;
	}

	public static MapStateToRestrictedModel mapStateToRestrictedModel()
	{
		return MAP_STATE_TO_RESTRICTED_MODEL;
	}

	public static MapTransitionToRestrictedModel mapTransitionToRestrictedModel()
	{
		return MAP_TRANSITION_TO_RESTRICTED_MODEL;
	}

	public static final class MapStateToOriginalModel
			implements PairMapping<DTMCRestricted, Integer, Integer>, UnaryMethod<DTMCRestricted, Integer, Integer>
	{
		@Override
		public MappingFromInteger<Integer> curry(final DTMCRestricted model)
		{
			return new MappingFromInteger<Integer>()
			{
				@Override
				public Integer apply(final int state)
				{
					return model.mapStateToOriginalModel(state);
				}
			};
		}

		@Override
		public Integer apply(final DTMCRestricted model, final Integer state)
		{
			return model.mapStateToOriginalModel(state);
		}
	}

	public static final class MapStateToRestrictedModel
			implements PairMapping<DTMCRestricted, Integer, Integer>, UnaryMethod<DTMCRestricted, Integer, Integer>
	{
		@Override
		public MappingFromInteger<Integer> curry(final DTMCRestricted model)
		{
			return new MappingFromInteger<Integer>()
			{
				@Override
				public Integer apply(final int state)
				{
					return model.mapStateToRestrictedModel(state);
				}
			};
		}

		@Override
		public Integer apply(final DTMCRestricted model, final Integer state)
		{
			return model.mapStateToRestrictedModel(state);
		}
	}

	public static final class MapTransitionToRestrictedModel
			implements PairMapping<DTMCRestricted, Entry<Integer, Double>, Entry<Integer, Double>>,
			UnaryMethod<DTMCRestricted, Entry<Integer, Double>, Entry<Integer, Double>>
	{
		@Override
		public Entry<Integer, Double> apply(final DTMCRestricted model, final Entry<Integer, Double> transition)
		{
			return model.mapTransitionToRestrictedModel(transition);
		}
	}
}
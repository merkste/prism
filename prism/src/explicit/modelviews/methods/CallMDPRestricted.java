package explicit.modelviews.methods;

import java.util.Map.Entry;

import common.functions.AbstractPairMapping;
import common.functions.primitive.AbstractMappingFromInteger;
import common.functions.primitive.MappingFromInteger;
import common.methods.UnaryMethod;
import explicit.modelviews.MDPRestricted;

/**
 * @deprecated
 * Use J8 Functions instead.
 */
public class CallMDPRestricted
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
			extends AbstractPairMapping<MDPRestricted, Integer, Integer>
			implements UnaryMethod<MDPRestricted, Integer, Integer>
	{
		@Override
		public MappingFromInteger<Integer> curry(final MDPRestricted model)
		{
			return new AbstractMappingFromInteger<Integer>()
			{
				@Override
				public Integer get(final int state)
				{
					return model.mapStateToOriginalModel(state);
				}
			};
		}

		@Override
		public Integer get(final MDPRestricted model, final Integer state)
		{
			return model.mapStateToOriginalModel(state);
		}
	}

	public static final class MapStateToRestrictedModel
			extends AbstractPairMapping<MDPRestricted, Integer, Integer>
			implements UnaryMethod<MDPRestricted, Integer, Integer>
	{
		@Override
		public MappingFromInteger<Integer> curry(final MDPRestricted model)
		{
			return new AbstractMappingFromInteger<Integer>()
			{
				@Override
				public Integer get(final int state)
				{
					return model.mapStateToRestrictedModel(state);
				}
			};
		}

		@Override
		public Integer get(final MDPRestricted model, final Integer state)
		{
			return model.mapStateToRestrictedModel(state);
		}
	}

	public static final class MapTransitionToRestrictedModel
			extends AbstractPairMapping<MDPRestricted, Entry<Integer, Double>, Entry<Integer, Double>>
			implements UnaryMethod<MDPRestricted, Entry<Integer, Double>, Entry<Integer, Double>>
	{
		@Override
		public Entry<Integer, Double> get(final MDPRestricted model, final Entry<Integer, Double> transition)
		{
			return model.mapTransitionToRestrictedModel(transition);
		}
	}
}
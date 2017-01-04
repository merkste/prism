package explicit.conditional.prototype;

import java.util.BitSet;
import java.util.function.IntFunction;

import explicit.BasicModelTransformation;
import explicit.Model;
import explicit.ModelTransformation;

@Deprecated
public class ConditionalReachabilitiyTransformation<OM extends Model, TM extends Model> extends BasicModelTransformation<OM, TM> implements ReachabilityTransformation<OM, TM>
{
	protected final BitSet goalStates;

	public ConditionalReachabilitiyTransformation(final OM originalModel, final TM transformedModel, IntFunction<Integer> mapping, final BitSet goalStates, final BitSet transformedStatesOfInterest)
	{
		super(originalModel, transformedModel, transformedStatesOfInterest, mapping);
		this.goalStates = goalStates;
	}

	@Deprecated
	public ConditionalReachabilitiyTransformation(final OM originalModel, final TM transformedModel, final Integer[] mapping, final BitSet goalStates, final BitSet transformedStatesOfInterest)
	{
		super(originalModel, transformedModel, transformedStatesOfInterest, mapping);
		this.goalStates = goalStates;
	}

	public ConditionalReachabilitiyTransformation(final ConditionalReachabilitiyTransformation<OM, TM> transformation)
	{
		super(transformation);
		this.goalStates = transformation.goalStates;
	}

	public ConditionalReachabilitiyTransformation(final ModelTransformation<? extends OM, ? extends TM> transformation, final BitSet goalStates)
	{
		super(transformation);
		this.goalStates = goalStates;
	}

	@Override
	public BitSet getGoalStates()
	{
		return goalStates;
	}
}
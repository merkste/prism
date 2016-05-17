package explicit.conditional.transformer.mdp;

import java.util.BitSet;

import explicit.BasicModelTransformation;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.conditional.transformer.ReachabilityTransformation;

//FIXME ALG: add comment
public class ConditionalReachabilitiyTransformation<OM extends Model, TM extends Model> extends BasicModelTransformation<OM, TM> implements ReachabilityTransformation<OM, TM>
{
	protected final BitSet goalStates;
	protected final BitSet transformedStatesOfInterest;

	public ConditionalReachabilitiyTransformation(final OM originalModel, final TM transformedModel, final Integer[] mapping, final BitSet goalStates, final BitSet transformedStatesOfInterest)
	{
		super(originalModel, transformedModel, mapping);
		this.goalStates = goalStates;
		this.transformedStatesOfInterest = transformedStatesOfInterest;
	}

	public ConditionalReachabilitiyTransformation(final ConditionalReachabilitiyTransformation<OM, TM> transformation)
	{
		super(transformation);
		this.goalStates = transformation.goalStates;
		this.transformedStatesOfInterest = transformation.transformedStatesOfInterest;
	}

	public ConditionalReachabilitiyTransformation(final ModelTransformation<? extends OM, ? extends TM> transformation, final BitSet goalStates)
	{
		super(transformation);
		this.goalStates = goalStates;
		this.transformedStatesOfInterest = transformation.getTransformedStatesOfInterest();
	}

	@Override
	public BitSet getGoalStates()
	{
		return goalStates;
	}

	@Override
	public BitSet getTransformedStatesOfInterest()
	{
		return transformedStatesOfInterest;
	}
}
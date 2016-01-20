package explicit.conditional.transformer.mdp;

import java.util.BitSet;

import explicit.BasicModelTransformation;
import explicit.MDP;
import explicit.ModelTransformation;
import explicit.conditional.transformer.ReachabilityTransformation;

//FIXME ALG: add comment
public class ConditionalMDPTransformation extends BasicModelTransformation<MDP, MDP> implements ReachabilityTransformation<MDP, MDP>
{
	private final BitSet goalStates;
	private final BitSet transformedStatesOfInterest;

	public ConditionalMDPTransformation(final MDP originalModel, final MDP transformedModel, final Integer[] mapping, final BitSet goalStates, final BitSet transformedStatesOfInterest)
	{
		super(originalModel, transformedModel, mapping);
		this.goalStates = goalStates;
		this.transformedStatesOfInterest = transformedStatesOfInterest;
	}

	public ConditionalMDPTransformation(final ConditionalMDPTransformation transformation)
	{
		super(transformation);
		this.goalStates = transformation.goalStates;
		this.transformedStatesOfInterest = transformation.transformedStatesOfInterest;
	}

	public ConditionalMDPTransformation(final ModelTransformation<MDP, MDP> transformation, final BitSet goalStates)
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